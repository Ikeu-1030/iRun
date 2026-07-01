# 验证 Checklist — 断言矩阵

每个 Step 的断言项、期望值和严重级别。

## 缩写说明

| 缩写 | 含义 |
|------|------|
| `B` | **BLOCKER** — 流程中断，必须修复才能继续 |
| `H` | **HIGH** — 功能缺陷，记录但可跳过该 Step 继续 |
| `M` | **MEDIUM** — 非关键问题，记录后可继续 |
| `L` | **LOW** — 建议性改进，不影响流程 |

---

## Phase 1: 认证与注册

### Step 1.1 — SMS 发送 + 频率限制

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.1.1 | HTTP 200 | `curl -w "%{http_code}"` | 200 | B |
| 1.1.2 | code=1 | `jq '.code'` | 1 | B |
| 1.1.3 | msg 包含"发送成功" | `jq '.msg'` | 含"发送成功"或"验证码" | M |
| 1.1.4 | Redis 验证码存在 | redis-cli GET | 6 位数字 | M |
| 1.1.5 | 验证码 TTL ≈ 300s | redis-cli TTL | 290-300 | L |
| 1.1.6 | 第 6 次 SMS code=0 | 连续调用 | code=0, rate limited | H |
| 1.1.7 | Frequency key expire set | redis-cli TTL | 60s 内有 expire | M |

### Step 1.2 — 用户注册

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.2.1 | HTTP 200 | curl | 200 | B |
| 1.2.2 | code=1 | jq | 1 | B |
| 1.2.3 | data.token 非空 | jq '.data.token' | 非空 JWT 字符串 | B |
| 1.2.4 | data.refreshToken 非空 | jq '.data.refreshToken' | 非空 JWT 字符串 | B |
| 1.2.5 | data.userId 非空 | jq '.data.userId' | > 0 | M |
| 1.2.6 | DB user 记录存在 | MySQL SELECT | username/phone 匹配 | H |
| 1.2.7 | DB balance=0 | MySQL SELECT | 0 | M |
| 1.2.8 | DB is_certify=0 | MySQL SELECT | 0 | M |
| 1.2.9 | DB password BCrypt 加密 | MySQL SELECT | `$2a$` 前缀 | H |
| 1.2.10 | Redis refresh token 存在 | redis-cli KEYS | 至少 1 个 | M |
| 1.2.11 | Redis 验证码已删除 | redis-cli GET | (nil) | H |

### Step 1.3 — 密码登录

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.3.1 | code=1 | jq | 1 | B |
| 1.3.2 | token + refreshToken 返回 | jq | 双 token | B |
| 1.3.3 | last_login_time 更新 | MySQL SELECT | 非 NULL，接近当前时间 | M |
| 1.3.4 | 错误密码 code=0 | curl + jq | code=0, msg 含"错误" | H |
| 1.3.5 | 不存在用户 code=0 | curl + jq | code=0 | M |

### Step 1.4 — 验证码登录

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.4.1 | code=1 | jq | 1 | B |
| 1.4.2 | 验证码已消费 | redis-cli GET | (nil) | H |
| 1.4.3 | 错误验证码 code=0 | curl + jq | code=0 | H |

### Step 1.5 — Token 刷新

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.5.1 | code=1 | jq | 1 | B |
| 1.5.2 | 新 token ≠ 旧 token | 字符串比较 | 不同 | H |
| 1.5.3 | 新 refreshToken ≠ 旧 | 字符串比较 | 不同 | H |
| 1.5.4 | 旧 refresh token 已删除 | redis-cli KEYS count | 1（仅新的） | H |
| 1.5.5 | 旧 access token 仍可用（未强制失效） | curl | 200 OK (或业务过期错误) | M |

