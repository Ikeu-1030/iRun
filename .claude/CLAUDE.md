# 小i跑腿 — runningerrands

校园跑腿服务平台：用户发布代取快递、代拿餐食、校内代办、代购物品等任务，跑腿员接单配送。包含微信小程序端（uni-app）、管理后台（Vue 3）、Spring Boot 后端。

## 技术栈

| 层 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.0, Java 21, MyBatis-Plus 3.5.5, MySQL 8, Redis, Redisson |
| 管理端 | Vue 3, TypeScript, Vite, Element Plus, ECharts 5, Pinia, Axios |
| 移动端 | uni-app (Vue 3), 微信小程序, Pinia, STOMP over WebSocket |
| 认证 | JWT 双令牌（access + refresh），管理端与用户端密钥/拦截器隔离 |
| 文档 | Knife4j (Swagger), springdoc-openapi |

## 项目结构

```
F:/ikeu_runningerrands/
├── backend/                              # Maven 多模块后端
│   ├── pom.xml                           # 父 POM（Group: com.ikeu, Artifact: runningerrands-parent）
│   ├── runningerrands-common/            # 共享工具：constant, context, enums, exception, properties, result, utils
│   ├── runningerrands-model/             # 数据模型：entity, dto, vo
│   ├── runningerrands-server/            # Spring Boot 应用：controller, service, mapper, config, interceptor, aspect
│   ├── runningerrands.sql               # 数据库建表脚本（14 表 + ALTER）
│   ├── Dockerfile                        # 多阶段 Docker 构建
│   └── .dockerignore
├── admin/                                # 管理端（Vue 3 + TS + Element Plus）
│   └── src/
│       ├── api/                          # 10 模块按领域拆分
│       ├── views/                        # 13 视图（用户/跑腿员/任务/订单/流水/通知/日志/员工/审核/仪表盘/登录/404/设置）
│       ├── router/                       # history /api/，beforeEach 守卫 + 角色过滤
│       ├── stores/                       # Pinia：auth（token/adminInfo） + app（侧边栏）
│       ├── utils/                        # request.ts（Axios + 双 token 刷新队列）、constants.ts、tokenStore.ts
│       └── styles/theme.css              # CSS 设计令牌
├── mobile/                               # 移动端（uni-app 微信小程序）
│   ├── pages.json                        # 34 页面注册 + 5 标签自定义 TabBar
│   ├── api/index.js                      # 10 API 模块统一导出
│   ├── store/                            # Pinia：主 Store + 聊天 Store（STOMP 管理）
│   ├── utils/                            # request.js, stomp.js, config.js, draft-save.js, campus-data.js 等
│   └── components/                       # custom-navbar, custom-tabbar, pay-password-dialog 等
├── docker/                               # Docker 部署配置
│   ├── docker-compose.yml               # MySQL 8 + Redis 7 + Backend
│   └── .env.example                     # 环境变量模板
├── docs/
│   ├── pre-1.0.0/                       # 1.0 发布前工作记录（按日期）
│   ├── changelogs/                      # 发布后修复日志
│   ├── versions/                        # 版本发布日志（v1.0.0 →）
│   ├── plans/                           # 后续迭代计划
│   ├── test_guide.md                    # 测试指南（含 E2E 全链路测试）
│   └── nginx-config.md                  # Nginx 部署配置
├── e2e/                                  # E2E 全链路测试
│   ├── .env.e2e                          # 测试环境变量
│   └── scripts/                         # setup, bootstrap, run, teardown, quick-verify, full-flow
├── .agent/                               # 子代理定义（16 个）
└── .claude/
    ├── CLAUDE.md                         # ← 本文件
    └── settings.local.json               # 本地命令自动批准
```

## 编码规范

### Java 后端

