# 业务流程 API 调用序列

完整的端到端 API 调用序列，每个 Step 包含 curl 命令、期望响应和验证查询。

## 测试变量

```bash
# 测试手机号（前缀 1380000 隔离生产数据）
PHONE_A="13800000001"    # 用户A — 发布者
PHONE_B="13800000002"    # 用户B — 跑腿员
TEST_CODE="888888"

# 后端地址
API="http://localhost:8080/api"

# 辅助函数
assert_code() { [ "$(echo "$1" | jq -r '.code')" = "$2" ] || echo "FAIL: expected code=$2, got $(echo "$1" | jq -r '.code')"; }
assert_http() { [ "$1" = "$2" ] || echo "FAIL: expected HTTP $2, got $1"; }
extract_token() { echo "$1" | jq -r '.data.token'; }
```

---

## Phase 1: 认证与注册

### Step 1.1 — SMS 发送验证码 + 频率限制

```bash
# === 正常发送 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"'"$PHONE_A"'","operation":"register"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
# 期望: HTTP 200, code=1, msg="验证码发送成功"
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证 Redis 中存在验证码
docker exec rr-redis redis-cli -n 1 GET "user:code:register:$PHONE_A"
# 期望: "888888" (或真实 6 位数字)

# === 频率限制：连续 6 次触发 5/min 限制 ===
for i in 1 2 3 4 5 6; do
  RESP=$(curl -s -X POST $API/user/send \
    -H "Content-Type: application/json" \
    -d '{"phone":"'"$PHONE_A"'","operation":"register"}')
  CODE=$(echo "$RESP" | jq -r '.code')
  echo "Attempt $i: code=$CODE"
done
# 期望: 第 1 次 code=1, 第 6 次 code=0 (SMS_RATE_LIMITED)
```

### Step 1.2 — 用户注册

```bash
# 先注入验证码到 Redis（Docker 环境无阿里云 SMS）
docker exec rr-redis redis-cli -n 1 SET "user:code:register:$PHONE_A" "$TEST_CODE" EX 300

RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone":"'"$PHONE_A"'",
    "code":"'"$TEST_CODE"'",
    "username":"e2e_user_a",
    "password":"Test123456",
    "sex":"M"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
# 期望: HTTP 200, code=1, data 含 token+refreshToken+userId
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TOKEN_A=$(extract_token "$BODY")
REFRESH_A=$(echo "$BODY" | jq -r '.data.refreshToken')
USER_ID_A=$(echo "$BODY" | jq -r '.data.userId')
echo "User A: id=$USER_ID_A token=$TOKEN_A"

# 验证 DB: user 表存在记录
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,username,phone,is_certify,balance FROM user WHERE id=$USER_ID_A"
# 期望: username=e2e_user_a, phone=13800000001, is_certify=0, balance=0

# 验证 Redis: refresh token 存在
docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:$USER_ID_A:*"
# 期望: 1 个 key

# 验证 Redis: 验证码已删除（一次性消费）
docker exec rr-redis redis-cli -n 1 GET "user:code:register:$PHONE_A"
# 期望: (nil)
```

### Step 1.3 — 密码登录（loginType=1）

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"e2e_user_a","password":"Test123456"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TOKEN_A=$(extract_token "$BODY")
REFRESH_A=$(echo "$BODY" | jq -r '.data.refreshToken')
echo "Login OK: token=$TOKEN_A"

# 验证: last_login_time 更新
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT last_login_time FROM user WHERE id=$USER_ID_A"
# 期望: 非 NULL，接近当前时间
```

### Step 1.4 — 验证码登录（loginType=2）

```bash
# 注入验证码
docker exec rr-redis redis-cli -n 1 SET "user:code:login:$PHONE_A" "$TEST_CODE" EX 300

RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":2,"phone":"'"$PHONE_A"'","code":"'"$TEST_CODE"'"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TOKEN_A=$(extract_token "$BODY")
echo "Code login OK"

# 验证: 验证码已消费
docker exec rr-redis redis-cli -n 1 GET "user:code:login:$PHONE_A"
# 期望: (nil)
```

### Step 1.5 — Token 刷新

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/refresh \
  -H "X-Refresh-Token: $REFRESH_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 新旧 token 不应相同
NEW_TOKEN=$(extract_token "$BODY")
NEW_REFRESH=$(echo "$BODY" | jq -r '.data.refreshToken')
[ "$TOKEN_A" != "$NEW_TOKEN" ] || echo "FAIL: token should be rotated"
[ "$REFRESH_A" != "$NEW_REFRESH" ] || echo "FAIL: refresh token should be rotated"
TOKEN_A="$NEW_TOKEN"
REFRESH_A="$NEW_REFRESH"
echo "Token refresh OK"

# 验证: 旧 refresh token 已删除（令牌轮换）
OLD_KEYS=$(docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:$USER_ID_A:*")
[ "$(echo "$OLD_KEYS" | wc -l)" -eq 1 ] || echo "FAIL: old refresh token should be deleted"
```

