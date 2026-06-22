# 2026/06/16 — 支付密码重构 + 常量类规范化 + 安全加固 + Docker 部署

## 完成工作

### 1. 常量类规范化

6 个常量类统一为 `public final class` + 私有构造器：

| 类 | 修改 |
|---|------|
| `StatusConstant` | ➕ `final`, ➕ 私有构造器 |
| `RedisConstant` | ➕ `final`, ➕ 私有构造器 |
| `TaskTypeConstant` | ➕ `final`（已有私有构造器） |
| `MessageConstant` | ➕ `final`, ➕ 私有构造器 |
| `JwtClaimsConstant` | ➕ `final`, ➕ 私有构造器 |
| `TaskSpecKeys` | 已是标杆 |

提交：`b201b4b`

### 2. 支付密码系统重构

**变更范围**：后端 6 文件 + 前端 4 文件

#### 首次设置支付密码 → 无需校验

- `SetPayPasswordDTO` 删除 `loginPassword`/`code` 字段，仅保留 `payPassword`（加 `@Pattern(regexp = "^\\d{6}$")` 服务端校验）
- `UserServiceImpl.setPayPassword` 精简为：查用户 → 判重复 → 直接加密存储（-25 行）
- 前端 `set` 模式移除微信用户/普通用户分支和全部验证字段

#### 忘记密码 → 短信验证码重置

| 端点 | 用途 | 校验链 |
|------|------|--------|
| `PUT /user/password/reset` | 重置登录密码 | userId 空值 → 用户存在 → 手机号归属 → SMS 验证码 |
| `PUT /user/pay-password/reset` | 重置支付密码 | 同上 |

- `ResetPasswordDTO` 复用为两种重置（`phone` + `code` + `newPassword`）
- 手机号归属校验：`user.getPhone().equals(dto.getPhone())` 防跨账户攻击
- `userId` 空值防御统一：`setPayPassword`/`changePayPassword`/`hasPayPassword` 补齐守卫

#### 前端交互

- `pay-password-edit.vue`：`login`/`change` 模式左下角蓝色"忘记密码？"链接 → 切换 `forgot` 模式（区分 login/pay 目标）
- `privacy-security.vue`：移除独立"忘记登录密码"入口（改为内联链接）
- `api/user.js`：`setPayPassword` 精简为单参数 + 新增 `resetPassword`/`resetPayPassword`

提交：`4c4b8e2`

### 3. 缺陷修复

#### 草稿自动保存 — 完整修复（7 项）

| # | 问题 | 根因 | 修复 |
|---|------|------|------|
| 1 | 退出再返回草稿丢失 | `onUnload` 未调 `persist()`，navigateBack/switchTab 路径草稿从未写入 | `onUnload` 补 `persist()` → `dispose()` |
| 2 | 切 tab 回来草稿丢失 | `dispose()` 内 `clearDraft()` 误删持久化数据 | `dispose()` 移除 `clearDraft()`，草稿生命周期由 TTL 管理 |
| 3 | 不同子页面草稿穿透 | 共享 ref 未重置 + subType 变化触发 auto-save 把旧值写入新 key | 动态 key `draft_sp_${type}_${subType}` 隔离 + 切换时重置 22 个共享 ref + 删除旧 key |
| 4 | 3 个字段未被草稿跟踪 | `privateExpressNote`/`privateFoodNote`/`itemWeight` 漏加入 `formRefs` | 补入 formRefs 和 reset 列表 |
| 5 | `subType` 在 formRefs 中有重入风险 | restoreDraft 写 subType 会触发 watch 循环 | 从 formRefs 移除（key 已编码 subType） |
| 6 | `deadlineDate`/`deadlineTime` 切换时未重置 | 漏加入 reset 列表 | 补入 |
| 7 | catch 块静默吞错 | 4 处 `catch(_){}` 无日志 | 改为 `console.warn('[draft-save]', e.message)` |

#### 组件 CSS 丢失

| # | 组件 | 修复 |
|---|------|------|
| 4 | `FeeCard.vue` | 新增 `<style scoped>` 含 `.fee-row`/`.chip`/`.custom-tip-*` 等全部依赖样式 |
| 5 | `GenderRestriction.vue` | 新增 `<style scoped>` 含 `.form-card`/`.chip-row`/`.chip--active` |

根因：`1e3848e` 提取组件时 CSS 留在父组件 `<style scoped>` 中，Vue scoped 不穿透子组件内部元素。

#### FeeCard NaN 守卫

`v-model.number` 清空输入时 emit `NaN` → `isNaN(val) ? 0 : val` 守卫。

提交：`aef7f2b` / `e88fe23`

### 4. 安全加固

#### S1 — SMS 验证码操作作用域化

