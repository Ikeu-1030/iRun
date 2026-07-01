# 2026/06/17 — 信用分机制 + 技术债清理

## 完成工作

### 1. 跑腿员信用分机制

**评分规则：**

| 场景 | 分值 | reason_type |
|------|:--:|------|
| 提前完成（实际 < 预计） | +5 | `REWARD` |
| 按时完成（~预计） | +1 | `REWARD` |
| 超时 ≤30min | -2 | `TIMEOUT` |
| 超时 30-60min | -5 | `TIMEOUT` |
| 超时 >60min | -10 | `TIMEOUT` |
| 投诉（预留） | -10 | `COMPLAINT` |

**冻结/恢复机制：**
- 信用分 < 60 → `is_banned=1` + `ban_until=NOW()+3天`（仅首次冻结，后续扣分不重置计时）
- `CreditRecoveryTask` 每 5 分钟扫描 `ban_until < NOW()` → 原子恢复至 60 + 解除封禁
- `acceptOrder` 信用检查引用 `CreditConstant.CREDIT_FREEZE_THRESHOLD`
- 管理端 `toggleRunnerBan` 原子 SQL 不触及 `credit_score`，防止丢失更新
- `is_banned` 区分为管理员手动封禁（`ban_until=null`）与信用分冻结（`ban_until IS NOT NULL`）

**变更范围：** 后端 18 文件 + 移动端 3 文件 + SQL

| 组件 | 文件 |
|------|------|
| 常量 | `CreditConstant.java`（阈值/天数/原因类型） |
| 实体 | `CreditLog.java` + `credit_log` 表 |
| 核心 | `CreditServiceImpl`（`recordAndApply` + 冻结/恢复 + 提前+5 + `confirmTime`） |
| 原子 SQL | `RunnerProfileMapper`（`updateCreditScoreAndFreeze` / `restoreCreditAndUnfreeze` / `toggleBan`） |
| AOP | `CreditScoreAspect`（+`@Order` 强制方面先于事务执行） |
| 定时 | `CreditRecoveryTask`（每 5 分钟扫描过期冻结恢复至 60） |
| VO | `UserInfoVO` / `RunnerInfoVO`（+creditScore / +banUntil） |
| 服务 | `UserServiceImpl` / `RunnerProfileServiceImpl` / `AdminRunnerServiceImpl` / `TaskOrderServiceImpl` |
| DTO | `OnTimeStatsDTO`（替代 `Map<String,Object>` 解决 MyBatis `@MapKey` 报错） |
| 移动端 | `store`（isCreditFrozen getter）+ `task-hall`（限制接单按钮） |

提交：`0a7c21f`

### 2. 技术债清理

| # | 内容 | 文件 | 变更 |
|---|------|------|:--:|
| 6 | CSS 去重 | `service-publish.vue` | -9 行（FeeCard 专用类移除） |
| 8 | displayDescription 统一 | `useTaskSpecs.js` + `order-delivering` / `order-completed` | 共享 computed，各 -17 行 |
| 10 | import 清理 | `order-waiting` / `order-delivering` / `order-completed` | -7 未使用 import |
| 7 | onSubmit 拆分 | `service-publish.vue` | 提取 `validateForm()`，226→140 行 |

### 3. 缺陷修复

| Bug | 文件 | 修复 |
|-----|------|------|
| `countCompletedOnTime` MyBatis `@MapKey` 报错 | `TaskOrderMapper` + XML + `RunnerProfileServiceImpl` | 新建 `OnTimeStatsDTO` 替代 `Map<String,Object>` |
| 安全 — `privateNote` 在共享 `displayDescription` 中无条件暴露 | `useTaskSpecs.js` | 从共享版本移除，仅 `order-waiting` 本地受控版本保留 |
| UX — 冻结+未认证按钮文案与动作不匹配 | `task-hall.vue` | `isCreditFrozen` 优先级提至 `isCertifiedRunner` 之前 |
| Bug B — `order-completed` 降级路径遗漏 `taskSpecs`/`publicDesc` | `order-completed.vue` | 手动构造对象补字段 |

提交：`fa05e57`

### 4. 代码审查与测试

**审查（6 轮）：**