### Step 1.6 — 多端登录互踢 + Logout

```bash
# 模拟第二个设备登录（同一个用户，多次登录生成多个 refresh token）
RESP=$(curl -s -X POST $API/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"e2e_user_a","password":"Test123456"}')
TOKEN_DEV2=$(extract_token "$RESP")
echo "Device 2 login OK"

# 验证: Redis 中存在 2 个 refresh token
REFRESH_COUNT=$(docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:$USER_ID_A:*" | wc -l)
[ "$REFRESH_COUNT" -eq 2 ] || echo "FAIL: expected 2 refresh tokens, got $REFRESH_COUNT"

# 执行 logout（清除所有 refresh token）
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/logout \
  -H "authentication: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: 所有 refresh token 已删除
AFTER_LOGOUT=$(docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:$USER_ID_A:*")
[ -z "$AFTER_LOGOUT" ] || echo "FAIL: all refresh tokens should be deleted after logout"

# 验证: 旧 token 不再能访问受保护接口
RESP=$(curl -s http://localhost:8080/api/user/info -H "authentication: $TOKEN_A")
echo "$RESP" | jq -r '.msg'
# 期望: 认证失败相关消息（token 无效或已过期）

echo "Logout & multi-session invalidation OK"
```

---

## Phase 2: 用户资料与认证

> 前置：重新登录获取有效 token

```bash
RESP=$(curl -s -X POST $API/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"e2e_user_a","password":"Test123456"}')
TOKEN_A=$(extract_token "$RESP")
```

### Step 2.1 — 设置支付密码

```bash
# 先查询未设置状态
RESP=$(curl -s $API/user/pay-password/status -H "authentication: $TOKEN_A")
# 期望: code=1, data=false（首次设置前为 false）

# 首次设置支付密码
RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/user/pay-password \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"payPassword":"123456"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"
# 期望: msg="支付密码设置成功"

# 再次设置应失败（已设置过）
RESP=$(curl -s -X PUT $API/user/pay-password \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"payPassword":"111111"}')
# 期望: code=0, msg 包含"已设置"

# 验证: DB 中 pay_password 非空
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT pay_password IS NOT NULL AS has_pwd FROM user WHERE id=$USER_ID_A"
# 期望: has_pwd=1

echo "Pay password setup OK"
```

### Step 2.2 — 修改个人资料

```bash
RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/user/profile \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "nickname":"测试用户A",
    "campus":"成都校区",
    "sex":"M",
    "signature":"这是一个E2E测试用户"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: 返回最新的用户信息
echo "$BODY" | jq '.data.nickname'  # 期望: "测试用户A"
echo "$BODY" | jq '.data.campus'    # 期望: "成都校区"

# 验证: DB 已更新
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT nickname,campus,signature FROM user WHERE id=$USER_ID_A"
# 期望: 测试用户A | 成都校区 | 这是一个E2E测试用户

echo "Profile update OK"
```

### Step 2.3 — 地址簿 CRUD

```bash
# === 新增地址 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/address/save \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "contactName":"张三",
    "contactPhone":"'"$PHONE_A"'",
    "sex":"M",
    "detail":"成都校区宿舍楼3栋501室",
    "isDefault":1
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"
ADDR_ID=$(echo "$BODY" | jq -r '.data.id // empty')
# 如果返回 data 含 id，使用该 id；否则从 list 中获取
if [ -z "$ADDR_ID" ]; then
  ADDR_RESP=$(curl -s $API/user/address/list -H "authentication: $TOKEN_A")
  ADDR_ID=$(echo "$ADDR_RESP" | jq -r '.data.records[0].id')
fi
echo "Address created: id=$ADDR_ID"

# === 查询地址列表 ===
RESP=$(curl -s $API/user/address/list -H "authentication: $TOKEN_A")
echo "$RESP" | jq '.data.records | length'  # 期望: >= 1
echo "$RESP" | jq '.data.records[0].isDefault'  # 期望: 1

# === 修改地址 ===
RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/user/address/$ADDR_ID \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "contactName":"张三(已更新)",
    "contactPhone":"'"$PHONE_A"'",
    "sex":"M",
    "detail":"成都校区教学楼A栋201",
    "isDefault":1
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"

# === 新增第二个地址 ===
curl -s -X POST $API/user/address/save \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"contactName":"李四","contactPhone":"'"$PHONE_A"'","sex":"F","detail":"成都校区图书馆","isDefault":0}' > /dev/null

# === 设置默认地址 ===
ADDR2_ID=$(curl -s $API/user/address/list -H "authentication: $TOKEN_A" | jq -r '.data.records[1].id')
curl -s -X PUT $API/user/address/$ADDR2_ID/default -H "authentication: $TOKEN_A" > /dev/null

# 验证: 原默认变非默认，新地址变默认
curl -s $API/user/address/list -H "authentication: $TOKEN_A" | jq '.data.records[] | {id, isDefault}'

# === 删除非默认地址 ===
RESP=$(curl -s -w "\n%{http_code}" -X DELETE $API/user/address/$ADDR_ID \
  -H "authentication: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"

echo "Address CRUD OK"
```

