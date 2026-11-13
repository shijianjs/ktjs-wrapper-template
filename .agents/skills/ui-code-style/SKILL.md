---
name: ui-code-style
description: 用原生 DOM 写 UI 时的组织与样式规范：目录/文件按业务语义切分而不是按技术类型（禁止 css/、utils/ 这种技术分类目录），组件用「保留模式」而不是 state→render，样式优先内联（element.style），只有 :hover / 伪元素 / 媒体查询这类内联表达不了的才写组件自己的小段 CSS、不堆成集中式大字符串，adoptedStyleSheets 只给真正共享的东西，图标用官方 iconify-icon。新增或评审 UI 组件、拆分目录、决定某段样式放哪、把页面拆成组件、被要求「统一管理 CSS」「用 CSS-in-JS」时使用；具体项目文档更严格时以项目为准。
---

# UI 代码风格

写任何界面代码（TS 原生 DOM、Kotlin/JS 自定义元素、Docusaurus/MDX 首页、样式表）时按这份规范落地。
主线只有三句话：

1. **按业务归属切分**，技术类型只是业务模块内部的次级文件。
2. **组件是一群长期存在的 DOM 节点**，不是 state 的函数。
3. **样式是组件的私有属性**：默认内联，只有内联表达不了才写 CSS，只有真正共享才提取。

动手前先答三个问题，答案决定文件放哪、代码怎么写、样式写在哪：

| 问题 | 判定 |
| --- | --- |
| 这个东西属于哪个业务？ | 决定它放进哪个文件夹（以及是否根本不该单独成文件） |
| 这段样式能被一条普通声明表达吗？ | 能 → 内联；不能 → 该组件自己的小段 CSS |
| 这段样式有两个以上组件必须一起变吗？ | 是 → 共享（reset / 主题 token / 父容器布局）；否 → 谁也不给 |

机制细节分别见 `references/elements.md`（组件契约、生命周期、两套平台差异）与
`references/styling.md`（样式三级阶梯的写法、CSS 取值类型词汇、常见坑）。

## 适用场景

- 新增一个 UI 组件、页面分区，或把一段页面拆成组件。
- 评审 UI 代码：目录结构、DOM 构造方式、样式落在哪。
- 被要求「把 CSS 集中管理」「抽一个公共样式文件」「换成 CSS-in-JS / CSS DSL」时（先按本规范判断是否该做）。
- 两种实现材料（代码与样式、代码与测试）该不该分开目录时。

## 一、组织：按业务语义切，可递归

**一个能力一处存放**：它的代码、样式、测试、资源都放在同一个目录；目录名回答「这东西属于哪个业务」，
而不是「这是 ts 还是 css」。技术类型只在业务模块内部做次级文件。

这条是**跨尺度的默认逻辑，可以递归**：类、模块、目录、包、子项目、项目、项目组，每层都问同一个问题。

- **类**：一个类的字段、方法、事件处理聚在一个 class 里，不拆成摊在多文件的 partial / mixin。
- **模块 / 目录**：`home/` 装齐首页那几件套的代码与样式；`sql/` 装齐可运行 SQL 的元素、运行时、渲染器。
- **包**：按能力分目录（`home/`、`toc-toggle/`、`theme/`），不按 `css/`、`elements/`、`utils/` 分。
- **项目 / 多层项目**：每个业务一个顶层目录，各自内部递归同一逻辑。

**只有物理上放不到一起才按技术栈分**：Rust 运行时与 npm 文档站构建系统、依赖管理、产物形态都不同，
强行合并只会互相污染——这是生态限制，不是「按技术分类更好」。能收拢的必须收拢。

技术类型分割的代价：业务与文件树正交，定位一圈要跳两次以上；样式与它服务的组件各在一处，改一处要读两处。

## 二、组件：保留模式，不做 mini 框架

**禁止** React / Vue / Lit 的「状态 → render → 重建 DOM」模型，也禁止引入它们的运行时。组件要像一个
**持有内部 DOM 对象的普通类**。冲突时按此优先级裁决（上面的赢）：

