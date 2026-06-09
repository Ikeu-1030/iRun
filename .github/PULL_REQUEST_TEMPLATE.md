## 变更类型

- [ ] 新功能 (`feat:`)
- [ ] Bug 修复 (`fix:`)
- [ ] 重构 (`refactor:`)
- [ ] 文档 (`docs:`)
- [ ] 测试 (`test:`)
- [ ] 构建/CI (`build:` / `ci:`)
- [ ] 杂项 (`chore:`)

## Summary

<!-- 1-3 条要点概括本次 PR 做了什么 -->

-

## Files changed

<!-- 列出关键改动文件及说明 -->

| File | Change |
|------|--------|
| | |

## Test plan

<!-- 说明如何验证变更 -->

- [ ] 后端编译通过 (`./mvnw compile -q -DskipTests`)
- [ ] 管理端类型检查通过 (`npx vue-tsc --noEmit`)
- [ ] 管理端已重新构建并同步后端 `static/`
- [ ] 数据库变更已同步 `runningerrands.sql`
- [ ] 手动测试：

## Checklist

- [ ] task_specs JSON key 统一使用中文，无英文 key
- [ ] 前端 ngrok 路径以注释保留，未删除
- [ ] 移动端 `config.js` 指向 localhost（开发态）
- [ ] 无硬编码密钥/密码
- [ ] 无安全漏洞（SQL 注入、XSS、未授权访问等）

## Screenshots

<!-- 如有 UI 变更，请附上截图 -->

## Related

<!-- Closes #xxx -->
