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

### P0 — 订单详情页样式分块优化 ✅ 已完成

| # | 内容 | 状态 |
|---|------|:--:|
| 1 | 抽离共享 `useTaskSpecs` composable（-268 行重复代码） | ✅ `c7d1140` |
| 2 | 统一 info-card 标签/值间距规范 | ✅ `c7d1140` |
| 3 | 任务类型专属区块视觉区分（边框色/背景色） | ✅ CSS 已就绪 `1e3848e` |

### P1 — 数据库字段抽取 ✅ 已完成

| 内容 | 状态 |
|------|:--:|
| `tip` / `delivery_fee` / `product_cost` 三列独立 + Entity/DTO/VO 全链更新 | ✅ `c1863b7` |

### P2 — 代码审查结果（17 项发现） ✅ 全部修复

| # | 问题 | 修复提交 |
|---|------|------|
| 1 | `displayDescription` 缺少 type=1 快递包裹解析 | ✅ `1e3848e` |
| 2 | type=4 分支死逻辑 | ✅ `1e3848e` |
| 3-5 | 3 处死导入 `parseShoppingItemsFromSpecs` | ✅ `1e3848e` |
| 6 | 送达信息卡片缺少收货人/联系电话 | ✅ `c7d1140` |
| 7 | 非发布者视图缺少 `extraFee` 行 | ✅ `1e3848e` |
| 8 | order-waiting type=4 productTags 未过滤 | ✅ `c7d1140` |
| 9 | QueueWaitAspect 硬编码 → `TaskSpecKeys` 常量类 | ✅ `c7d1140` |
| 10 | admin TaskDetailView `statusTag` 映射补齐 | ✅ `c7d1140` |
| — | 12 个 computed 抽为 `useTaskSpecs` composable | ✅ `c7d1140` |
| — | 命名不一致 (`deliveryContactName`/`deliveryContactPhone`) | ✅ `c7d1140` |
| — | 生命周期钩子不统一 (`onUnload` vs `onUnmounted`) | ✅ `762f1d9` |
| — | 旧版 `packageSize:` 兼容代码移除 | ✅ `c7d1140` |

### P3 — 已知待优化项

| # | 内容 | 状态 |
|---|------|:--:|
| — | `order-waiting` 用 `TaskDetailVO`，其他两页用 `OrderDetailVO`，字段访问不一致 | ⬜ |
| — | 管理端详情页样式 → Element Plus 默认风格优化 | ✅ `facf02a` |
| — | `service-publish.vue` 过大（~900 行 → 拆分子组件） | ⬜ → `c7d1140` 已提取 `FeeCard`/`GenderRestriction`，onSubmit 仍 ~220 行 |
