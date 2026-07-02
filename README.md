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

本项目是一个校园跑腿服务平台。平台提供从用户发布代取快递、代拿餐食、校内代办、代购物品等任务，到跑腿员接单配送完成等完整业务流程。整体项目主要包含微信小程序端（uni-app）、管理后台端（Vue 3）、服务后端（SpringBoot）。

---

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| Java | 21 | 开发语言 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 | 缓存 · 分布式锁 · 登录保护 |
| Redisson | 3.x | 分布式锁 |
| Knife4j | 4.5.0 | 接口文档（Swagger） |
| Maven | wrapper | 构建工具 |
| Lombok | 1.18.34 | 对象映射 |
| JWT | 0.12.6 | 双令牌认证（access + refresh） |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| TypeScript | 6.0 | 开发语言 |
| Vite | 8 | 构建工具 |
| Vue Router | 4 | 路由管理 |
| Element Plus | 2.14 | UI 组件库 |
| Pinia | 3 | 状态管理 |
| ECharts | 6 | 数据图表 |
| GSAP | 3 | 动画引擎 |
| Axios | — | HTTP 客户端 |
| Iconfont | — | 图标库（阿里巴巴矢量图标库） |
| uni-app | — | 移动端跨端框架 |
| STOMP | 1.2 | 移动端 WebSocket 即时通讯 |

---

## 页面概览

### 管理端

<table>
<tr>

| 仪表盘 | 用户管理 | 认证审核 | 任务管理 |
|:------:|:------:|:------:|:------:|
| ![](docs/imgs/admin-dashboard.png) | ![](docs/imgs/admin-usermanage.png) | ![](docs/imgs/admin-verifymanage.png) | ![](docs/imgs/admin-taskmanage.png) |

| 订单管理 | 交易流水 | 系统设置 | 消息设置 |
|:------:|:------:|:------:|:------:|
| ![](docs/imgs/admin-ordermanage.png) | ![](docs/imgs/admin-transaction.png) | ![](docs/imgs/admin-systemmanage.png) | ![](docs/imgs/admin-notificationmanage.png) |

</tr>
</table>

### 移动端

<table>
<tr>

| 首页 | 任务大厅 | 任务发布 | 订单详情 |
|:------:|:------:|:------:|:------:|
| <img src="docs/imgs/index.jpg" width="180"> | <img src="docs/imgs/hall.jpg" width="180"> | <img src="docs/imgs/task-publish.jpg" width="180"> | <img src="docs/imgs/order-detail.jpg" width="180"> |

| 订单列表 | 消息中心 | 个人中心 | 跑腿员面板 |
|:------:|:------:|:------:|:------:|
| <img src="docs/imgs/order-list.jpg" width="180"> | <img src="docs/imgs/message.jpg" width="180"> | <img src="docs/imgs/user-profile.jpg" width="180"> | <img src="docs/imgs/runner-dashboard.jpg" width="180"> |

</tr>
</table>

---

## 项目结构

### 系统架构

<table><tr>