### Step 2.4 — 实名认证 + 管理端审核

```bash
# === 提交认证（需要先上传图片，这里用已上传的图片 URL） ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/certify \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "realName":"张三",
    "studentId":"20240001",
    "certifyImg":"https://example.com/cert_sample.jpg"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"
# 期望: msg="实名认证提交成功"

# 验证: is_certify 变为 1（审核中）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT is_certify,real_name,student_id FROM user WHERE id=$USER_ID_A"
# 期望: is_certify=1, real_name=张三

# === 管理端审核通过 ===
# 先获取 admin token
ADMIN_RESP=$(curl -s -X POST $API/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')
ADMIN_TOKEN=$(extract_token "$ADMIN_RESP")

RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/admin/users/$USER_ID_A/certify \
  -H "Content-Type: application/json" \
  -H "token: $ADMIN_TOKEN" \
  -d '{"isCertify":2,"remark":"E2E测试通过"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: is_certify 变为 2（已认证）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT is_certify FROM user WHERE id=$USER_ID_A"
# 期望: is_certify=2

echo "Certification flow OK"
```

---

## Phase 3: 跑腿员认证

### Step 3.1 — 申请成为跑腿员

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/runner/apply \
  -H "authentication: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: runner_profile 记录存在，verify_status=1（审核中）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT user_id,verify_status,credit_score FROM runner_profile WHERE user_id=$USER_ID_A"
# 期望: verify_status=1, credit_score=100
```

### Step 3.2 — 管理端审核跑腿员

```bash
# 获取 runner_profile id
RUNNER_PROFILE_ID=$(docker exec rr-mysql mysql -uroot -proot123 runningerrands -N -e \
  "SELECT id FROM runner_profile WHERE user_id=$USER_ID_A")

RESP=$(curl -s -w "\n%{http_code}" \
  -X PUT "$API/admin/runners/$RUNNER_PROFILE_ID/review?verifyStatus=2&remark=E2E测试通过" \
  -H "token: $ADMIN_TOKEN")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: verify_status=2（已通过）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT verify_status FROM runner_profile WHERE user_id=$USER_ID_A"
# 期望: verify_status=2

echo "Runner certification OK"
```

### Step 3.3 — 跑腿员上线 + 设置接单数

```bash
# === 上线 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/runner/online \
  -H "authentication: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: is_online=1
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT is_online FROM runner_profile WHERE user_id=$USER_ID_A"
# 期望: is_online=1

# === 设置最大接单数 ===
RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/runner/max-orders \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"maxOrders":5}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT max_concurrent_orders FROM runner_profile WHERE user_id=$USER_ID_A"
# 期望: max_concurrent_orders=5

echo "Runner online & max orders OK"
```

---

## Phase 4: 任务发布

### Step 4.1 — 钱包充值

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/user/transactions/recharge \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"amount":100.00,"payPassword":"123456"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: balance=100
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_A"
# 期望: balance=100.00

# 验证: 充值流水记录
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT amount,type,balance_after FROM transaction_record WHERE user_id=$USER_ID_A AND type=3"
# 期望: amount=100.00, type=3(充值)

echo "Recharge OK"
```

### Step 4.2 — 发布代取快递任务（type=1）

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/task/publish \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "type":"代取快递",
    "subType":"小件快递",
    "publicDesc":"测试代取快递 — 菜鸟驿站取顺丰小件",
    "privateNote":"取件码: SF12345678",
    "taskSpecs":"{\"包裹列表\":[{\"规格\":\"小件\",\"数量\":1}]}",
    "tip":3,
    "deliveryFee":5,
    "productCost":0,
    "payPassword":"123456",
    "pickupAddress":"菜鸟驿站(成都校区)",
    "pickupCode":"SF12345678",
    "deliveryAddressId":'"$ADDR2_ID"',
    "expireMinutes":60,
    "contactName":"张三",
    "contactPhone":"'"$PHONE_A"'",
    "requireSex":"不限"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TASK_ID_1=$(echo "$BODY" | jq -r '.data.taskId')
TASK_NO_1=$(echo "$BODY" | jq -r '.data.taskNo')
echo "Task 1: id=$TASK_ID_1 no=$TASK_NO_1"

