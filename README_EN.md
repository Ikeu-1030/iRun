<p align="center">
  <img src="admin/public/logo.svg" alt="runningerrands" width="120" />
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

A campus errand-running service platform. Students publish tasks (package pickup, food delivery, campus errands, shopping proxy), and registered runners accept and fulfill orders. Includes a WeChat Mini Program (uni-app), admin dashboard (Vue 3), and Spring Boot backend.

---

## Features

| Module | Stack | Description |
|--------|-------|-------------|
| **Backend** | Spring Boot 3.2 · Java 21 · MyBatis-Plus 3.5 · MySQL 8 · Redis · Redisson · WebSocket | REST API, JWT dual-token auth, RBAC, audit logging |
| **Admin** | Vue 3 · TypeScript · Vite · Element Plus · ECharts · Pinia · GSAP | Dashboard, user/runner/task/order management, transactions, auditing, operation logs |
| **Mobile** | uni-app (Vue 3) · WeChat Mini Program · Pinia · STOMP WebSocket | Task publishing/browsing, order tracking, real-time chat, wallet, identity verification |

### Mobile

- Four service types: Package Pickup · Food Delivery · Campus Errands · Shopping Proxy
- Task hall for browsing and accepting orders, real-time chat
- Wallet top-up / withdrawal with payment password protection
- Runner certification, online/offline status toggle, performance stats
- Real-name verification, address book management

### Admin Dashboard

- Dashboard overview with ECharts data visualization
- CRUD for users, runners, and administrators
- Task and order lifecycle management
- Transaction records, identity & runner verification auditing
- Push notifications, operation log auditing

---

## Project Structure

```
runningerrands/
├── backend/
│   ├── runningerrands-common/     # Shared utilities, constants, exceptions, result wrappers
│   ├── runningerrands-model/      # Entities, DTOs, VOs
│   ├── runningerrands-server/     # Controllers, services, mappers, config, interceptors, aspects
│   └── runningerrands.sql         # Database schema
├── admin/                         # Vue 3 admin dashboard
├── mobile/                        # uni-app WeChat Mini Program
└── .claude/                       # AI assistant configuration
```

---

## Quick Start

### Prerequisites

- JDK 21 · MySQL 8 · Redis · Maven 3.8+
- Node.js 22+ · HBuilderX (for mobile)

### Backend

```bash
cd backend

# Initialize database
mysql -u root -p < runningerrands.sql

# Configure database, Redis, Alibaba Cloud OSS/SMS, Tencent Maps in application.yml

# Build & run
./runningerrands-server/mvnw install -DskipTests
./runningerrands-server/mvnw spring-boot:run
# Server runs at http://localhost:8080/api
# Default superadmin: admin / admin20260510
```

### Admin Dashboard

```bash
cd admin
npm install
npm run dev         # http://localhost:3001
npx vite build       # Production build
```

### Mobile

1. Open `mobile/` in HBuilderX
2. Update the server address in `mobile/utils/config.js`
3. Run → WeChat Mini Program (requires WeChat DevTools)

---

## API Documentation

After starting the backend, visit [http://localhost:8080/api/doc.html](http://localhost:8080/api/doc.html) (Knife4j / Swagger).

---

## Notes

> **The WeChat Mini Program has not passed enterprise verification.** WeChat Pay and Alibaba Cloud SMS are running in sandbox/simulation mode. Replace with production credentials before deployment.

- Admin `/admin/**` and user APIs use separate JWT keys and interceptors
- Auth annotations: `@RequireRole({1, 2})` (1=superadmin, 2=regular admin), `@RequireCertify`
- Operation logs are auto-recorded via the `@OperationLog` annotation
- Keep `runningerrands.sql` in sync with entity changes

---

## License

[MIT](LICENSE) © 2025 ikeu
