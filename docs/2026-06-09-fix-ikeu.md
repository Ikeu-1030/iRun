# 2026/06/09 — task_specs 展示修复 & 格式统一

## 问题背景

task_specs JSON 数据已正确存储到数据库，但前端存在以下问题：

1. 订单详情页缺少多个 task_specs 展示区块（itemExpress、extraFee、serviceDuration）
2. type=2（代拿餐食）的商家信息存入 publicDesc 而非 task_specs
3. task_specs 中存在英文 key（`serviceEndTime`）和冗余字段（`时长标签`）
4. 各详情页"取件信息"标题未根据任务类型动态切换
5. type=4（代购物品）的商品信息展示在"任务描述"下药丸标签中

## 修复内容

### 后端

| 文件 | 改动 |
|------|------|
| `QueueWaitAspect.java` | `serviceEndTime` → `服务截止时间` |

### task_specs key 规范统一

统一后的 key 集合（全部中文）：

| Key | 用途 | 适用类型 |
|-----|------|---------|
| `包裹列表` | 快递规格列表 | type=1 |
| `商家` | 餐饮店名 | type=2 |
| `餐品` | 餐品描述 | type=2 |
| `物品名称`、`重量` | 急送物品 | type=3 |
| `服务时长`、`基础服务费`、`服务截止时间` | 办事代排 | type=3 |
| `书本数量` | 还书 | type=3 |
| `打印类型`、`打印方式` | 资料打印 | type=3 |
| `商品列表`、`预估商品费` | 代购 | type=4 |
| `额外费用` | 通用代办 | type=5 |
| `配送费` | 基础配送费（mergeTaskSpecs 注入） | 所有类型 |

移除的 key：`时长标签`（冗余，前端生成）、`商家信息`→`商家`、`serviceEndTime`→`服务截止时间`

### 发布页

| 文件 | 改动 |
|------|------|
| `service-publish.vue` | type=2 构建 task_specs（`商家`+`餐品`），校外餐饮加商家输入框，办事代排 deliveryFee=0，移除旧 key |
| `coffee-order.vue` | key `商家信息`→`商家` |

### 订单详情页

| 页面 | 改动 |
|------|------|
| `order-waiting.vue` | 动态标题（取件→取餐/代办/代购），补 itemExpress/extraFee/serviceDuration 展示 |
| `order-delivering.vue` | 动态标题，补 extraFee/serviceDuration，displayDescription 修复 |
| `order-completed.vue` | 动态标题，补 serviceDuration，type=4 商品信息移至代购信息下纯文本 |

### 管理端

- 重新构建 admin SPA，更新后端 `static/`（旧构建不含最新 task-specs-parser）
- `task-specs-parser.ts` key 同步更新

### 工具函数

- `campus-data.js`：新增 `parseFoodItemsFromSpecs`，key 更新
- `task-spec-keys.js`：新增 6 个 key 常量，移除 `DURATION_LABEL`
- `task-normalizer.js`：新增 `foodItems` 解析

## 涉及文件（源码）

```
admin/src/utils/task-specs-parser.ts
backend/.../aspect/QueueWaitAspect.java
mobile/pages/coffee-order/coffee-order.vue
mobile/pages/order-completed/order-completed.vue
mobile/pages/order-delivering/order-delivering.vue
mobile/pages/order-waiting/order-waiting.vue
mobile/pages/service-publish/service-publish.vue
mobile/utils/campus-data.js
mobile/utils/task-normalizer.js
mobile/utils/task-spec-keys.js
```

## 验证结果