```mermaid
flowchart TD
    subgraph PRESENTATION["展示层 (Presentation)"]
        direction LR
        MOBILE["📱 移动端<br/>uni-app 微信小程序<br/>34个页面 · 5 Tab"]
        ADMIN["🖥️ 管理端<br/>Vue 3 + Element Plus<br/>15个视图"]
    end

    GATEWAY["API 网关层 (Nginx)<br/>路由分发 · 静态资源 · WebSocket 代理<br/>端口 80/443 → 后端 8080"]

    subgraph BUSINESS["业务层 (Business) · Spring Boot 3.2.0"]
        direction TB

        subgraph CONTROLLER["Controller 控制层 (21个)"]
            direction LR
            CTRL_ADMIN["admin 包<br/>AdminController<br/>UserManageController..."]
            CTRL_USER["user 包<br/>UserController<br/>TaskController..."]
            CTRL_COMMON["通用接口<br/>CommonController<br/>文件上传/下载"]
        end

        INTERCEPTOR["拦截器层<br/>JwtTokenAdminInterceptor (token 头)<br/>JwtTokenUserInterceptor (authentication 头)"]

        subgraph SERVICE["Service 服务层 (21个)"]
            direction LR
            SVC_CORE["核心业务<br/>UserServiceImpl/TaskServiceImpl/TaskOrderServiceImpl/RunnerProfileServiceImpl/PaymentServiceImpl"]
            SVC_SUPPORT["支撑业务<br/>ChatMessageServiceImpl/ReviewServiceImpl/NotificationServiceImpl/TransactionServiceImpl/AddressServiceImpl/CreditLogServiceImpl"]
            SVC_ADMIN["管理业务<br/>AdminDashboardServiceImpl/OperationLogServiceImpl/SystemConfigServiceImpl/AdminServiceImpl"]
        end

        AOP["AOP 切面层 (7个)<br/>@OperationLog 操作日志记录<br/>@SendNotification 通知推送<br/>@RedisDefend 防缓存穿透/击穿"]

        MAPPER["Mapper 数据访问层 (15个)<br/>MyBatis-Plus BaseMapper<br/>LambdaWrapper 参数化查询"]
    end

    subgraph DATA["数据层 (Data)"]
        direction LR
        MYSQL[("MySQL 8<br/>runningerrands<br/>15张表")]
        REDIS[("Redis 7<br/>缓存 · 分布式锁<br/>登录保护 · 验证码")]
        OSS["阿里云 OSS<br/>图片/文件存储"]
    end

    subgraph CROSS["跨切面基础设施"]
        direction LR
        JWT["JWT 双令牌<br/>access(2h) + refresh(7d)"]
        REDISSON["Redisson<br/>分布式锁"]
        STOMP["STOMP WebSocket<br/>即时通讯"]
        SWAGGER["Knife4j<br/>API 文档"]
    end

    PRESENTATION -->|"HTTPS /api"| GATEWAY
    GATEWAY -->|"反向代理"| CONTROLLER
    CONTROLLER --> INTERCEPTOR
    INTERCEPTOR --> SERVICE
    SERVICE --> MAPPER
    MAPPER --> MYSQL
    MAPPER --> REDIS
    SERVICE --> OSS
    AOP -.-> CONTROLLER
    AOP -.-> SERVICE
    INTERCEPTOR -.-> JWT
    SERVICE -.-> REDISSON
    SERVICE -.-> STOMP
    CONTROLLER -.-> SWAGGER

    style PRESENTATION fill:#FFF0ED,stroke:#FF6B4A,stroke-width:3px
    style BUSINESS fill:#D4F5F0,stroke:#2EC4B6,stroke-width:3px
    style DATA fill:#EEF2FB,stroke:#5B9BD5,stroke-width:3px
    style CROSS fill:#FFF7ED,stroke:#C8925D,stroke-width:2px
    style GATEWAY fill:#F5F5F0,stroke:#8F8D88,stroke-width:2px
    style CONTROLLER fill:#FFFFFF,stroke:#D4D2CC,stroke-width:1px
    style SERVICE fill:#FFFFFF,stroke:#D4D2CC,stroke-width:1px
    style MAPPER fill:#FFFFFF,stroke:#D4D2CC,stroke-width:1px
    style INTERCEPTOR fill:#FEF3C7,stroke:#F59E0B,stroke-width:2px
    style AOP fill:#EDE9FE,stroke:#8B5CF6,stroke-width:2px
    style MOBILE fill:#FF6B4A,color:#FFFFFF
    style ADMIN fill:#FF6B4A,color:#FFFFFF
```

</tr></table>

### 请求生命周期

<table><tr>