1. 保留 DOM 节点——节点长期存在，不随状态重建
2. 类字段持有 DOM 引用
3. 直接修改 DOM（property / attribute / text / class / 监听）
4. 局部更新
5. 必要时才替换**局部集合**
6. 禁止整组件 re-render
7. 禁止响应式 state 驱动 render

具体规则：

- **不拼 HTML 字符串**：不许 `innerHTML = \`...\``、不许 `insertAdjacentHTML`。节点一律
  `createElement` / 平台 helper 建出来。
- **字段持引用**：反复访问的节点存字段，直接对字段调方法。`querySelector` 不得当组件内部的状态管理，
  只用于「从外部挂载点找目标」这类不属于自身结构的地方。
- **命名 setter，不要通用刷新**：`setResult(v)`、`setLoading(b)` 只碰它负责的那几个节点。不允许
  `render()` / `update()` / `apply()` / `rebuild()` 这类通用入口，也不允许因为一个字段变化重建子树。
- **`replaceChildren()` 分级**：允许用于「确实需要整体更新的局部集合」（一次给出全新条目、固定的
  「图标 + 文本」组合）；禁止拿它清空组件自己的根子树再重建。变长列表能增量就增量（按长度增删、
  复用已有条目对象）。
- **内容入口是标记**：属性（标量直接读，结构按 JSON）或子标签（手工书写的可读写法），两条通道都只
  汇入命名 setter。不要开放「整份 data blob」入口——框架侧也不靠对象 prop 传内容（SSR 会把
  `[object Object]` 写进 HTML）。子标签被框架管理时**不要删**，用 CSS 隐藏。
- **构造函数建树**，`connectedCallback()` 只做一次性初始化（启动监听、注册外部资源）。构造函数里
  不得给 `this` 加属性或子节点（Custom Elements 规范），但 `attachShadow()` 合法，所以结构挂进
  shadow root、light DOM 保持空。
- **监听器按归属**：挂在自有节点上 → 写在构造函数，节点与元素同生命周期，无需清理；挂在
  `document` / `window` / `matchMedia` 上 → 必须在 `disconnectedCallback()` 里解绑，并带幂等标志
  （元素移动或框架重挂会再次触发连接）。
- **一个业务一个 class，没有组件基类**：不做 `create()` / `apply()` 式模板方法抽象——那会把通用刷新
  重新引进来。函数只留给纯工具。

## 三、样式：三级阶梯，能升一级就不升两级

| 层级 | 放什么 | 怎么落 |
| --- | --- | --- |
| 1. 内联 | 一条普通声明能表达的一切：盒模型、间距、字体、颜色、圆角、网格 | `el.style.* = …`，有类型库就用类型化 API |
| 2. 该组件自己的小段 CSS | 内联表达不了的：`:hover` / `:focus-visible`、`::before` / `::after`、`@media`、`@supports`、`@keyframes`、后代选择器 | 与组件同一个文件（同一目录），只注入该组件自己的 shadow root |
| 3. 真正共享的样式表 | reset、主题 token、父容器对子组件的统一布局、多个组件必须一起变的视觉 | 独立的共享入口，跨组件复用 |

判定与纪律：

- **样式写在渲染它的那段代码旁边**。一个元素的样式与它的 DOM 结构、行为逻辑应在同一处：改一处不用读两处。
- **不主动抽取**，不为「代码行数 / 文件数量 / 看起来整齐」拆分。两处属性相同不等于该提取；判断标准是
  是否存在**真实业务复用与共同变化关系**。
- **绝不生成集中式大字符串**：把几十上百行 CSS 堆进一个常量、再由各组件按 class 取用，等于把每个组件
  的私有样式挪进公共文件——这不是共享，只是搬运。评审看到几百行 CSS 常量就是违规。
