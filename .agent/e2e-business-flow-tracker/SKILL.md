---
name: "e2e-business-flow-tracker"
description: "全链路业务流程跟踪代理。模拟用户从注册登录→认证→任务发布→订单流转→管理端审查的完整业务流程，进行 1.0 发布前最终验证。确保所有代码可运行、无 MEDIUM 及以上 Bug、前端 UI 渲染正常。"
model: sonnet
memory: user
---

# E2E Business Flow Tracker

端到端业务流程全链路跟踪，模拟真实用户操作验证系统完整性和正确性。

## When to use this skill

**触发场景（必须触发）：**
- 1.0 版本发布前最终验证
- 重大重构或架构变更后
- 数据库 schema 变更后
- 安全机制（JWT/限流/权限）变更后
- 状态机逻辑修改后
- 支付/退款流程变更后
- 用户明确要求"全链路测试"、"端到端验证"、"发布前检查"

**DO NOT use when:**
- 仅修改文档或注释 → 无运行影响
- 仅修改 CSS/样式 → 前端代码审查即可
- 单接口 Bug 修复 → 使用 `docker-test-agent` 针对性测试
- 纯 UI 调整 → 使用 `frontend-code-reviewer`

## How to use this skill

```
/agent:e2e-business-flow-tracker --scope <full|phase1-7|quick> --env <local|docker>
```

### 执行模式

| 模式 | 覆盖范围 | 预估耗时 |
|------|---------|---------|
| `full` | 全部 7 个 Phase，26 个 Step | 15-20 min |
| `quick` | Phase 1-2-5 核心路径（认证+任务+订单） | 5-8 min |
| `phase<N>` | 指定 Phase 1-7 | 2-5 min/phase |

### 执行流程

```
Phase 0: Pre-flight Check（编译+环境就绪）
Phase 1: 认证与注册（6 steps）
Phase 2: 用户资料与认证（4 steps）
Phase 3: 跑腿员认证（3 steps）
Phase 4: 任务发布（5 steps）
Phase 5: 订单全生命周期（8 steps）
Phase 6: 管理端全功能（4 steps）
Phase 7: 边界与异常（集成在各 Step 断言中）
Report: 汇总通过/失败/阻塞清单
```

## Agent 工作流

### Step 0: Pre-flight Check

```bash
# 1. 编译后端
cd F:/ikeu_runningerrands/backend
./runningerrands-server/mvnw compile -q -DskipTests

# 2. 前端类型检查
cd F:/ikeu_runningerrands/admin
npx vue-tsc --noEmit

# 3. 环境健康检查
curl -s http://localhost:8080/api/actuator/health
# 或 Docker: docker compose ps
```

### Step 1: 环境准备

1. 清理测试数据（删除上次运行产生的用户/任务/订单）
2. 注入 SMS 验证码到 Redis
3. 确认测试账号可用
4. 获取 admin JWT（用于管理端操作）

### Step 2: 按 Phase 顺序执行

每个 Step 包含：
1. **API 请求** — curl 命令发送请求
2. **响应断言** — HTTP 状态码 + 业务 code + msg + data 字段
3. **DB 验证** — 必要时查 MySQL 确认数据落库
4. **Redis 验证** — 必要时查 Redis 确认缓存/锁/限流状态
5. **UI 验证**（Phase 6） — MCP chrome-devtools 截图 + Console 检查

### Step 3: 输出报告

```
═══════════════════════════════════════════
  E2E 业务流程全链路跟踪 — 验证报告
  日期: 2026-06-22  环境: Docker
═══════════════════════════════════════════

Phase 1: 认证与注册 ............. 6/6 ✅
Phase 2: 用户资料与认证 ......... 4/4 ✅
Phase 3: 跑腿员认证 ............. 3/3 ✅
Phase 4: 任务发布 ................ 5/5 ✅
Phase 5: 订单全生命周期 ......... 8/8 ✅
Phase 6: 管理端全功能 ........... 4/4 ✅
Phase 7: 边界与异常 ............. 7/7 ✅

总计: 37 通过, 0 失败, 0 跳过
UI 验证: 8 页面截图通过, 0 Console 错误

阻塞项: 无
═══════════════════════════════════════════
✅ 1.0 发布就绪
```

## General Rules

### 断言标准

每个 API 调用必须验证以下维度：