```mermaid
sequenceDiagram
    participant Client as 客户端 (Mobile/Admin)
    participant Nginx as Nginx 网关
    participant Interceptor as JWT 拦截器
    participant Controller as Controller
    participant Service as Service
    participant AOP as AOP 切面
    participant Mapper as Mapper
    participant DB as MySQL/Redis

    Client->>Nginx: HTTPS /api/xxx
    Nginx->>Interceptor: 转发请求
    Interceptor->>Interceptor: 解析 Token · 校验签名 · 提取 userId
    Interceptor->>Interceptor: 写入 ThreadLocal (BaseContext)
    Interceptor->>Controller: 放行
    Controller->>Controller: @Valid 参数校验
    Controller->>Service: 调用业务方法
    Service->>AOP: @RedisDefend 防缓存穿透
    AOP->>DB: 查 Redis 缓存
    DB-->>AOP: 未命中
    AOP->>Mapper: 查数据库
    Service->>AOP: @OperationLog 记录操作
    Service->>AOP: @SendNotification 推送通知
    Mapper->>DB: MyBatis-Plus SQL
    DB-->>Mapper: 结果集
    Mapper-->>Service: Entity
    Service-->>Controller: VO/DTO
    Controller-->>Client: Result<T> JSON
```

</tr></table>

### 订单状态机

<table><tr>

```mermaid
stateDiagram-v2
    [*] --> WAITING : 发布任务
    WAITING --> ACCEPTED : 跑腿员接单<br/>(Redisson 分布式锁)
    WAITING --> CANCELLED : 发布者取消 / 超时
    ACCEPTED --> DELIVERING : 确认取货<br/>(上传凭证)
    ACCEPTED --> CANCELLED : 管理端取消
    DELIVERING --> WAIT_CONFIRM : 确认送达<br/>(上传凭证)
    DELIVERING --> CANCELLED : 管理端取消
    WAIT_CONFIRM --> COMPLETED : 确认完成 / 24h 自动完成
    COMPLETED --> [*] : 结算 + 信用分更新 + 评价
    CANCELLED --> [*] : 退款

    note right of COMPLETED
        触发连锁操作:
        - 跑腿员收款 (幂等)
        - 信用分更新
        - 排行榜缓存清除
    end note
```

详见 [CLAUDE.md](.claude/CLAUDE.md) 获取完整项目规范。

---

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端编译运行 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存 + 分布式锁 + 登录保护 |
| Maven | 3.8+ | 或使用 `mvnw` wrapper |
| Node.js | 18+ / 22+ | 管理端前端 |
| HBuilderX | 最新版 | 移动端开发与编译 |
| 微信开发者工具 | 最新版 | 小程序调试 |

---

## 快速开始

### 方式一：Docker（推荐，无需配置外部服务）

```bash
cd docker
cp .env.example .env
docker compose up -d        # MySQL 8 + Redis 7 + Spring Boot + Nginx
```

首次启动自动建表 + 创建超管。详见 [docker/README.md](docker/README.md)。

### 方式二：本地开发

#### 1. 初始化数据库

```bash
mysql -u root -p < backend/runningerrands.sql
```

脚本创建 `runningerrands` 数据库及全部表结构，不含种子数据。首次启动后 `AdminInitializer` 自动创建超管账号。

### 2. 后端配置

```bash
cd backend

# 从模板复制开发环境配置
cp runningerrands-server/src/main/resources/application-template.yml \
   runningerrands-server/src/main/resources/application-dev.yml

# 编辑 application-dev.yml，填写本地数据库、Redis 等信息
# application-dev.yml 已在 .gitignore 中，不会被提交
```

