# 状态机转换矩阵

## Task 状态转换

```
                    ┌──────────┐
           publish  │ WAITING  │  cancel(publisher)
         ┌─────────→│    1     │──────────┐
         │          └────┬─────┘          │
         │               │ accept          │
         │               ↓                │
         │          ┌──────────┐          │
         │          │ ACCEPTED │          │
         │          │    2     │          │
         │          └────┬─────┘          │
         │               │ pickup          │
         │               ↓                │
         │          ┌───────────┐         │
         │          │ DELIVERING│         │
         │          │    3      │         │
         │          └─────┬─────┘         │
         │                │ deliver        │
         │                ↓               │
         │          ┌──────────────┐      │
         │          │ WAIT_CONFIRM │      │
         │          │      4       │      │
         │          └──────┬───────┘      │
         │                 │ confirm/auto  │
         │                 ↓              │
         │          ┌──────────┐         │
         │          │COMPLETED │         │
         │          │    5     │         │
         │          └──────────┘         │
         │                               │
         │          ┌──────────┐         │
         │          │CANCELLED │←────────┘
         │          │    6     │
         │          └──────────┘
         │
   timeout (60s 定时任务自动取消 status=1 且 expire_time < NOW 的任务)
```

### Task 合法转换

| 当前状态 | → 可转换到 | 触发操作 | 权限 |
|---------|-----------|---------|------|
| 1 WAITING | → 2 ACCEPTED | 跑腿员接单 | Runner |
| 1 WAITING | → 6 CANCELLED | 发布者取消 | Publisher |
| 1 WAITING | → 6 CANCELLED | 超时自动取消 | System (TaskTimeoutChecker) |
| 2 ACCEPTED | → 3 DELIVERING | 确认取货 | Runner |
| 2 ACCEPTED | → 5 COMPLETED | 管理端强制完成 | Admin(1) |
| 2 ACCEPTED | → 6 CANCELLED | 管理端强制取消 / 订单取消回退 | Admin/System |
| 3 DELIVERING | → 4 WAIT_CONFIRM | 确认送达 | Runner |
| 3 DELIVERING | → 5 COMPLETED | 管理端强制完成 | Admin(1) |
| 3 DELIVERING | → 6 CANCELLED | 管理端强制取消 | Admin(1) |
| 4 WAIT_CONFIRM | → 5 COMPLETED | 发布者确认 / 24h 自动完成 | Publisher/System |
| 5 COMPLETED | — | 终态 | — |
| 6 CANCELLED | — | 终态 | — |

### Task 非法转换（应被状态机拦截）

| 尝试操作 | 原因 |
|---------|------|
| 2 ACCEPTED → 发布者取消 | "任务已被接单，请联系跑腿员" |
| 3 DELIVERING → 发布者取消 | "任务已被接单，请联系跑腿员" |
| 4 WAIT_CONFIRM → 发布者取消 | 已完成配送，只能确认 |
| 5 COMPLETED → 任意 | 终态不可变 |
| 6 CANCELLED → 任意 | 终态不可变 |
| 非跑腿员 → accept | @RequireCertify 门控 |
| 信用分 < 60 → accept | 跑腿员服务冻结 |
| 已封禁 → accept | 跑腿员被禁 |

---

## Order 状态转换

```
                    ┌─────────────┐
          accept    │WAIT_PICKUP  │  cancel(runner, ≤5min)
         ┌─────────→│      1      │──────────┐
         │          └──────┬──────┘          │
         │                 │ pickup           │
         │                 ↓                  │
         │          ┌───────────┐            │
         │          │DELIVERING │            │
         │          │    2      │            │
         │          └─────┬─────┘            │
         │                │ deliver           │
         │                ↓                  │
         │          ┌──────────────┐         │
         │          │WAIT_CONFIRM  │         │
         │          │      3       │         │
         │          └──────┬───────┘         │
         │                 │ confirm/auto     │
         │                 ↓                 │
         │          ┌──────────┐            │
         │          │COMPLETED │            │
         │          │    4     │            │
         │          └──────────┘            │
         │                                  │
         │          ┌──────────┐            │
         │          │CANCELLED │←───────────┘
         │          │    5     │
         │          └──────────┘
         │
   timeout (30s 定时任务自动取消 status=1 且超 expect_finish_time 的订单)
```

### Order 合法转换