- `SendCodeDTO` ➕ `operation` 字段，`sendCode()` 内 `VALID_CODE_OPERATIONS` 白名单校验
- Redis key 从 `user:code:{phone}` → `user:code:{operation}:{phone}`（5 种操作隔离）
- 5 个消费者（register/login/changePhone/resetPassword/resetPayPassword）各读各的 key
- 前端 4 个调用点传入对应 operation

#### S2 — 密码重置暴力破解防护

- `resetPassword`/`resetPayPassword` 新增失败计数锁定（`user:reset:pwd:fail:{userId}` / `user:reset:paypwd:fail:{userId}`）
- 参照 `login()` 模式：5 次/300s 锁定，成功后清除计数
- 复用 `LOGIN_MAX_FAIL_COUNT` / `LOGIN_LOCK_SECONDS`

#### Minor — 登录锁定文案修复

"请15分钟后再试" → `LOGIN_FAIL_LOCKED_USER` 常量（"请5分钟后再试"），与 `LOGIN_LOCK_SECONDS=300` 一致。

#### M1 — 后端异常消息统一封装

10 处硬编码 → `MessageConstant`，新增 9 个常量，涉及 5 文件：

| 文件 | 替换处 |
|------|:--:|
| `UserServiceImpl` | 1 |
| `WeChatAuthUtil` | 2 |
| `ChatServiceImpl` | 1 |
| `OrderStateMachine` | 3 |
| `TaskStateMachine` | 3 |
| `RunnerProfileServiceImpl` | 2 |

提交：`20365cc`

Docker 实测通过：S1 跨操作拒绝 ✅ / S2 5 次锁定 ✅ / 密码重置 ✅

### 5. Docker 部署

**新建文件：**

| 文件 | 用途 |
|------|------|
| `backend/Dockerfile` | 多阶段构建：Maven 3.9 + Temurin 21 → JRE 21 Alpine，含 Healthcheck |
| `backend/.dockerignore` | 排除 target/.git/node_modules 等 |
| `docker/docker-compose.yml` | MySQL 8 + Redis 7 Alpine + Backend，网络隔离，端口 3308/6381 |
| `docker/.env.example` | 全部可配环境变量模板 |
| `docker/README.md` | Docker 部署指南（快速启动/预置账号/API 测试/Redis 注入验证码） |
| `backend/.../application-docker.yml` | Docker profile：容器主机名、跳过 SMS/OSS/微信 |
| `backend/.../config/DockerTestDataInitializer.java` | `@Profile("docker")` 启动时创建 testuser/testuser2/admin |

**预置测试账号：**

| 用户名 | 密码 | 手机号 | 跑腿员 | 支付密码 |
|--------|------|--------|:-----:|:------:|
| `testuser` | `123456` | `13800000001` | ✅ | ✅ |
| `testuser2` | `123456` | `13800000002` | ❌ | ✅ |
| `admin` | `admin` | — | — | — |

提交：`cb9df90` / `6444143`

### 6. 代码审查（2 轮）

**第 1 轮** — `deep-code-reviewer`：8 项发现（1 CRITICAL 为设计决策，2 项已修复，2 项已记录为安全加固计划）

**第 2 轮** — 前端审查 `agent-skills:code-reviewer`：8 项发现（2 项已修复 — 注释更正 + NaN 守卫，2 项后续优化 — CSS 去重 + TTL 可配置）

### 7. 文档同步

- `docs/2026-06-09-fix-summary.md` — P0/P1/P2 全部标记完成，P3 更新状态
- `docs/2026-06-10-refactor-summary.md` — P0/P1b/P3 标记完成，其余标记进行中
- `docs/2026-06-15-work-summary.md` — P0 标记完成，新增 P0.5 安全加固计划，Git 记录补充
- `docs/2026-06-16-work-summary.md` — 本文件（含汇总）
- `.claude/CLAUDE.md` — 同步 Docker、安全规范、SMS 作用域等最新约定

---

## 涉及文件

```
backend/
  Dockerfile / .dockerignore / runningerrands.sql
  runningerrands-common/.../constant/
    MessageConstant.java              (+10 常量，安全加固 + M1)
    RedisConstant.java                (+2 常量，S2)
    StatusConstant.java               (final)
    TaskTypeConstant.java             (final)
    JwtClaimsConstant.java            (final)
  runningerrands-common/.../enums/
    TaskStateMachine.java             (硬编码→MessageConstant)
    OrderStateMachine.java            (硬编码→MessageConstant)
  runningerrands-common/.../utils/
    WeChatAuthUtil.java               (硬编码→MessageConstant)
  runningerrands-model/.../dto/
    SendCodeDTO.java                  (+operation, S1)
    SetPayPasswordDTO.java            (精简)
    ResetPasswordDTO.java             (新建 — 复用)
  runningerrands-server/.../
    config/DockerTestDataInitializer.java (新建)
    config/AdminInitializer.java
    controller/user/UserController.java  (+2 端点)
    service/UserService.java             (+2 方法签名)
    service/impl/UserServiceImpl.java    (S1+S2 核心 + M1)
    service/impl/ChatServiceImpl.java    (M1)
    service/impl/RunnerProfileServiceImpl.java (M1)

docker/
  docker-compose.yml / .env.example / README.md

mobile/
  api/user.js                        (sendCode 签名 +2 新函数)
  pages/login/login.vue              (sendCode +operation)
  pages/pay-password-edit/           (四模式重构 + sendCode +operation)
  pages/phone-edit/phone-edit.vue    (sendCode +operation)
  pages/service-publish/service-publish.vue  (草稿隔离)
  components/
    FeeCard.vue                      (scoped CSS + NaN 守卫)
    GenderRestriction.vue            (scoped CSS)
  utils/draft-save.js                (TTL + onUnload + 隔离)

docs/
  2026-06-09-fix-summary.md / 2026-06-10-refactor-summary.md
  2026-06-15-work-summary.md / 2026-06-16-work-summary.md

.claude/
  CLAUDE.md                          (同步最新状态)
.gitignore                           (+!application-docker.yml +docs/test_local_log.md)
```

