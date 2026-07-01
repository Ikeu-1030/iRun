# 2026/06/18 — 管理端完善 + 通知中心重设计 + 1.0 里程碑

## 完成工作

### 1. 管理端视觉升级

**订单/任务详情页任务类型彩色标签：**

| 类型 | 色值 | 图标 |
|------|------|------|
| 代取快递 | `#E8734A` / `#FFF2ED` | Box |
| 代拿餐食 | `#2EB89E` / `#EDFAF7` | KnifeFork |
| 校内代办 | `#5B9BD5` / `#EFF5FB` | Document |
| 代购物品 | `#8B6BAE` / `#F6F1FA` | ShoppingCart |
| 通用代办 | `#C8925D` / `#FDF3EB` | MoreFilled |

- 详情页 `color-card` 统一类：左 3px 彩色强调线 + 头部浅色背景，覆盖 UserDetail / RunnerDetail / TaskDetail / OrderDetail
- 列表页任务类型列 + 报酬列样式统一
- 审核管理菜单前移至用户管理→跑腿员管理之间
- 通用代办类型补全 `TaskTypeConstant.ALL_TYPES`

提交：`bfb0221`

### 2. 跑腿员详情页

- `RunnerDetailView.vue`：4 张独立配色 color-card（蓝基本信息 / 橙认证状态 / 动态信用 / 紫接单数据）
- 信用分 ≥80=绿良好 / 60-79=琥珀一般 / <60=红风险，卡片头部颜色动态联动
- 列表页移除行内审核按钮，详情按钮跳转 `/runners/:id`
- GSAP 入场动画

### 3. 系统设置页 `/settings`

**后端（7 新建 + 3 修改）：**

| 层 | 文件 |
|------|------|
| SQL | `system_config` 表 + 8 条种子数据（4 分组） |
| Entity | `SystemConfig.java` |
| DTO | `SystemConfigBatchUpdateDTO.java`（含 `@Valid` 级联校验） |
| VO | `SystemConfigVO.java` |
| Mapper | `SystemConfigMapper.java` |
| Service | `AdminSystemConfigService` + `Impl`（批量查询优化 + `int`/`decimal` 值类型校验） |
| Controller | `AdminSystemConfigController`（`GET/PUT /admin/settings`，`@RequireRole({1})`） |

**配置项：**

| 分组 | Key | 类型 |
|------|-----|------|
| 基础设置 | `platform.hotline`, `platform.announcement` | string |
| 订单规则 | `order.task_expiry_hours`, `order.auto_confirm_hours`, `order.min_withdrawal` | int/int/decimal |
| 跑腿限制 | `runner.max_concurrent`, `runner.min_credit_score` | int |
| 费率设置 | `platform.fee_rate` | decimal |

**前端：**
- `SettingsView.vue`：4 张 color-card 分组 + 行内编辑 + 值格式化（% / ¥ / h）
- `GET /common/announcement` 公开接口 → 移动端首页 `uni-notice-bar` 动态公告

提交：`468bbc2`

### 4. 通知中心重设计 + 消息页改造

**后端：**
- `PUT /notification/batch-read` 批量标记已读
- `GET /user/transactions/summary` 流水汇总（全量聚合，不受分页限制）

**移动端 `message.vue`：**
- 通知入口改为紧凑列表式（68rpx 淡色圆 icon + 名称 + 描述 + 未读角标）
- 3 类通知：系统(coral) / 物流(orange) / 活动(purple)
- 图标改用 CSS `background-image` + `backgroundColor`，绕过微信小程序 `<image>` 原生组件 GPU 层覆盖

**移动端 `notifications.vue`：**
- 紧凑聊天列表式布局（68rpx 图标 / 20rpx padding / 28rpx 标题）
- 自定义滑动操作（touchstart/move/end + CSS `translateX`），复刻 `address-list.vue` 方案
- 未读：左滑露出「已读」+「删除」 | 已读：仅「删除」
- 长按进入多选模式（全选 + 批量已读 + 批量删除）
- 点击弹窗详情（类型标签 + 标题 + 全文 + 关联订单快捷入口）
- 4s 自动关闭滑动 + 滚动关闭滑动

**图标扩展：**
- `iconpark-icon.vue` 新增 `fireworks` SVG（活动提醒专用）
- 别名 `'trend' → 'fireworks'`

提交：`294ddba`

### 5. Bug 修复

| Bug | 文件 | 修复 |
|-----|------|------|
| Admin 改任务状态 → 大厅缓存空标记未清除 | `AdminTaskServiceImpl.java` | 手动清除 `CACHE_TASK_HALL` + 空标记 wildcard delete |
| 账单页总收支仅计算当前页（15条） | `bills.vue` + `UserTransactionController` | 新增 `GET /user/transactions/summary` 全量聚合 |
| `v-for` 数字 `:key` 导致 `vueIds.split` 报错 | `message.vue` / `notifications.vue` | `:key="'n-'+item.id"` 字符串化 |
| `scoped` 动态类名不匹配小程序 | `notifications.vue` | 改用 `:style` 内联背景 |
| `hasMore` 基于过滤后数组导致分页提前截断 | `notifications.vue` | 基于原始数据判断 |
| 重复 `onShow` + 顶层双重调用 | `message.vue` | 移除顶层块 |
| 死别名 `medal-filled` 双重映射 | `iconpark-icon.vue` | 删除被覆盖条目 |

### 6. 代码审查与测试