# 验证: task 表 status=1（WAITING）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,task_no,status,reward FROM task WHERE id=$TASK_ID_1"
# 期望: status=1, reward=8.00 (tip+deliveryFee)

# 验证: 余额扣减
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_A"
# 期望: balance=92.00 (100 - 8)

echo "Express task publish OK"
```

### Step 4.3 — 发布代拿餐食任务（type=2）

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/task/publish \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "type":"代拿餐食",
    "subType":"校外餐饮",
    "publicDesc":"测试代拿餐食 — 校门口麻辣烫店取餐",
    "privateNote":"要加辣",
    "taskSpecs":"{\"商家\":\"老成都麻辣烫\",\"餐品\":\"麻辣烫大份+米饭\"}",
    "tip":5,
    "deliveryFee":4,
    "productCost":25,
    "payPassword":"123456",
    "pickupAddress":"校门口美食街老成都麻辣烫",
    "pickupCode":"198",
    "deliveryAddressId":'"$ADDR2_ID"',
    "expireMinutes":60,
    "contactName":"张三",
    "contactPhone":"'"$PHONE_A"'"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TASK_ID_2=$(echo "$BODY" | jq -r '.data.taskId')
echo "Task 2: id=$TASK_ID_2"

# 验证: 余额再次扣减 (92 - (5+4+25))
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_A"
# 期望: balance=58.00

echo "Food task publish OK"
```

### Step 4.4 — 发布校内代办任务（type=3，物品急送）

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/task/publish \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "type":"校内代办",
    "subType":"物品急送",
    "publicDesc":"测试物品急送 — 教学楼→宿舍楼送文件",
    "privateNote":"放在门口桌上即可",
    "taskSpecs":"{\"物品名称\":\"重要文件\",\"重量\":\"轻\"}",
    "tip":2,
    "deliveryFee":3,
    "productCost":0,
    "payPassword":"123456",
    "pickupAddress":"成都校区教学楼A栋301",
    "deliveryAddressId":'"$ADDR2_ID"',
    "expireMinutes":60,
    "contactName":"张三",
    "contactPhone":"'"$PHONE_A"'"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TASK_ID_3=$(echo "$BODY" | jq -r '.data.taskId')
echo "Task 3 (errand): id=$TASK_ID_3"

echo "Errand task publish OK"
```

### Step 4.5 — 发布代购任务（type=4）

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/task/publish \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "type":"代购物品",
    "subType":"校内代购",
    "publicDesc":"测试代购 — 超市买文具",
    "privateNote":"要晨光品牌",
    "taskSpecs":"{\"商品列表\":[{\"名称\":\"笔记本\",\"数量\":2},{\"名称\":\"签字笔\",\"数量\":3}],\"预估商品费\":30}",
    "tip":3,
    "deliveryFee":4,
    "productCost":30,
    "payPassword":"123456",
    "pickupAddress":"成都校区教育超市",
    "deliveryAddressId":'"$ADDR2_ID"',
    "expireMinutes":60,
    "contactName":"张三",
    "contactPhone":"'"$PHONE_A"'"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

TASK_ID_4=$(echo "$BODY" | jq -r '.data.taskId')
echo "Task 4 (shopping): id=$TASK_ID_4"

# 验证: 最终余额 (58 - (3+4+30) = 21)
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_A"
# 期望: balance=21.00

echo "Shopping task publish OK"
```

---

## Phase 5: 订单全生命周期

> 前置：创建第二个用户（用户B）作为跑腿员。用户B 完成注册→认证→申请跑腿员→上线→充值流程。

```bash
# === 快速创建用户B并完成前置流程 ===
# 1. 注册用户B
docker exec rr-redis redis-cli -n 1 SET "user:code:register:$PHONE_B" "$TEST_CODE" EX 300
RESP=$(curl -s -X POST $API/user/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"'"$PHONE_B"'","code":"'"$TEST_CODE"'","username":"e2e_user_b","password":"Test123456","sex":"F"}')
TOKEN_B=$(extract_token "$RESP")
USER_ID_B=$(echo "$RESP" | jq -r '.data.userId')
echo "User B: id=$USER_ID_B"

# 2. 设置支付密码
curl -s -X PUT $API/user/pay-password \
  -H "Content-Type: application/json" -H "authentication: $TOKEN_B" \
  -d '{"payPassword":"123456"}' > /dev/null

# 3. 实名认证
curl -s -X POST $API/user/certify \
  -H "Content-Type: application/json" -H "authentication: $TOKEN_B" \
  -d '{"realName":"李四","studentId":"20240002","certifyImg":"https://example.com/cert_b.jpg"}' > /dev/null

# 4. 管理端审核通过
curl -s -X PUT $API/admin/users/$USER_ID_B/certify \
  -H "Content-Type: application/json" -H "token: $ADMIN_TOKEN" \
  -d '{"isCertify":2}' > /dev/null

# 5. 申请跑腿员
curl -s -X POST $API/runner/apply -H "authentication: $TOKEN_B" > /dev/null

# 6. 管理端审核跑腿员
RUNNER_PROFILE_ID_B=$(docker exec rr-mysql mysql -uroot -proot123 runningerrands -N -e \
  "SELECT id FROM runner_profile WHERE user_id=$USER_ID_B")
curl -s -X PUT "$API/admin/runners/$RUNNER_PROFILE_ID_B/review?verifyStatus=2" \
  -H "token: $ADMIN_TOKEN" > /dev/null

# 7. 充值 + 上线
curl -s -X POST $API/user/transactions/recharge \
  -H "Content-Type: application/json" -H "authentication: $TOKEN_B" \
  -d '{"amount":50,"payPassword":"123456"}' > /dev/null
curl -s -X POST $API/runner/online -H "authentication: $TOKEN_B" > /dev/null

echo "User B (runner) ready: id=$USER_ID_B"
```