### Step 1.6 — Logout + 多端踢出

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 1.6.1 | logout code=1 | jq | 1 | B |
| 1.6.2 | 所有 refresh token 已删除 | redis-cli KEYS | 空 | H |
| 1.6.3 | 旧 access token 被拦截 | curl 受保护接口 | 401 或业务认证错误 | H |

---

## Phase 2: 用户资料与认证

### Step 2.1 — 支付密码

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 2.1.1 | 初始 status=false | jq '.data' | false | M |
| 2.1.2 | 首次设置 code=1 | jq | 1, msg="支付密码设置成功" | B |
| 2.1.3 | 重复设置 code=0 | jq | 0, msg 含"已设置" | H |
| 2.1.4 | DB pay_password 非 NULL | MySQL SELECT | `$2a$` BCrypt hash | H |
| 2.1.5 | 设置后 status=true | jq '.data' | true | M |

### Step 2.2 — 修改个人资料

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 2.2.1 | code=1 | jq | 1 | B |
| 2.2.2 | 返回最新信息 | jq '.data.nickname' | 匹配请求值 | H |
| 2.2.3 | DB 已更新 | MySQL SELECT | 字段匹配 | M |

### Step 2.3 — 地址簿 CRUD

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 2.3.1 | 新增 code=1 | jq | 1 | B |
| 2.3.2 | 列表 ≥ 1 条 | jq '.data.records \| length' | >= 1 | M |
| 2.3.3 | isDefault 正确 | jq '.data.records[0].isDefault' | 1 | M |
| 2.3.4 | 修改 code=1 | jq | 1 | M |
| 2.3.5 | 设为默认后旧默认变非默认 | 查询验证 | 两地址 default 交换 | M |
| 2.3.6 | 删除 code=1 | jq | 1 | M |

### Step 2.4 — 实名认证 + 管理端审核

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 2.4.1 | 提交认证 code=1 | jq | 1 | B |
| 2.4.2 | DB is_certify=1 | MySQL SELECT | 1 | H |
| 2.4.3 | real_name/student_id 正确 | MySQL SELECT | 匹配请求 | M |
| 2.4.4 | 管理端审核通过 code=1 | jq | 1 | B |
| 2.4.5 | DB is_certify=2（已认证） | MySQL SELECT | 2 | H |
| 2.4.6 | DB certify_remark | MySQL SELECT | "E2E测试通过" | M |

---

## Phase 3: 跑腿员认证

### Step 3.1 — 申请跑腿员

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 3.1.1 | code=1 | jq | 1 | B |
| 3.1.2 | DB verify_status=1 | MySQL SELECT | 1 | H |
| 3.1.3 | DB credit_score=100 | MySQL SELECT | 100 | M |
| 3.1.4 | DB max_concurrent_orders=3（默认） | MySQL SELECT | 3 | M |

### Step 3.2 — 管理端审核

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 3.2.1 | 审核通过 code=1 | jq | 1 | B |
| 3.2.2 | DB verify_status=2 | MySQL SELECT | 2 | H |

### Step 3.3 — 上线 + 设置接单数

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 3.3.1 | 上线 code=1 | jq | 1 | B |
| 3.3.2 | DB is_online=1 | MySQL SELECT | 1 | H |
| 3.3.3 | 设置 max=5 code=1 | jq | 1 | M |
| 3.3.4 | DB max_concurrent_orders=5 | MySQL SELECT | 5 | M |

---

## Phase 4: 任务发布

### Step 4.1 — 充值

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 4.1.1 | 充值 code=1 | jq | 1 | B |
| 4.1.2 | balance=100 | MySQL SELECT | 100.00 | H |
| 4.1.3 | 交易流水 type=3 | MySQL SELECT | type=3, amount=100 | M |
| 4.1.4 | 错误支付密码 code=0 | curl + jq | 0, msg 含"密码错误" | H |
| 4.1.5 | 幂等：1 秒内重复充值不重复到账 | MySQL SELECT balance | balance 不变 | H |