- **`adoptedStyleSheets` 是共享机制，但要看清共享的粒度**。第二级那部分规则**写成该组件类的一份
  `CSSStyleSheet`**（模块级常量，或惰性建一次），由该组件的**所有实例** adopt —— 这就是 Lit
  `static styles` 的做法。**绝不要每个实例建一个 `<style>` 节点**：规则是静态的，一份 sheet 已经够用，
  每实例一份只是把同样的文本与解析开销乘以节点数（20×20 的空格、几十张卡片尤其明显）。再上一级才是
  跨组件共享（reset、主题 token、父容器布局）。
- **默认用 shadow root**：宿主全局 CSS 进不来、组件 CSS 漏不出去，不靠人工命名空间防污染。主题靠
  **继承的自定义属性**穿过边界（`var(--ifm-*)` / `var(--品牌-*)`），组件里不写 `[data-theme]` 选择器。
  只有必须复用宿主 light DOM 规则时才退回 light DOM，并说清依赖了哪条规则。
- **`:host` 的 `display` 与 `box-sizing` 只能写在 sheet 里**：自定义元素默认 `display: inline`，而构造
  函数里给 `this` 设 `style` 会加属性、让 `document.createElement(tag)` 抛错；`box-sizing` 不是继承属性，
  宿主的 `* { box-sizing: border-box }` 选不进来，带 padding 的盒子会按 content-box 算、布局阈值随之移位。
  两个都放进所有组件共用的那份 reset sheet。

## 四、内联 vs CSS：必须知道的边界

内联能做的：直接设置属性、动态修改、CSS 变量。内联**做不到**：伪类、伪元素、媒体查询、后代选择器、
`@keyframes`。

三条由此而来的硬约束：

1. **会被状态覆盖的属性必须留在 CSS 里**。元素上写了内联 `background-color`，`:hover` 的
   `background-color` 就永远不生效（内联优先级更高）。所以「hover 会变的那些属性 + 它们的 base 值 +
   `transition`」一起放进该组件的小段 CSS，其余布局仍内联。
2. **被 `@media` 覆盖的属性同理不能内联**。写在内联里的 `padding` 会让断点里的 `padding` 失效——要么
   那组属性留在 CSS，要么改写成 `clamp()` 之类的流式值后彻底删掉断点。
3. **简写与长写不要分家**。内联 `border` 会把 `border-color` 一起写死，CSS 里的 `border-color`
   就盖不住它——要么整组留在 CSS，要么整组内联。

能用 CSS 函数表达的别升级成媒体查询：`clamp()` / `min()` / `max()` / `calc()` 往往一条内联就够
（流式间距比断点更平滑）。真的需要断点时才写 `@media`。

## 五、页面与框架边界

- **页面用标记，不用组件树**：静态为主的首页写 MDX/HTML，标签是真正的自定义元素（`<x-hero>`、
  `<x-feature>`），元素自己渲染进 shadow root。确实需要计算 props 的交互再引组件树；不要为了「一个页面」
  拉进整个 React/Vue 组件层。
- **升级时机**：静态 `<script>` 写在 `<head>` 会早于框架 hydration，元素先升级会让 React 报 hydration
  mismatch。注册脚本放在 hydration 之后动态插入，或用框架提供的 client module 钩子。
- **不要删框架管理的子节点**：预渲染出来的子标签是真正被 React 管理的节点，删掉会让 fiber 树与真实 DOM
  失配；用 `x-*:defined > * { display: none }` 之类隐藏。
- `href` / `src` 写**相对页面**的路径，多语言前缀才不会写死。

## 六、图标

用官方 `<iconify-icon>`（npm `iconify-icon`），side-effect import 注册，`setAttribute('icon',
'lucide:sparkles')`。**不要**自己封装 SVG（不手写路径、不拼 `data:image/svg+xml`、不做 CSS mask 助手）。
数据契约里图标就是 Iconify 名字字符串。尺寸**只认 `font-size`**（内部是 `<svg width="1em">`），给宿主设
`width` / `height` 只会撑大空盒子。

## 七、两种落地语法

同一套规范，两套机制（细节与取值词汇见 `references/styling.md`、`references/elements.md`）：