### Step 5.1 — 任务大厅浏览 + 缓存验证

```bash
# === 任务大厅列表 ===
RESP=$(curl -s "$API/task/list?page=1&size=10" -H "authentication: $TOKEN_B")
echo "$RESP" | jq '.data.total'      # 期望: >= 4
echo "$RESP" | jq '.data.records | length'

# 验证: Redis 缓存已生成
TASK_HALL_KEY=$(docker exec rr-redis redis-cli -n 1 KEYS "task:hall:*")
echo "Task hall cache keys: $TASK_HALL_KEY"
# 期望: 非空

# === 任务详情（查看 Task 1） ===
RESP=$(curl -s "$API/task/$TASK_ID_1" -H "authentication: $TOKEN_B")
echo "$RESP" | jq '.data.taskNo'     # 期望: 匹配 TASK_NO_1
echo "$RESP" | jq '.data.status'     # 期望: 1 (WAITING)
echo "$RESP" | jq '.data.publicDesc' # 期望: 含"代取快递"

# 验证: Redis 详情缓存
TASK_DETAIL_KEY=$(docker exec rr-redis redis-cli -n 1 GET "task:detail:$TASK_ID_1")
[ -n "$TASK_DETAIL_KEY" ] && echo "Task detail cached OK" || echo "Task detail cache MISS"

echo "Task hall browse OK"
```

### Step 5.2 — 跑腿员接单

```bash
# === 用户B 接 Task 1 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/order/accept/$TASK_ID_1 \
  -H "authentication: $TOKEN_B")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

ORDER_ID=$(echo "$BODY" | jq -r '.data.orderId')
echo "Order created: id=$ORDER_ID"

# 验证: task_order 表 status=1（WAIT_PICKUP）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,status,task_id,runner_id FROM task_order WHERE id=$ORDER_ID"
# 期望: status=1, task_id=TASK_ID_1, runner_id=USER_ID_B

# 验证: task 表 status=2（ACCEPTED）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM task WHERE id=$TASK_ID_1"
# 期望: status=2

# 验证: 跑腿员 current_orders=1
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT current_orders FROM runner_profile WHERE user_id=$USER_ID_B"
# 期望: current_orders=1

# 验证: 任务详情缓存已清除（状态变更后）
TASK_DETAIL_AFTER=$(docker exec rr-redis redis-cli -n 1 GET "task:detail:$TASK_ID_1")
[ -z "$TASK_DETAIL_AFTER" ] && echo "Task detail cache evicted OK"

# === 并发接单冲突验证（同一任务不能重复接单） ===
RESP=$(curl -s -X POST $API/order/accept/$TASK_ID_1 \
  -H "authentication: $TOKEN_A")
echo "$RESP" | jq '.code'  # 期望: 0（任务已被接或状态不允许）
echo "Duplicate accept rejected: $(echo "$RESP" | jq -r '.msg')"

echo "Order acceptance OK"
```

### Step 5.3 — 确认取货

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/order/$ORDER_ID/pickup \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_B" \
  -d '{"imageUrls":["https://example.com/pickup_proof.jpg"]}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: order status=2（DELIVERING）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status,pickup_time FROM task_order WHERE id=$ORDER_ID"
# 期望: status=2, pickup_time 非 NULL

# 验证: task status=3（DELIVERING）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM task WHERE id=$TASK_ID_1"
# 期望: status=3

echo "Pickup confirmation OK"
```

### Step 5.4 — 确认送达

```bash
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/order/$ORDER_ID/deliver \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_B" \
  -d '{"imageUrls":["https://example.com/deliver_proof.jpg"]}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: order status=3（WAIT_CONFIRM）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status,deliver_time FROM task_order WHERE id=$ORDER_ID"
