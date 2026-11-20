# 样式落地细节

配套 [`../SKILL.md`](../SKILL.md) 第三节「样式：静态进组件 sheet，内联只留给动态值」。这里只放**怎么写**与**坑**。

## 1. 内联：只写运行时才知道的值

内联不是默认落点。它解决的问题只有一个：**值在运行时才知道，而且会变**——拖拽位置、滚动进度、用户挑的
颜色、第三方库塞回来的尺寸。这种属性写成 `style`，一个节点一条；其余一切静态声明都进组件 sheet（第 2 节）。

判据：这个值能不能在代码里枚举完？能（常态/选中、可用/不可用、成功/失败）→ class 切换；不能 → 内联。
内联时**只写那一个属性**，不要因为「顺手」把 padding、边框、字号一起写成内联。

### TypeScript / 原生 DOM

```ts
// 值来自运行时：唯一适合内联的地方
progress.style.width = `${percent}%`;

// 更好：把动态值交给 sheet，规则本身仍是静态的、还能复用
list.style.setProperty('--progress', `${percent}%`);
// list.css: .x-progress { width: var(--progress) }
```

### Kotlin / JS（kotlin-wrappers + csstype）

`HTMLElement.style` 来自 lib.dom，属性的类型是 `String`。要拿到类型化视图，用 csstype 的
`Properties`（本项目里由 `ktjs-dom-kit` 的 `HTMLElement.inlineStyle` 提供，实现就是一次
`unsafeCast(style)`——同一个 `CSSStyleDeclaration` 对象，只是换成类型化接口看它）：

```kotlin
// 只写动态的那一个属性
progress.inlineStyle.width = percent.pct

// 动态值经自定义属性传进静态规则
list.inlineStyle.setProperty(CustomPropertyName<Any>("--progress"), percent.pct.toString())
```

`inlineStyle` 的价值在于**类型**，不在于让静态样式走内联：`Display.flex`、`Padding(...)` 这些取值词汇
同样用在 `cssSheet { }` 里（见第 2 节），两边共用一套。

常用取值词汇（`web.cssom`）：

| 想要 | 写法 |
| --- | --- |
| 长度 | `1.5.rem`、`16.px`、`100.vw`、`50.pct`、`0.px` |
| 百分比 / 计算 | `50.pct`、`50.pct - 50.vw`（生成 `calc(50% - 50vw)`） |
| 流式尺寸 | `clamp(2.25.rem, 5.vw, 3.25.rem)`、`min(17.rem, 100.pct)`、`max(...)` |
| 无单位数字（行高、opacity、flex-grow/shrink） | `number(1.65)`、`number(0.0)` |
| 整数（font-weight、z-index） | `integer(600)`、`integer(-1)` |
| 关键字 | `Display.flex`、`Position.relative`、`Isolation.isolate`、`Overflow.clip`、`TextAlign.center`、`FlexWrap.wrap`、`AlignItems.center`、`JustifyContent.center`、`AutoRepeat.autoFit`、`AutoLength`/`Auto.auto` |
| `none` | `None.none`（它同时是 `Display` / `PointerEvents` / `TextDecorationLine` / `BackgroundImage` … 的子类型） |
| 颜色（含主题变量） | `Color("var(--accent, var(--ifm-color-primary))")`、`Color.transparent`、`Color("inherit")` |
| 简写 | 单值直接赋长度（`padding = 1.5.rem`）；多值用 `Padding(vertical, horizontal)`、`Padding(top, horizontal, bottom)`、`Padding(top, right, bottom, left)`、`Margin(0.px, Auto.auto)`、`Border(1.px, LineStyle.solid, Color.transparent)` |
| 阴影 / 滤镜 / 变换 | `dropShadow(0.px, 14.px, 28.px, Color(...))`、`translatex(-50.pct)`（`BoxShadow(...)`、`Transition(...)` 有工厂，但带状态时按下面第 3 条留在 CSS 里） |

闭合联合类型里没有工厂的值（例如 `repeat(auto-fit, minmax(min(17rem, 100%), 1fr))`——cssom 的
`minmax` 两端要求 `GridAutoTracks`，`min()` 不是），用 `unsafeCast("…")` 并**在注释里写明为什么**。
不要因为写不出来就退回 `String`：那等于放弃这层类型检查。

## 2. 组件自己的静态 sheet（默认落点）

**静态声明全放进这里**，节点只带语义 class。与组件同目录——一个组件一份，不合并成「首页样式表」：

