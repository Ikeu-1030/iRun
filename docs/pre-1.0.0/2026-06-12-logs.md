# 2026/06/11-12 — AdminService 领域拆分 + 缓存防护 + Hutool 引入

## 完成工作

### 1. AdminService 单体拆分（核心重构）

将 804 行单体 `AdminService/AdminServiceImpl` 拆分为 7 个领域服务，每个服务职责单一：

| 新服务 | 职责 | 对应 Controller |
|--------|------|-----------------|
| `AdminAuthService` | 登录、令牌刷新、登出、获取管理员信息 | `AdminAuthController` |
| `AdminDashboardService` | 仪表盘统计、趋势、分类、订单分布 | `AdminDashboardController` |
| `AdminEmployeeService` | 管理员 CRUD、状态切换、密码重置 | `AdminEmployeeController` |
| `AdminOrderService` | 订单列表、详情、强制状态更新 | `AdminOrderController` |
| `AdminRunnerService` | 跑腿员列表、详情、认证、封禁 | `AdminRunnerController` |
| `AdminTaskService` | 任务列表、详情、强制状态更新 | `AdminTaskController` |
| `AdminUserService` | 用户列表、详情、封禁、认证审核 | `AdminUserController` |

**变更要点**：
- 删除 `AdminService.java`（70行接口）和 `AdminServiceImpl.java`（804行实现）
- 各 Controller 注入对应领域服务，消除对旧 `AdminService` 的依赖
- 无业务逻辑变更，纯结构重构

### 2. Redis 缓存防护

新增 `@RedisDefend` 注解 + `RedisDefendUtil` 工具类，提供缓存穿透 + 缓存击穿组合防护：

- **防穿透**: 空结果用 `TASK_HALL_NULL_PREFIX` 标记缓存，避免不存在的数据反复查询数据库
- **防击穿**: `TASK_HALL_LOCK_KEY` 互斥锁，热点数据过期时只有一个线程重建缓存
- 任务大厅多页缓存 key 格式 `page:size`，TTL 600s

### 3. Hutool 工具库引入

- `BeanUtil.copyProperties` 替代手动字段拷贝
- `JSONUtil.toList` 简化 JSON 数组解析
- 应用于 VO 构建、规格解析等场景

### 4. VO 内部类提取

- `DashboardVO` 内部类 `TrendPointVO`、`CategoryPieVO` 提取为独立 VO 文件
- 便于复用和独立测试

### 5. 安全策略调整

- 登录锁定时间从 15 分钟缩减为 5 分钟（300s），减少误锁用户等待时间

## 涉及文件

```
backend/runningerrands-server/src/main/java/com/ikeu/server/
  ├── annotation/RedisDefend.java (新增)
  ├── service/
  │   ├── AdminService.java (删除)
  │   ├── AdminAuthService.java (新增)
  │   ├── AdminDashboardService.java (新增)
  │   ├── AdminOrderService.java (新增)
  │   ├── AdminRunnerService.java (新增)
  │   ├── AdminTaskService.java (新增)
  │   ├── AdminUserService.java (新增)
  │   └── impl/
  │       ├── AdminServiceImpl.java (删除)
  │       ├── AdminAuthServiceImpl.java (新增)
  │       ├── AdminDashboardServiceImpl.java (新增)
  │       ├── AdminEmployeeServiceImpl.java
  │       ├── AdminOrderServiceImpl.java (新增)
  │       ├── AdminRunnerServiceImpl.java (新增)
  │       ├── AdminTaskServiceImpl.java (新增)
  │       └── AdminUserServiceImpl.java (新增)
  ├── util/RedisDefendUtil.java (新增)
  └── controller/admin/ (各 Controller 注入字段更新)
backend/runningerrands-model/src/main/java/com/ikeu/model/vo/
  ├── TrendPointVO.java (提取)
  └── CategoryPieVO.java (提取)
```

## Git 记录

```
2fbc33d refactor: extract AdminService into domain services, add cache defense, adopt Hutool
151d56c Merge pull request #19 from Ikeu-1030/master
```
