# 组件契约与平台差异

配套 [`../SKILL.md`](../SKILL.md) 第二节「保留模式」。这里放自定义元素的落地契约，以及 Kotlin/JS 与
TS 两套机制的差异。

## 1. 一个组件类的标准形状

```ts
class XCard extends HTMLElementBase {
  readonly #title: HTMLSpanElement;   // 会被改的节点 → 字段
  readonly #arrow: HTMLElement;

  constructor() {
    super();
    this.#title = el('span', {class: 'x-card-title'});
    this.#arrow = el('iconify-icon', {attrs: {icon: 'lucide:arrow-right', 'aria-hidden': 'true'}});
    const body = el('span', {class: 'x-card-body'}, (b) => b.append(this.#title));
    const root = el('a', {class: 'x-card'}, (a) => a.append(body, this.#arrow));
    const shadow = this.attachShadow({mode: 'open'});   // 构造函数里合法
    shadow.adoptedStyleSheets = [cardStyles()];         // 只有该组件自己的规则
    shadow.append(root);
  }

  connectedCallback(): void {
    // 只做一次性初始化：读标记、启动外部监听
  }

  setTitle(text: string): void {          // 命名 setter，只碰自己负责的节点
    this.#title.textContent = text;
  }
}
```

要点：

- **结构在构造函数里组装完毕**：元素从诞生起结构完整，setter 在连接前调用也安全，
  `connectedCallback()` 不再承担「挂载」职责。
- **构造函数里不得给 `this` 加属性或子节点**（Custom Elements 规范：`document.createElement` 与
  HTML 解析创建的元素会抛错）；`attachShadow()` 不在禁止之列，所以结构挂进 shadow root、
  light DOM 保持空。
- **一个组件一个 class，没有共享基类**（平台必需的 SSR 基类除外）。`create()` / `apply()` /
  `mount()` 这类模板方法会把通用刷新重新引进来。
- **字段初始化器按声明顺序求值**，不要在里面读声明在下面的字段（TDZ）；需要互相引用时在构造函数里组装，
  或用「第三参数 `init` 回调」就地描述不再单独引用的结构。

## 2. 生命周期与监听器归属

| 钩子 | 该做什么 |
| --- | --- |
| `constructor()` | 建树、挂 shadow root、给自有节点绑监听 |
| `connectedCallback()` | 读一次标记（属性/子标签）、启动外部监听（带幂等标志）、开始懒加载 |
| `disconnectedCallback()` | 解绑外部监听、清定时器、释放飞行中的异步结果 |
| `attributeChangedCallback()` | 只在 `observedAttributes` 里声明过的属性上触发；解析后调用命名 setter，**绝不重建结构** |

- 挂在**自有节点**上的监听写在构造函数：节点与元素同生命周期，不需要清理，也不会因重连重复注册。
- 挂在 **`document` / `window` / `matchMedia`** 上的必须在 `disconnectedCallback()` 解绑；注册要幂等，
  因为元素被移动或框架重挂载会再触发 `connectedCallback()`。
- 异步操作（懒 `import()`、fetch）落地前检查 `isConnected`，否则断开后仍会把节点塞进已经不属于页面的树。

## 3. 内容入口：属性与子标签

两条等价通道，最后都汇入命名 setter，**组件内部没有 `render()`**：

- **属性**：标量直接读（`title`、`section-title`），结构按 JSON 解析。属性变化走
  `attributeChangedCallback` → setter，框架只要 diff 属性就能驱动组件，不需要 ref。
- **子标签**：`<x-action>` / `<x-feature>` 这类，在 `connectedCallback()` 里读一次。手写标记时比 JSON
  属性可读。它们不是自定义元素、只是数据载体；被框架管理时**不要删**，用
  `x-hero:defined > * { display: none }` 隐藏。

约定：

- 属性名一律 kebab-case（HTML 属性大小写不敏感）。
- **不做 attribute reflection**：setter 不把值写回属性；与全局 HTML 属性同名的输入（如 `title`）消费后
  `removeAttribute`，免得浏览器把同一段文案再画成 tooltip。
- 新增内容形态 = 新增一个 setter，不要回头引入 `setData()` / `update()`。

## 4. Kotlin/JS：三个必须绕开的编译产物问题

Kotlin 类**不能**直接 `: HTMLElement` 走官方写法，前两条都会炸（2026-11 在 Kotlin 2.4.21 实测）：

1. **目标级别低于 ES2015 时**，`class X : HTMLElement()` 编译成
   `function X() { HTMLElement.call(this); }`，DOM 构造函数只接受 `new`，浏览器抛
   `Please use the 'new' operator`。最低要 `target = "es2015"`（更高档位在 2.5 之前不支持）。
