# 前端路由与 UI 验证清单

MCP chrome-devtools 验证时使用的页面 URL 和检查点。

## 管理端 (Vue 3 + Element Plus)

**Dev server**: `http://localhost:3001` (Vite proxy → backend 8080)
**登录凭据**: `admin` / `admin`

### 页面清单

| # | 路由 | 页面标题 | 角色 | UI 检查点 |
|---|------|---------|:--:|------|
| 1 | `/login` | 登录 | — | 毛玻璃登录卡片、粒子 canvas、GSAP 动画、表单验证提示 |
| 2 | `/dashboard` | 仪表盘 | 1,2 | 6 统计卡片、4 ECharts 图表（折线/柱状/饼图/柱状）、GSAP 数字递增动画 |
| 3 | `/users` | 用户管理 | 1,2 | el-table(9列)、搜索筛选表单、el-pagination、状态标签彩色渲染 |
| 4 | `/users/:id` | 用户详情 | 1,2 | el-page-header 返回、3 块 el-card、CSS 入场动画、非超管手机号脱敏 |
| 5 | `/audit` | 认证审核 | 1,2 | el-tabs(用户认证/跑腿员认证)、双表格+分页、通过/驳回对话框、图片预览 |
| 6 | `/runners` | 跑腿员管理 | 1,2 | el-table(12列)、搜索筛选、在线/离线标签、信用分颜色分级 |
| 7 | `/runners/:id` | 跑腿员详情 | 1,2 | 4 块 el-card、信用分等级徽章、接单率统计 |
| 8 | `/tasks` | 任务管理 | 1,2 | el-table(10列)、状态筛选下拉、类型标签彩色图标、状态修改对话框+状态机验证 |
| 9 | `/tasks/:id` | 任务详情 | 1,2 | el-page-header、费用分解 el-popover、地址信息卡片、任务图片网格 |
| 10 | `/orders` | 订单管理 | 1,2 | el-table(9列)、状态筛选、无操作按钮（只读列表） |
| 11 | `/orders/:id` | 订单详情 | 1,2 | 7 块 el-card、时间线展示、取货/送达凭证图片、非超管手机号脱敏 |
| 12 | `/transactions` | 交易流水 | 1,2 | el-table(7列)、类型筛选、用户ID搜索、日期范围选择器 |
| 13 | `/notifications` | 消息管理 | 1,2 | el-tabs(发送/历史)、广播/定向切换、用户多选搜索、发送历史表格展开行 |
| 14 | `/employees` | 员工管理 | **1** | el-table(7列)、新增/编辑/禁用/重置密码对话框 |
| 15 | `/settings` | 系统设置 | **1** | 分组 el-card、内联编辑输入框、轮播图管理（上传/排序/删除）、间隔 slider |
| 16 | `/logs` | 操作日志 | **1** | el-table(8列)、模块筛选、日期范围、展开行显示请求参数（敏感字段脱敏） |
| 17 | `/:pathMatch(.*)*` | 404 | — | 大号 404 渐变文字、SVG 插画、GSAP 浮动动画、"返回首页"按钮 |

### UI 验证流程（MCP chrome-devtools）

```bash
# 1. 导航到管理端
mcp__chrome-devtools__navigate_page --url http://localhost:3001/login

# 2. 登录
mcp__chrome-devtools__take_snapshot  # 确认登录表单渲染
mcp__chrome-devtools__fill --uid <username_input> --value "admin"
mcp__chrome-devtools__fill --uid <password_input> --value "admin"
mcp__chrome-devtools__click --uid <login_button>

# 3. 逐页验证
# 对每个列表页:
#   - take_snapshot 确认表格+分页渲染
#   - 筛选操作验证交互
# 对每个详情页:
#   - take_snapshot 确认卡片布局
#   - 返回按钮可用
# 对每个表单页:
#   - 必填验证提示
#   - 提交/取消交互

# 4. Console 检查
mcp__chrome-devtools__list_console_messages --types error
# 期望: 无红色错误（允许黄色 warning）
```

---

## 移动端 (uni-app 微信小程序)

**运行方式**: HBuilderX → 运行 → 微信小程序
**工具**: 微信开发者工具

### 关键页面检查清单

