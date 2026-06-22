# 2026/06/10 — 管理端详情页重构 + 费用字段拆分

## 完成工作

### 1. 移动端 P2 代码审查修复（7 项）

| # | 文件 | 修复 |
|---|------|------|
| 1 | `order-completed.vue` | `displayDescription` 新增 type=1 快递包裹解析 |
| 2 | `order-delivering.vue` | 移除 type=4 分支死逻辑（if/else 双臂相同） |
| 3-5 | 三个详情页 | 移除未使用的 `parseShoppingItemsFromSpecs` 导入 |
| 6 | `order-completed.vue` | 送达信息卡片补充收货人、联系电话 |
| 7 | `order-waiting.vue` | 非发布者视图新增 `extraFee` 渲染行 |

额外修复：`productFeeText` 三页统一 `Number()` 包装，防止 JSON 字符串值引发 TypeError。

### 2. 管理端详情页分块重构

三个详情页从单卡片 `el-descriptions` 平铺改为分块卡片布局：

| 页面 | 块数 | 结构 |
|------|:--:|------|
| UserDetailView | 3 | 基本信息 → 账户与认证 → 认证材料 |
| TaskDetailView | 4 | 任务信息 → 取送信息 → 描述信息 → 取消原因+图片 |
| OrderDetailView | 5 | 订单信息 → 取送信息 → 人员信息 → 时间记录 → 图片凭证 |

关键改进：
- 用户认证照片改为图片网格（最多 3 张），参照凭证图片布局
- 费用明细改为「合计支付」悬停弹窗（el-popover），不破坏卡片布局
- 统一 `el-page-header` 返回导航 + 卡片间距 16px + `--anim-order` 交错入场动画
- Label 列宽度统一 9em，值列自动填充

### 3. Vite 代理配置修复

**根因**: `/api` 全量代理到后端 8080，Vite dev server 沦为纯代理，HMR 完全失效。

**修复**: `vite.config.ts` 改为仅代理 `/api/(admin|user/|common|ws)`，前端资源由 Vite 直接提供。`/api/user/` 精确匹配移动端 API，不拦截 `/api/users/`（SPA 路由）。

### 4. 数据库费用字段拆分

task 表新增三列：

```sql
ALTER TABLE task ADD COLUMN tip          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '小费';
ALTER TABLE task ADD COLUMN delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '配送费';
ALTER TABLE task ADD COLUMN product_cost DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '预估商品费';
```

配套变更：
- **Entity** `Task.java` — 3 个 `@TableField` 字段
- **DTO** `TaskPublishDTO.java` — `reward` 字段重命名 `tip`，校验注解中文更新
- **VO** `TaskDetailVO.java`、`TaskListVO.java`、`OrderDetailVO.java` — 新增三个字段
- **Service** `AdminServiceImpl.java` — builder 补充 `.tip/.deliveryFee/.productCost`（根因所在）
- **Service** `TaskServiceImpl.java` — `publishTask` 写入新列，`mergeTaskSpecs` 简化
- **全局**「赏金」→「小费」文案替换（8 个文件）
- **移动端** 5 个发布页的 API 字段 `reward:` → `tip:`

### 5. 文档

- 新增 `docs/nginx-config.md` — 生产环境 nginx 反向代理 + WebSocket 配置
- 更新 `CLAUDE.md` — Vite 代理说明、构建部署工作流

### 6. 代码审查修复

| 问题 | 文件 | 修复 |
|------|------|------|
| `@Schema(name=...)` 与文件约定不一致 | `OrderDetailVO.java` | 改为 `@Schema(description=...)` |
| 调试 SELECT 残留在迁移 SQL | `runningerrands.sql` | 移除 |

## 涉及文件

```
backend/runningerrands.sql
backend/runningerrands-model/src/main/java/com/ikeu/model/
  ├── entity/Task.java
  ├── dto/TaskPublishDTO.java
  └── vo/{TaskDetailVO,TaskListVO,OrderDetailVO}.java
backend/runningerrands-server/src/main/java/com/ikeu/server/
  └── service/impl/{AdminServiceImpl,TaskServiceImpl}.java
admin/
  ├── vite.config.ts
  └── src/views/
      ├── tasks/TaskDetailView.vue
      ├── orders/OrderDetailView.vue
      └── users/UserDetailView.vue
mobile/pages/
  ├── order-completed/order-completed.vue
  ├── order-delivering/order-delivering.vue
  ├── order-waiting/order-waiting.vue
  ├── coffee-order/coffee-order.vue
  ├── general-publish/general-publish.vue
  ├── paper-express/paper-express.vue
  ├── print-order/print-order.vue
  └── service-publish/service-publish.vue
docs/
  ├── nginx-config.md (新增)
  └── 2026-06-10-work-summary.md (本文件)
.claude/CLAUDE.md
```

---

## 后续计划

### P0 — 用户端地址管理增强 ✅ 已完成

| 内容 | 状态 |
|------|:--:|
| 自定义地址入口 + 详细地址/联系人/经纬度 + 统一管理 | ✅ `c35e619` |

### P1 — 任务发布页体验优化

| # | 内容 | 状态 |
|---|------|:--:|
| 1a | 联系人信息智能填充（图书馆还书/办事代排自动填默认地址联系人） | ⬜ |
| 1b | 发布页草稿自动保存（localStorage，退出恢复，发布后清除） | ✅ `c35e619` |

### P2 — 跑腿员信用分机制 ⬜

- 初始 100，超时/投诉扣分，<60 冻结，`credit_log` 表 — 未开始

### P3 — 用户端订单详情页改进（06-09 上期遗留） ✅ 已完成

| # | 内容 | 状态 |
|---|------|:--:|
| — | 三个详情页抽共享 `useTaskSpecs` composable | ✅ `c7d1140` |
| — | 统一 `info-card` 标签/值间距规范 | ✅ `c7d1140` |
| — | 任务类型专属区块视觉区分（边框色/背景色） | ✅ CSS 已就绪 `1e3848e` |
| — | `service-publish.vue` 拆分 | ⬜ 已提取 `FeeCard`/`GenderRestriction`，onSubmit 仍 ~220 行 |

### P4 — 建议扩展功能（长期规划）

| # | 内容 | 状态 |
|---|------|:--:|
| 4a | 任务大厅智能推荐 | ⬜ |
| 4b | 微信小程序订阅消息推送 | ⬜ |
| 4c | 数据看板增强（跑腿员排行榜 + 用户统计） | ⬜ |
| 4d | 订单纠纷处理 | ⬜ |
| 4e | 批量发布/接单 | ⬜ |
| 4f | 评价体系完善（双方互评 + 标签化 + 匿名） | ⬜ |