2. **升到 es2015 之后**，Kotlin 会给继承来的 external 成员生成**自递归转发方法**：

   ```js
   querySelector(selectors) { return this.querySelector(selectors); }
   ```

   回调里第一句调 `querySelector` 就 `Maximum call stack size exceeded`。同时官方写法
   （`override val connectedCallback`）编译成 getter，浏览器只认方法，回调静默不执行。

3. **跨模块构造器的默认参数不要用函数字面量**：development 与 production 是两次独立编译，同一个
   构造器会算出**不同的混淆名**——库那侧只编译一次、导出 `new_…_imfsn8_k$`，development executable
   里却调用 `Entry.new_…_28n0j_k$`，于是 `jsBrowserDevelopmentRun` 白屏并抛
   `Uncaught TypeError: …new_…_k$ is not a function`（production bundle 反而是对的，e2e 全绿也照样
   会漏）。规矩：公开构造器的默认值只用常量（`null` / `0` / `""`），函数类型参数要么必填、要么默认
   `null` 由调用方 `?.invoke`。所以 **dev 产物也要单独验收一次**，别只跑 production 的 e2e。

因此用「适配基类 + 注册前剥桥」的固定做法，不要在元素类里手写转发：

- 基类声明全部生命周期钩子为 `@JsName("connectedCallback") fun`（等方法，不是 getter）。
- 注册前对**原型链上每一层**（Kotlin 每层都生成一份桥）`Reflect.deleteProperty` 掉这些名字，删到
  `HTMLElement.prototype` 之前为止。
- 之后元素里直接用原生拼写（`querySelectorAll`、`append`、`animate`、`focus`），不需要 `…Super`。
  元素类只写 `override fun connectedCallback()`，不再起别名。

自查：产物里 `grep 'HTMLElement.call'` 应为 0；`grep 'querySelector(selectors) {'` 不应出现。

## 5. 两套语法对照

| | TypeScript | Kotlin/JS |
| --- | --- | --- |
| 建节点 | `el('a', {class, href})`（按标签收窄，方法/对象属性进不来） | `TagName<T>.createElement { … }` |
| 自定义元素基类 | `HTMLElementBase`（Node 下回退空类），或直接 `HTMLElement`（若从不进 Node） | element adapter 基类 + 注册时剥桥 |
| 样式（静态） | 组件目录里的 `.css`（`?inline`）+ 语义 class | 同文件的 `X_CSS` / `cssSheet { … }` + 模块级 `X_SHEET`，节点只带 class |
| 样式（动态值） | 只对需要的那一个属性 `node.style.x = …` | 只对需要的那一个属性 `inlineStyle.apply { … }`，能换 class 切换的不要内联 |
| 注入 | `shadow.adoptedStyleSheets = [baseStyles(), ownStyles()]`（一个类一份，惰性建） | `attachStyledShadow(RESET, X_SHEET)`（顶层 `val` 首次访问才建） |
| 生命周期 | 方法覆写 | `@JsName` 方法，`override fun connectedCallback()` |
| 注册 | `customElements.define(name, Class)`，包一层 `defineIfMissing` 便于重复 import | 同上；名字用 `TagName` 常量持有 |

**不要在 `createElement` 上再包一层**（`element(tag, className, vararg children)` 这种）：`createElement`
的 lambda 已经是元素自己的接收者，`textContent`、`classList`、`append` 直接写在块里；多包一层反而只剩
「建完再 `.apply { … }`」这一条路，比直接用还长。确实需要按语义反复 create 的（`paragraph()`、`codeText()`）
才建命名 helper，而且它们内部也该写 `createElement { }`。

### 元素不是自己的时候：属性做成「绑定元素的对象」

`attr { }` 这种把属性委托给 attribute 的写法只适用于**自己的组件**（它在自己的元素上注册、上报变化、
持有默认值）。wrapper 仓库返回的元素（`createButton()` → `ButtonElement`、`createCard()` →
`CardElement`）是 `external` 类，成员列表固定，能力要靠**扩展属性交出一个操作对象**拿到：

```kotlin
val ButtonElement.labelText: Attribute<String> get() = attribute("aria-label")

button.labelText.value = "Close"   // 写
button.labelText.value             // 读
button.labelText.clear()           // 删
```

- 内置类型有工厂：`attribute(name)`（`String`）、`booleanAttribute`、`intAttribute`、
  `doubleAttribute`；自定义 codec 用 `attribute(name, codec)`——调用点不写类型参数。
- 对象不持状态：读走 `getAttribute`、写走 `setAttribute`，任何来源写入的值下一次读都能看到。
- 词列表（`theme`、`aria-describedby`、`data-tags`）用 `element.tokenListAttribute(name)` 拿
  `TokenListAttribute`，在它上面 `add` / `remove` / `toggle` / `clear` / `value`；写空列表即删属性。