- **包结构**: `com.ikeu.{module}.{layer}`，controller 按 `admin`/`user` 分包
- **注入**: `@RequiredArgsConstructor` 构造器注入，禁止字段注入
- **返回**: 统一使用 `Result<T>` / `PageResult<T>`（`com.ikeu.common.result`）
- **实体**: MyBatis-Plus `@TableName` + `@TableId(type = IdType.AUTO)`，`LocalDateTime` 映射自动开启
- **鉴权**: 管理端和用户端采用双token认证，`JwtTokenAdminInterceptor` 从 `token` 头提取，`JwtTokenUserInterceptor` 从 `authentication` 头提取
- **缓存策略**:
  - *仪表盘* — `AdminDashboardService` 5 个方法各用 `@Cacheable(value = "admin:dashboard", key = "...")` 独立缓存，写操作（订单/任务/用户/跑腿员变更）触发 `@CacheEvict(allEntries = true)` 全量清除
  - *任务大厅/详情* — `@RedisDefend` + `RedisDefendUtil.getOrLoad()` 组合防护（缓存穿透 + 击穿），空结果用 `TASK_HALL_NULL_PREFIX` 标记防穿透，`TASK_HALL_LOCK_KEY` 互斥锁防击穿，多页缓存 key 为 `page:size`
  - *排行榜* — `@Cacheable(value = "runner:leaderboard", key = "#sortBy + ':' + #limit")`
  - *过期策略* — 任务缓存 TTL 600s，仪表盘缓存默认 TTL
  - *分布式锁* — Redisson `RLock` 手动加锁，锁前缀定义在 `RedisConstant`（接单/超时/自动结算/通知清理），等待 3s 持有 10s
  - *登录保护* — Redis 失败计数，5 次错误锁定 300s
- **SQL 同步**: 实体字段变更时同步更新 `runningerrands.sql`（CREATE TABLE + ALTER TABLE 语句）
- **日志安全**: 不在日志中打印 token 明文、密码等敏感信息
- **异常消息**: 所有 `throw new BusinessException(...)` 等异常消息必须使用 `MessageConstant` 中定义的常量，禁止硬编码字符串（工具类中动态拼接的运行时异常除外）
- **SMS 验证码**: Redis key 格式为 `user:code:{operation}:{phone}`，operation 取值 `login`/`register`/`change_phone`/`reset_password`/`reset_pay_password`，`sendCode()` 内 `VALID_CODE_OPERATIONS` 白名单校验，5 个消费者各读各的 key 防跨操作复用
- **密码重置保护**: `resetPassword()`/`resetPayPassword()` 有失败计数锁定（`user:reset:pwd:fail:{userId}` / `user:reset:paypwd:fail:{userId}`），5 次/300s 锁定，与登录保护同模式
- **注释**: 采用JavaDoc注释，遵循阿里巴巴注释规范，类注释写明类的作用，标注作者(`@author`)和创建日期(`@since`)，方法注释注明方法逻辑，参数(`@param`)以及返回值(`@return`)，重要方法嵌入HTML标签详细描述

### Vue 3 管理端

- **技术**: Vue 3.5 Composition API (`<script setup>`), TypeScript 6.0, Vite 8, Pinia 3, Vue Router 4, Element Plus 2.14, ECharts 6 + vue-echarts 8, GSAP 3
- **目录结构**:
  - `api/` — 11 模块按领域拆分（auth, dashboard, employees, logs, notifications, orders, runners, tasks, transactions, users, settings）
  - `views/` — 13 视图按模块分目录，列表页统一 `el-table` + `el-pagination` 模式，详情页 v2 块式布局（`el-card` + `--anim-order` CSS 序列动画）
  - `stores/` — `app.ts`（侧边栏折叠）+ `auth.ts`（token/adminInfo/login/logout），组合式 API 风格
  - `utils/` — `request.ts`（Axios + 双 token 刷新队列）、`constants.ts`（状态枚举映射）、`task-specs-parser.ts`（任务规格 JSON → 可读摘要）、`tokenStore.ts`
  - `styles/theme.css` — CSS 自定义属性设计系统，覆盖 Element Plus 变量
  - `components/` — 目录存在但为空，所有 UI 直接用 Element Plus 组件
- **路由**: history base `/api/`，`meta.role: [1]` 限制超管路由，全局 `beforeEach` 守卫检查 token（无 token → `/login`，有 token → `/dashboard`），角色过滤在 `AdminLayout` 侧边栏 `visibleMenu` 中完成
- **HTTP**: Axios 实例，请求拦截器附加 `token` 头（非 `Authorization: Bearer`），响应拦截器解包 `code === 1` 返回 `data`，`code !== 1` toast 错误；并发 401 使用 `isRefreshing` + `refreshQueue` 队列防止并行刷新竞争
- **认证**: access + refresh 双 token 存 localStorage（`admin_token` / `admin_refresh_token`），刷新后同步更新 `auth` store 中的 `adminInfo`
- **动画**: 登录页、仪表盘计数器、404 页使用 GSAP；详情页 v2 使用纯 CSS `@keyframes`（`slideInLeft`、`fadeUp`、`imgPop`）通过 `:class="{ entered }"` + `--anim-order` 控制序列
- **入口**: `main.ts` 全局注册 Element Plus（中文 locale）+ 全部 `@element-plus/icons-vue`，无需手动导入图标

