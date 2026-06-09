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
│   └── runningerrands.sql               # 数据库建表脚本
├── admin/                                # 管理端（Vue 3 + TS + Element Plus）
├── mobile/                               # 移动端（uni-app 微信小程序）
└── .claude/
    ├── CLAUDE.md                         # ← 本文件
    └── settings.local.json               # 本地命令自动批准
```

## 编码规范

### Java 后端

- **包结构**: `com.ikeu.{module}.{layer}`，controller 按 `admin`/`user` 分包
- **注入**: `@RequiredArgsConstructor` 构造器注入，禁止字段注入
- **返回**: 统一使用 `Result<T>` / `PageResult<T>`（`com.ikeu.common.result`）
- **异常**: 业务异常抛 `BusinessException`（→ 400），`NotFoundException`（→ 404），由 `GlobalExceptionHandler` 统一处理
- **实体**: MyBatis-Plus `@TableName` + `@TableId(type = IdType.AUTO)`，`LocalDateTime` 映射自动开启
- **鉴权**: 管理端 `@RequireRole({1, 2})`（1-超管 2-普通管理员），`JwtTokenAdminInterceptor` 从 `token` 头提取
- **日志**: 操作日志用 `@OperationLog(module, action, description)`，参数支持 `#paramName` 占位符
- **缓存**: 仪表盘用 Spring Cache（`RedisConstant.CACHE_DASHBOARD`），数据变更时 `@CacheEvict`
- **SQL 同步**: 实体字段变更时同步更新 `runningerrands.sql`（CREATE TABLE + ALTER TABLE 语句）
- **task_specs**: 任务规格存储为 JSON 列，key 统一使用中文（`包裹列表`、`商家`、`餐品`、`服务时长`、`服务截止时间` 等），禁止英文 key；`配送费` 由后端 `mergeTaskSpecs()` 注入，不作为独立列
- **日志安全**: 不在日志中打印 token 明文、密码等敏感信息
- **注释**: 采用JavaDoc注释，遵循阿里巴巴注释规范，类注释写明类的作用，标注作者(`@author`)和创建日期(`@since`)，方法注释注明方法逻辑，参数(`@param`)以及返回值(`@return`)，重要方法嵌入HTML标签详细描述

### Vue 3 管理端

- **目录**: `views/` 按模块分目录，`api/` 一个模块一个文件，`stores/` 按职责拆分
- **HTTP**: 统一使用 `@/utils/request`（Axios 实例，自动附加 `token` 头、401 刷新、错误 toast）
- **样式**: Element Plus 组件 + scoped CSS，不引入额外 CSS 框架
- **角色**: 侧边栏菜单通过 `roles` 字段控制可见性，页面默认无需额外角色检查

### uni-app 移动端

- **API**: `utils/request.js` 封装，支持 `auth: 'user'/'admin'/'none'`
- **Store**: Pinia `defineStore('main', ...)`，用户信息/钱包/跑腿员状态
- **错误**: `ClassifiedError` + `ErrorType` 枚举，`handlePageError()` 统一处理
- **支付密码**: 通过 `promptPayPassword()` 弹窗获取，不在页面中持久化

## 代理使用指南

**优先使用子代理处理以下任务**，避免主对话上下文膨胀：

| 场景 | 代理 | 命令示例 |
|------|------|---------|
| 阅读/探索大型代码库 | `Explore` agent | 搜索文件、grep 关键字、理解项目结构 |
| 全面代码审查（PR 前） | `deep-code-reviewer` | 静态分析 + 自动修复 |
| 安全审查 | `security-auditor` | 漏洞检测、威胁建模 |
| Java 注释规范化 | `java-comment-enforcer` | Alibaba 规范 Javadoc |
| 测试生成 | `autonomous-test-generator` | 自动生成 + 迭代调试 |
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

npm run dev           # http://localhost:3001 (代理 /api → 8080)
npx vue-tsc --noEmit  # 类型检查
npx vite build         # 构建

# ===== 移动端 =====
# 在 HBuilderX 中打开 mobile/，运行 → 微信小程序
```

## 数据库

- 数据库名: `runningerrands`
- 初始化: 执行 `runningerrands.sql`，`AdminInitializer` 启动时自动创建超管
- 超管默认: `admin` / `admin`（可通过 `runningerrands.admin.init.*` 配置覆盖）
- 密码存储: BCrypt（`PasswordConfiguration` → `BCryptPasswordEncoder`）

## 关键约定

- 每次写代码前先计划思考，切勿直接动手，仔细分析代码链路关系，优先参考`/skills/karpathy-skills`
- Git commit请先询问，允许后才可提交，切勿直接提交
- Maven wrapper (`mvnw`) 位于 `runningerrands-server/`，从 `backend/` 根目录使用
- Git Bash 路径用正斜杠 `/`，Windows 盘符 `F:/`
- 上下文路径 `/api`，管理端路径 `/admin/**`
- 数据库变更必须同步 `runningerrands.sql`
- 新建 controller 注意包位置：管理端 → `controller/admin/`，用户端 → `controller/user/`，通用 → `controller/`
