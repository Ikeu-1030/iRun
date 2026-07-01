# 2026/06/19 — 前端安全加固 + 状态机审查修复 + 命名规范统一

## 一、管理端前端浏览器安全加固

### 1.1 Token 存储加固

**问题**：access token + refresh token 明文存储于 `localStorage`，任何 XSS 均可窃取。

**方案**：新建 `admin/src/utils/tokenStore.ts` 模块
- access token 存放 JS 模块闭包变量（控制台不可直接访问），sessionStorage 作为页面刷新恢复的兜底
- refresh token 存放 sessionStorage，标签页关闭后自动清除
- 函数签名与原 `localStorage` 方案完全兼容，`request.ts` 改为从 tokenStore 重导出

### 1.2 CSP 策略

| 环境 | 机制 |
|------|------|
| 生产构建 | `vite.config.ts` 中 `transformIndexHtml` 钩子构建时注入 `<meta http-equiv="Content-Security-Policy">` |
| 开发环境 | 不注入 CSP（避免阻断 Vite HMR 内联脚本和 WebSocket） |

CSP 策略：
```
default-src 'self'; script-src 'self';
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com;
img-src 'self' data: blob:; connect-src 'self';
frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

### 1.3 安全响应头

| 头部 | 值 |
|------|-----|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` |

配置位置：
- **Vite 开发服务器** — `admin/vite.config.ts` 的 `server.headers`
- **Spring Boot 生产环境** — `WebMvcConfiguration.securityHeadersFilter`

HSTS 说明：理想配置点在反向代理/CDN 边缘层，应用层设置作为纵深防御兜底。

---

## 二、状态机安全审查与修复

### 2.1 锁键不匹配（CRITICAL）

**发现**：`AdminOrderServiceImpl.updateOrderStatus()` 锁在 `orderId`，但全系统另外 8 处代码锁在 `taskId`——管理员操作与跑腿员接单/取件/送达/定时任务之间零互斥。

**修复**：
- `AdminOrderServiceImpl`：先查订单取 taskId → 锁 `ORDER_LOCK_KEY + taskId` → 锁内重查消除 TOCTOU
- `AdminTaskServiceImpl`：由孤儿 `TASK_LOCK_KEY` 切换为 `ORDER_LOCK_KEY + taskId`
- `ORDER_LOCK_KEY` 值由 `"order:accept:"` 改为 `"order:lock:"`（全代码引用常量，透明变更）

### 2.2 状态机逻辑修正

**移除的不合理转换**：

| 转换 | 移除原因 |
|------|------|
| `WAITING → DELIVERING` | 跳过接单创建订单环节，会导致无关联订单的孤立任务 |
| `ACCEPTED → WAITING` | 订单已创建，回退留下孤儿订单 |
| `DELIVERING → WAITING` | 物品已在跑腿员手中，回退待接单无业务意义 |

**新增的业务闭合转换**：

| 转换 | 场景 |
|------|------|
| `ACCEPTED → COMPLETED` | 跑腿员已接单，线下解决，管理员直接闭环结算 |
| `DELIVERING → COMPLETED` | 跑腿员已配送，发布者不确认，管理员强制完成 |
| `WAIT_PICKUP → COMPLETED` | 订单已存在，管理员直接闭环 |
| `DELIVERING(订单) → COMPLETED` | 同上 |

### 2.3 订单同步修复

`AdminTaskServiceImpl.updateTaskStatus()` 取消任务时，新增同步取消关联订单（含退款），防止孤儿订单。

### 2.4 代码质量修复

- `AdminOrderServiceImpl` 异常类型：`RuntimeException` → `BusinessException`（与全局异常处理一致）
- `AdminTaskServiceImpl` 移除本地重复锁常量，改用 `RedisConstant`
- `AdminOrderServiceImpl` 新增 `TaskStateMachine.validate()` 校验（防止 `mapOrderStatusToTaskStatus` 映射变更导致非法任务状态）
- 缓存清除：`Objects.requireNonNull` → `if (cache != null) cache.clear()`（防 NPE）
- 订单服务新增空结果缓存清理

---

## 三、后端 Javadoc 全面完善

3 个并行 agent 覆盖 **19 个非测试 Java 文件**：

| 分组 | 关键改进 |
|------|------|
| Service 接口（9 文件） | 全部方法补充 @param/@return；`payForTask` 描述由"冻结或扣除"修正为"扣减余额+幂等校验" |
| Service 实现（7 文件） | `AdminNotificationServiceImpl`（2 方法）、`AdminEmployeeServiceImpl`（7 方法）从零→完整 Javadoc |
| Task/Aspect（3 文件） | `CreditRecoveryTask.recoverExpiredFreezes()` 从零注释→含分布式锁/TOCTOU/SQL 原子更新完整描述；`RoleCheckAspect.checkRole()` 从零注释→含 RBAC 前置通知流程 |

---

## 四、移动端命名规范统一

### 4.1 `rider-*` → `runner-*`

| 重命名 | 说明 |
|------|------|
| `pages/rider-cert/` → `pages/runner-cert/` | 统一"跑腿员"术语，消除 rider/runner 混用 |
| `pages/rider-profile/` → `pages/runner-profile/` | 同上 |

同步更新：
- `pages.json` 路由注册 + 标题（"骑手认证"→"跑腿员认证"，"骑手主页"→"跑腿员主页"）
- 3 个 `goRiderProfile()` 函数 → `goRunnerProfile()`
- 中文 UI 文本全局替换（"我的骑手"→"我的跑腿"，"骑手功能菜单"→"跑腿功能菜单"等）
- 图标名 `my-rider` → `my-runner`

### 4.2 其他命名修正

| 重命名 | 原因 |
|------|------|
| `pages/userprofile/` → `pages/user-profile/` | 补连字符，统一 kebab-case |
| `pages/pay-password-edit/` → `pages/password-manage/` | 实际处理 set/change/login/forgot 四种密码模式 |
| 删除 `uni-list - 副本.vue` | 中文"副本"残留文件 |

### 4.3 前端状态机 UI

管理端任务列表"修改状态"对话框改造：
- 显示当前状态彩色标签 → 箭头 → 仅合法目标状态的下拉框
- 终态显示"无法变更"提示
- `constants.ts` 新增 `TASK_STATE_MACHINE` 映射，与后端一致

---

## 涉及文件（累计 ~50 文件）

| 模块 | 文件数 | 主要变更 |
|------|:--:|------|
| 前端安全加固 | 5 | tokenStore 新建、request.ts、index.html、vite.config.ts、WebMvcConfiguration.java |
| 状态机修复 | 5 | TaskStateMachine.java、OrderStateMachine.java、AdminTaskServiceImpl.java、AdminOrderServiceImpl.java、RedisConstant.java |
| Javadoc 完善 | 19 | Service 接口/实现、Task、Aspect |
| 移动端重命名 | 16 | 目录+文件+pages.json+14 个引用文件 |
| 前端状态机 UI | 2 | constants.ts、TaskListView.vue |
| 文档 | 1 | 06/19-work-summary.md |

## 待续

- 临界性并发测试（锁键修正后的 TOCTOU 验证）
- 管理员端到端状态变更流程测试
- 订单管理页面前端状态机 UI（类似任务管理页）
