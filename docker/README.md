# 小i跑腿 — Docker 部署指南

## 前置条件

- **Docker** ≥ 24.0（含 Docker Compose v2）
- 空闲端口 `8080` / `3308` / `6381`（可在 `.env` 中自定义）

## 快速启动

```bash
# 1. 进入 docker 目录
cd docker

# 2. （可选）复制并编辑环境变量
cp .env.example .env

# 3. 构建并启动
docker compose up -d

# 4. 查看日志（等待约 30s 初始化完成）
docker compose logs -f backend
```

首次启动会：
1. 拉取 `mysql:8.0`、`redis:7-alpine` 镜像
2. 构建后端镜像（Maven 多模块编译，约 3-5 分钟）
3. MySQL 自动执行 `runningerrands.sql` 建表
4. 后端启动后自动创建测试账号（`DockerTestDataInitializer`）

## 服务端口

| 服务 | 容器内 | 宿主机 | 说明 |
|------|:-----:|:-----:|------|
| Nginx | 80 | **80** | 反向代理 + 静态资源 + WebSocket |
| MySQL 8 | 3306 | **3308** | root / `${MYSQL_ROOT_PASSWORD}` |
| Redis 7 | 6379 | **6381** | 无密码，database=1 |
| Spring Boot | 8080 | **8080** | context-path=/api |

## 预置账号

| 用户名 | 密码 | 手机号 | 跑腿员 | 支付密码 |
|--------|------|--------|:-----:|:------:|
| `testuser` | `123456` | `13800000001` | ✅ 已认证 | ✅ 已设 |
| `testuser2` | `123456` | `13800000002` | ❌ | ✅ 已设 |
| `admin` | `admin` | — | — (超管) | — |

## 验证部署

```bash
# 健康检查
curl http://localhost:8080/api/actuator/health

# 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser","password":"123456"}'

# 管理员登录
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

## API 自动化测试（绕过 SMS）

Docker 环境不配置阿里云 SMS，测试验证码相关接口时直接向 Redis 注入验证码：

```bash
# 1. 注入验证码到 Redis（有效期 300s）
docker exec rr-redis redis-cli -n 1 \
  SET "user:code:reset_password:13800000001" "888888" EX 300

# 2. 登录获取 JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser","password":"123456"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 3. 调用密码重置（使用注入的验证码）
curl -X PUT http://localhost:8080/api/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"phone":"13800000001","code":"888888","newPassword":"654321"}'

# 4. 暴力破解防护测试（5 次失败后锁定）
for i in 1 2 3 4 5 6; do
  curl -s -X PUT http://localhost:8080/api/user/password/reset \
    -H "Content-Type: application/json" \
    -H "authentication: $TOKEN" \
    -d '{"phone":"13800000001","code":"000000","newPassword":"123456"}' \
    | grep -o '"msg":"[^"]*"'
done
# → 前 5 次: "验证码错误或已过期"
# → 第 6 次: "密码重置尝试次数过多，请5分钟后再试"
```

### 各操作对应的 Redis Key

| 操作 | Redis Key |
|------|-----------|
| 注册 | `user:code:register:{phone}` |
| 验证码登录 | `user:code:login:{phone}` |
| 修改手机号 | `user:code:change_phone:{phone}` |
| 重置登录密码 | `user:code:reset_password:{phone}` |
| 重置支付密码 | `user:code:reset_pay_password:{phone}` |

## 常用命令

```bash
# 查看容器状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 进入 MySQL
docker exec -it rr-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD:-root123} runningerrands

# 进入 Redis
docker exec -it rr-redis redis-cli -n 1

# 重启后端（代码修改后重新构建）
docker compose up -d --build backend

# 停止并清理所有数据
docker compose down -v
```

## 环境变量参考

全部变量见 `.env.example`。核心变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL root 密码 |
| `MYSQL_PORT` | `3308` | MySQL 宿主机端口 |
| `REDIS_PORT` | `6381` | Redis 宿主机端口 |
| `BACKEND_PORT` | `8080` | 后端 API 端口 |
| `JWT_ADMIN_SECRET` | *(内置默认)* | 管理端 JWT 密钥（生产必改） |
| `JWT_USER_SECRET` | *(内置默认)* | 用户端 JWT 密钥（生产必改） |
| `ALIYUN_ACCESS_KEY_ID` | *(空)* | 阿里云 AK（SMS/OSS 需要） |
| `WECHAT_APP_ID` | *(空)* | 微信小程序 AppID |

## 目录结构

```
docker/
├── docker-compose.yml    # 服务编排（MySQL + Redis + Backend + Nginx）
├── .env.example          # 环境变量模板
├── README.md             # 本文件
└── nginx/
    ├── nginx.conf         # Nginx 反向代理配置
    └── Dockerfile         # Nginx 镜像构建

backend/
├── Dockerfile            # 多阶段构建定义
├── .dockerignore         # 构建忽略规则
├── runningerrands.sql    # 数据库初始化（挂载到 MySQL）
└── runningerrands-server/src/main/resources/
    └── application-docker.yml   # Docker 环境 Spring 配置
```