```
home/
├── Hero.ts        + hero.css          # 这个组件的规则 + 它自己的 sheet
├── Features.ts    + features.css
├── NextSteps.ts   + next-steps.css
├── Section.ts     + section.css       # 两个分区共用的外壳（真共享）
├── base.css                           # :host + box-sizing，所有组件都用
└── styles.ts                          # ?inline 收集 + 惰性 sheet
```

注入方式是**一个组件类一份 `CSSStyleSheet`，该组件的所有实例 adopt 同一份**（Lit 的 `static styles`
就是这个模型）。不要每个实例建 `<style>` 节点，也不要每个实例 `new CSSStyleSheet()`：规则是静态的，
一份就够，实例多时差别是「一份解析」对「N 份文本 + N 次解析」。

```ts
// styles.ts：每份文件惰性建一次，导出取 sheet 的函数
function lazySheet(css: string): () => CSSStyleSheet {
  let sheet: CSSStyleSheet | null = null;
  return () => {
    if (!sheet) {
      sheet = new CSSStyleSheet();
      sheet.replaceSync(css);
    }
    return sheet;
  };
}
export const heroStyles = lazySheet(heroCss);

// 组件：共享 reset 在前，自己的规则在后
const shadow = this.attachShadow({mode: 'open'});
shadow.adoptedStyleSheets = [baseStyles(), heroStyles()];
```

```kotlin
// 同文件、同一个类里：规则文本 + 该类的 sheet（companion 是 Kotlin 放「类静态成员」的地方，
// 对应 Lit 的 static styles；名字统一成 CSS / SHEET，每个元素类都可复用这两个名字）
class Action : CustomElementAdapter() {
    init {
        val shadow = attachStyledShadow(HOME_RESET, SHEET)
        …
    }

    companion object : CustomElementReferenceAdapter<Action>(Action::class.js, HomeMarkup.ACTION) {
        // NAME 由适配器提供（TagName<Action>）；读它一次就完成注册 + 创建，不需要 newAction() 这种工厂

        private val CSS: String = """
            .ktjs-button-primary:hover { … }
        """.trimIndent()

        private val SHEET: CSSStyleSheet = CSSStyleSheet().apply { replaceSync(CSS) }
    }
}
```

规则变多、值开始重复之后，可以把这段文本换成 csstype 的规则 DSL，序列化交给
`@emotion/serialize` + `stylis`（只声明用到的导出，不要写完整 wrapper）：

```kotlin
// 自定义属性名各自一个具名常量：字符串只写一次，类型参数是 hook 承载的值类型
private val cardBorderHook = CustomPropertyName<Color>("--ktjs-card-border")
private val shadowHook = CustomPropertyName<BoxShadow>("--ktjs-shadow")
private val cardClass = ClassName("ktjs-feature-card")   // 选择器也类型化，invoke 负责补点号
// 一次性用可写 (ClassName("x")) { … }：不加括号会被当成「工厂调用 + 尾随 lambda」

private val SHEET: CSSStyleSheet = cssSheet {          // ktjs-dom-kit：emotion + stylis 序列化
    cardClass {
        border = Border(
            1.px,
            LineStyle.solid,
            var(cardBorderHook, rgb(127, 127, 127, 0.24)),   // 兜底值也是类型化的
        )
        transition = many(                                  // cssom 的多值拼接
            Transition(PropertyName.borderColor, 0.2.s, TransitionTimingFunction.ease),
            Transition(PropertyName.boxShadow, 0.2.s, TransitionTimingFunction.ease),
        )
        Pseudos.hover {                             // 伪类也是类型化常量
            boxShadow = var(shadowHook, BoxShadow(0.px, 10.px, 24.px, rgb(31, 111, 235, 0.18)))
        }
        pseudos(Pseudos.hover, Pseudos.focusVisible) { … }   // 多个状态共用一个块（many 拼 selector）
    }
    `@media`(MediaType.screen and maxWidth(576.px)) {     // csstype.RuleBuilder 自带
        cardClass { display = Display.block }
    }
    supports(                                              // SupportsQuery 仿 MediaQuery：条件也类型化
        supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text) or
            supportsDeclaration(webkitBackgroundClipName, BackgroundClip.text),
    ) { … }
}
```

