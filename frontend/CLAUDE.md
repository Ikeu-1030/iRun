# Frontend Design — Operations Manual

> 前端设计技能操作手册。本文件覆盖 UI 审查、设计升级、动效、品牌视觉等全流程。
> 相关技能已安装至 `~/.agents/skills/`，通过 Skill 工具按名称调用。

---

## 技能优先级（来自全局 CLAUDE.md）

当项目需要前端设计时，按以下优先级调用技能：

| 优先级 | 技能名 | 用途 |
|--------|--------|------|
| 1 | `redesign-existing-projects` | 审计现有 UI → 诊断通用 AI 模式 → 针对性升级 |
| 2 | `taste-skill` 系列 | 反套壳、高自主性设计方向（落地页 / 作品集 / 品牌） |
| 3 | `baseline-ui` | Tailwind 项目：动画时长、排版比例、组件无障碍、布局反模式 |
| 4 | `impeccable` | 像素级 UI 打磨与设计 QA（marketplace: `pbakaus/impeccable`） |
| 5 | `gsap-skills` | 高级滚动触发动画与动效设计 |

---

## 1. redesign-existing-projects — 设计升级审计

**文件:** `~/.agents/skills/redesign-existing-projects/SKILL.md`

### 工作流程

1. **Scan** — 识别框架、样式方案（Tailwind / vanilla CSS / styled-components）、现有设计模式
2. **Diagnose** — 按审计清单逐条检查并列出所有问题
3. **Fix** — 在现有技术栈上应用定向升级，不重写，只改进

### 修复优先级（由高到低）

1. **字体替换** — 视觉提升最大，风险最低
2. **调色板清理** — 移除冲突或过饱和的颜色
3. **悬停/激活状态** — 让界面有生命感
4. **布局与间距** — 正确网格、max-width、一致的内边距
5. **替换通用组件** — 用现代替代方案替换陈词滥调的组件模式
6. **补充加载/空/错误状态** — 让界面感觉完成
7. **打磨排版比例与间距** — 最后的高级触点

### 审计维度

- **排版:** 字体选择、标题存在感、正文宽度 (65ch)、字重层次、数字字体、字母间距、孤行
- **色彩与表面:** #000 纯黑禁止、饱和度 < 80%、仅一个强调色、冷暖灰统一、禁止 AI 紫蓝渐变、阴影着色、纹理叠加
- **布局:** 打破对称居中、3 列等宽卡片禁止、`h-screen` → `min-h-[100dvh]`、避免复杂 flex 百分比计算、卡片底部对齐、光学对齐
- **交互与状态:** hover/active/focus 状态、loading skeleton、empty state、error state、页面导航标识、平滑滚动
- **内容:** 真实名称、有机数据、禁止 AI 文案套话 ("Elevate"/"Seamless")、句子大小写
- **组件模式:** 卡片阴影/边框/白色背景三件套、FAQ 手风琴、3 卡片轮播推荐、定价表、模态弹窗滥用
- **图标:** 禁止 Lucide/Feather 作为默认、避免陈腐隐喻（火箭=启动）、统一描边宽度、favicon
- **代码质量:** 语义 HTML、禁止内联样式、相对单位、alt 文本、z-index 体系、死代码清理
- **策略性遗漏:** 法律链接、返回导航、自定义 404、表单验证、skip-to-content

---

## 2. baseline-ui — Tailwind CSS 基线约束

**文件:** `~/.agents/skills/baseline-ui/SKILL.md`

### 使用方式

```
/baseline-ui              # 对当前对话中所有 UI 工作应用约束
/baseline-ui <file>       # 审查指定文件，输出违规 + 原因 + 修复建议
```

### 核心约束速查

| 类别 | 规则 |
|------|------|
| **技术栈** | Tailwind CSS 默认值优先；动画用 `motion/react`；微动画用 `tw-animate-css`；class 合并用 `cn` |
| **组件** | 无障碍基元 (`Base UI` / `React Aria` / `Radix`)；图标按钮必须有 `aria-label`；禁止手写键盘/焦点行为 |
| **交互** | 破坏性操作用 `AlertDialog`；加载态用骨架屏；`h-screen` → `h-dvh`；固定元素尊重 `safe-area-inset`；错误信息就近显示 |
| **动画** | 仅当明确要求时才加动画；只动画 `transform`/`opacity`；禁止动画 `width`/`height`/`top`/`left`；入场用 `ease-out`；交互反馈 ≤ 200ms；循环动画离屏暂停；尊重 `prefers-reduced-motion` |
| **排版** | 标题用 `text-balance`，正文用 `text-pretty`；数据用 `tabular-nums`；密集 UI 用 `truncate`/`line-clamp`；禁止修改 `letter-spacing` |
| **布局** | 固定 z-index 体系；方形元素用 `size-*` |
| **性能** | 禁止动画大面积 `blur()`/`backdrop-filter`；禁止在非活动动画上使用 `will-change`；禁止用 `useEffect` 替代渲染逻辑 |
| **设计** | 无明确要求禁止渐变；禁用紫色/多彩渐变；禁用发光效果作为主要视觉元素；默认使用 Tailwind 阴影体系；空状态给出一个明确的下一步操作；每视图最多一个强调色 |