**必须配置的环境变量**（或写在 `application-dev.yml` 中）：

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_PASSWORD` | MySQL 密码 | `your_password` |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey | `LTAI5t...` |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessSecret | |
| `WECHAT_APP_ID` | 微信小程序 AppID | `wx74e8...` |
| `WECHAT_APP_SECRET` | 微信小程序 AppSecret | |
| `TENCENT_MAP_API_KEY` | 腾讯地图 WebService API Key | |
| `RUNNING_ERRANDS_JWT_ADMIN_SECRET` | 管理端 JWT 签名密钥 | 随机 32+ 位字符串 |
| `RUNNING_ERRANDS_JWT_USER_SECRET` | 用户端 JWT 签名密钥 | 随机 32+ 位字符串 |

```bash
# 编译 & 启动
./runningerrands-server/mvnw compile -q -DskipTests
./runningerrands-server/mvnw spring-boot:run
# 服务运行在 http://localhost:8080/api
# Swagger 文档：http://localhost:8080/api/doc.html
# 超管默认账号：admin / admin
```

### 3. 管理端前端

```bash
cd admin
npm install
npm run dev          # http://localhost:3001
```

Vite 自动将 `/api` 开头的请求代理到 `http://localhost:8080`，本地开发无需额外配置。

### 4. 移动端

1. HBuilderX 打开 `mobile/` 目录
2. 修改 `mobile/manifest.json` 中的微信小程序 AppID（`mp-weixin.appid`）
3. 修改 `mobile/utils/config.js` 中的后端地址（本地开发默认 `localhost:8080`）
4. 运行 → 微信小程序

### 5. 管理端 + Nginx 部署

```bash
# 1. 构建前端
cd admin
npm install
npx vite build              # 输出到 dist/

# 2. 将 dist/ 部署到 nginx 静态目录
# 项目已提供开箱即用的 nginx 配置：docker/nginx/nginx.conf
# 核心路由:
#   /api/admin/    → proxy_pass 后端 :8080
#   /api/user/     → proxy_pass 后端 :8080
#   /api/common/   → proxy_pass 后端 :8080
#   /api/ws/       → WebSocket 升级到后端
#   /api/assets/   → dist/assets/ (1 年强缓存)
#   /api/          → dist/index.html (SPA 兜底)

# 3. 启动 nginx
nginx -c F:/ikeu_runningerrands/docker/nginx/nginx.conf

# 4. 访问 http://localhost
# 管理端入口 → SPA 路由接管
# API 请求 → nginx 反向代理到 Spring Boot
```

> 本地开发时推荐直接 `npm run dev`（Vite HMR 热更新），无需 nginx。
> 构建部署用 nginx，生产配置见 [docker/nginx/nginx.conf](docker/nginx/nginx.conf)。

---

## 开发环境配置指南

### 环境架构

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  移动端 uni-app │────→│  后端 Spring Boot  │←────│  管理端 Vue 3  │
│  (微信开发者工具) │     │  :8080/api       │     │  :3001 → proxy │
│  config.js     │     │  application.yml │     │  vite.config.ts│
└──────────────┘     └─────────────────┘     └──────────────┘
```

### 后端环境切换

后端通过 `spring.profiles.active` 切换环境，配置在 `application.yml` 中：

```
application.yml              # 公共配置（所有环境共享）
application-template.yml     # 开发环境模板（提交到 git，新成员复制使用）
application-dev.yml          # 本地开发配置（.gitignore，每个开发者自己维护）
application-test.yml         # 测试环境配置（.gitignore）
application-prod.yml         # 生产环境配置（.gitignore，通过环境变量注入）
```

**切换方式**：

```bash
# 开发环境（默认）
./mvnw spring-boot:run

# 测试环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 生产环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 前端 API 地址配置

项目支持两种开发模式：**本地直连**（推荐，最简单）和 **内网穿透**（需真机调试时使用）。

#### 方式一：本地直连（默认）

##### 管理端（Vite 代理）

编辑 `admin/vite.config.ts`：

```typescript
server: {
  port: 3001,
  proxy: {
    '^/api/(admin|user/|common|ws)': {
      target: 'http://localhost:8080',   // ← 指向你的后端地址
      changeOrigin: true,
    }
  }
}
```

##### 移动端（本地调试）

微信开发者工具内置了对 localhost 的支持，只需：

