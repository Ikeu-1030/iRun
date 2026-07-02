# 测试环境与数据管理

## 环境信息

### Docker 环境（推荐）

```bash
# 服务地址
API="http://localhost:8080/api"
MYSQL="docker exec rr-mysql mysql -uroot -proot123 runningerrands"
REDIS="docker exec rr-redis redis-cli -n 1"

# 服务端口
# Backend: 8080
# MySQL:   3308 (root/root123)
# Redis:   6381 (db=1)
```

### 本地开发环境

```bash
API="http://localhost:8080/api"
MYSQL="mysql -uroot -proot123 runningerrands"
REDIS="redis-cli -n 1"
```

### 管理员登录

```bash
ADMIN_RESP=$(curl -s -X POST $API/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | jq -r '.data.token')
# 默认超管: admin / admin（可通过环境变量 RUNNING_ERRANDS_ADMIN_* 覆盖）
```

---

## 测试手机号段

| 前缀 | 用途 |
|------|------|
| `1380000xxxx` | E2E 测试专用（不与生产数据冲突） |

---

## SMS 验证码注入

Docker 环境无阿里云 SMS，直接写 Redis：

```bash
# 格式
docker exec rr-redis redis-cli -n 1 SET "user:code:<operation>:<phone>" "<code>" EX 300

# 各 operation 对应的 Redis key
# user:code:register:13800000001       — 注册
# user:code:login:13800000001          — 登录
# user:code:change_phone:13800000001   — 修改手机号
# user:code:reset_password:13800000001 — 重置登录密码
# user:code:reset_pay_password:13800000001 — 重置支付密码

# 批量注入
for op in register login change_phone reset_password reset_pay_password; do
  docker exec rr-redis redis-cli -n 1 SET "user:code:$op:13800000001" "888888" EX 300
done
```

---

## Redis 关键 Key 模式

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `user:code:{op}:{phone}` | SMS 验证码 | 300s |
| `user:refresh:token:{userId}:{jti}` | 用户 refresh token | 7d |
| `admin:refresh:token:{adminId}:{jti}` | 管理端 refresh token | 7d |
| `user:sms:rate:{ip}` | SMS 频率限制 | 60s |
| `user:login:rate:{ip}` | 登录频率限制 | 60s |
| `user:refresh:rate:{ip}` | 刷新频率限制 | 60s |
| `admin:login:rate:{ip}` | 管理端登录频率限制 | 60s |
| `admin:refresh:rate:{ip}` | 管理端刷新频率限制 | 60s |
| `user:login:fail:{account}` | 登录失败计数 | 300s |
| `admin:login:fail:{username}` | 管理端登录失败计数 | 300s |
| `user:reset:pwd:fail:{userId}` | 重置密码失败计数 | 300s |
| `user:reset:paypwd:fail:{userId}` | 重置支付密码失败计数 | 300s |
| `user:upload:daily:{userId}:{date}` | 每日上传计数 | 1d |
| `task:hall:{page}:{size}` | 任务大厅缓存 | 120s |
| `task:detail:{taskId}` | 任务详情缓存 | 600s |
| `task:notExist:{taskId}` | 空结果防穿透标记 | 60s |
| `task:notExist:hall:{page}:{size}` | 空页防穿透标记 | 60s |
| `task:hall:lock:{page}` | 任务大厅缓存重建锁 | — |
| `task:detail:lock:{taskId}` | 任务详情缓存重建锁 | — |
| `order:lock:{taskId}` | 订单操作分布式锁 | 10s |
| `task:timeout:lock` | 任务超时扫描锁 | — |
| `order:timeout:lock` | 订单超时扫描锁 | — |
| `order:autoComplete:lock` | 自动完成扫描锁 | — |
| `runner:leaderboard:{sortBy}:{limit}` | 排行榜缓存 | 300s |
| `admin:dashboard:*` | 仪表盘缓存 | 180s |
| `order:delay:reminded:{orderId}` | 延迟提醒标记 | 24h |

## Redis 调试命令

