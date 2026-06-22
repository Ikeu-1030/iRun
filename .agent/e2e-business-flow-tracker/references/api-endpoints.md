# API 端点速查

所有路径前缀 `/api`，Context Path `/api`。

## 认证头

| 端 | Header | 拦截器 | Token 存储位置 |
|----|--------|--------|-------------|
| 用户端 | `authentication: <jwt>` | `JwtTokenUserInterceptor` | `d2d_user_token` (localStorage) |
| 管理端 | `token: <jwt>` | `JwtTokenAdminInterceptor` | `admin_at` (sessionStorage + memory) |
| 公开 | 无需认证头 | — | — |

## 通用响应格式

```json
// 成功（单对象）
{"code":1, "msg":null, "data":{...}}

// 成功（分页）
{"code":1, "msg":null, "data":{"total":100, "records":[...]}}

// 业务错误
{"code":0, "msg":"错误描述", "data":null}

// HTTP 401 — Token 过期或无效
// HTTP 403 — 权限不足
```

---

## 用户端 — 认证 (UserController)

| 方法 | 路径 | 认证 | DTO | 说明 |
|------|------|:--:|------|------|
| POST | `/user/send` | none | `SendCodeDTO{phone, operation}` | 发送 SMS；operation: login/register/change_phone/reset_password/reset_pay_password |
| POST | `/user/register` | none | `UserRegisterDTO{phone,code,username,password,sex}` | 注册后自动登录返回双 token |
| POST | `/user/login` | none | `UserLoginDTO{loginType,username?,phone?,password?,code?}` | loginType=1密码, 2验证码 |
| POST | `/user/wechat/login` | none | `WeChatLoginDTO{code}` | 微信登录，首次自动注册 |
| POST | `/user/refresh` | none | Header `X-Refresh-Token` | 令牌轮换 |
| GET | `/user/info` | user | — | 返回 `UserInfoVO` 含 balance/isCertify |
| POST | `/user/logout` | user | — | 清除所有 refresh token |
| PUT | `/user/phone` | user | `ChangePhoneDTO{newPhone, code}` | 修改手机号（需验证码） |
| PUT | `/user/password` | user | `ChangePasswordDTO{oldPassword, newPassword}` | 修改登录密码（需旧密码） |
| PUT | `/user/password/reset` | user | `ResetPasswordDTO{phone, code, newPassword}` | 重置登录密码（SMS），5次/300s 锁定 |
| PUT | `/user/pay-password` | user | `SetPayPasswordDTO{payPassword}` | 首次设置（已设置则拒绝） |
| PUT | `/user/pay-password/change` | user | `ChangePayPasswordDTO{oldPayPassword, newPayPassword}` | 修改支付密码 |
| PUT | `/user/pay-password/reset` | user | `ResetPasswordDTO{phone, code, newPassword}` | 重置支付密码（SMS），5次/300s 锁定 |
| GET | `/user/pay-password/status` | user | — | 返回 `Boolean` |
| POST | `/user/certify` | user | `RealNameAuthDTO{realName, studentId, certifyImg}` | 提交实名认证 |
| GET | `/user/certify/status` | user | — | 返回 `CertifyStatusVO` |
| PUT | `/user/profile` | user | `UpdateProfileDTO{nickname?,avatarUrl?,campus?,sex?,signature?}` | 部分更新 |
| DELETE | `/user/account` | user | — | 物理删除用户+关联数据 |

## 用户端 — 任务 (TaskController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/task/publish` | user + `@RequireCertify` | 发布任务；需 payPassword + 余额充足 |
| GET | `/task/list` | user | 任务大厅；params: type,subType,minReward,maxReward,lng,lat,page,size |
| GET | `/task/mine` | user | 我发布的任务；params: status,page,size |
| GET | `/task/{taskId}` | user | 任务详情（含缓存防穿透/击穿） |
| PUT | `/task/{taskId}/cancel` | user + `@RequireCertify` | 取消任务；仅 status=1 可取消 |
| GET | `/task/search` | user | 关键词搜索 |
| GET | `/task/filter` | user | 高级筛选 |
| GET | `/task/nearby` | user | 附近任务 |
| GET | `/task/statistics` | user | 我的任务统计 |

