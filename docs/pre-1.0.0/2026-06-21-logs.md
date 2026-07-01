# 2026/06/21 — Round 3 审查修复 + 首页轮播图功能

## 一、Round 3 — 代码深度审查与修复

### 1.1 后端发现与修复

| # | 严重度 | 文件 | 问题 | 修复 |
|---|--------|------|------|------|
| C1 | CRITICAL | `AdminAuthServiceImpl.java` | JWT token 不含 `role` claim → 所有管理员 PII 脱敏失效（超管也看不到完整手机号） | `login()` + `refreshAccessToken()` 添加 `"role", admin.getRole()` 到 JWT claims |
| C2 | CRITICAL | `WebUtil.java` | `getRemoteAddr()` 返回 null 时 `ip.indexOf(',')` 触发 NPE | 添加 null guard，返回 `"unknown"` |
| H1 | HIGH | `AdminOrderServiceImpl.java` | 强制完成订单未清除排行榜缓存 | `@CacheEvict` 增加 `CACHE_LEADERBOARD` |
| H1 | HIGH | `AdminTaskServiceImpl.java` | 强制取消任务未清除排行榜缓存 | `clearCache` 增加 `CACHE_LEADERBOARD` |

### 1.2 前端审查

0 个新增问题 — 12 个文件（8 admin + 4 mobile）全部通过。PII 脱敏、自操作保护、表单验证、GSAP 计数器、refresh queue 清空等均正确实现。

### 1.3 补充修复

- **`PiiMaskUtil.java`** — 实际文件存在但未纳入版本管理（已解决）
- **`CreditServiceImplTest.java`** — 5 个测试方法补充第 5 参数 `maxScore`

---

## 二、首页轮播图功能（零新表方案）

### 2.1 设计

利用现有 `system_config` 表存储两个配置项，不新增数据库表：

| config_key | config_value 示例 | 说明 |
|------------|------------------|------|
| `banner.images` | `url1,url2,url3` | 逗号分隔的 OSS 图片 URL，最多 5 张 |
| `banner.interval_seconds` | `3` | 轮播切换间隔（秒），1-10 |

### 2.2 后端

- **`CommonController.java`** — 新增 `GET /common/banners` 公开接口（无需鉴权）
  - 提取常量 `CONFIG_KEY_BANNER_IMAGES` / `CONFIG_KEY_BANNER_INTERVAL` / `BANNER_INTERVAL_DEFAULT`
  - 返回强类型 `Result<BannerVO>`（`BannerVO.java` 新建），不硬编码 KV
- **`runningerrands.sql`** — 2 行 INSERT 种子 + `config_value VARCHAR(512→1024)` 扩容 + ALTER TABLE

### 2.3 管理端 — 设置页 Banner 管理

**`SettingsView.vue`** — 新增粉色"轮播图"分组卡片（`groupMeta` order:6）：
- 上传按钮：函数式 ref `:ref="(el) => fileInputEl = el"` + `triggerFileUpload()`，前端校验 5MB/格式
- 缩略图预览 + 上移/下移排序 + 删除
- `el-input-number` 轮播间隔 1-10 秒
- 满 5 张自动隐藏上传按钮并提示
- 所有变更通过现有 `PUT /admin/settings` 接口保存

### 2.4 移动端 — 首页轮播图

**`pages/index/index.vue`**：
- 有图片时渲染 `<swiper>`（`autoplay` + `circular` + 指示器），无图片时回退静态活动横幅
- 两侧半透明毛玻璃箭头按钮（`@click.stop` + `manualSwitching` 标志位防冲突）
- 透明度 `rgba(255,255,255,0.35)` / 模糊 `blur(12rpx)`

**秒级渲染缓存架构** — 新建 `utils/banner-cache.js`：
- 模块级顶层变量常驻内存，`getBannerSnapshot()` 同步读取（毫秒级）
- `refreshBanners()` 后台静默刷新 + 请求去重（`_pending` 防并发）
- **`App.vue`** `onLaunch` 预加载（静态 `import`），页面渲染前数据已就绪

**渲染时序**：
```
首次：App.onLaunch 预加载 → index.setup 读 storage（可能空）→ 静态占位 → API 返回 → 轮播出现
二次：App.onLaunch 预加载 → index.setup 读内存缓存 → 首帧即轮播（秒渲染）
```

### 2.5 API 模块

**`api/common.js`** — 新增 `getBanners()` 调用 `GET /common/banners`

---

## 三、数据库变更

| 变更 | 说明 |
|------|------|
| `system_config` +2 行 INSERT | `banner.images`、`banner.interval_seconds` |
| `config_value` VARCHAR(512→1024) | CREATE TABLE 修改 + ALTER TABLE 追加 |
| 运行库已同步 | `ALTER TABLE system_config MODIFY COLUMN` 已执行 |

---

## 涉及文件

| 操作 | 文件 |
|------|------|
| 修改 | `backend/runningerrands.sql` |
| 新建 | `runningerrands-model/.../vo/BannerVO.java` |
| 修改 | `runningerrands-server/.../controller/CommonController.java` |
| 修改 | `admin/src/views/settings/SettingsView.vue` |
| 修改 | `mobile/pages/index/index.vue` |
| 新建 | `mobile/utils/banner-cache.js` |
| 修改 | `mobile/App.vue` |
| 修改 | `mobile/api/common.js` |

## 待明日验证

- [ ] 管理端上传图片 → 设置页轮播图卡片交互
- [ ] 移动端首页轮播展示 + 箭头切换
- [ ] 秒渲染缓存效果（二次进入是否首帧即显）
- [ ] 静态占位回退（清空 banner 后是否显示活动横幅）
- [ ] 轮播间隔设置生效
