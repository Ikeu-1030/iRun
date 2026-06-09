<p align="center">
  <img src="admin/public/logo.svg" alt="小i跑腿" width="120" />
</p>

<h1 align="center">小i跑腿 · runningerrands</h1>

<p align="center">
  <a href="README.md">中文</a>
  ·
  <a href="README_EN.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/release-v1.0.0-blue?style=plastic" alt="release" />
  <img src="https://img.shields.io/badge/springboot-3.2.0-brightgreen?style=plastic&logo=springboot" alt="springboot" />
  <img src="https://img.shields.io/badge/Vue3-grey?style=plastic&logo=vue.js" alt="vue" />
</p>

---

校园跑腿服务平台。用户发布代取快递、代拿餐食、校内代办、代购物品等任务，跑腿员接单配送。包含微信小程序端（uni-app）、管理后台（Vue 3）、Spring Boot 后端。

---

## 功能概览

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| **后端** | Spring Boot 3.2 · Java 21 · MyBatis-Plus 3.5 · MySQL 8 · Redis · Redisson · WebSocket | REST API，JWT 双令牌认证，RBAC 权限，操作审计 |
| **管理后台** | Vue 3 · TypeScript · Vite · Element Plus · ECharts · Pinia · GSAP | 仪表盘、用户/跑腿员/任务/订单管理，资金流水，审核，操作日志 |
| **移动端** | uni-app (Vue 3) · 微信小程序 · Pinia · STOMP WebSocket | 任务发布/浏览，订单跟踪，实时聊天，钱包，认证 |

### 移动端

- 四大服务：代取快递 · 代取餐食 · 校内代办 · 代购物品
- 任务大厅浏览接单，实时聊天沟通
- 钱包充值/提现，支付密码保护
- 跑腿员认证、骑手状态切换、绩效统计
- 实名认证、收货地址管理

### 管理后台

- 仪表盘数据概览（ECharts 可视化）
- 用户/跑腿员/管理员 CRUD
- 任务与订单全生命周期管理
- 资金流水查询、审核管理（实名/跑腿员）
- 通知推送、操作日志审计

---

## 项目结构

```
runningerrands/
├── backend/
│   ├── runningerrands-common/     # 共享工具、常量、异常、结果封装
│   ├── runningerrands-model/      # 实体、DTO、VO
│   ├── runningerrands-server/     # Controller、Service、Mapper、配置、拦截器、切面
│   └── runningerrands.sql         # 建表脚本
├── admin/                         # Vue 3 管理后台
├── mobile/                        # uni-app 微信小程序
└── .claude/                       # 项目 AI 辅助配置
```

---

## 快速开始

### 环境要求

- JDK 21 · MySQL 8 · Redis · Maven 3.8+
- Node.js 22+ · HBuilderX（移动端）

### 后端

```bash
cd backend

# 初始化数据库（执行 runningerrands.sql）
mysql -u root -p < runningerrands.sql

# 配置 application.yml 中的数据库、Redis、阿里云 OSS/短信、腾讯地图参数

# 编译 & 启动
./runningerrands-server/mvnw install -DskipTests
./runningerrands-server/mvnw spring-boot:run
# 服务运行在 http://localhost:8080/api
# 超管默认账号: admin / admin
```

### 管理后台

```bash
cd admin
npm install
npm run dev         # http://localhost:3001
npx vite build       # 生产构建
```

### 移动端

1. 使用 HBuilderX 打开 `mobile/` 目录
2. 修改 `mobile/utils/config.js` 中的服务器地址
3. 运行 → 微信小程序（需配置微信开发者工具）

---

## API 文档

启动后端后访问 [http://localhost:8080/api/doc.html](http://localhost:8080/api/doc.html)（Knife4j）。

---

## 注意事项

> **本小程序尚未通过微信小程序企业认证。** 微信支付和阿里云短信服务均在模拟沙箱环境中运行，生产部署前请替换为正式环境配置。

- 管理端 `/admin/**` 与用户端 API 使用独立的 JWT 密钥和拦截器
- 认证注解：`@RequireRole({1, 2})`（1=超管, 2=普通管理员），`@RequireCertify`
- 操作日志通过 `@OperationLog` 注解自动记录
- 数据库变更请同步更新 `runningerrands.sql`

---

## License

[MIT](LICENSE) © 2025 ikeu