| 轮次 | 类型 | 发现 | 修复 |
|:--:|------|:--:|:--:|
| 1 | `deep-code-reviewer` — 全量 | 11 项（1 CRITICAL + 3 HIGH + 4 MEDIUM + 3 LOW） | 修复 5 项 |
| 2 | `deep-code-reviewer` — 二轮 | 6 项（1 CRITICAL + 2 HIGH + 2 MEDIUM + 1 LOW） | 修复 3 项 |
| 3-4 | `agent-skills:code-reviewer` — 前端 | APPROVE（2 suggestion） | 修复 1 项 UX |
| 5-6 | `agent-skills:code-reviewer` — validateForm | APPROVE | 零差异 |

**测试：**

| 阶段 | 框架 | 测试数 | 结果 |
|------|------|:--:|:--:|
| 单元测试 | JUnit 5 + Mockito | 61 | ✅ 全通过 |
| Docker 集成 | curl + MySQL | 15 steps | ✅ 信用分检查正确拦截/放行 |

---

## 涉及文件

```
backend/
  runningerrands-common/.../constant/
    CreditConstant.java              (新建 — 阈值/原因类型)
    MessageConstant.java             (+CREDIT_FROZEN_TOAST)
    RedisConstant.java               (+CREDIT_RECOVERY_LOCK_KEY)
  runningerrands-model/.../
    dto/OnTimeStatsDTO.java          (新建 — 替代 Map<String,Object>)
    entity/CreditLog.java            (新建 — credit_log 实体)
    entity/RunnerProfile.java        (+banUntil)
    vo/RunnerInfoVO.java             (+banUntil + LocalDateTime import)
    vo/UserInfoVO.java               (+creditScore + banUntil)
  runningerrands-server/.../
    aspect/CreditScoreAspect.java    (+@Order)
    mapper/CreditLogMapper.java      (新建)
    mapper/RunnerProfileMapper.java  (+3 个原子 SQL)
    mapper/TaskOrderMapper.java      (Map→OnTimeStatsDTO)
    service/impl/
      AdminRunnerServiceImpl.java    (原子 toggleBan)
      CreditServiceImpl.java         (核心重构: recordAndApply + 冻结 + 提前+5 + confirmTime)
      RunnerProfileServiceImpl.java  (OnTimeStatsDTO 消费)
      TaskOrderServiceImpl.java      (硬编码60→CreditConstant)
      UserServiceImpl.java           (注入 creditScore/banUntil)
    task/CreditRecoveryTask.java     (新建 — 原子恢复 + TOCTOU 防护)
    resources/mapper/TaskOrderMapper.xml (resultType 更新)
  runningerrands.sql                 (+credit_log 表 + ban_until 列)

test/
  CreditConstantTest.java           (新建 — 6 tests)
  CreditServiceImplTest.java        (新建 — 17 tests)
  CreditRecoveryTaskTest.java       (新建 — 6 tests)
  RunnerProfileServiceImplTest.java (新建 — 31 tests)

mobile/
  utils/useTaskSpecs.js             (+displayDescription)
  pages/service-publish/service-publish.vue (validateForm 提取 + CSS 去重)
  pages/order-delivering/order-delivering.vue (共享 displayDescription + import 清理)
  pages/order-completed/order-completed.vue   (共享 displayDescription + 降级路径修复)
  pages/order-waiting/order-waiting.vue       (import 清理)
  pages/task-hall/task-hall.vue     (限制接单按钮 + 优先级修复)
  store/index.js                    (+isCreditFrozen)
```

## Git 记录

```
fa05e57 refactor: consolidate displayDescription, extract validateForm, clean dead imports/CSS
0a7c21f feat: add runner credit scoring system with freeze/recovery and audit trail
```

---

## 📅 明日 (06/18) — 管理端完善

| # | 内容 | 预估 |
|---|------|:--:|
| 2 | 管理端跑腿员详情页 `/runners/:id` | 小 |
| 3 | 管理端系统设置页 `/settings` | 小 |
| 5 | 订单详情页任务类型视觉区分（CSS 已就绪） | 小 |

### P3 — 长期规划

地图 API · 智能推荐 · 微信推送 · 数据看板增强 · 纠纷处理 · 评价体系 · 批量发布/接单 · CI/CD