### uni-app 移动端

- **技术**: uni-app (Vue 3), 微信小程序, Pinia, 自制 STOMP 1.2 WebSocket 客户端
- **页面**: 34 个页面（`pages.json` 注册），全 `navigationStyle: "custom"`，5 标签自定义 TabBar（毛玻璃效果 `backdrop-filter: blur(20px)`），`uni.switchTab` / `uni.navigateTo` 导航
- **API 层**:
  - `api/index.js` — 统一命名空间导出 10 个模块（user, task, order, chat, runner, address, transaction, notification, review, common）
  - `utils/request.js` — 导出 `get/post/put/del` 便捷方法，`auth` 参数控制认证类型（`'user'`/`'admin'`/`'none'`）
  - 上传使用 `uni.uploadFile`（非 `uni.request`），手动注入 `authentication` 头
- **HTTP 认证**:
  - 双 token 体系：用户端用 `authentication` 头，管理端用 `token` 头
  - token 持久化键：`d2d_user_token` / `d2d_user_refresh_token` / `d2d_admin_token` / `d2d_admin_refresh_token`
  - 401 自动刷新：`isRefreshing` + 请求队列，刷新失败清除 token 重定向登录页
  - 响应解包：`code === 1` 返回 `data`（分页返回 `{ total, records }`），日期自动 `formatDates()` 去 `T`
- **Store**:
  - `store/index.js` — 主 Store（options API），管理登录态、用户信息（16 字段含 balance/isCertify）、`hasPayPassword` 持久化；login/logout 时自动连接/断开 STOMP
  - `store/chat.js` — 聊天 Store（composition API），全局单例管理 STOMP 连接、消息缓存（`Map<peerUserId, messages[]>`）、乐观消息替换、去重、分页历史
- **WebSocket/STOMP**: 自制 `StompClient` 类（`utils/stomp.js`），基于 `uni.connectSocket`，手动构建/解析 STOMP 帧，心跳 10s（发送 `\n`），连接超时 10s，3 次重试后尝试 refresh token，微信连接数限制检测
- **错误处理**: `ClassifiedError`（Symbol-branded，9 种 `ErrorType`），`classifyError()` 按 HTTP 状态码/业务码/网络异常三级分类，`handlePageError()` 支持 `customHandlers` 映射，`showError` + `handled` 标志防重复 toast
- **支付密码**: 全局 promise 式弹窗 `promptPayPassword(title)`，共享 `ref()` 状态驱动 `<pay-password-dialog>` 组件，6 位数字键盘输入，store 持久化 `hasPayPassword` 布尔标记（不存储密码原文）
- **工具模块**:
  - `config.js` — 动态环境检测（微信 `envVersion` → develop/trial/release 自动切换后端地址）
  - `constants.js` — 状态码枚举 + 徽章样式映射 + API 序列化/反序列化辅助函数
  - `toast.js` — 智能 toast（短消息带图标、中消息 `icon:'none'`、长消息自动降级 `uni.showModal`）
  - `submit-guard.js` — `useSubmitLock` 防重复提交 composable
  - `task-normalizer.js` — API 记录 → 统一任务卡片格式
  - `campus-data.js` — 双校区硬编码数据（建筑/驿站）+ `buildAddressString` / `parseDeliveryAddress` 纯前端地址解析
  - `custom-icons.js` — 阿里巴巴 iconfont 内联 SVG 图标字典
- **样式**: `App.vue` 中 CSS 自定义属性设计令牌，暖珊瑚橙主色调（`#FF6B4A`）+ 青绿强调色（`#2EC4B6`），暖白表面层级（background/surface/raised），7 种入场动画关键帧（`fadeInUp`、`scaleIn`、`slideInRight` 等），通用 `.card` / `.badge` / `.safe-area-bottom` 工具类
- **组件**: `custom-icon`（内联 SVG）、`custom-navbar`、`custom-tabbar`（毛玻璃）、`iconpark-icon`、`pay-password-dialog`（支付密码弹窗）、`upload-grid`（图片上传网格）

## 代理使用指南

**优先使用子代理处理以下任务**，避免主对话上下文膨胀：