收益不看行数，看**还剩多少 CSS 文本要手写**：同一段规则手写是 455 个字符的 CSS，上面一条声明也不用
手写，只剩名字（自定义属性名）。几条实测约束：① 伪类用 `Pseudos` + `Pseudos.hover { }` / `pseudos(…)`
（`TagName` + `HtmlTagName` 那套形态，64 个常量、每个就是伪类文本本身，如 `Pseudos.hover` = `:hover`）；
短写法来自 `context(RuleBuilder<T>)` + `operator Pseudo.invoke`（Kotlin 2.2+ context parameters，调用点要
import `…dom.css.dsl.invoke`），和官方 `hover { }` 一样、能组合，`&` 由这个作用域补（逗号分开的每一支各补
一个，否则后几支会被 stylis 当成后代）；`&` 不在值里，所以同一个常量也能当选择器零件
（`pseudo(Pseudos.not, className(HIDDEN))`）。`hover {}` 糖自带的选择器是 `:hover`、没有 `&`，会被
stylis 当成**后代**选择器，而且就算上游 JetBrains/kotlin-wrappers#2933 修好也写不出组合态，别等它；
② 过渡属性名用 `PropertyName.borderColor`（`PropertyName<T> : Ident`、
`IdentType` 里包含 `TransitionProperty`），**不需要 cast**；③ at-rule 别写 `"@media …"` 字符串，官方有
`RuleBuilder.@media` 与 `web.cssom.AtRules`，媒体条件有 `MediaType` / `maxWidth` / `prefersColorScheme`
等一整包工厂；`@supports` 条件用 `SupportsQuery` + `supportsDeclaration(PropertyName<T>, T)` +
`and/or/not`（仿 `MediaQuery` 补的），别传条件字符串；④ cssom 缺的（`radial-gradient`、`-webkit-` 属性、
`pointer-events:none`、supports 条件类型）在项目自己的 `css` 包里按官方风格补，别退回文本；
⑤ 不要用 `kotlin-css`（`kotlinx.css`）那套旧 DSL——值不做校验、
`transition` 会补 `0s`、`&` 不解析；逐条 `insertRule` 灌 CSSOM 也不要：`fallback()` 阶梯会被压成最后一条。
完整清单见 `ktjs-dom-kit` 站点的「Typed stylesheets」页：<https://shijianjs.github.io/ktjs-dom-kit/dom-kit/css-sheet/>
（源码在该仓库的 `docs/src/jsMain/resources/docs/dom-kit/css-sheet.md`，中文版在对应的 `i18n/zh-Hans/` 下）。

类名只留给这些规则选中的节点：每个被规则选中的节点带一个语义 class，没人用的 class 是噪音（读者会以为
别处还有规则在用）。反过来说，既然静态规则都在这儿，**节点就应该带 class**——别指望用内联去省掉它。

没有 shadow root 的页面（普通 DOM、MDX 首页）怎么办：把每份组件 sheet 收集起来，由页面**一次性** adopt 到
`document`（或所属的 shadow root），规则用组件根 class 做前缀把作用域圈住：

```kotlin
// 页面入口：一份 reset 在前，各组件自己的 sheet 在后，全部只 adopt 一次
internal fun installPageStyle(vararg componentSheets: CSSStyleSheet) {
    document.adoptedStyleSheets = arrayOf(*document.adoptedStyleSheets, RESET_SHEET, *componentSheets)
}
```

```kotlin
// 组件文件里：规则 + 该组件的 sheet，与组件本身同一个文件
internal val BROWSER_CLASS: ClassName = ClassName("browser")
internal val CELL_CLASS: ClassName = ClassName("cell")

internal val BROWSER_SHEET: CSSStyleSheet = cssSheet {
    BROWSER_CLASS { marginTop = 14.px }
    CELL_CLASS { padding = Padding(9.px, 12.px); borderBottom = Border(1.px, LineStyle.solid, hairline) }
}
```

作用域靠「组件专属 class 名 + 根 class 前缀」而不是全局标签选择器：`.browser .cell` 这类规则只会命中
这个组件，不会误伤别处同名的 `td`。

**惰性创建是 SSR 的要求**：`CSSStyleSheet` 在 Node 里不存在，模块级 `new CSSStyleSheet()` 会让预渲染
直接崩。上面两种写法都是第一次用到才建（Kotlin/JS 的顶层 `val` 本身也是惰性初始化，但只在浏览器侧
被求值的模块里才安全）。

## 3. 内联与状态的冲突（最容易做错）

```ts
// 反例：hover 永远不生效
run.style.backgroundColor = 'var(--accent)';
// 组件 CSS: .x-run:hover { background-color: var(--accent-strong); }
```

内联声明优先级高于任何选择器规则（`!important` 除外，而 `!important` 只会让下一个人更难改）。
所以：

- **被 `:hover` / `:focus-visible` / `[aria-pressed]` 改变的属性**（背景、边框色、字色、阴影）
  → 连同它们的 base 值、`transition` 一起写在组件 CSS 里。
- **被 `@media` 改变的属性**（窄屏换成的 `padding`、`flex-direction`）同理：内联会让断点失效。
  要么这组属性留在 CSS，要么改写成 `clamp()` 之类的流式值、把断点整段删掉——后者更省事。