1. 开发者工具 → 详情 → 本地设置 → 勾选"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"
2. `mobile/utils/config.js` 中 `develop` 环境指向 `http://localhost:8080`

```javascript
develop: {
  SERVER_ORIGIN: 'http://localhost:8080',
}
```

#### 方式二：内网穿透（真机调试 / 外部设备访问）

当需要用真机扫码测试、或外部设备无法直接访问开发机时，使用 ngrok / frp / localtunnel 等内网穿透工具。

##### 1. 启动内网穿透

以 ngrok 为例：

```bash
# 启动 ngrok 隧道 → 后端端口
ngrok http 8080
# 输出：Forwarding  https://xxxx.ngrok-free.dev -> http://localhost:8080
```

##### 2. 修改前端配置

**管理端** `admin/vite.config.ts`：

```typescript
proxy
  '^/api/(admin|user/|common|ws)': {
    target: 'https://xxxx.ngrok-free.dev',   // ← 替换为你的 ngrok 地址
    changeOrigin: true,
    headers: {
      'ngrok-skip-browser-warning': 'true'    // 跳过 ngrok 浏览器警告
    }
  }
```

**移动端** `mobile/utils/config.js`：

```javascript
develop: {
  SERVER_ORIGIN: 'https://xxxx.ngrok-free.dev',   // ← 替换为你的 ngrok 地址
}
```

移动端 `mobile/utils/request.js` 中 `BASE_URL` 和 `WS_URL` 会自动从 `SERVER_ORIGIN` 拼接。WebSocket 协议也会自动从 `https` 转为 `wss`。

> **注意**：隧道地址是个人临时使用的，**不要提交到 git**。

### 移动端微信小程序 AppID 配置

每个开发者必须使用**自己的微信小程序 AppID**：