## 用户端 — 订单 (TaskOrderController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/order/accept/{taskId}` | user + `@RequireCertify` | 接单；分布式锁 `order:lock:{taskId}` |
| POST | `/order/{orderId}/pickup` | user + `@RequireCertify` | 确认取货；`ProofImageDTO{imageUrls}` |
| POST | `/order/{orderId}/deliver` | user + `@RequireCertify` | 确认送达；`ProofImageDTO{imageUrls}` |
| POST | `/order/{orderId}/confirm` | user + `@RequireCertify` | 确认完成（发布者）；触发结算+信用分更新 |
| GET | `/order/{orderId}` | user | 订单详情 |
| GET | `/order/task/{taskId}` | user | 按任务 ID 查订单 |
| GET | `/order/mine` | user | 我接的订单；params: status,page,size |
| POST | `/order/{orderId}/cancel` | user + `@RequireCertify` | 跑腿员取消（5分钟内）；`CancelOrderDTO{reason}` |
| DELETE | `/order/{orderId}` | user | 软删除（完成超过7天） |

## 用户端 — 跑腿员 (RunnerController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/runner/apply` | user + `@RequireCertify` | 申请跑腿员（需 isCertify=2） |
| GET | `/runner/profile` | user | 跑腿员档案 |
| POST | `/runner/online` | user | 上线 |
| POST | `/runner/offline` | user | 离线 |
| PUT | `/runner/max-orders` | user | 设置最大接单数（1-5） |
| GET | `/runner/leaderboard` | user | 排行榜；params: sortBy(orders/rating), limit |
| GET | `/runner/performance/{runnerId}` | user | 跑腿员表现数据 |

## 用户端 — 交易 (UserTransactionController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| GET | `/user/transactions/summary` | user | 收支汇总 |
| GET | `/user/transactions` | user | 流水列表；params: type,start,end,page,size |
| POST | `/user/transactions/recharge` | user | 充值；`RechargeDTO{amount, payPassword}` |
| POST | `/user/transactions/withdraw` | user | 提现；`RechargeDTO{amount, payPassword}` |

## 用户端 — 其他

| 方法 | 路径 | 认证 | 模块 | 说明 |
|------|------|:--:|------|------|
| POST | `/user/address/save` | user | Address | 保存地址 |
| DELETE | `/user/address/{id}` | user | Address | 删除地址 |
| PUT | `/user/address/{id}` | user | Address | 更新地址 |
| GET | `/user/address/list` | user | Address | 地址列表 |
| PUT | `/user/address/{id}/default` | user | Address | 设默认 |
| POST | `/review` | user + `@RequireCertify` | Review | 创建评价；`ReviewCreateDTO{taskId,targetUserId,rating,content?,tags?}` |
| PUT | `/review/{reviewId}` | user | Review | 修改评价（7天内） |
| DELETE | `/review/{reviewId}` | user | Review | 删除评价（7天内） |
| GET | `/review/user/{targetUserId}` | user | Review | 查看用户评价 |
| GET | `/review/task/{taskId}` | user | Review | 查看任务评价 |
| POST | `/review/{reviewId}/followup` | user | Review | 追评 |
| GET | `/notification` | user | Notification | 通知列表 |
| PUT | `/notification/{id}/read` | user | Notification | 标记已读 |
| PUT | `/notification/read-all` | user | Notification | 全部已读 |
| PUT | `/notification/batch-read` | user | Notification | 批量已读 |
| DELETE | `/notification/{id}` | user | Notification | 删除通知 |
| GET | `/chat/contacts` | user | Chat | 联系人列表 |
| GET | `/chat/history/{targetUserId}` | user | Chat | 聊天历史 |
| PUT | `/chat/read/{senderId}` | user | Chat | 标记已读 |
| DELETE | `/chat/message/{messageId}` | user | Chat | 删除消息 |
| PUT | `/chat/message/recall/{messageId}` | user | Chat | 撤回消息（5分钟内） |

---

## 管理端 (Admin)