| 场景 | 代理 | 命令示例 |
|------|------|---------|
| 阅读/探索大型代码库 | `Explore` agent | 搜索文件、grep 关键字、理解项目结构 |
| 全面代码审查（PR 前） | `deep-code-reviewer` | 静态分析 + 自动修复 |
| 安全审查 | `security-auditor` | 漏洞检测、威胁建模 |
| Java 注释规范化 | `java-comment-enforcer` | Alibaba 规范 Javadoc |
| 测试生成 | `autonomous-test-generator` | 自动生成 + 迭代调试 |
| Docker API 自动化测试 | `docker-test-agent` | 启动容器 → 注入数据 → 跑测试 → 报告 |
| 业务流程全链路跟踪 | `e2e-business-flow-tracker` | 注册→认证→发布→接单→完成 全流程验证 |
| 错误排查 | `sre-debugger` | 堆栈跟踪分析、根因定位 |
| 多文件重构 | `Plan` agent | 先设计方案再编码 |
| 文档同步 | `doc-sync-monitor` | 接口变更后自动更新文档 |

**原则**: 预计需要 3 次以上搜索/读取的任务，直接委派给子代理，结果汇总后继续。

## 常用命令

```bash
# ===== 后端 =====
cd F:/ikeu_runningerrands/backend

# 编译（静默）
./runningerrands-server/mvnw compile -q -DskipTests

# 安装全部模块
./runningerrands-server/mvnw install -q -DskipTests

# 启动服务
./runningerrands-server/mvnw spring-boot:run

# ===== 管理端前端 =====
cd F:/ikeu_runningerrands/admin

npm run dev           # http://localhost:3001 (代理后端 API: /api/admin|user/|common|ws → 8080，前端 HMR)
npx vue-tsc --noEmit  # 类型检查
npx vite build         # 构建 → dist/
# 构建后部署到后端: cp -r dist/* ../backend/runningerrands-server/src/main/resources/static/

# ===== Docker 部署 =====
cd F:/ikeu_runningerrands/docker

docker compose up -d       # 启动全部服务（MySQL 8 + Redis 7 + Backend）
docker compose ps           # 查看服务状态
docker compose logs -f backend  # 查看后端日志
docker compose down -v      # 停止并清理数据

# 注入验证码测试（Docker 环境无阿里云 SMS）
docker exec rr-redis redis-cli -n 1 SET "user:code:reset_password:13800000001" "888888" EX 300

# ===== E2E 测试 =====
cd F:/ikeu_runningerrands

bash e2e/scripts/quick-verify.sh       # 28 项 API 快速验证（~1min）
python e2e/scripts/full_flow.py        # 订单全生命周期验证
bash e2e/scripts/run.sh --quick        # 编排入口（setup → bootstrap → 执行 → teardown）
bash e2e/scripts/teardown.sh           # 清理测试数据

# ===== 移动端 =====
# 在 HBuilderX 中打开 mobile/，运行 → 微信小程序
```

## 数据库

- 数据库名: `runningerrands`
- 初始化: 执行 `runningerrands.sql`，`AdminInitializer` 启动时自动创建超管
- 超管默认: `admin` / `admin`（可通过 `runningerrands.admin.init.*` 配置覆盖）
- 密码存储: BCrypt（`PasswordConfiguration` → `BCryptPasswordEncoder`）

## 注意事项

> 项目未企业认证，以下功能受限：**微信支付** 接口已就绪但未对接真实渠道（余额为模拟数据）；**SMS 短信** 无阿里云服务（Docker 通过 Redis 直写验证码，开发环境复用 `e2e/scripts/bootstrap.sh` 注入）；**微信 OAuth** 需已上架小程序（本地通过手机验证码登录绕过）。核心业务流程不受影响。

## 关键约定

- 每次写代码前先计划思考，切勿直接动手，仔细分析代码链路关系，优先参考`/skills/karpathy-skills`
- Git commit请先询问，允许后才可提交，切勿直接提交
- **Git 分支规则**：代码提交至 `master` 或 feature 分支，通过 PR 合入 `main`，**禁止直接 push 到 `main` 分支**
- Maven wrapper (`mvnw`) 位于 `runningerrands-server/`，从 `backend/` 根目录使用
- Git Bash 路径和Windows 盘符用正斜杠 `/`
- 上下文路径 `/api`，管理端路径 `/admin/**`
- 数据库变更必须同步 `runningerrands.sql`
- 新建 controller 注意包位置：管理端 → `controller/admin/`，用户端 → `controller/user/`，通用 → `controller/`