| 任务类型 | 管理端任务规格 | 状态 |
|---------|--------------|:--:|
| 代取快递（type=1） | 包裹: 小件 x1 (¥3), 中件 x1 (¥6) | ✅ |
| 代拿餐食（type=2） | 旧数据无商家信息，新发布正常 | ✅ |
| 物品急送（type=3） | 物品名称: 钥匙, 重量: < 1kg | ✅ |
| 办事代排（type=3） | 服务时长: 10分钟 (基础费: ¥5) | ✅ |
| 图书馆还书（type=3） | 书本数量: 2本 | ✅ |
| 资料打印（type=3） | 打印: 黑白 / 双面打印 | ✅ |
| 代购物品（type=4） | 商品: 可乐 x1, 薯片 x1; 预估商品费: ¥50 | ✅ |

## 后续计划

### P0 — 订单详情页样式分块优化

三个订单详情页（order-waiting/delivering/completed）存在以下问题：

- 页面结构相似但样式分散，卡片内间距/字体/颜色不统一
- type=2 取餐信息、type=3 代办信息、type=4 代购信息的专属区块缺少视觉区分（当前全用同一 `info-card` 样式）
- 取件/取餐/代购信息区域的标签（商家、餐品、商品、重量等）缺少统一的排版规范

建议：
1. 抽离共享的订单信息展示组件（减少三个页面间的代码重复）
2. 统一 info-card 内部标签/值的间距规范
3. 为不同任务类型的专属信息区块添加差异化的视觉提示（如不同边框色或背景色）

### P1 — 数据库字段抽取

当前 `配送费` 和 `预估商品费` 存储于 task_specs JSON 中，属于通用字段但未独立成列。

- `delivery_fee`：所有任务类型都有，从 JSON 查询不方便
- `product_cost`：type=4 特有，但已有独立 DTO 字段传送

建议：将 `delivery_fee` 作为 task 表独立列，`task.reward` 保持合计金额不变。需要 DDL 迁移 + Entity/DTO/VO 全链更新。

### P2 — 代码审查结果（17 项发现，0 CRITICAL，7 MEDIUM）

#### MEDIUM（建议近期修复）

| # | 文件 | 问题 |
|---|------|------|
| 1 | `order-completed.vue` | `displayDescription` 缺少 type=1 快递包裹解析（不会显示"快递规格：小件x1"） |
| 2 | `order-delivering.vue` | `displayDescription` type=4 分支两臂相同，死逻辑 |
| 3-5 | 三个详情页 | `parseShoppingItemsFromSpecs` 已导入但从未使用（3 处死导入） |
| 6 | `order-completed.vue` | 送达信息卡片缺少收货人/联系电话（不同于其他两页） |
| 7 | `order-waiting.vue` | 非发布者视图的取件信息卡片缺少 `extraFee` 渲染行 |

#### LOW

| # | 问题 |
|---|------|
| 8 | order-waiting 发布者视图未对 type=4 纸品速达过滤 productTags |
| 9 | QueueWaitAspect.java 硬编码中文字符串，Java 侧无 SPEC_KEYS 常量类 |
| 10 | admin TaskDetailView `statusTag` 映射不完整（状态 2/4 缺标签类型） |

#### INFO — 架构建议

- **12 个 computed 属性在三个详情页完全相同**（约 200 行重复代码），建议抽为 `useTaskSpecs` composable
- 三个页面的 `deliveryContactName`/`deliveryContactPhone` 命名不一致
- `order-delivering` 使用 `onUnload`，其他两页用 `onUnmounted`，生命周期钩子不统一
- `order-delivering` 保留旧版 `packageSize:` 格式兼容代码，确认数据已迁移后可移除

### P3 — 已知待优化项

- 三个订单详情页的 computed 属性大量重复（`pickupSectionTitle`、`pickupAddressLabel`、`foodItems` 等），可抽为 composable
- `order-waiting.vue` 使用 TaskDetailVO，`order-delivering/completed` 使用 OrderDetailVO，字段访问不一致（`task.value.X` vs `order.value.X`）
- 管理端详情页样式为统一的 Element Plus 默认风格，可增加自定义样式提升可读性
- 移动端发布页 `service-publish.vue` 文件过大（~900 行），建议按任务类型拆分子组件