- **属性名字符串只写一处**：与页面包并列开一个包（如 `wrapper/`），声明
  `val ThemePropertyMixin.themeAttr: TokenListAttribute get() =
  unsafeCast<HTMLElement>().tokenListAttribute("theme")`，页面里统一调 `themeAttr`。`external` 类上的
  属性名编译器查不出错，字符串重复一次就多一个错处。
- **没有 `by` 版本**：`external` 类上只能声明扩展属性，而 Kotlin 不委托扩展属性——这就是做成对象的理由。

## 6. 升级时机与命名

- **注册放在框架 hydration 之后**：静态 `<script>` 写在 `<head>` 会早于 hydration，元素先升级就让 React
  报 hydration mismatch（#418）。用框架的 client-module 钩子动态插入 `<script type="module">`。
- 脚本 `src` 用**带 baseUrl 的绝对路径**（`/my-site/x-client/index.mjs`），相对路径在深页面上会解析到
  `.../docs/intro/x-client/...`。
- 标签名一律带项目前缀（`ktjs-` / `dfk-` / `x-`），便于 grep，也避免与宿主冲突。
- 只在 Node 求值的包**不要** import 定义元素的模块：顶层 `class X extends HTMLElement` 会在 Node 求值时
  炸。把浏览器专属代码单独一个入口，由浏览器包加载。

### 由祖先切换显隐的 `<iconify-icon>` 要带 `noobserver`

`iconify-icon` 用 IntersectionObserver 决定要不要保留自己 shadow 里的 `<svg>`：判定不可见就删掉，
但 `status` 仍然是 `rendered`；它断连时又把 `_visible` 复位成 `true`。净效果是一个死循环——

1. 菜单/抽屉/折叠分组关掉 → 子项不可见 → `<svg>` 被删；
2. 元素断连 → `_visible` 回到 `true`；
3. 再打开 → 观察者报 `isIntersecting === true`，与 `_visible` 相同 → 不触发重绘 → 图标永久消失；
   元素还在、占着 18px 空位，`status` 还写着 `rendered`。

所以凡是**显隐由祖先决定**的图标（下拉项、折叠分组、抽屉里的列表）创建时就写
`setAttribute('noobserver', '')`——这是 iconify-icon 自带的开关，它跳过可见性观察，svg 就留在 shadow
树里。只在页面加载时可见的静态图标可以不管。验收要「关掉再打开」量一次，打开一次看不出来：
元素仍存在、`status` 仍是对的，只有 shadow 里的 svg 和宽度是 0。

## 7. 组合式外壳与页面契约

### 外壳组件：自己持 chrome，正文走 slot

「标题 + 提示 + 内容盒」这类重复结构做成**一个容器组件**，不是使用者继承的基类：

```text
<kmp-section>                  ← 外壳组件：自己的 shadow root = h2 + p + <slot>
  └─ shadow: h2 / p / slot     ← 标题与提示由外壳的 sheet 上色；:host 承担区块间距
```

- 使用者（各区块）把正文节点 append 到外壳这个元素上，由 `<slot>` 投影进外壳的盒子；正文仍在**使用者自己
  的 shadow 树**里，吃使用者的 sheet。
- 外壳**不** adopt 使用者的 sheet，使用者**不** adopt 外壳的 sheet（唯一共享的只有 reset）。
- 外壳提供命名 setter 改标题/提示（`setText(...)`），使用者不碰外壳内部节点。
- 对比反例：让使用者继承外壳、或把 `h2`/`p`/`div` 三个裸节点交给使用者自己塞——前者引入基类，后者让
  「外壳是容器」这件事消失，页面看起来还是散的。

### 页面：一个列表 + 一个契约接口

页面元素只做组合，最省事也最不容易漏的写法是让组件自己声明能力：

```kotlin
/** 页面挂载、并在换语言时就地重贴文案的部件。实现者都是自定义元素，所以它自己就是那个节点。 */
internal interface PageBlock {
    val node: HTMLElement get() = this as HTMLElement   // 9 个实现者零样板
    fun applyTranslation(translation: Translation)
}

internal class DemoPage : CustomElementAdapter(), … {
    private val blocks: List<PageBlock> = listOf(header, browser, invocation, …)
    init { blocks.forEach { shadow.appendChild(it.node) } }
    private fun applyTranslation(t: Translation) = blocks.forEach { it.applyTranslation(t) }
}
```

反例按「坏的程度」排：① 每个组件字段各写一次 `appendChild` + 换语言时各写一次调用（加一个区块要改两处，
且容易漏）；② 元素列表配一份 `(Translation) -> Unit` 的 lambda 列表（两套清单、契约落在页面这边）；
③ 用 `as?` 就地强转去找「谁能换语言」（拼写错误与漏实现都变成静默失效）。

三条纪律：契约由**组件**声明（不是页面替它声明）；页面只持有一个列表；新增部件 = 加一个字段 + 往列表加一行。
