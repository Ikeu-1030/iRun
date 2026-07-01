# 2026/06/15 — Agent 体系建立 + 订单详情页重构 + 模型层规范化

## 完成工作

### 1. 后端解耦 & 模型规范

- `AdminNotificationController` 抽取 `AdminNotificationService`，消除 Controller 对 `UserMapper` 的直接依赖
- 12 个 DTO/VO 格式统一：补齐 `Serializable` + 字段空行 + `@Schema` 注解
- 配置治理：新增 `application-template.yml`，删除 `application-dev.yml.example`，修正 `.gitignore`（`!application-template.yml`）
- SMS 配置写入模板（使用 `<...>` 占位符）

### 2. Agent 体系

新建 8 个文件夹式 agent，删除 2 个遗留 agent，去重合并：

| Agent | 职责 |
|-------|------|
| `init` | 最高优先级入口——新会话一键探索检查项目 |
| `backend-code-reviewer` | Java/Spring Boot 审查（3 个 references） |
| `frontend-code-reviewer` | Vue/uni-app 审查（1 个 references） |
| `frontend-agent` | 前端开发（含 DESIGN.md 设计系统） |
| `git-workflow-agent` | Git 工作流增强（合并 git-tree-master） |
| `security-auditor` | 安全审计 |
| `api-consistency-checker` | API 规范一致性 |
| `project-explorer` | 三端并行探索 |
| `autonomous-test-generator` | 链路测试（文件夹版替代遗留 .md） |

审计修复：9 项发现全部修复（1 CRITICAL + 3 HIGH + 5 MEDIUM/LOW）。

### 3. 订单详情页重构

- 新建 `useTaskSpecs.js` composable：22 个共享 computed 属性从 3 页抽取
- 3 页各减少 ~90 行重复代码（-268 行总计）
- 代码审查修复：`QueueWaitAspect` 硬编码 → `TaskSpecKeys` 常量类、`TaskDetailView` statusTag 补齐、遗留 `packageSize` 代码移除、命名统一

### 4. service-publish 优化

- 修复 TDZ bug（`packageQtys`、`selectedDuration`、`customMinutes` 声明顺序）
- 统一 `skipDelivery` 逻辑（消除重复判断）
- 提取 `FeeCard.vue`（费用/小费卡片）+ `GenderRestriction.vue`（性别选择器）
- 共享 CSS：`common/order-card.css`（任务类型左边界视觉区分）

### 5. 文档

- README 重写：当前项目结构 + 双模式开发环境（本地直连/内网穿透）+ 14 项新开发者配置清单

### 6. 移动端清理

- `identity-verify.vue` 注册到 pages.json（33 页）
- 空目录 `details/` 删除
- 移动端探索指南同步更新

## 涉及文件

```
backend/
  runningerrands-server/.../controller/admin/AdminNotificationController.java
  runningerrands-server/.../service/AdminNotificationService.java (新增)
  runningerrands-server/.../service/impl/AdminNotificationServiceImpl.java (新增)
  runningerrands-server/.../aspect/QueueWaitAspect.java
  runningerrands-common/.../constant/TaskSpecKeys.java (新增)
  runningerrands-model/.../dto/ (8 文件)
  runningerrands-model/.../vo/ (4 文件)
  runningerrands-server/src/main/resources/application-template.yml
  runningerrands.sql

admin/src/views/tasks/TaskDetailView.vue

mobile/
  utils/useTaskSpecs.js (新增)
  utils/useTaskSpecs.js (新增)
  common/order-card.css (新增)
  components/FeeCard.vue (新增)
  components/GenderRestriction.vue (新增)
  pages/order-waiting/order-waiting.vue
  pages/order-delivering/order-delivering.vue
  pages/order-completed/order-completed.vue
  pages/service-publish/service-publish.vue
  pages.json
  App.vue

.agent/ (24 文件新增, 3 文件删除, 5 文件修改)
docs/ (本文件)
README.md
```

## Git 记录