| 维度 | 方法 | 失败级别 |
|------|------|---------|
| HTTP 状态码 | `curl -w "%{http_code}"` | **BLOCKER** — 流程中断 |
| 业务 code | `jq '.code'` 期望 1 或特定 0 | **BLOCKER** — 流程中断 |
| 响应字段 | `jq '.data.xxx'` 非空/类型正确 | **HIGH** — 记录但可继续 |
| DB 状态 | MySQL 查询确认记录落库 | **MEDIUM** — 记录 |
| Redis 状态 | redis-cli 检查 key 存在性/TTL | **MEDIUM** — 记录 |
| UI 渲染 | MCP 截图 + Console 错误 | **HIGH** — 记录 |

### 测试数据隔离

- 测试用户手机号前缀：`1380000xxxx`（不与生产数据冲突）
- 测试任务自动过期时间：60 分钟
- 测试完成后的数据可选择保留（用于后续 Phase）或清理

### 安全验证（内嵌于各 Step）

- [ ] SMS 验证码跨操作隔离（register 码 ≠ login 码）
- [ ] 暴力破解锁定（5 次错误 → 300s 锁定）
- [ ] 用户 Token 不能访问管理端接口
- [ ] 管理端 Token 不能访问用户端接口
- [ ] 支付幂等（重复扣款拦截）
- [ ] 频率限制（SMS 5/min、登录 10/min）
- [ ] 文件上传鉴权（无 Token 拒绝）

### 状态机验证（Phase 5）

- [ ] Task: 1→2→3→4→5 完整正向流
- [ ] Task: 1→6 发布者取消
- [ ] Task: 2/3 状态禁止发布者取消
- [ ] Order: 1→2→3→4 完整正向流
- [ ] Order: 1→5 跑腿员取消（5 分钟内）
- [ ] Order: 3→4 自动完成（可手动触发确认）

## 业务流程总览

完整业务流程拓扑（详见 `references/business-flows.md`）：

```
用户注册 ──→ 登录 ──→ 设置支付密码 ──→ 实名认证 ──→ 管理端审核通过
                                                         │
                                                         ↓
                                              申请跑腿员 ──→ 管理端审核通过 ──→ 上线
                                                                                │
                                                                                ↓
钱包充值 ──→ 发布任务（4 种类型） ──→ 任务大厅展示 ──→ 跑腿员接单
                                                         │
                                                         ↓
                                          确认取货 ──→ 确认送达 ──→ 确认完成
                                                                      │
                                                                      ↓
                                                              自动结算 + 信用分更新
                                                                      │
                                                                      ↓
                                                              互相评价 + 追评
```

## 快速执行

无需 Agent，直接在终端运行：

```bash
# 完整流程
bash e2e/run.sh

# 仅核心路径（~5min）
bash e2e/run.sh --quick

# 保留测试数据
bash e2e/run.sh --keep

# 分步执行
bash e2e/setup.sh              # 编译 + 环境检查
bash e2e/bootstrap.sh          # 注入测试数据
bash e2e/run.sh --phase 5      # 仅跑订单流转
bash e2e/teardown.sh           # 手动清理
```

## 参考规范

| 文件 | 内容 |
|------|------|
| `references/business-flows.md` | 7 个 Phase 的完整 API 调用序列 + curl 命令 + 断言 |
| `references/api-endpoints.md` | 全部 60+ API 端点的请求/响应格式速查 |
| `references/test-data.md` | 测试账号、Redis 注入、数据库准备/清理 |
| `references/state-machines.md` | Task/Order 状态转换矩阵 + 守卫条件 |
| `references/frontend-routes.md` | 管理端 17 路由 + 移动端 34 页面的 UI 验证清单 |
| `references/verification-checklist.md` | 每 Step 的断言矩阵 + 通过/失败标准 |
| `../../e2e/setup.sh` | 环境就绪：编译 + Docker 启动 + 健康检查 |
| `../../e2e/bootstrap.sh` | 数据注入：SMS 验证码、清理旧数据、预取 Admin Token |
| `../../e2e/run.sh` | 编排入口：setup → bootstrap → Phase 1-7 → teardown |
| `../../e2e/teardown.sh` | 清理：测试用户/任务/订单/Redis key |
| `../../e2e/.env.e2e` | 测试变量（手机号、验证码、API 地址等） |
