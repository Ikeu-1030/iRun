# 贡献指南

## 分支策略

```
main    ← 只读，仅通过 PR 合并，禁止直接推送和强制推送
master  ← 开发主分支，所有修改从此分支提交 PR 至 main
pr/*    ← 您的pull分支，我们会定期查看拉取至master，也可以由您创建拉取请求至master
feature/* ← 功能分支（可选，从 master 切出）
```

- **禁止**直接推送到 `main` 分支
- **禁止**对 `main` 分支执行 `push --force`
- 所有变更通过 `master → main` 的 PR 合并

## PR 流程

1. 在 `master` 分支上完成开发和自测/创建 `pr/*`完成自己的分支开发然后拉取请求至 `master`
2. 确认变更通过代码审查（使用 `deep-code-reviewer` agent）
3. 提交 PR 至 `main` 分支，使用 PR 模板填写完整信息
4. 至少一名仓库管理员审查通过后方可合并

## 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>: <简短描述>

feat: 新增跑腿员信用评分系统
fix: 修复任务发布时小费校验范围错误
docs: 更新 API 文档中的认证说明
refactor: 抽取订单详情页共享 composable
test: 为 TaskStateMachine 补充状态转换测试
```

| 类型 | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `refactor` | 重构（无功能变更） |
| `test` | 测试代码 |
| `build` | 构建系统 / 依赖 |
| `ci` | CI 配置 |
| `chore` | 杂项（清理、格式等） |

## 代码审查

合并前必须通过以下检查：

- [ ] Java 后端编译通过
- [ ] 管理端 TypeScript 类型检查通过
- [ ] `deep-code-reviewer` 审查无 CRITICAL 或 HIGH 问题
- [ ] 数据库变更已同步 `runningerrands.sql`
- [ ] 无安全漏洞
- [ ] 前端配置文件中 ngrok 路径以注释保留

## 项目约定

- **task_specs JSON key**：统一使用中文命名，禁止英文 key
- **管理端构建**：每次修改后需 `vite build` 并同步到后端 `static/`
- **密码/密钥**：通过环境变量注入，不硬编码在配置文件中
- **移动端配置**：`config.js` 默认指向 localhost，ngrok/生产地址注释保留