| 轮次 | 代理 | 发现 | 处理 |
|:--:|------|:--:|:--:|
| 1 | `deep-code-reviewer` — 系统设置全量 | 11 项（1 auto-fix + 2 HIGH + 4 MEDIUM + 4 LOW） | 修复 6 项 |
| 2 | `java-comment-enforcer` — 后端注释 | 7 文件注释完善 | ✅ |
| 3 | `sre-debugger` — 通知中心三问题排查 | 3 根因定位 | ✅ |
| 4 | `agent-skills:code-reviewer` — 前端全量 | 5 issues + 5 suggestions | 修复 5 项 |
| 5 | `agent-skills:test-engineer` — 端到端测试 | 5 类 API 测试全通过 | ✅ |
| 6 | `deep-code-reviewer` — 账单汇总 | 2 auto-fix + 1 HIGH + 1 MEDIUM + 2 LOW | 修复 2 项 |
| 7 | `java-comment-enforcer` — 账单汇总注释 | 4 文件 | ✅ |

---

## 涉及文件

```
backend/
  runningerrands-common/.../constant/
    MessageConstant.java              (+2 系统配置消息)
  runningerrands-model/.../
    dto/SystemConfigBatchUpdateDTO.java (新建)
    entity/SystemConfig.java           (新建)
    vo/SystemConfigVO.java             (新建)
  runningerrands-server/.../
    controller/admin/AdminSystemConfigController.java (新建)
    controller/user/NotificationController.java       (+batch-read)
    controller/user/UserTransactionController.java     (+summary)
    controller/CommonController.java                   (+announcement)
    mapper/SystemConfigMapper.java                     (新建)
    mapper/TransactionRecordMapper.java                (+sumSummaryByUserId)
    service/AdminSystemConfigService.java              (新建)
    service/NotificationService.java                   (+markBatchRead)
    service/TransactionService.java                    (+getSummary)
    service/impl/AdminSystemConfigServiceImpl.java     (新建)
    service/impl/NotificationServiceImpl.java           (+markBatchRead)
    service/impl/TransactionServiceImpl.java            (+getSummary)
    service/impl/AdminTaskServiceImpl.java              (修复缓存清除)
    resources/mapper/TransactionRecordMapper.xml        (+sumSummaryByUserId)
  runningerrands.sql                  (+system_config 表)

admin/
  src/api/settings.ts                 (新建)
  src/views/settings/SettingsView.vue (新建)
  src/views/orders/OrderDetailView.vue (任务类型彩标)
  src/views/tasks/TaskDetailView.vue   (任务类型彩标)
  src/views/tasks/TaskListView.vue     (任务类型列标签)
  src/views/users/UserDetailView.vue   (color-card 独立配色)
  src/views/runners/RunnerDetailView.vue (新建 — 信用动态配色)
  src/views/runners/RunnerListView.vue  (精简操作按钮)
  src/layouts/AdminLayout.vue          (+系统设置菜单)
  src/router/index.ts                  (+/settings + /runners/:id)

mobile/
  api/settings.ts                     (通过 admin 端)
  api/notification.js                 (+markBatchRead)
  api/transaction.js                  (+getSummary)
  api/common.js                       (+getAnnouncement)
  components/iconpark-icon/iconpark-icon.vue (+fireworks + trend别名 + medal-filled修复)
  pages/message/message.vue           (通知入口重设计 + 活动提醒 + 去搜索栏)
  pages/notifications/notifications.vue (完全重写: 紧凑布局 + 滑动 + 多选 + 弹窗)
  pages/bills/bills.vue               (汇总改为服务端聚合)
  pages/index/index.vue               (公告改为动态获取)
```

## Git 记录

```
294ddba feat: add batch notification read, fireworks icon, and swipe-to-action
468bbc2 feat: add system settings page with announcement sync
bfb0221 feat: add runner detail page, light-color tag system, and task type visual labels
```

---

## 📅 明日 (06/19) — 安全加固 + 1.0 发布就绪

### P0 — 阻塞发布

| # | 内容 | 预估 |
|---|------|:--:|
| 1 | `POST /common/upload` 新增 JWT 鉴权（当前完全未鉴权） | 小 |
| 2 | JWT secret 默认空值 → 启动时校验最小长度 | 小 |
| 3 | 安全响应头注入（CSP / X-Frame-Options / X-Content-Type-Options） | 小 |
| 4 | `request.ts` 第 83 行 `removeRefreshToken` → `removeAdminRefreshToken` | 小 |
| 5 | 微信默认密码 `123456` 修复（阻止微信用户密码登录） | 小 |
| 6 | 定时任务（TaskTimeout / OrderTimeout / AutoComplete）补全缓存清除 | 中 |

### P1 — 边缘性测试

| # | 内容 | 预估 |
|---|------|:--:|
| 7 | 创建边缘性/临界性测试 agent | 中 |
| 8 | 全面边界条件测试（空值、并发、超时、断网、过期 token） | 中 |
| 9 | 安全审查 agent — 深度漏洞扫描 | 中 |

### P2 — 发布后跟进

| # | 内容 | 预估 |
|---|------|:--:|
| 10 | 身份证号 AES-256 TypeHandler 加密 | 中 |
| 11 | 管理端登录页面接入图形验证码 | 中 |
| 12 | 手机号/真实姓名等敏感字段加密评估 | 中 |
| 5 | 纠纷处理流程 | 中 |
