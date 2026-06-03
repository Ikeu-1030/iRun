# 小i跑腿 — runningerrands CLAUDE.md

校园跑腿服务平台，后端 Spring Boot 3.2 + MyBatis-Plus，管理端 Vue 3 + TypeScript，移动端 uni-app。

## 项目结构

```
F:/ikeu_runningerrands/
├── backend/
│   ├── pom.xml                          # 父 POM（Maven 多模块）
│   ├── runningerrands-common/           # 共享：常量、异常、工具类、JWT、properties
│   ├── runningerrands-model/            # 实体、DTO、VO
│   ├── runningerrands-server/           # Spring Boot 应用
│   │   └── src/main/java/com/ikeu/server/
│   │       ├── controller/              # CommonController（根）
│   │       │   ├── admin/               # 管理端接口（/admin/**）
│   │       │   └── user/                # 用户端接口
│   │       ├── service/ + impl/
│   │       ├── mapper/                  # MyBatis-Plus Mapper
│   │       ├── config/                  # 配置类 + AdminInitializer
│   │       ├── interceptor/             # JWT 拦截器（admin + user 分离）
│   │       ├── aspect/                  # 切面：通知、操作日志、角色检查
│   │       ├── annotation/              # @SendNotification, @OperationLog, @RequireRole
│   │       └── task/                    # 定时任务
│   └── runningerrands.sql              # 建库建表 + ALTER 语句
├── frontend/
│   ├── admin/                           # Vue 3 + TS + Element Plus 管理端
│   └── mobile/                          # uni-app 微信小程序
└── .claude/
    └── settings.local.json              # 本地命令自动批准
```

## 角色与权限

| 角色 | 值 | Admin 表 | 权限 |
|------|----|---------|------|
| 超级管理员 | 1 | admin.role | 全部功能，包括员工管理、操作日志、禁止跑腿员接单 |
| 普通管理员 | 2 | admin.role | 仪表盘、用户/跑腿员/任务/订单/流水/消息管理、审核 |

- 仅超管可对普通管理员 CRUD；超管不能停用/删除自己
- 创建员工强制 role=2，超管仅一人
- `@RequireRole` 注解 + `RoleCheckAspect` 切面实现 RBAC

## 关键注解

- `@RequireRole({1, 2})` — 角色权限（默认仅超管）
- `@OperationLog(module, action, description)` — 自动记录操作日志
- `@SendNotification` — 方法成功后自动推送站内通知

## 常用命令

```bash
# 后端编译
cd F:/ikeu_runningerrands/backend && ./runningerrands-server/mvnw compile -q -DskipTests

# 后端启动
cd F:/ikeu_runningerrands/backend && ./runningerrands-server/mvnw spring-boot:run

# 管理端前端
cd F:/ikeu_runningerrands/frontend/admin
npm run dev          # 开发服务器（端口 3001，代理 /api → localhost:8080）
npx vue-tsc --noEmit # TypeScript 类型检查
npx vite build        # 生产构建

# 移动端（HBuilderX 项目，在 IDE 中编译运行）
```

## 可用技能 / Skills

| 技能 | 用途 |
|------|------|
| `/code-review` | 代码审查 |
| `/code-simplifier` | 代码精简化 |
| `/security-guidance` | 安全审查 |
| `/skill-creator` | 创建新技能 |
| `/commit-commands` | Git 提交辅助 |
| `/feature-dev` | 功能开发流程 |
| `/doc-coauthoring` | 文档协作 |

## 可用子代理 / Agents

| 代理 | 用途 |
|------|------|
| `code-reviewer` | 深度代码审查（正确性/可读性/架构/安全/性能） |
| `security-auditor` | 专项安全审计 |
| `test-engineer` | 测试策略与用例生成 |
| `code-simplifier` | 代码简化与重构 |
| `deep-code-reviewer` | 综合静态分析 + 自动修复 |
| `doc-sync-monitor` | 代码变更后同步文档 |
| `java-comment-enforcer` | Java 注释规范化 |
| `sre-debugger` | 错误日志/堆栈跟踪根因分析 |
| `understand-anything:*` | 代码库架构/领域知识分析 |