# 期望: status=3, deliver_time 非 NULL

# 验证: task status=4（WAIT_CONFIRM）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM task WHERE id=$TASK_ID_1"
# 期望: status=4

echo "Delivery confirmation OK"
```

### Step 5.5 — 发布者确认完成 → 自动结算

```bash
# === 用户A（发布者）确认完成 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/order/$ORDER_ID/confirm \
  -H "authentication: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: order status=4（COMPLETED）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status,confirm_time FROM task_order WHERE id=$ORDER_ID"
# 期望: status=4, confirm_time 非 NULL

# 验证: task status=5（COMPLETED）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM task WHERE id=$TASK_ID_1"
# 期望: status=5

# 验证: 跑腿员收到报酬（balance 增加）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_B"
# 期望: balance > 50（50 + 任务收入）

# 验证: 跑腿员 current_orders 回归 0
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT current_orders FROM runner_profile WHERE user_id=$USER_ID_B"
# 期望: current_orders=0

# 验证: 收入流水
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT amount,type FROM transaction_record WHERE user_id=$USER_ID_B AND type=2 ORDER BY id DESC LIMIT 1"
# 期望: type=2, amount=TASK_1_REWARD

# 验证: 支付幂等（重复确认不重复付款）
RESP=$(curl -s -X POST $API/order/$ORDER_ID/confirm \
  -H "authentication: $TOKEN_A")
echo "$RESP" | jq '.code'  # 期望: 0（不应再次结算）
echo "Idempotency check: $(echo "$RESP" | jq -r '.msg')"

echo "Order completion & settlement OK"
```

### Step 5.6 — 互相评价

```bash
# === 发布者评价跑腿员 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/review \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{
    "taskId":'"$TASK_ID_1"',
    "targetUserId":'"$USER_ID_B"',
    "rating":5,
    "content":"非常快！态度好！",
    "tags":"[\"速度快\",\"态度好\",\"包装仔细\"]"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"
REVIEW_ID=$(echo "$BODY" | jq -r '.data.reviewId')
echo "Review created: id=$REVIEW_ID"

# === 跑腿员评价发布者 ===
curl -s -X POST $API/review \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_B" \
  -d '{
    "taskId":'"$TASK_ID_1"',
    "targetUserId":'"$USER_ID_A"',
    "rating":4,
    "content":"任务描述清晰，沟通顺畅"
  }' > /dev/null

# === 追评 ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/review/$REVIEW_ID/followup \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_B" \
  -d '{"content":"谢谢老板！下次有任务还可以找我"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

echo "Reviews OK"
```

### Step 5.7 — 跑腿员取消订单（5分钟内）

```bash
# === 用户B 接 Task 2 ===
RESP=$(curl -s -X POST $API/order/accept/$TASK_ID_2 \
  -H "authentication: $TOKEN_B")
ORDER_ID_2=$(echo "$RESP" | jq -r '.data.orderId')
echo "Order 2: id=$ORDER_ID_2"

# === 跑腿员立即取消（5分钟内） ===
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/order/$ORDER_ID_2/cancel \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_B" \
  -d '{"reason":"临时有事无法接单"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: order status=5（CANCELLED）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status,cancel_reason FROM task_order WHERE id=$ORDER_ID_2"
# 期望: status=5, cancel_reason="临时有事无法接单"

# 验证: task status 回退到 1（WAITING）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM task WHERE id=$TASK_ID_2"
# 期望: status=1

# 验证: 跑腿员 current_orders 未增加
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT current_orders FROM runner_profile WHERE user_id=$USER_ID_B"
# 期望: current_orders=0