---

## 3. impeccable — 像素级 UI 打磨

**来源:** marketplace `pbakaus/impeccable`

### 用途
- 视觉 QA：检测像素偏移、对齐问题、间距不一致
- 精细化：对已完成页面进行逐像素级别的校正
- 跨浏览器一致性检查

> 此技能从 marketplace 安装，调用时使用完整引用或已安装的技能名。

---

## 4. gsap-skills — 高级动效设计

### 用途
- GSAP ScrollTrigger 驱动的滚动叙事（sticky-stack、horizontal-pan、parallax）
- 物理级动效（spring physics、magnetic hover、inertia scrolling）
- 时间线编排（staggered entry、sequence animation）

### taste-skill 中内置的 GSAP 规范（Section 5）

**Sticky Stack（卡片堆叠滚动）:**
- `start: "top top"`（非 `"top center"`）
- `pin: true`, `pinSpacing: false`
- 最后一张卡片不 pin
- 使用 `useReducedMotion()` 降级

**Horizontal Pan（水平滚动劫持）:**
- 同样 `start: "top top"`, `pin: true`
- `end: "+=${distance}"`（滚动距离 = 轨道宽度 - 视口宽度）
- `scrub: 1`；`invalidateOnRefresh: true`

**禁止模式:**
- `window.addEventListener("scroll", ...)` 硬性禁止
- 禁止 `window.scrollY` 驱动 React state
- 禁止 `requestAnimationFrame` 触碰 React state
- 禁止 GSAP / Three.js / Motion 在同一组件树混用

---

## 5. taste-skill 系列 — 反套壳前端设计

### 5.1 design-taste-frontend（主力技能）

**文件:** `~/.agents/skills/design-taste-frontend/SKILL.md`

#### 设计流程（三阶段）

**阶段 0: 设计读解（Brief Inference）**
- 输出一行 "设计读解"：`"Reading this as: <页面类型> for <受众>, with a <氛围> language, leaning toward <设计系统或美学流派>."`
- 读取信号：页面类型、氛围词、参考信号、受众、已有品牌资产、隐性约束
- 模糊时只问一个问题，不猜测

**阶段 1: 三旋钮设定**

| 旋钮 | 范围 | 基线值 |
|------|------|--------|
| `DESIGN_VARIANCE` | 1=完美对称 / 10=艺术混乱 | 8 |
| `MOTION_INTENSITY` | 1=静态 / 10=电影级 | 6 |
| `VISUAL_DENSITY` | 1=美术馆/通风 / 10=驾驶舱/密集 | 4 |

**阶段 2: 设计系统映射**
- 真实设计系统优先（Fluent UI / Material 3 / Carbon / Polaris / Primer / GOV.UK / USWDS）
- 纯美学方向（Glassmorphism / Bento / Brutalism / Editorial）

#### 核心禁令（AI Tells）

- **排版:** Inter 不作为默认字体；Fraunces 和 Instrument_Serif 严格禁用为默认衬线体
- **色彩:** 禁止 AI 紫蓝渐变；禁止 premium-consumer 默认 beige+brass+oxblood+espresso 调色板（cookware/wellness 类项目）
- **布局:** 禁止 3 列等宽卡片特性行；禁止连续 3+ 个 zigzag 图片+文字交替段落
- **Eyebrow 限制:** 每 3 个 section 最多 1 个 eyebrow（`uppercase tracking` 小标签）
- **Em-dash 禁令:** `—` 和 `–` 字符完全禁止！任何可见位置都不允许出现
- **Hero:** H1 ≤ 2 行；副文本 ≤ 20 词且 ≤ 4 行；CTA 不需滚动即可见；顶部 padding ≤ `pt-24`；最多 4 个文本元素
- **内容:** 禁止 "Elevate"/"Seamless"/"Unleash" 等 AI 套话；禁止 "Quietly in use at" 式社会证明标题
- **禁止 `<div>` 假截图:** 用真图、生成图，或直接跳过