1. 登录 [微信公众平台](https://mp.weixin.qq.com) → 开发管理 → 开发设置 → 获取 AppID
2. 修改 `mobile/manifest.json`：
   ```json
   {
     "mp-weixin": {
       "appid": "wx你的小程序AppID"
     }
   }
   ```
3. 后端 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET` 需与此 AppID 一致

---

## 完整配置清单

新开发者接入项目时，需要修改以下个人相关配置：

### 后端配置项

| # | 文件 | 配置项 | 说明 |
|---|------|--------|------|
| 1 | `application-dev.yml` | `spring.datasource.*` | MySQL 连接信息 |
| 2 | `application-dev.yml` | `spring.data.redis.*` | Redis 连接信息 |
| 3 | `application-dev.yml` | `runningerrands.alioss.bucket-name` | 替换为自己的 OSS 桶名 |
| 4 | `application-dev.yml` | `runningerrands.sms.sign-name` | 替换为自己的短信签名 |
| 5 | `application-dev.yml` | `runningerrands.sms.template-code` | 替换为自己的短信模板 |
| 6 | `application.yml` | `runningerrands.jwt.admin-secret-key` | 设置随机密钥（通过环境变量） |
| 7 | `application.yml` | `runningerrands.jwt.user-secret-key` | 设置随机密钥（通过环境变量） |
| 8 | `application.yml` | `runningerrands.wechat.app-id` | 微信小程序 AppID |
| 9 | `application.yml` | `runningerrands.wechat.app-secret` | 微信小程序 AppSecret |
| 10 | `application.yml` | `runningerrands.map.api-key` | 腾讯地图 API Key |

### 前端配置项

| # | 文件 | 配置项 | 说明 |
|---|------|--------|------|
| 11 | `admin/vite.config.ts` | `target` | 代理目标后端地址（本地开发默认 localhost:8080） |
| 12 | `mobile/manifest.json` | `mp-weixin.appid` | 替换为你的微信小程序 AppID |
| 13 | `mobile/manifest.json` | `appid` | 替换为你的 DCloud appid |
| 14 | `mobile/utils/config.js` | `develop.trial.release` | 三环境后端地址 |

---

## 认证与权限

```
管理端：token 头 → JwtTokenAdminInterceptor → /admin/**
用户端：authentication 头 → JwtTokenUserInterceptor → /user/**
WebSocket：JwtHandshakeInterceptor → AuthChannelInterceptor
```

| 角色 | 值 | 权限范围 |
|------|-----|---------|
| 超级管理员 | 1 | 全部功能，包括员工管理、操作日志 |
| 普通管理员 | 2 | 仪表盘、用户/跑腿员/任务/订单/流水/消息管理、审核 |

注解权限控制：`@RequireRole({1, 2})` + `RoleCheckAspect` 切面。

---

## 测试

```bash
# E2E 快速验证（28 项 API 断言）
bash e2e/scripts/quick-verify.sh

# 订单全生命周期验证
python e2e/scripts/full_flow.py

# 完整流程编排
bash e2e/scripts/run.sh --quick
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](.claude/CLAUDE.md) | 项目规范 + 编码约定 + 常用命令 |
| [docs/versions/v1.0.0.md](docs/versions/v1.0.0.md) | v1.0.0 发布说明（功能全景 + 安全体系 + 已知限制） |
| [docs/changelogs/CHANGELOG.md](docs/changelogs/CHANGELOG.md) | 版本修复日志 |
| [docs/plans/v1.1-plan.md](docs/plans/v1.1-plan.md) | 后续迭代计划 |
| [docs/test_guide.md](docs/test_guide.md) | 测试环境部署 + E2E 测试指南 |
| [docs/触发路径设计图.md](docs/触发路径设计图.md) | 8 张控制层 → 服务层触发路径图 |
| [docs/项目架构设计图.md](docs/项目架构设计图.md) | Maven 模块 + 前后端 + 技术栈架构图 |
| [nginx.conf](nginx.conf) | 根目录 Nginx 部署配置，开箱即用 |
| [docker/README.md](docker/README.md) | Docker 部署指南 + 预置账号 + SMS 注入 |
| `http://localhost:8080/api/doc.html` | Swagger API 文档（启动后端后访问） |

## 注意事项

> [!IMPORTANT]
> 本项目当前处于 **个人开发者预览阶段**，以下功能受限于企业资质，已使用替代方案实现：

| 受限项 | 原因 | 当前替代方案 |
|--------|------|-------------|
| **微信支付** | 需要企业认证的小程序才能接入微信支付 API | 钱包余额为模拟数据；充值/提现接口已就绪但未对接真实支付渠道 |
| **支付 UI** | 支付接口未接入导致前端支付流程无法完整体验 | 支付密码弹窗、余额展示、流水记录等 UI 均已完成，仅缺后端对接收银台 |
| **SMS 短信** | 阿里云 SMS 需要已备案的企业主体 | Docker 环境通过 Redis 直写验证码；开发环境可复用 `e2e/scripts/bootstrap.sh` 注入 |
| **微信 OAuth** | 需要已上架的微信小程序 | 本地调试时可通过手机号验证码登录绕过；`DockerTestDataInitializer` 预置了测试账号 |

> 上述限制不影响项目 **核心业务流程**（注册→认证→跑腿员→发布→接单→取货→送达→完成→评价）的完整跑通和验证。

## 后续计划

| 优先级 | 功能 | 计划版本 |
|:--:|------|:--:|
| P0 | 排行榜 UI、账户注销页、附近任务（LBS）、任务统计 | v1.1 |
| P1 | 腾讯地图 SDK 接入（选点 + 路径规划） | v1.1 |
| P2 | 任务发布页组件化、订单详情 VO 统一 | v1.2 |
| P3 | 数据导出、仪表盘增强 | v1.2 |

详见 [docs/plans/v1.1-plan.md](docs/plans/v1.1-plan.md)。

---

## License

[MIT](LICENSE) © 2025–2026 ikeu