| 方法 | 路径 | 角色 | 说明 |
|------|------|:--:|------|
| POST | `/admin/login` | none | 管理员登录；`AdminLoginDTO{username,password}` |
| POST | `/admin/refresh` | none | 刷新 Token；Header `X-Refresh-Token` |
| GET | `/admin/info` | 1,2 | 获取管理员信息 |
| POST | `/admin/logout` | 1,2 | 退出登录 |
| GET | `/admin/dashboard` | 1,2 | 仪表盘数据（统计+趋势+分类） |
| GET | `/admin/users` | 1,2 | 用户列表；params: status,isCertify,keyword,page,size |
| GET | `/admin/users/{userId}` | 1,2 | 用户详情 |
| PUT | `/admin/users/{userId}/status` | **1** | 禁用/启用用户；param: enabled |
| PUT | `/admin/users/{userId}/certify` | 1,2 | 审核用户认证；params: isCertify,remark |
| GET | `/admin/runners` | 1,2 | 跑腿员列表；params: verifyStatus,keyword,page,size |
| GET | `/admin/runners/{profileId}` | 1,2 | 跑腿员详情 |
| PUT | `/admin/runners/{profileId}/review` | 1,2 | 审核跑腿员；params: verifyStatus,remark |
| PUT | `/admin/runners/{profileId}/ban` | **1** | 封禁/解封；param: banned |
| GET | `/admin/tasks` | 1,2 | 任务列表；params: status,page,size |
| GET | `/admin/tasks/{taskId}` | 1,2 | 任务详情 |
| PUT | `/admin/tasks/{taskId}/status` | **1** | 强制修改任务状态 |
| GET | `/admin/orders` | 1,2 | 订单列表；params: status,page,size |
| GET | `/admin/orders/{id}` | 1,2 | 订单详情 |
| PUT | `/admin/orders/{id}/status` | 1,2 | 强制修改订单状态 |
| GET | `/admin/transactions` | 1,2 | 流水列表；params: type,userId,start,end,page,size |
| POST | `/admin/notifications/send` | 1,2 | 定向推送 |
| POST | `/admin/notifications/broadcast` | 1,2 | 全量广播 |
| GET | `/admin/logs` | **1** | 操作日志；params: module,adminId,start,end,page,size |
| GET | `/admin/employees` | 1,2 | 管理员列表 |
| GET | `/admin/employees/{id}` | 1,2 | 管理员详情 |
| POST | `/admin/employees` | **1** | 创建管理员 |
| PUT | `/admin/employees/{id}` | **1** | 编辑管理员 |
| PUT | `/admin/employees/{id}/status` | **1** | 启用/禁用 |
| PUT | `/admin/employees/{id}/password` | **1** | 重置密码 |
| DELETE | `/admin/employees/{id}` | **1** | 删除管理员 |
| GET | `/admin/settings` | **1** | 系统配置列表 |
| PUT | `/admin/settings` | **1** | 批量更新配置 |

> 角色: 1=超级管理员, 2=普通管理员

---

## 通用 (CommonController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/common/upload` | * | 文件上传；手动解析 `token` 或 `authentication` 头；max 5MB；白名单后缀；每日限额 |
| GET | `/common/announcement` | none | 平台公告 |
| GET | `/common/banners` | none | 轮播图数据 `BannerVO{images, interval}` |

---

## WebSocket / STOMP

| 端点 | 协议 | 说明 |
|------|------|------|
| `/ws/chat` | STOMP over WebSocket + SockJS | 聊天；JWT 从 `?token=` query param |
| `/ws/notification` | STOMP over WebSocket + SockJS | 实时通知推送；JWT 从 `?token=` query param |

**Broker 配置:**
- Application prefix: `/app`
- Broker prefix: `/user`, `/queue`
- User destination prefix: `/user`

**客户端发送:**
- `/app/chat.send` — 发送聊天消息 `ChatSendDTO{receiverId, content, messageType}`

**客户端订阅:**
- `/user/queue/chat` — 接收聊天消息
- `/user/notification` — 接收通知推送

---

## 枚举速查

### Task 状态 (1-6)
| Code | Label | Terminal |
|:----:|-------|:--------:|
| 1 | 待接单 (WAITING) | |
| 2 | 已接单 (ACCEPTED) | |
| 3 | 配送中 (DELIVERING) | |
| 4 | 待确认 (WAIT_CONFIRM) | |
| 5 | 已完成 (COMPLETED) | ✓ |
| 6 | 已取消 (CANCELLED) | ✓ |

### Order 状态 (1-5)
| Code | Label | Terminal |
|:----:|-------|:--------:|
| 1 | 待取货 (WAIT_PICKUP) | |
| 2 | 配送中 (DELIVERING) | |
| 3 | 待确认 (WAIT_CONFIRM) | |
| 4 | 已完成 (COMPLETED) | ✓ |
| 5 | 已取消 (CANCELLED) | ✓ |

### 交易类型 (1-5)
| Code | Label |
|:----:|-------|
| 1 | 任务悬赏支出 |
| 2 | 跑腿收入 |
| 3 | 充值 |
| 4 | 提现 |
| 5 | 退款 |

### 认证状态 (0-3)
| Code | Label |
|:----:|-------|
| 0 | 未认证 |
| 1 | 审核中 |
| 2 | 已认证 |
| 3 | 已驳回 |

### 通知类型 (1-3)
| Code | Label |
|:----:|-------|
| 1 | 系统通知 |
| 2 | 订单状态 |
| 3 | 活动提醒 |