### Step 4.2 — 发布代取快递任务

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 4.2.1 | code=1 | jq | 1 | B |
| 4.2.2 | taskId + taskNo 返回 | jq | 非空 | B |
| 4.2.3 | DB status=1 (WAITING) | MySQL SELECT | 1 | H |
| 4.2.4 | DB reward = tip + deliveryFee + productCost | MySQL SELECT | SUM 匹配 | M |
| 4.2.5 | balance 扣减正确 | MySQL SELECT | 原余额 - SUM | H |
| 4.2.6 | task_specs JSON 正确存储 | MySQL SELECT | 含"包裹列表" | M |

### Step 4.3-4.5 — 其他类型任务

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 4.X.1 | code=1 | jq | 1 | B |
| 4.X.2 | DB type 正确 | MySQL SELECT | 对应 type 值 | M |
| 4.X.3 | DB status=1 | MySQL SELECT | 1 | M |
| 4.X.4 | balance 持续扣减正确 | MySQL SELECT | 累计扣减一致 | H |

---

## Phase 5: 订单全生命周期

### Step 5.1 — 任务大厅

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.1.1 | total ≥ 发布数 | jq '.data.total' | >= 4 | M |
| 5.1.2 | 任务详情 status=1 | jq '.data.status' | 1 | M |
| 5.1.3 | Redis 缓存已生成 | redis-cli KEYS | task:hall:* 非空 | M |
| 5.1.4 | task:detail:{id} 缓存存在 | redis-cli GET | 非空 | M |

### Step 5.2 — 接单

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.2.1 | code=1 | jq | 1 | B |
| 5.2.2 | orderId 返回 | jq '.data.orderId' | > 0 | B |
| 5.2.3 | DB order status=1 | MySQL SELECT | 1 | H |
| 5.2.4 | DB task status=2 (ACCEPTED) | MySQL SELECT | 2 | H |
| 5.2.5 | DB current_orders=1 | MySQL SELECT | 1 | H |
| 5.2.6 | 任务缓存已清除 | redis-cli GET task:detail:{id} | (nil) | M |
| 5.2.7 | 重复接单被拒绝 code=0 | curl + jq | 0 | H |

### Step 5.3 — 确认取货

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.3.1 | code=1 | jq | 1 | B |
| 5.3.2 | DB order status=2 (DELIVERING) | MySQL SELECT | 2 | H |
| 5.3.3 | DB task status=3 (DELIVERING) | MySQL SELECT | 3 | H |
| 5.3.4 | pickup_time 非 NULL | MySQL SELECT | 非 NULL | H |
| 5.3.5 | 非跑腿员取货 code=0 | curl + jq | 0 | H |

### Step 5.4 — 确认送达

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.4.1 | code=1 | jq | 1 | B |
| 5.4.2 | DB order status=3 (WAIT_CONFIRM) | MySQL SELECT | 3 | H |
| 5.4.3 | DB task status=4 (WAIT_CONFIRM) | MySQL SELECT | 4 | H |
| 5.4.4 | deliver_time 非 NULL | MySQL SELECT | 非 NULL | H |

### Step 5.5 — 确认完成 + 自动结算

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.5.1 | code=1 | jq | 1 | B |
| 5.5.2 | DB order status=4 (COMPLETED) | MySQL SELECT | 4 | H |
| 5.5.3 | DB task status=5 (COMPLETED) | MySQL SELECT | 5 | H |
| 5.5.4 | confirm_time 非 NULL | MySQL SELECT | 非 NULL | H |
| 5.5.5 | 跑腿员 balance 增加 | MySQL SELECT | > 原来值 | H |
| 5.5.6 | 收入流水 type=2 存在 | MySQL SELECT | type=2 | H |
| 5.5.7 | current_orders 回归 0 | MySQL SELECT | 0 | H |
| 5.5.8 | 幂等：重复确认不重复付款 | MySQL SELECT balance | 不变 | H |