#### 发布前检查清单（Pre-Flight Check）

包含 60+ 项强制检查，涵盖：设计读解、旋钮、设计系统、em-dash 禁令、主题一致性、色彩/形状一致、按钮对比度、Hero 规则、eyebrow 计数、布局重复、内容密度、动效动机、图片策略、无障碍、性能等。

### 5.2 gpt-taste — 精英 UX/UI + GSAP

**文件:** `~/.agents/skills/gpt-taste/SKILL.md`

- **Python 随机化:** 输出前模拟 Python `random.choice()` 选择 Hero 架构、字体栈、组件、GSAP 范式
- **AIDA 结构:** Attention → Interest → Desire → Action
- **Hero 铁律:** H1 不超过 2-3 行，使用超宽容器 (`max-w-5xl` / `max-w-6xl`)
- **无间隙 Bento Grid:** 强制 `grid-auto-flow: dense`；3-5 张精选卡片 > 8 张杂乱卡片
- **GSAP ScrollTriggers:** 严格的 pinning / stacking / scrubbing / 文字渐显

### 5.3 stitch-design-taste — Google Stitch 设计系统

**文件:** `~/.agents/skills/stitch-design-taste/SKILL.md`

- 为 Google Stitch 生成 `DESIGN.md` 文件
- 语义化设计语言：氛围、色彩、排版、组件行为、布局原则、动效哲学、反模式

### 5.4 辅助技能

| 技能 | 用途 |
|------|------|
| `high-end-visual-design` | 高端 agency 级设计教学：字体、间距、阴影、卡片结构、动画 |
| `minimalist-ui` | 简洁编辑风格：暖单色调、排版对比、扁平 bento、柔和粉彩 |
| `industrial-brutalist-ui` | 工业粗野主义：瑞士印刷 × 军事终端、刚性网格、模拟衰变效果 |
| `brandkit` | 高端品牌套件图像生成：logo 系统、身份牌组、视觉世界 |
| `imagegen-frontend-web` | 落地页图像方向生成：每个 section 独立图像 |
| `imagegen-frontend-mobile` | 移动端 App 屏幕概念图生成 |
| `image-to-code` | 图像→代码：先生成设计图像，深度分析后实现匹配 |
| `full-output-enforcement` | 完整输出强制：禁用占位符模式、处理 token 限制分割 |

---

## 快速决策矩阵

| 需求场景 | 首选技能 | 流程 |
|----------|----------|------|
| 现有项目 UI 升级 | `redesign-existing-projects` | Scan → Diagnose → Fix |
| 新建落地页/作品集 | `design-taste-frontend` | Brief → Dials → Design System → Build → Pre-Flight |
| Tailwind 组件审查 | `baseline-ui` | `/baseline-ui <file>` |
| 视觉 QA / 像素对齐 | `impeccable` | 页面完成后逐像素审查 |
| 滚动动效实现 | `gsap-skills` + taste-skill Section 5 | 规范骨架 + 项目适配 |
| 品牌/Logo 设计 | `brandkit` | 生成品牌套件 |
| 高级动效+创意布局 | `gpt-taste` | AIDA + GSAP ScrollTriggers |
| Google Stitch 设计 | `stitch-design-taste` | 生成 DESIGN.md |
| 移动端设计 | `imagegen-frontend-mobile` | 生成概念图 → 实现 |
| 图像→代码还原 | `image-to-code` | 生成设计图 → 分析 → 实现 |

---

## 技术栈默认约定

- **框架:** React / Next.js（优先 Server Components）
- **样式:** Tailwind v4（默认）→ v3（存量项目兼容）
- **动画:** Motion（`motion/react`）用于 UI 动画；GSAP + ScrollTrigger 用于全页滚动叙事
- **图标:** Phosphor / HugeIcons / Radix Icons / Tabler Icons（Lucide 需明确请求）
- **字体:** `next/font` 或自托管 + `font-display: swap`
- **视口:** `min-h-[100dvh]`，禁止 `h-screen`
- **z-index:** 系统化分层（nav / modal / overlay / grain），禁止随意 `z-50`

---

## 项目结构

```
F:\ikeu_runningerrands\frontend\
├── admin/          # 管理后台（React + Vite + TypeScript）
├── mobile/         # 移动端（uni-app + Vue）
├── CLAUDE.md       # 本文件
└── ../skills/      # 技能文件（实际位于 ~/.agents/skills/）
```