| | TypeScript + 原生 DOM | Kotlin/JS |
| --- | --- | --- |
| 建节点 | `el('img', {class, alt, width})`（按标签收窄的 helper）；需要反复访问的节点存字段 | `TagName<T>.createElement { … }`；自定义元素继承 element adapter 基类 |
| 内联样式 | `node.style.padding = '1.5rem'` | `inlineStyle.apply { padding = 1.5.rem }`（类型化视图：`Display.flex`、`Padding(...)`、`Color("var(--x, …)")`） |
| 组件自己的 sheet | 组件目录里的 `.css` + `?inline`，惰性 `new CSSStyleSheet()`，所有实例 adopt 同一份 | 同文件里的 `private const val X_CSS` + 模块级 `private val X_SHEET`，`attachStyledShadow(RESET, X_SHEET)` |
| 跨组件共享样式 | 共享入口的 sheet（惰性创建，Node 求值时不碰 `CSSStyleSheet`） | 共享常量 sheet，同样交给 `attachStyledShadow` |
| 生命周期 | `constructor()` 建树 + `attachShadow()` | 构造函数/`init` 建树；注意编译产物差异 |

## 八、评审清单

写完或评审一段 UI 代码，逐条过：

- [ ] 目录/文件是按业务语义放的；没有为了「整齐」新开 `css/`、`utils/` 这类技术目录。
- [ ] 新组件没有引入 render/rebuild 式刷新；状态变化只改相关节点。
- [ ] 没有 `innerHTML` 拼标记；需要反复访问的节点有字段。
- [ ] 每个元素的样式在它自己的代码旁；没有几百行的集中式 CSS 常量。
- [ ] 是「一个组件类一份 sheet」——所有实例 adopt 同一份，没有每个实例建 `<style>` 或新建 sheet。
- [ ] 被 `:hover` / `:focus-visible` / `@media` 覆盖的属性没有写在内联里。
- [ ] 共享 sheet 里只有真正必须一起变的规则（reset、主题、父容器布局）。
- [ ] shadow 树里重新声明了 `box-sizing` 与 `:host { display: block }`；组件里没有 `[data-theme]` 选择器。
- [ ] 图标走 `iconify-icon`，尺寸用 `font-size`。
- [ ] 框架管理的子节点没被删；注册脚本在 hydration 之后。
- [ ] 流式尺寸优先用 `clamp()` / `min()` / `max()`，而不是堆断点。

## 九、禁止事项

- 禁止按技术类型组织的目录结构（`src/css/`、`src/utils/`、`src/components/` 一刀切）。
- 禁止 React / Vue / Lit 式状态驱动重渲染，以及任何 `render()` / `rebuild()` / `update(all)` 通用刷新入口。
- 禁止 `innerHTML` / `insertAdjacentHTML` / 模板字符串拼标记。
- 禁止把多个组件的样式集中到一个大字符串或一个公共 CSS 文件。
- 禁止每个组件实例建 `<style>` 节点或新 `CSSStyleSheet`（静态规则本来就只需要一份）。
- 禁止为了「少写几行」引入运行时注入式的 CSS-in-JS（把样式塞进 `document` 的 `<style>`）：它绕不开
  shadow root 的隔离。**类型化的 CSS DSL 不在此列**——用类型约束属性名、把「魔法字符串」压到只剩值层面，
  是这条规范想要的，不是它禁止的（csstype 的 `PropertiesBuilder` + `@emotion/serialize` 这类序列化器就是
  这条路）。判断标准不是源码行数：jOOQ 写出来比 SQL 文本长，也没覆盖 SQL 的全部细节，这不妨碍它比裸 SQL
  类型安全得多——看的是**还剩多少手写 CSS 文本**，不是行数。
- 禁止为了「可复用」提前抽取：没有两个以上必须一起变的调用方就不要提取。
- 禁止在构造函数里给自定义元素的 `this` 加属性或子节点。
- 禁止自研图标/ SVG 封装。

> 具体项目文档（如仓库的 `AGENTS.md` / `CONVENTIONS.md`）比本规范更严格时，按项目来；发现冲突先读回
> 项目文档，再把差异记进该项目，不要两边各写一半。