echo "Runner cancel OK"
```

### Step 5.8 — 发布者取消任务

```bash
# === 用户A 取消 Task 2（当前 status=1 WAITING） ===
RESP=$(curl -s -w "\n%{http_code}" -X PUT $API/task/$TASK_ID_2/cancel \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"reason":"不需要了"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: task status=6（CANCELLED）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status,cancel_reason FROM task WHERE id=$TASK_ID_2"
# 期望: status=6

# 验证: 已退款（balance 恢复）
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT balance FROM user WHERE id=$USER_ID_A"
# 期望: balance 恢复了 TASK_2 的费用

# 验证: 退款流水
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT amount,type FROM transaction_record WHERE user_id=$USER_ID_A AND type=5 ORDER BY id DESC LIMIT 1"
# 期望: type=5(退款)

echo "Publisher cancel & refund OK"
```

---

## Phase 6: 管理端全功能

> 使用 MCP chrome-devtools 进行 UI 验证。

### Step 6.1 — 仪表盘数据验证

```bash
# API 验证
RESP=$(curl -s "$API/admin/dashboard" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data | {userCount, taskCount, orderCount, runnerCount}'
# 期望: 各统计数字 > 0

echo "$RESP" | jq '.data.userTrend | length'     # 期望: > 0
echo "$RESP" | jq '.data.taskCategories | length' # 期望: > 0
```

**UI 验证（MCP chrome-devtools）:**
- 导航到 `http://localhost:3001/dashboard`
- 验证 6 个统计卡片渲染（用户数/任务数/订单数/跑腿员数/在线数/今日收入）
- 验证 4 个图表（用户趋势折线/收入柱状/任务分类饼图/订单状态柱状）
- Console 检查：无红色错误
- 截图保存

### Step 6.2 — 用户管理

```bash
# API 验证 — 用户列表
RESP=$(curl -s "$API/admin/users?page=1&size=10" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data.total'  # 期望: >= 2

# API 验证 — 用户详情
RESP=$(curl -s "$API/admin/users/$USER_ID_A" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data | {username, isCertify, balance}'
# 期望: username=e2e_user_a, isCertify=2

# API 验证 — 禁用/启用用户
curl -s -X PUT "$API/admin/users/$USER_ID_A/status?enabled=0" -H "token: $ADMIN_TOKEN" > /dev/null
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM user WHERE id=$USER_ID_A"
# 期望: status=0（禁用）

curl -s -X PUT "$API/admin/users/$USER_ID_A/status?enabled=1" -H "token: $ADMIN_TOKEN" > /dev/null
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT status FROM user WHERE id=$USER_ID_A"
# 期望: status=1（正常）
```

**UI 验证:**
- 导航到 `http://localhost:3001/users`
- 验证用户列表表格渲染（含分页）
- 验证搜索/筛选功能
- 进入用户详情页 `http://localhost:3001/users/$USER_ID_A`
- 验证详情卡片渲染（基本信息/账户认证/认证材料）

### Step 6.3 — 任务与订单管理

```bash
# 任务列表
RESP=$(curl -s "$API/admin/tasks?page=1&size=10" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data.total'

# 任务详情
RESP=$(curl -s "$API/admin/tasks/$TASK_ID_1" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data | {taskNo, status, reward}'

# 订单列表
RESP=$(curl -s "$API/admin/orders?page=1&size=10" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data.total'

# 订单详情
RESP=$(curl -s "$API/admin/orders/$ORDER_ID" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data | {status, taskNo}'
```

**UI 验证:**
- 导航到 `http://localhost:3001/tasks` — 验证任务列表 + 状态筛选
- 导航到 `http://localhost:3001/orders` — 验证订单列表
- 导航到 `http://localhost:3001/orders/$ORDER_ID` — 验证订单详情（时间线/人员信息/凭证图片）

### Step 6.4 — 通知推送 + 操作日志

```bash
# 广播通知
RESP=$(curl -s -w "\n%{http_code}" -X POST $API/admin/notifications/broadcast \
  -H "Content-Type: application/json" \
  -H "token: $ADMIN_TOKEN" \
  -d '{
    "type":1,
    "title":"E2E测试通知",
    "content":"这是一条来自全链路测试的广播通知"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
assert_http "$HTTP" "200"
assert_code "$BODY" "1"

# 验证: notification 表记录
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT title,content FROM notification WHERE title='E2E测试通知' LIMIT 1"
# 期望: 存在记录

# 操作日志
RESP=$(curl -s "$API/admin/logs?page=1&size=10" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data.total'  # 期望: > 0
```

**UI 验证:**
- 导航到 `http://localhost:3001/notifications` — 验证发送/历史 Tab
- 导航到 `http://localhost:3001/logs` — 验证日志列表（超管专属）

---

## Phase 7: 边界与异常

### Step 7.1 — 未认证访问拒绝

```bash
# 无 Token 访问受保护接口
RESP=$(curl -s -w "\n%{http_code}" $API/user/info)
HTTP=$(echo "$RESP" | tail -1)
# 期望: HTTP 401 或业务错误码（msg 含"登录"）
echo "No-auth guard: HTTP=$HTTP msg=$(echo "$RESP" | head -n -1 | jq -r '.msg')"

# 用户 Token 访问管理端接口
RESP=$(curl -s -w "\n%{http_code}" $API/admin/dashboard \
  -H "token: $TOKEN_A")
HTTP=$(echo "$RESP" | tail -1)
echo "Cross-auth (user→admin): HTTP=$HTTP"

# 管理端 Token 访问用户接口
RESP=$(curl -s -w "\n%{http_code}" $API/user/info \
  -H "authentication: $ADMIN_TOKEN")
HTTP=$(echo "$RESP" | tail -1)
echo "Cross-auth (admin→user): HTTP=$HTTP"
```

### Step 7.2 — SMS 跨操作验证码隔离

```bash
# register 验证码不能用于 reset_password
docker exec rr-redis redis-cli -n 1 SET "user:code:register:$PHONE_A" "777777" EX 300

RESP=$(curl -s -X PUT $API/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"phone":"'"$PHONE_A"'","code":"777777","newPassword":"NewPass123"}')
echo "$RESP" | jq '.code'  # 期望: 0（跨操作拒绝）
echo "SMS cross-operation guard: $(echo "$RESP" | jq -r '.msg')"
```

### Step 7.3 — 支付密码错误处理

```bash
# 错误支付密码
RESP=$(curl -s -X POST $API/user/transactions/recharge \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN_A" \
  -d '{"amount":10,"payPassword":"wrong"}')
echo "$RESP" | jq '.msg'  # 期望: 含"支付密码错误"
```

### Step 7.4 — 未认证用户无法接单

```bash
# 使用未认证用户尝试接单（如果有第三用户）
# 直接验证: 未认证用户访问 @RequireCertify 接口
# 因为已有用户已完成认证，此步骤验证 API 返回正确的认证错误
RESP=$(curl -s -X POST $API/order/accept/$TASK_ID_3 \
  -H "authentication: $TOKEN_A")
# 用户A 是发布者（非跑腿员），尝试接自己发布的任务
echo "$RESP" | jq '.code'  # 期望: 0
echo "Self-accept guard: $(echo "$RESP" | jq -r '.msg')"
```

### Step 7.5 — 文件上传鉴权

```bash
# 无 Token 上传（不传文件，只验证鉴权）
RESP=$(curl -s -X POST $API/common/upload \
  -H "Content-Type: multipart/form-data")
echo "$RESP" | jq '.msg'  # 期望: 含"登录"或"认证"
```

### Step 7.6 — 数据脱敏验证

```bash
# 管理员查看用户详情（超管看明文）
RESP=$(curl -s "$API/admin/users/$USER_ID_A" -H "token: $ADMIN_TOKEN")
echo "$RESP" | jq '.data.phone'  # 期望: 13800000001（明文）

# 如果是普通管理员（role=2），手机号应脱敏
# 可通过创建 role=2 管理员验证
```

### Step 7.7 — 最终数据库完整性检查

```bash
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e "
SELECT '=== User ===' AS '';
SELECT id,username,phone,is_certify,balance,status FROM user WHERE username LIKE 'e2e_%';

SELECT '=== Task ===' AS '';
SELECT id,task_no,type,status,publisher_id FROM task WHERE publisher_id IN ($USER_ID_A,$USER_ID_B);

SELECT '=== TaskOrder ===' AS '';
SELECT id,task_id,runner_id,status FROM task_order WHERE runner_id IN ($USER_ID_A,$USER_ID_B);

SELECT '=== Review ===' AS '';
SELECT id,task_id,reviewer_id,target_user_id,rating FROM review WHERE task_id IN ($TASK_ID_1,$TASK_ID_2,$TASK_ID_3,$TASK_ID_4);

SELECT '=== Transaction ===' AS '';
SELECT id,user_id,type,amount,balance_after FROM transaction_record WHERE user_id IN ($USER_ID_A,$USER_ID_B) ORDER BY id;
"
```

---

## 清理测试数据

```bash
# 清理命令（测试完成后执行）
# docker exec rr-mysql mysql -uroot -proot123 runningerrands -e "
#   DELETE FROM review WHERE task_id IN ($TASK_ID_1,$TASK_ID_2,$TASK_ID_3,$TASK_ID_4);
#   DELETE FROM task_image WHERE task_id IN ($TASK_ID_1,$TASK_ID_2,$TASK_ID_3,$TASK_ID_4);
#   DELETE FROM task_order WHERE task_id IN ($TASK_ID_1,$TASK_ID_2,$TASK_ID_3,$TASK_ID_4);
#   DELETE FROM payment_idempotent WHERE idempotent_key LIKE '%$TASK_ID_1%' OR idempotent_key LIKE '%$TASK_ID_2%';
#   DELETE FROM task WHERE publisher_id IN ($USER_ID_A,$USER_ID_B);
#   DELETE FROM transaction_record WHERE user_id IN ($USER_ID_A,$USER_ID_B);
#   DELETE FROM notification WHERE title='E2E测试通知';
#   DELETE FROM runner_profile WHERE user_id IN ($USER_ID_A,$USER_ID_B);
#   DELETE FROM user_address WHERE id IN ($ADDR_ID,$ADDR2_ID);
#   DELETE FROM user WHERE username LIKE 'e2e_%';
# "
# docker exec rr-redis redis-cli -n 1 KEYS "user:code:*" | xargs -r docker exec rr-redis redis-cli -n 1 DEL
# docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:*" | xargs -r docker exec rr-redis redis-cli -n 1 DEL
```
