# 测试环境部署文档

## 环境信息

| 项目 | 说明 |
|------|------|
| 虚拟机 | Ubuntu|
| 应用端口 | 8080 |
| 外部访问 | 宿主机 8081 → NAT → 虚拟机:8080 |
| 数据库 | MySQL 8 |
| 缓存 | Redis |

## 1. 虚拟机环境准备

### 1.1 安装 Java 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

### 1.2 安装 MySQL 8（如未安装）

```bash
sudo apt install -y mysql-server
sudo mysql_secure_installation
```

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS runningerrands DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

导入表结构（将 `runningerrands.sql` 上传至虚拟机后执行）：

```bash
mysql -u root -p runningerrands < runningerrands.sql
```

### 1.3 安装 Redis（如未安装）

```bash
sudo apt install -y redis-server
sudo systemctl enable redis-server
sudo systemctl start redis-server
```

## 2. 打包项目

在开发机上执行：

```bash
cd F:/ikeu_runningerrands/backend
./runningerrands-server/mvnw clean package -DskipTests -Ptest
```

生成的 JAR 包位于：
```
backend/runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar
```

## 3. 上传 JAR 到虚拟机

```bash
# 将 JAR 上传至虚拟机（替换 <user> 为虚拟机用户名）
scp backend/runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar <user>@192.168.100.133:/home/<user>/app/

# 上传 SQL 建表脚本
scp backend/runningerrands.sql <user>@192.168.100.133:/home/<user>/app/
```

## 4. 配置环境变量

在虚拟机上创建 `/home/<user>/app/env.sh`：

```bash
#!/bin/bash
# 数据库
export RUNNING_ERRANDS_MYSQL_URL=jdbc:mysql://localhost:3306/runningerrands?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT%2B8
export RUNNING_ERRANDS_MYSQL_USERNAME=root
export RUNNING_ERRANDS_MYSQL_PASSWORD=<your-mysql-password>

# Redis
export RUNNING_ERRANDS_REDIS_HOST=localhost
export RUNNING_ERRANDS_REDIS_PORT=6379

# JWT 密钥（必须配置，两段随机字符串）
export RUNNING_ERRANDS_JWT_ADMIN_SECRET=<admin-jwt-secret>
export RUNNING_ERRANDS_JWT_USER_SECRET=<user-jwt-secret>

# 阿里云 OSS
export ALIYUN_ACCESS_KEY_ID=<your-access-key-id>
export ALIYUN_ACCESS_KEY_SECRET=<your-access-key-secret>

# 微信小程序
export WECHAT_APP_ID=<your-wechat-app-id>
export WECHAT_APP_SECRET=<your-wechat-app-secret>

# 腾讯地图
export TENCENT_MAP_API_KEY=<your-map-api-key>
```

## 5. 启动服务

```bash
# SSH 登录虚拟机
ssh <user>@192.168.100.133

# 加载环境变量 & 启动
cd /home/<user>/app
source env.sh
nohup java -jar -Dspring.profiles.active=test runningerrands-server-1.0-SNAPSHOT.jar > app.log 2>&1 &
```

## 6. 验证

### 6.1 检查进程

```bash
ps -ef | grep runningerrands-server
```

### 6.2 检查日志

```bash
tail -f /home/<user>/app/app.log
# 看到 "Started ... in X seconds" 表示启动成功
```

### 6.3 接口验证

```bash
# 在虚拟机本地测试（Swagger 文档页）
curl http://localhost:8080/api/v3/api-docs

# 从宿主机访问（通过 NAT 转发）
curl http://<宿主机IP>:8081/api/v3/api-docs
```

浏览器访问 Swagger 文档：`http://<宿主机IP>:8081/api/swagger-ui.html`

### 6.4 管理端登录

- 默认超管账号：`admin` / `admin20260510`
- 管理端 API 路径：`http://<宿主机IP>:8081/api/admin/**`

## 7. 停止服务

```bash
# 查找进程 ID
ps -ef | grep runningerrands-server
kill <pid>
```

## 8. 更新部署

后续代码更新后，重复步骤 2～5：

```bash
# 开发机：打包 & 上传
cd F:/ikeu_runningerrands/backend
./runningerrands-server/mvnw clean package -DskipTests
scp runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar <user>@192.168.100.133:/home/<user>/app/

# 虚拟机：重启服务
ssh <user>@192.168.100.133
kill $(pgrep -f runningerrands-server)
cd /home/<user>/app
source env.sh
nohup java -jar -Dspring.profiles.active=test runningerrands-server-1.0-SNAPSHOT.jar > app.log 2>&1 &
```