## Git 记录

```
6444143 docs: create docker/ directory with compose, env template, and deployment README
cb9df90 feat: add Docker deployment with test data initializer and automated test support
20365cc fix: scope SMS codes by operation, add brute-force protection on password reset, unify hardcoded messages
aef7f2b fix: draft-save persist on unload, isolate sub-page drafts, fix missing fields
e88fe23 fix: draft-save cross-page contamination, missing component CSS, NaN guard
4c4b8e2 feat: refactor password system — first-time pay password skip verification, add forgot password (login + pay) via SMS
b201b4b refactor: make all constant classes final with private constructors
```

---

## 后续计划

### P1 — 功能开发

| # | 内容 | 来源 | 预估 |
|---|------|------|:--:|
| 1 | **跑腿员信用分机制** — 初始 100，超时/投诉扣分，<60 冻结，`credit_log` 表 | 06-10 P2 | 中 |
| 2 | 管理端跑腿员详情页 `/runners/:id` | 06-15 P1 | 小 |
| 3 | 管理端系统设置页 `/settings` | 06-15 P1 | 小 |
| 4 | **Docker 自动化测试 Agent** — 启动容器→注入验证码→跑 API 测试→报告 | 06/16 新 | 中 |
| 5 | 订单详情页任务类型视觉区分（CSS 已就绪，待接线） | 06-15 P1 | 小 |

### P2 — 技术债务

| # | 内容 | 预估 |
|---|------|:--:|
| 6 | `FeeCard`/`GenderRestriction` 与 `service-publish.vue` CSS 去重 | 小 |
| 7 | `service-publish.vue` onSubmit 拆分（~220 行） | 中 |
| 8 | `displayDescription` 3 种变体统一 | 小 |
| 9 | `order-waiting` 用 `TaskDetailVO`，其他两页用 `OrderDetailVO`，字段访问不一致 | 小 |
| 10 | 各页未使用 import 清理 | 小 |

### P3 — 长期规划

地图 API 接入、任务大厅智能推荐、微信订阅消息推送、数据看板增强、订单纠纷处理、评价体系完善、批量发布/接单、CI/CD

### 已知注意事项

- `docs/test_local_log.md` 含真实 IP 已加 `.gitignore`，但历史版本仍可见 — 需评估 `git filter-branch` 清理

### P0 — 安全加固 ✅ 已完成

| # | 内容 | 状态 |
|---|------|:--:|
| S1 | SMS 验证码 Redis key 操作作用域化 | ✅ `20365cc` |
| S2 | 密码重置端点暴力破解防护 | ✅ `20365cc` |
| M1 | 后端异常消息统一封装为 MessageConstant | ✅ `20365cc` |
| 4 | Docker 自动化测试 Agent | ✅ `039c477` |

### 📅 明日 (06/17) — 信用分 + 技术债清理

| # | 内容 | 预估 |
|---|------|:--:|
| 1 | 跑腿员信用分机制（初始100，超时扣分，<60冻结，credit_log表） | 中 |
| 6 | FeeCard/GenderRestriction 与 service-publish CSS 去重 | 小 |
| 7 | service-publish.vue onSubmit 拆分（~220 行） | 中 |
| 8 | displayDescription 3 种变体统一 | 小 |
| 9 | order-waiting vs 其他详情页 VO 类型不一致 | 小 |
| 10 | 各页未使用 import 清理 | 小 |

### 📅 后天 (06/18) — 管理端完善

| # | 内容 | 预估 |
|---|------|:--:|
| 2 | 管理端跑腿员详情页 /runners/:id | 小 |
| 3 | 管理端系统设置页 /settings | 小 |
| 5 | 订单详情页任务类型视觉区分（CSS 已就绪） | 小 |

### P3 — 长期规划

地图 API、智能推荐、微信推送、数据看板、纠纷处理、评价体系、批量发布、CI/CD
