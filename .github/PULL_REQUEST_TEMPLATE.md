## 变更类型

- [ ] 新功能 (`feat:`)
- [ ] Bug 修复 (`fix:`)
- [ ] 重构 (`refactor:`)
- [ ] 文档 (`docs:`)
- [ ] 测试 (`test:`)
- [ ] 构建/CI (`build:` / `ci:`)

## 变更摘要

<!-- 用 1-3 句话描述本次 PR 做了什么 -->

## 变更明细

<!-- 列出关键改动点和涉及的模块 -->

| 模块 | 文件 | 说明 |
|------|------|------|
| | | |

## 测试计划

<!-- 说明如何验证变更，包含手动测试步骤或自动化测试 -->

- [ ] 后端编译通过 (`./runningerrands-server/mvnw compile -q -DskipTests`)
- [ ] 管理端类型检查通过 (`npx vue-tsc --noEmit`)
- [ ] 管理端已重新构建并更新后端 `static/`
- [ ] 数据库变更已同步 `runningerrands.sql`
- [ ] 手动测试：

## 审查清单

- [ ] 代码已通过 `deep-code-reviewer` 审查
- [ ] 无安全漏洞（SQL 注入、XSS、未授权访问等）
- [ ] 无硬编码密钥/密码
- [ ] task_specs JSON key 统一使用中文，无英文 key
- [ ] 前端 ngrok/内网穿透路径以注释保留，未删除
- [ ] 移动端 `config.js` 指向 localhost（开发态）

## 截图 / 录屏

<!-- 如有 UI 变更，请附上截图 -->

## 关联 Issue

<!-- Closes #xxx -->