| # | 页面路径 | 页面标题 | 检查点 |
|---|---------|---------|--------|
| 1 | `pages/login/login` | (无标题) | 微信一键登录按钮、底部"其他登录方式"链接 |
| 2 | `pages/account-login/account-login` | 账户登录 | 密码/SMS tab 切换、注册底部弹窗、60s 倒计时 |
| 3 | `pages/index/index` | 首页 (Tab1) | 轮播图 Banner、5 服务入口、公告滚动条、GSAP 入场动画、FAB 通用发布按钮 |
| 4 | `pages/task-hall/task-hall` | 任务大厅 (Tab2) | 搜索栏、类型筛选 chips、高级筛选面板、任务卡片列表（normalizeTaskCard 格式）、下拉刷新、无限滚动 |
| 5 | `pages/orders/orders` | 订单 (Tab3) | 角色 Tab(我的任务/我的接单)、状态筛选、订单卡片列表 |
| 6 | `pages/message/message` | 消息 (Tab4) | 通知分类（系统/物流/活动）+ 未读角标、聊天联系人列表 |
| 7 | `pages/user-profile/user-profile` | 我的 (Tab5) | 头像+昵称+认证状态、钱包卡片、功能菜单（跑腿员/钱包/认证/设置） |
| 8 | `pages/wallet/wallet` | 钱包 | 余额渐变卡片、快捷充值金额（10/50/100/200）、提现表单、支付密码弹窗 |
| 9 | `pages/service-publish/service-publish` | 需求发布 | 按 task type 动态表单（驿站选择/商家输入/物品急送/代购清单等）、UploadGrid(图片上传)、FeeCard(费用卡片)、GenderRestriction、PayPasswordDialog |
| 10 | `pages/general-publish/general-publish` | 通用任务 | 描述/私密描述/取送地址/额外费用/小费/Tip chips/UploadGrid |
| 11 | `pages/order-waiting/order-waiting` | 订单详情 | 状态横幅、非所有者信息遮罩、地址脱敏、支付方式/配送进度 |
| 12 | `pages/order-delivering/order-delivering` | 订单详情 | 状态横幅呼吸动画、联系人卡片（聊天+电话）、配送进度 uni-steps、取货/送达凭证图片、操作按钮（取货/送达/确认/取消） |
| 13 | `pages/order-completed/order-completed` | 订单详情 | 完成状态横幅、评价区域（1-5星+标签+文字）、评价追评问答流、再来一单按钮 |
| 14 | `pages/certify/certify` | 实名认证 | 表单态/审核中/已通过/已驳回四种状态、学生证 UploadGrid |
| 15 | `pages/runner-cert/runner-cert` | 跑腿员认证 | 前置学生认证 Gate、申请/审核中/已通过/已驳回四种状态 |
| 16 | `pages/runner-dashboard/runner-dashboard` | 数据概览 | 上线/离线开关、接单容量条、统计网格(总单/成功/完成率/准时率/信用分/收入)、最大接单数设置 |
| 17 | `pages/chat-detail/chat-detail` | 聊天 | 消息列表(左/右布局)、输入框+发送按钮、长按菜单(复制/删除/撤回/多选)、乐观消息替换、时间分隔 |
| 18 | `pages/address-list/address-list` | 收货地址 | 地址卡片列表、默认徽章、左滑删除、选择模式回调 |
| 19 | `pages/address-edit/address-edit` | 编辑地址 | 校园地址联动选择器(校区→楼栋→楼层→房间)、自定义地址输入 |

### 移动端关键交互

| 组件 | 验证点 |
|------|--------|
| `custom-tabbar` | 5 Tab 渲染、选中态高亮(#FF6B4A)、毛玻璃 backdrop-filter |
| `pay-password-dialog` | 6 位数字输入、自动过滤非数字、确认/取消回调 |
| `upload-grid` | 图片缩略图+删除、添加按钮(相机/相册)、max=3 限制 |
| `FeeCard` | 基础费显示、小费 chips (2/5/10/自定义)、预估商品费(type=4)、总额 |
| `GenderRestriction` | 三选一 chips (不限/仅男生/仅女生) |

---

## 前端验证 Checklist

### 管理端

- [ ] 登录页表单渲染 + GSAP 动画 + 登录成功跳转
- [ ] 仪表盘 6 统计卡片 + 4 图表 + GSAP 数字递增
- [ ] 用户列表搜索/筛选/分页 + 详情页 CSS 入场动画
- [ ] 认证审核双 Tab + 通过/驳回对话框
- [ ] 任务列表状态筛选 + 状态修改对话框(状态机约束)
- [ ] 订单详情时间线 + 凭证图片预览
- [ ] 员工管理 CRUD 对话框
- [ ] 系统设置内联编辑 + 轮播图管理
- [ ] 操作日志敏感字段脱敏
- [ ] 非超管角色限制页面不可见 + 手机号脱敏
- [ ] **Console 0 红色错误**

### 移动端

- [ ] TabBar 5 标签渲染 + 毛玻璃效果
- [ ] 首页轮播图加载（验证 CSP 不拦截 OSS 图片）
- [ ] 任务大厅搜索/筛选/无限滚动
- [ ] 发单页按类型动态表单
- [ ] 订单流转页面(等待→配送→完成)
- [ ] 评价(星级+文字+标签+追评)
- [ ] 支付密码弹窗(设置/验证/取消)
- [ ] 聊天消息实时收发
- [ ] 实名认证状态四态切换
- [ ] 钱包充值/提现