| 当前状态 | → 可转换到 | 触发操作 | 条件 |
|---------|-----------|---------|------|
| 1 WAIT_PICKUP | → 2 DELIVERING | 确认取货 | Runner 匹配 |
| 1 WAIT_PICKUP | → 4 COMPLETED | 管理端强制完成 | Admin |
| 1 WAIT_PICKUP | → 5 CANCELLED | 跑腿员取消 | ≤ 5min |
| 1 WAIT_PICKUP | → 5 CANCELLED | 超时自动取消 | System |
| 2 DELIVERING | → 3 WAIT_CONFIRM | 确认送达 | Runner 匹配 |
| 2 DELIVERING | → 4 COMPLETED | 管理端强制完成 | Admin |
| 2 DELIVERING | → 5 CANCELLED | 管理端强制取消 | Admin |
| 3 WAIT_CONFIRM | → 4 COMPLETED | 发布者确认 / 24h 自动完成 | Publisher/System |
| 4 COMPLETED | — | 终态 | — |
| 5 CANCELLED | — | 终态 | — |

### Order 完成后的连锁操作

```
Order COMPLETED 触发:
├── Task status → 5 COMPLETED
├── PaymentService.payToRunner() — 跑腿员收款
│   ├── Idempotent key: INCOME:task:{taskId}:runner:{runnerId}
│   └── Record transaction type=2 (INCOME)
├── RunnerProfile success_orders++, total_orders++
├── RunnerProfile current_orders--
├── Clear cache: runner:leaderboard, admin:dashboard
└── CreditService.processCreditOnComplete() (REQUIRES_NEW)
    ├── 提前 (+5), 准时 (+1)
    ├── 延迟 0-30min (-2), 30-60min (-5), 60min+ (-10)
    └── Record credit_log
```

---

## 接单守卫条件

跑腿员接单需 **全部满足**：

| 条件 | 检查点 | 失败消息 |
|------|--------|---------|
| Task status = 1 (WAITING) | `TaskOrderService.acceptOrder()` | "任务已被接单或已过期" |
| Task 未过期 (expire_time > NOW) | 同上 | "任务已过期" |
| 接单者 ≠ 发布者 | 同上 | "不能接自己发布的任务" |
| 性别限制匹配 | 同上 | "该任务限制了性别要求" |
| 跑腿员 is_certify = 2 | `@RequireCertify` | "请先完成实名认证" |
| 跑腿员 verify_status = 2 | `TaskOrderService.acceptOrder()` | "您还不是认证跑腿员" |
| 跑腿员 is_online = 1 | 同上 | "请先上线" |
| 信用分 >= 60 | 同上 | "信用分不足，已限制接单" |
| 未被封禁 (is_banned=0 或 ban_until < NOW) | 同上 | "账户已被封禁" |
| current_orders < max_concurrent_orders | 同上 | "已达到最大接单数" |
| 分布式锁获取成功 | Redisson `order:lock:{taskId}` | 并发冲突 |

---

## 支付幂等 Key 模式

| 业务操作 | Idempotent Key | 存储位置 |
|---------|---------------|---------|
| 任务支出 | `PAY:task:{taskId}` | `payment_idempotent` 表 |
| 任务退款 | `REFUND:task:{taskId}` | `payment_idempotent` 表 |
| 跑腿收入 | `INCOME:task:{taskId}:runner:{runnerId}` | `payment_idempotent` 表 |
| 充值 | `RECHARGE:user:{userId}:{unixSecond}` | `payment_idempotent` 表 |
| 提现 | `WITHDRAW:user:{userId}:{unixSecond}` | `payment_idempotent` 表 |

所有写操作使用 `SELECT ... FOR UPDATE` 行锁 + 幂等键双重保护。

---

## 定时任务

| 任务 | 频率 | 锁 Key | 行为 |
|------|------|--------|------|
| `TaskTimeoutChecker` | 60s | `task:timeout:lock` | 取消过期未接单任务，退款 |
| `OrderTimeoutChecker` | 30s | `order:timeout:lock` | 取消超时未取货订单；延迟提醒 |
| `OrderAutoCompleteChecker` | 60s | `order:autoComplete:lock` | 24h 未确认自动完成，结算 |
| `CreditRecoveryTask` | 5min | `credit:recovery:lock` | 恢复冻结期满的信用分 |
| `NotificationCleanupTask` | 每日 3am | `notification:cleanup:lock` | 清理过期通知（500/batch，max 20） |
| `PaymentIdempotentCleanupTask` | 每日 4am | `payment:idempotent:cleanup:lock` | 清理 7 天前的幂等记录 |