- **布局属性**（`display`、`gap`、`padding`、`border-radius`、字号）**本来就在 sheet 里**，不要挪去内联：
  它们是最典型的「一对多」声明，几十个同类节点共用一份。
- **简写与长写**：内联 `border` / `margin` / `padding` 会同时写死各长写属性，CSS 里的
  `border-color` / `margin-top` 就盖不住。要么整组留在 CSS，要么整组内联，不要一半一半。
- **`@supports` 守卫的增强**（例如 `background-clip: text` 的渐变标题）留在 CSS：内联没法条件降级，
  而内联的 `color` 又会压住 CSS 里的 `color: transparent`。
- **`:host` 自身**：自定义元素的 `display` 不能从构造函数写（给 `this` 加属性会让
  `document.createElement` 抛错），所以 `:host { display: block }` 只能落在共享 reset 里。

## 4. 迁移：从「内联铺满」到「sheet + class」

早期按「全部内联」写出来的页面，会出现大量重复的 `style=`（同一套 padding / 边框 / 字号在 N 个节点上各写
一遍）。迁移步骤：

1. **按节点种类归类**：把属性相同的节点归成一种（表格单元格、列表项、徽标……），每种一个语义 class。
2. **每类抽一条规则**，写进**该组件自己的 sheet**（不是公共样式表），值照抄原来内联的那份。
3. **删掉内联**，只留真正动态的属性（运行时才知道的那几个）；两态/多态改成 class 切换。
4. **检查规则的作用域**：用组件根 class 前缀或组件专属 class 名，别留裸标签选择器。
5. **剩下跨组件重复的规则**才进共享文件：reset、主题 token、父容器布局。一份都不剩就把共享文件删掉。
6. **流式尺寸替换断点**：能写成 `clamp()` / `min()` / `max()` 的 `@media` 就消掉；真正的断点才留。
7. **在浏览器里核对**：计算样式（`getComputedStyle`）、hover 态、窄屏布局，三者一起过一遍；顺手看一眼
   `outerHTML`，确认静态声明不再逐节点重复。只看构建通过说明不了样式对不对。

## 5. 主题与边界

- 主题颜色从页面继承进 shadow 树，所以组件里写 `var(--ifm-*)` / `var(--品牌-*)` 就够；
  `[data-theme]` 选择器留在**文档级**，组件里不要写。
- 需要给下游留覆盖点时，token 写成 `var(--组件-xxx, 回退值)`，覆盖点写在文档里的一处（例如
  `:root { --组件-accent: … }`），不要散落在各组件。
- `box-sizing` 不是继承属性：宿主的 `* { box-sizing: border-box }` 选不进 shadow 树，组件内所有盒子会
  退回 `content-box`，带 padding / max-width 的盒子尺寸随之变化（实测会把网格的列数阈值挪一档）。所以
  每棵树里都要 `*, *::before, *::after { box-sizing: border-box }`——这是共享 reset 的正当理由。
- 结果区、编辑器这类「必须复用宿主 light DOM 规则」的组件才退回 light DOM，并写清依赖了哪条规则；
  三方部件往 document 注入样式表时，shadow 边界会让它失效，这类部件也因此更适合留在 light DOM。

## 6. 共享样式的代价：为什么不主动共享

「把公共样式提取出来」在评审里几乎总被当成好事，所以这里把反对的理由写清楚——它不是审美偏好，是几条具体的坏结果。

- **全都共享，共享就不再共享。** 每段样式都被允许多个组件依赖时，「谁可以被谁改」这条边界就没了：读者看到一条规则，判断不出改动会波及哪几个页面，只能全量搜索。
- **全都私有，真正共享的那份也找不出来。** 私有规则铺满目录之后，「哪几条是大家一起用的」要从噪音里挑出来，改共享反而成了最危险的动作。
- **私有样式长在共享里，你不敢改、只敢加。** 它本来是某一个组件的细节，却因为住在共享文件里，任何改动都要所有组件一起接受；没人为它负责，于是只能往上叠，文件越来越长，最后谁也说不清哪条还在生效。
- **共享本身就是一份承诺。** `:host { display: block }`、`box-sizing`、reset、主题 token、父容器布局之所以能共享，是因为它们有明确语义（「每个组件都必须这样」），改它等于同时改所有人——这是一份可以被评审、被写进文档的契约。没有这份承诺的样式，不该共享。

所以顺序是：先默认私有（组件自己的 sheet），内联只当「运行时值」的例外，只有在两个以上组件**必须一起变**、
并且说得清那份承诺时，才升成共享。判定标准见 [`../SKILL.md`](../SKILL.md) 第三节与本文第 4 节。
