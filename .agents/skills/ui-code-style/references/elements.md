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

## 4. Kotlin/JS：两个必须绕开的编译产物问题

Kotlin 类**不能**直接 `: HTMLElement` 走官方写法，两条都会炸（2026-11 在 Kotlin 2.4.21 实测）：

1. **目标级别低于 ES2015 时**，`class X : HTMLElement()` 编译成
   `function X() { HTMLElement.call(this); }`，DOM 构造函数只接受 `new`，浏览器抛
   `Please use the 'new' operator`。最低要 `target = "es2015"`（更高档位在 2.5 之前不支持）。
2. **升到 es2015 之后**，Kotlin 会给继承来的 external 成员生成**自递归转发方法**：

   ```js
   querySelector(selectors) { return this.querySelector(selectors); }
   ```

   回调里第一句调 `querySelector` 就 `Maximum call stack size exceeded`。同时官方写法
   （`override val connectedCallback`）编译成 getter，浏览器只认方法，回调静默不执行。

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
| 样式 | `node.style.x = …` / 组件目录里的 `.css`（`?inline`） | `inlineStyle.apply { … }` / 同文件的 `X_CSS` + 模块级 `X_SHEET` |
| 注入 | `shadow.adoptedStyleSheets = [baseStyles(), ownStyles()]`（一个类一份，惰性建） | `attachStyledShadow(RESET, X_SHEET)`（顶层 `val` 首次访问才建） |
| 生命周期 | 方法覆写 | `@JsName` 方法，`override fun connectedCallback()` |
| 注册 | `customElements.define(name, Class)`，包一层 `defineIfMissing` 便于重复 import | 同上；名字用 `TagName` 常量持有 |

## 6. 升级时机与命名

- **注册放在框架 hydration 之后**：静态 `<script>` 写在 `<head>` 会早于 hydration，元素先升级就让 React
  报 hydration mismatch（#418）。用框架的 client-module 钩子动态插入 `<script type="module">`。
- 脚本 `src` 用**带 baseUrl 的绝对路径**（`/my-site/x-client/index.mjs`），相对路径在深页面上会解析到
  `.../docs/intro/x-client/...`。
- 标签名一律带项目前缀（`ktjs-` / `dfk-` / `x-`），便于 grep，也避免与宿主冲突。
- 只在 Node 求值的包**不要** import 定义元素的模块：顶层 `class X extends HTMLElement` 会在 Node 求值时
  炸。把浏览器专属代码单独一个入口，由浏览器包加载。