```bash
# 查看所有 key
docker exec rr-redis redis-cli -n 1 KEYS "*"

# 查看特定模式的 key
docker exec rr-redis redis-cli -n 1 KEYS "user:*"

# 查看 key 值 + TTL
docker exec rr-redis redis-cli -n 1 GET "user:code:login:13800000001"
docker exec rr-redis redis-cli -n 1 TTL "user:code:login:13800000001"

# 清空测试数据
docker exec rr-redis redis-cli -n 1 KEYS "user:code:*" | xargs -r docker exec rr-redis redis-cli -n 1 DEL
docker exec rr-redis redis-cli -n 1 KEYS "user:refresh:token:*" | while read k; do docker exec rr-redis redis-cli -n 1 DEL "$k"; done

# 查看频率限制计数
docker exec rr-redis redis-cli -n 1 GET "user:sms:rate:172.17.0.1"

# 查看分布式锁
docker exec rr-redis redis-cli -n 1 KEYS "order:lock:*"
docker exec rr-redis redis-cli -n 1 KEYS "task:timeout:lock"
```

---

## 数据库表清单

| 表 | 关键字段 | 索引 |
|----|---------|------|
| `user` | id, username(UNIQUE), phone(UNIQUE), password, pay_password, is_certify, balance, status | uk_username, uk_phone, idx_openid, idx_status |
| `user_address` | id, user_id(FK), contact_name, contact_phone, detail, is_default | idx_user_id |
| `runner_profile` | id, user_id(FK UNIQUE), verify_status, credit_score, is_online, max_concurrent_orders, current_orders, is_banned | uk_user_id, idx_verify_status, idx_online |
| `admin` | id, username(UNIQUE), phone(UNIQUE), password, role, status | — |
| `task` | id, task_no(UNIQUE), publisher_id(FK), type, sub_type, status, reward, tip, delivery_fee, product_cost, expire_time | uk_task_no, idx_publisher_status, idx_geo, idx_type, idx_task_hall |
| `task_order` | id, task_id(FK), runner_id(FK), status, accept_time, pickup_time, deliver_time, confirm_time, expect_finish_time | idx_runner_status, idx_task_deleted |
| `transaction_record` | id, user_id, task_id, amount, type, balance_before, balance_after | idx_user_time, idx_user_type_time |
| `review` | id, task_id(FK), reviewer_id(FK), target_user_id(FK), rating, content, tags(JSON), parent_id(self-FK) | uk_task_reviewer, idx_target_user, idx_target_parent |
| `notification` | id, user_id, type, title, content, is_read | idx_user_unread |
| `chat_message` | id, sender_id, receiver_id, content, message_type, is_read, is_deleted, is_recalled | idx_receiver_unread, idx_conversation_time |
| `task_image` | id, task_id, url, sort_order | idx_task_id |
| `payment_idempotent` | id, idempotent_key(UNIQUE), created_at | idx_created_at |
| `system_config` | id, config_key(UNIQUE), config_value, config_group | — |
| `operation_log` | id, admin_id, admin_name, module, action, description, ip | — |
| `credit_log` | id, runner_id, delta, score_before, score_after, reason_type, related_order_id | idx_runner_time |

## SQL 调试命令

```bash
# 查看用户
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,username,phone,is_certify,balance,status FROM user"

# 查看任务
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,task_no,type,status,publisher_id,reward FROM task"

# 查看订单
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,task_id,runner_id,status,accept_time,pickup_time,deliver_time,confirm_time FROM task_order"

# 查看跑腿员
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT rp.id,rp.user_id,u.username,rp.verify_status,rp.is_online,rp.credit_score,rp.current_orders \
   FROM runner_profile rp JOIN user u ON rp.user_id=u.id"

# 查看流水
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,user_id,type,amount,balance_before,balance_after FROM transaction_record ORDER BY id"

# 查看评价
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT id,task_id,reviewer_id,target_user_id,rating,content FROM review"

# 查看支付幂等
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e \
  "SELECT * FROM payment_idempotent ORDER BY created_at DESC LIMIT 10"

# 清理测试数据（谨慎使用）
# DELETE FROM review WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%'));
# DELETE FROM task_order WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%'));
# DELETE FROM task_image WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%'));
# DELETE FROM payment_idempotent WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR);
# DELETE FROM transaction_record WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%');
# DELETE FROM task WHERE publisher_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%');
# DELETE FROM runner_profile WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%');
# DELETE FROM user_address WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'e2e_%');
# DELETE FROM user WHERE username LIKE 'e2e_%';
```

---

## Docker 运维速查

```bash
cd F:/ikeu_runningerrands/docker

# 启动
docker compose up -d

# 重新构建后端
docker compose up -d --build backend

# 查看状态
docker compose ps

# 查看后端日志
docker logs rr-backend --tail 50
docker logs -f rr-backend

# 重启后端
docker compose restart backend

# 停止
docker compose down

# 停止并清理数据卷
docker compose down -v

# 健康检查
curl -s http://localhost:8080/api/actuator/health
```