### Step 5.6 — 评价

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.6.1 | 创建评价 code=1 | jq | 1 | B |
| 5.6.2 | reviewId 返回 | jq | > 0 | M |
| 5.6.3 | rating/content/tags 正确 | MySQL SELECT | 匹配请求 | M |
| 5.6.4 | 追评 code=1 | jq | 1 | M |
| 5.6.5 | 追评 parent_id 指向根评价 | MySQL SELECT | parent_id = reviewId | M |

### Step 5.7 — 跑腿员取消订单

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.7.1 | code=1 | jq | 1 | B |
| 5.7.2 | DB order status=5 (CANCELLED) | MySQL SELECT | 5 | H |
| 5.7.3 | DB task status 回退到 1 | MySQL SELECT | 1 | H |
| 5.7.4 | current_orders=0 | MySQL SELECT | 0 | H |

### Step 5.8 — 发布者取消任务 + 退款

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 5.8.1 | code=1 | jq | 1 | B |
| 5.8.2 | DB task status=6 (CANCELLED) | MySQL SELECT | 6 | H |
| 5.8.3 | cancel_reason 已记录 | MySQL SELECT | "不需要了" | M |
| 5.8.4 | balance 恢复 | MySQL SELECT | 退款后余额正确 | H |
| 5.8.5 | 退款流水 type=5 存在 | MySQL SELECT | type=5 | H |

---

## Phase 6: 管理端

### Step 6.1 — 仪表盘

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 6.1.1 | userCount >= 2 | jq '.data.userCount' | >= 2 | M |
| 6.1.2 | taskCount >= 4 | jq '.data.taskCount' | >= 4 | M |
| 6.1.3 | orderCount >= 1 | jq '.data.orderCount' | >= 1 | M |
| 6.1.4 | userTrend 数组非空 | jq '.data.userTrend \| length' | > 0 | M |
| 6.1.5 | taskCategories 非空 | jq '.data.taskCategories \| length' | > 0 | M |

### Step 6.2 — 用户管理

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 6.2.1 | 用户列表 total >= 2 | jq '.data.total' | >= 2 | M |
| 6.2.2 | 用户详情字段完整 | jq | username/isCertify/balance 正确 | M |
| 6.2.3 | 禁用用户 code=1 | jq | 1 | M |
| 6.2.4 | DB status=0 | MySQL SELECT | 0 | M |
| 6.2.5 | 启用用户 code=1 | jq | 1 | M |
| 6.2.6 | DB status=1 | MySQL SELECT | 1 | M |

### Step 6.3 — 任务与订单管理

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 6.3.1 | 任务列表 total >= 4 | jq | >= 4 | M |
| 6.3.2 | 任务详情状态正确 | jq '.data.status' | 对应状态值 | M |
| 6.3.3 | 订单列表 total >= 1 | jq | >= 1 | M |
| 6.3.4 | 订单详情时间线字段齐全 | jq | accept/pickup/deliver/confirm time | M |

### Step 6.4 — 通知推送 + 日志

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 6.4.1 | 广播通知 code=1 | jq | 1 | B |
| 6.4.2 | notification 表记录存在 | MySQL SELECT | 1+ | M |
| 6.4.3 | 操作日志 total > 0 | jq '.data.total' | > 0 | M |

---

## Phase 7: 边界与异常

### Step 7.1 — 未认证访问

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.1.1 | 无 Token HTTP 401 | curl -w | 401 或业务认证错误 | H |
| 7.1.2 | 用户 Token 不能访问管理端 | curl | 401/403 | H |
| 7.1.3 | 管理端 Token 不能访问用户端 | curl | 401/403 | H |

### Step 7.2 — SMS 跨操作隔离

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.2.1 | register 码不能 login | curl + jq | code=0 | H |
| 7.2.2 | login 码不能 reset_password | curl + jq | code=0 | H |