```
fa60177 fix: resolve TDZ, double defineEmits, and orphaned function bugs
1e3848e fix: resolve TDZ bug, extract FeeCard/GenderRestriction, add shared order-card styles
c7d1140 refactor: extract useTaskSpecs composable and fix code review items
3bfa91d fix: register orphan identity-verify page and update mobile exploration guide
0c04455 docs: add ngrok/tunnel-based dev environment alongside localhost approach
c64d3c4 docs: rewrite README with current project structure and unified dev environment guide
983c25a feat: add 8 specialized agents with SKILL.md + references, consolidate and de-duplicate
60a5c8c docs: sync README_EN with latest Chinese README structure
762f1d9 fix: restore onUnmounted/onUnload imports removed during refactoring
b201b4b refactor: make all constant classes final with private constructors
```

---

## 后续计划

### P0 — 支付密码重构 ✅ 已完成（06/16）

| # | 内容 | 状态 |
|---|------|:--:|
| 1 | 首次设置支付密码 → 取消校验（直接设置） | ✅ |
| 2 | 修改支付密码/登录密码 → 保持旧密码校验 | ✅ |
| 3 | 新增忘记登录密码 → 手机号短信验证码校验后重置 | ✅ |
| 3b | 新增忘记支付密码 → 手机号短信验证码校验后重置 | ✅ |

涉及：`SetPayPasswordDTO`、`ResetPasswordDTO`、`UserController`、`UserServiceImpl`、`pay-password-edit.vue`、`privacy-security.vue`、`api/user.js`

### P0.5 — 安全加固计划（06/16 代码审查发现）

| # | 内容 | 说明 |
|---|------|------|
| S1 | 短信验证码 Redis key 操作作用域化 | 注册/登录/重置共用 `user:code:{phone}` 可跨操作复用验证码，需加操作类型前缀（如 `user:code:reset:password:{phone}`）并扩展 `sendCode` 参数 |
| S2 | 密码重置端点暴力破解防护 | `resetPassword`/`resetPayPassword` 缺少失败计数锁定（登录端点已有 5 次/300s 机制），需加基于 userId 的限流 |

### P1 — 本次遗留项

| # | 内容 |
|---|------|
| 4 | 跑腿员信用分机制（初始 100，超时/投诉扣分，<60 冻结） |
| 5 | 管理端 `/runners/:id` 跑腿员详情页 |
| 6 | 管理端 `/settings` 系统设置页 |
| 7 | 订单详情页 info-card 任务类型视觉区分（CSS 已就绪） |
| 8 | 各页未使用 import 清理 |

### P2 — docs/ 工作总结遗留（06-09/06-10 → 06-15 已部分消化）

| # | 内容 | 来源 | 状态 |
|---|------|------|:--:|
| 9 | `displayDescription` 仍有 3 种变体待统一 | 06-09 P0 | ⬜ |
| 10 | `service-publish.vue` onSubmit 函数过长（~220 行），可进一步拆分验证/spec/描述 | 06-10 P1 | ⬜ |

### P3 — 长期规划（待排期）

| # | 内容 | 说明 |
|---|------|------|
| 11 | **地图 API 接入** | 腾讯地图已配置，开发功能：跑腿员实时位置追踪、配送路径规划、距离/时效预估、附近任务推荐 |
| 12 | 任务大厅智能推荐 | 基于用户历史偏好（类型、地址、预算）匹配任务 |
| 13 | 微信订阅消息推送 | 任务被接、即将超时、已送达等关键节点推送 |
| 14 | 数据看板增强 | 跑腿员排行榜 + 用户发布/消费统计 |
| 15 | 订单纠纷处理 | 申诉通道 + 管理员裁决 + 退款 + 关联信用分 |
| 16 | 评价体系完善 | 双方互评 + 标签化 + 匿名评价 |
| 17 | 批量发布/接单 | 代取快递一键多包裹 + 跑腿员顺路接单推荐 |
| 18 | CI/CD 配置 | 自动化测试 + 构建 + 部署流水线 |