### Step 7.3 — 支付密码错误

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.3.1 | 错误密码 code=0 | jq | 0 | H |
| 7.3.2 | 不重复扣款 | MySQL SELECT | 仅 1 条流水 | H |

### Step 7.4 — 未认证不能接单

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.4.1 | 未认证用户 code=0 | jq | 0 | H |
| 7.4.2 | 自己接自己的单 code=0 | jq | 0 | M |

### Step 7.5 — 文件上传鉴权

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.5.1 | 无 Token 上传被拒 | jq '.msg' | 含"登录"或"认证" | H |

### Step 7.6 — 数据脱敏

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.6.1 | 超管可见明文手机号 | jq '.data.phone' | 13800000001 | M |
| 7.6.2 | 非超管手机号脱敏 | 创建 role=2 管理员验证 | X****XXXX 格式 | M |

### Step 7.7 — 系统配置验证

| # | 断言 | 方法 | 期望 | 级别 |
|---|------|------|------|:--:|
| 7.7.1 | 11 条种子配置存在 | MySQL SELECT COUNT(*) | 11 | M |
| 7.7.2 | banner.images 可编辑 | curl PUT | code=1 | M |
| 7.7.3 | banner.interval 默认 3 | MySQL SELECT | 3 | M |

---

## 汇总统计

```
Phase 1: □□□□□□ (6 steps)
Phase 2: □□□□ (4 steps)
Phase 3: □□□ (3 steps)
Phase 4: □□□□□ (5 steps)
Phase 5: □□□□□□□□ (8 steps)
Phase 6: □□□□ (4 steps)
Phase 7: □□□□□□□ (7 items)

总断言数: ~120
BLOCKER: ~15 (任一失败则验证中止)
HIGH:    ~35 (任一失败则功能不完整)
MEDIUM:  ~50 (数据一致性检查)
LOW:     ~20 (建议性改进)
```

## 报告模板

```markdown
# E2E 全链路验证报告

**日期**: YYYY-MM-DD
**环境**: Docker / 本地
**执行时间**: X min Y sec

## 结果汇总

| Phase | 通过 | 失败 | 跳过 | 状态 |
|-------|:----:|:----:|:----:|:----:|
| 1: 认证与注册 | 6/6 | 0 | 0 | ✅ |
| 2: 资料与认证 | 4/4 | 0 | 0 | ✅ |
| 3: 跑腿员认证 | 3/3 | 0 | 0 | ✅ |
| 4: 任务发布 | 5/5 | 0 | 0 | ✅ |
| 5: 订单流转 | 8/8 | 0 | 0 | ✅ |
| 6: 管理端 | 4/4 | 0 | 0 | ✅ |
| 7: 边界异常 | 7/7 | 0 | 0 | ✅ |

## 断言统计

| 级别 | 总计 | 通过 | 失败 |
|------|:----:|:----:|:----:|
| BLOCKER | 15 | 15 | 0 |
| HIGH | 35 | 35 | 0 |
| MEDIUM | 50 | 50 | 0 |
| LOW | 20 | 20 | 0 |
| **总计** | **120** | **120** | **0** |

## UI 验证

| 页面 | Console 错误 | 渲染 | 截图 |
|------|:-----------:|:----:|:----:|
| 登录 /login | 0 | ✅ | ✅ |
| 仪表盘 /dashboard | 0 | ✅ | ✅ |
| 用户管理 /users | 0 | ✅ | ✅ |
| 认证审核 /audit | 0 | ✅ | ✅ |
| 任务管理 /tasks | 0 | ✅ | ✅ |
| 订单管理 /orders | 0 | ✅ | ✅ |
| 员工管理 /employees | 0 | ✅ | ✅ |
| 系统设置 /settings | 0 | ✅ | ✅ |

## 阻塞项

_(无阻塞项，或列出待修复项)_

## 结论

✅ 1.0 发布就绪 / ⚠️ 存在需修复项目
```
