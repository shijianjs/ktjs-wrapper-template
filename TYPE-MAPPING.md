# kotlin-wrappers 式类型安全封装指南

这份说明面向「把上游 npm 包的 TypeScript 声明翻译成 Kotlin/JS `external` 声明」的 wrapper 项目，记录
[kotlin-wrappers](https://github.com/JetBrains/kotlin-wrappers) 官方的封装手法，以及在没有生成器 / seskar
插件时如何写出等价效果。新增或修改 wrapper 声明前先读一遍。

核心原则：**上游 `.d.ts` 里写了类型，Kotlin 侧就必须是被编译器检查的类型。** `dynamic` 只用于「上游
自己也只写了裸 `Function` / `any` 且实现无法推断」的情况；`JsAny` 只用于「上游写 `any` / `unknown` /
`object` / manifest 缺类型」的成员。退化到 `dynamic` 或 `JsAny` 是最后手段，不是省事手段。

参考实现（本地克隆优先，不要抓网页）：

- `kotlin-web/src/webMain/generated/web/http/BodyInit.kt`：联合类型
- `kotlin-browser/src/webMain/generated/web/html/HtmlTagName.kt`：字符串字面量
- `kotlin-cssom-core/src/webMain/generated/web/cssom/`、`kotlin-browser/src/webMain/generated/web/canvas/`：
  CSS / canvas 值类型
- `kotlin-js-plain-objects`：`js.objects.unsafeJso`

## 1. 上游类型 → Kotlin 类型对照表

| 上游 TypeScript | Kotlin |
| --- | --- |
| `interface X { ... }` / `type X = { ... }` | `external interface X`（不写 `: JsAny`） |
| `A \| B \| C` | `external interface AOrBOrC` + 每个成员一个同名工厂函数（第 3 节） |
| `"a" \| "b"` 字面量联合 | `sealed external interface X` + `X(String)` 工厂 + `Xs` 常量对象（第 4 节） |
| `string` | `String` |
| `number` | `Double`（`int` 也只映射到 `Double`，JS 没有整数类型） |
| `boolean` | `Boolean` |
| `string \| number` | 自定义联合 `StringOrDouble`，不要退化成 `Any` |
| `T[]` / `Array<T>` | `Array<T>` |
| `Set<T>` | `js.collections.JsSet<T>` |
| `Map<K, V>` / `Record<K, V>` / 索引签名 | `js.objects.Record<K, V>` 或 `Record<String, T>` |
| `Date` | `kotlin.js.Date` |
| `Promise<T>` | `kotlin.js.Promise<T>` |
| `(...) => R` | Kotlin 函数类型 `(A) -> R` |
| `any` / `unknown` / `object` / 缺类型 | `kotlin.js.JsAny`（可空时 `JsAny?`） |
| 裸 `Function` | 先按实现补签名；确实无从推断才 `dynamic` |
| `HTMLElement` 等 DOM 类型 | `web.html.*` / `web.dom.*`（kotlin-browser），禁止 `org.w3c.dom.*` |
| CSS 关键字 / 长度 / 颜色 | `web.cssom.*`、`web.canvas.*`（第 5 节） |

## 2. 结构类型：`external interface`，不要写 `: JsAny`

```kotlin
/** 上游 `TitleOption`。 */
external interface TitleOption {
    var text: String?
    var subtext: String?
}
```

- `external interface` 本身就是 JavaScript 值类型，Kotlin 2.x 里它隐式属于 `JsAny`；再写
  `: JsAny` 只是噪声，而且会让「这个类型没有 JS 语义」的错觉传播到所有引用处。**不要写。**
- 字段用 `var name: T?` 表达「可选」：「未设置」就是 `undefined`，读取时得到 `null`。
- 只读字段用 `val`，回调字段直接用函数类型。
- `external interface` 不能有方法体、不能有构造器、不能实现接口；有行为的对象用 `external class`（第 9 节）。
- 不要在 `external interface` 里放 `@JsName` 以外的注解，也不要放 companion（除非确实是静态成员）。
- **上游的 mixin 只声明一个 Kotlin 接口**，凡是上游继承了它的类型都在 Kotlin 里继承同一个接口，不要各自
  复制一遍字段：字段类型要改时只改一处，调用方也能把实例赋给 mixin 类型。

```kotlin
// 上游 `interface SymbolOptionMixin<T>` 被十几个 series / item 继承
external interface SymbolOptionMixin {
    var symbol: SymbolShape?
    var symbolSize: SymbolSize?
    var symbolRotate: SymbolRotate?
    var symbolKeepAspect: Boolean?
    var symbolOffset: SymbolOffset?
}

external interface LineSeriesOption : SeriesOption, SymbolOptionMixin { /* 只写自己的成员 */ }
```

- 反过来，**Kotlin 侧自建的公共基接口不要收进「各具体类型里类型不同」的成员**。上游 `markPoint.symbol`
  是 `string | callback`、`markLine.symbol` 是 `string[] | string`，写进公共基接口就只剩并集；应当把这个
  成员下放到各具体类型，基接口只保留真正一致的部分。判断标准：上游各具体类型对同名成员的声明是否逐字
  相同。

## 3. 联合类型：不透明接口 + 同名构造函数

kotlin-wrappers 的写法（`web/http/BodyInit.kt`）：

```kotlin
/** Union of: [ReadableStream], [Blob], [BufferSource], [FormData], [URLSearchParams], [String]. */
@SubclassOptInRequired(InternalApi::class)
external interface BodyInit

inline fun BodyInit(value: String): BodyInit = unsafeCast(value)
inline fun BodyInit(value: Blob): BodyInit = unsafeCast(value)
// ...
```

没有生成器时写同样形状，只是省略 `@SubclassOptInRequired`（那是给生成代码防止外部继承用的）：

```kotlin
/**
 * 上游 `String | number`。
 *
 * 用同名工厂函数包装，调用方必须显式说明选的是哪一支：
 * `grid.top = StringOrDouble(96.0)`。
 */
external interface StringOrDouble

inline fun StringOrDouble(value: String): StringOrDouble = value.unsafeCast<StringOrDouble>()

inline fun StringOrDouble(value: Double): StringOrDouble = value.unsafeCast<StringOrDouble>()
```

规则：

- **一个成员一个同名重载**，参数类型就是该成员的原生类型，不做额外包装。
- 工厂用 `inline` + `unsafeCast`，零运行时开销；不需要 `@JsName`，也不需要新增 JS 对象。
- 命名按上游顺序拼接（`StringOrDouble`、`BooleanOrDouble`、`StringOrDoubleOrArray`）；上游是
  `HTMLElement | string` 就是 `HTMLElementOrString`。
- 联合里出现 `JsAny` / `any` 分支时，整个联合收敛为 `JsAny`：那一刻已经无法区分分支，包装没有意义。
- 联合在多个文件出现时按 **Kotlin 包去重**，只声明一次，避免同名接口在不同文件里冲突；**同一语义的联合
  在所有字段上都用同一个类型**（如 symbol 的 shape / size / rotate / offset 各一个联合，被所有 series 和
  item 复用），不要为每个字段另造一个近似类型。
- 联合的成员本身是结构类型时，同样先声明不透明接口再嵌套工厂：

```kotlin
external interface PatternObject

fun PatternObject(value: ImagePatternObject): PatternObject = value.unsafeCast<PatternObject>()
fun PatternObject(value: SVGPatternObject): PatternObject = value.unsafeCast<PatternObject>()
```

- 反过来，**不要**把联合写成 `Any`、`String`（"反正都是字符串"）、`JsAny`，也不要让调用方自己
  `unsafeCast`。

## 4. 字符串字面量：`sealed external interface` + 常量对象

kotlin-wrappers 用 seskar 编译插件把字面量常量写成 companion 成员：

```kotlin
// kotlin-browser/.../web/html/HtmlTagName.kt
object HtmlTagName {
    inline val a: TagName<HTMLAnchorElement> get() = TagName("a")
    inline val abbr: TagName<HTMLElement> get() = TagName("abbr")
}

// kotlin-cssom-core/.../web/cssom/FontStyle.kt（seskar @JsValue）
sealed external interface FontStyle {
    companion object {
        @JsValue("italic") val italic: FontStyle
    }
}
```

不引入插件时的等价写法，项目里的统一约定：

```kotlin
/** 上游 `'value' | 'category' | 'time' | 'log'`。 */
sealed external interface AxisType

/** 包装来自 JavaScript 的值。优先用 [AxisTypes] 里的常量。 */
inline fun AxisType(value: String): AxisType = value.unsafeCast<AxisType>()

/** [AxisType] 的全部字面量。 */
object AxisTypes {
    inline val `value`: AxisType get() = AxisType("value")
    inline val category: AxisType get() = AxisType("category")
    inline val time: AxisType get() = AxisType("time")
    inline val log: AxisType get() = AxisType("log")
}
```

要点：

- **不要用 `enum class`**：运行时上游要的是字符串，Kotlin `enum` 会编译成对象（或序号），赋给 JS 属性
  类型立刻不对，必须 `name` 手动转，反而更危险。
- **不要用 `String` 常量（`const val`）+ `String` 字段**：拼错无法被编译器发现，等于没建模。
- `sealed external interface` 表达「值只能由库内产生、调用方无法自己实现」，编译器不允许在 Kotlin 侧
  `object : AxisType {}`。
- 常量用 `inline val ... get()`：内联成裸字面量，不产生静态初始化，也不额外分配。
- 保留 `Xxx(String)` 工厂：JS 返回值、配置读进来的字符串都要能包装，且这是唯一合法的「外部值」入口。
- Kotlin 关键字用反引号：`` `value` ``、`` `line` ``、`` `map` ``、`` `break` ``、`` `default` ``。
- 上游是「字面量 | 函数」时，再补一个函数重载（如 easing：`AnimationEasing((Double) -> Double)`）。
- **CSS 已有的值类型不要重新造**（`FontStyle`、`FontWeight`、`TextAlign`、`VerticalAlign`、`LineCap`、
  `LineJoin`），直接引用 `web.cssom` / `web.canvas`（第 5 节）。
- 测试逐条断言 `constant.unsafeCast<String>() == "上游字面量"`，抄错字面量是这类代码最常见的 bug。

## 5. CSS / canvas 相关字段：先用现成类型

| 语义 | 类型 |
| --- | --- |
| `font-style` | `web.cssom.FontStyle`（`FontStyle.italic` …） |
| `font-weight` | `web.cssom.FontWeight`（`FontWeight.bold` …） |
| `text-align` | `web.cssom.TextAlign` |
| `vertical-align` | `web.cssom.VerticalAlign` |
| 线端 / 拐角 | `web.canvas.CanvasLineCap` / `web.canvas.CanvasLineJoin` |
| 长度（含单位） | `web.cssom.Length`，`12.px`、`1.5.em`、`Length("50%")` |
| 颜色 | `web.cssom.ColorType` / `web.cssom.Color`（`Color.transparent` 等） |
| `display` / `position` | `web.cssom.Display` / `web.cssom.Position` |

上游字段是「关键字或数字」时用联合（第 3 节）而不是 `Any`：

```kotlin
/** 上游 `FontWeight | number`。 */
external interface ZRFontWeight

fun ZRFontWeight(value: FontWeight): ZRFontWeight = value.unsafeCast<ZRFontWeight>()
fun ZRFontWeight(value: Double): ZRFontWeight = value.unsafeCast<ZRFontWeight>()
```

判断顺序：**kotlin-wrappers 已有类型 → 本项目声明字面量联合 → 最后才考虑 `String`（仅当上游真的是任意
字符串，例如字体名、任意 CSS 颜色字符串）**。

## 6. 构造普通对象：`js.objects.unsafeJso`

```kotlin
import js.objects.unsafeJso

val title = unsafeJso<TitleOption> {
    text = "Monthly revenue"
    subtext = "in thousands"
}
```

- 这是 kotlin-js-plain-objects 提供的官方构建器，编译成对象字面量，**不要**自己写
  `js("({})").unsafeCast<T>()`，也不要为每个选项类型手写 builder 类。
- 泛型参数无法从上下文推断时显式写 `unsafeJso<TitleOption> { }`。
- 只需要「不设置」的字段就留空；赋 `null` 会真的写入 `null`（JSON 里可见），语义与 `undefined` 不同。
- 嵌套对象同样用 `unsafeJso<X> { }`，数组用 `arrayOf`。
- 需要「纯数据」而不是 JS 对象时（不该传给 JS 的结构）才用 Kotlin 类；凡是交给上游库的，一律 JS 对象。

## 7. 回调、Promise、集合、日期

```kotlin
external interface TooltipOption {
    /** 上游 `(params) => string | HTMLElement`。 */
    var formatter: ((Array<JsAny>) -> String)?
}
```

- 回调直接展开成 Kotlin 函数类型，不用 `dynamic`、不用 `(dynamic) -> dynamic`。
- 异步返回 `Promise<T>`，不要包一层 `dynamic`。
- 集合 / 字典 / 日期 / 正则按第 1 节表格映射，别用 `Array<JsAny>` 当万能容器。
- 上游是 `T | T[]` 时用联合类型或 `Array<T>` + 单值工厂（如 `one(x)`），不要 `Any`。

## 8. `JsAny` 与 `dynamic` 的边界

允许：

- 上游 `any` / `unknown` / `object` / 索引签名值类型缺失；
- 事件 payload 里确实不透明的部分（`event.event`、自定义系列的数据项）；
- 与 `use(...)` 这类可变参数接口交接时，元素是注册对象、上游自己也没给类型。

不允许：

- 用 `JsAny` 当「懒得建模」的默认字段类型；
- 用 `JsAny` 表示联合类型、字面量、回调、集合；
- 在公开 API 里用 `dynamic`（`dynamic` 连成员名都不检查，`JsAny` 至少要求显式 cast）。

`kotlin.js.JsAny` 需要 `kotlin.js.ExperimentalWasmJsInterop` opt-in；在根构建脚本里对所有 KMP 子模块统一
开启，模块内不用逐个加 `@OptIn`。

## 9. 类、静态成员、构造器

- 有实例方法的对象声明为 `external class`（不能 `final`，因为要被实现/继承时不行），构造函数直接写
  `constructor(...)`。
- 静态成员（模块入口对象上的 `init`、`registerPainter` 一类）用 `private external interface
  XxxStaticMembers` + 同名 `object` 暴露成普通函数，调用方不需要 `companion` 语法。
- 上游导出的是「构造函数值」（不能 `new` 的类）时，用不透明接口表示，不强行建模成 `external class`：

```kotlin
/** 上游 `new (...args: any[]) => T`。 */
external interface Constructor<T>

fun <T> Constructor(value: JsAny): Constructor<T> = value.unsafeCast<Constructor<T>>()
```

- 成员名与 `HTMLElement` 冲突且类型不同时加 `Property` 后缀并用 `@JsName` 映射回原名（如 `hidden`、
  `inputMode`）；同名同类型时省略，由 DOM 提供。

## 10. 包与文件划分

- **一个上游 npm 包 = 一个 Kotlin 包 = 一个目录**，路径镜像上游的 npm 包名与子目录（如 `pkg/`、
  `pkg/options/`）。这是包划分，不产生新的 Gradle 模块。
- 子路径入口（`pkg/core`、`pkg/charts`）放在同一个 Kotlin 包内，用文件或 `external object` 区分。
- 大包按上游目录 / 主题拆文件（`XxxTypes.kt`、`XxxLiterals.kt`、`XxxOptions.kt`），不要把上千行声明堆进
  一个文件；手写类型与生成类型分开，便于重新生成。
- 选项类型数量多时按主题继续拆（组件 / 笛卡尔系列 / 层级系列 / 关系系列……），每个文件 400~600 行为宜；
  字面量也按主题拆（组件级一份、系列级一份），不要为了少建文件把两类混在一起。
- `@JsModule` 声明按 ESM 语义写：顶层函数/类会生成具名导入；顶层 `external val` 配 `@JsName` 会生成
  default import，对只有具名导出的包必须写成 `@JsModule` + `external object` + 具名属性。
- 模块级函数/变量拆成 `XxxExports.kt`（`@file:JsModule` + external 声明）与 `XxxValues.kt`（公开
  `object` 的属性访问器），避免同名变量在同一包内冲突。

## 11. 命名与 KDoc

- 类型名照抄上游（去掉 `I`/`Options` 之类的冗余前缀只在极端情况下），Kotlin 关键字加反引号。
- 与 Kotlin / DOM 名称冲突时用 `@JsName` 保留线上名字（如上游 `List` → Kotlin `SeriesData` +
  `@JsName("List")`），不要为了避开冲突改线上 API。
- 每个公开声明都要 KDoc：上游有说明就翻译，没有就按源码行为写；字面量常量写清对应字符串与含义。
- 联合类型 KDoc 里列出全部成员；不透明类型说明「怎么创建」（哪个工厂函数）。

## 12. 上游写法与 Kotlin 表达力不匹配时

上游 `.d.ts` 里有一些写法 Kotlin 的 `external interface` 表达不了。这些情况有固定处理方式，不要因为「写不出来」就退回 `JsAny`：

### `Omit<X, 'k'> & { k: Y }`

Kotlin 不能在子接口里删除或改类型地重声明继承来的成员（`var` 覆盖要求类型完全相同），所以不要试图 `: X` 然后重声明 `k`。

- 让新类型继承**更浅**的 mixin，再把剩下的成员逐个声明出来：

```kotlin
// 上游 `Omit<LineStyleOption, 'color'> & { color?: GaugeColorStop[] }`
external interface GaugeAxisLineStyleOption : ShadowOption {
    var color: Array<GaugeColorStop>?
    var width: Double?
    var type: ZRLineType?
    // …LineStyleOption 的其余成员
}
```

- 只有上游单纯把 `k` 收窄成子类型时才可以直接复用；收窄成**另一类类型**时必须走上面那条。
- 收窄是 `var`、Kotlin 无法表达时，保留基类型并在 KDoc 写清限制（例如系列级 `itemStyle` 已写进未建模清单）。

### `data?: unknown` 被各子类型收窄

上游把 `data` 声明在基接口上是 `unknown`（或 `unknown[]`），每个具体类型再收窄成自己的 item 类型。Kotlin 无法收窄 `var`，因此：

- **不要在基接口声明 `data`**（会挡住子接口的精确类型）；把它放到每个具体类型上：

```kotlin
external interface MarkOption { /* 没有 data */ }

external interface MarkPointOption : MarkOption {
    var data: Array<MarkPointDataItemOption>?
}
```

- 反过来，基接口已经有 `data`、又需要**更富**的 item 时（如层级图的节点），让 item 类型继承
  `OptionDataItemObject`，再用 `items(...)` 放进通用的 `Array<OptionDataItem?>`。上游没有显式继承但字段一致时同样这样做，并在 KDoc 注明。

### 元组类型 `[A, B]`

JS 侧要的是真数组，不能用对象顶替：

```kotlin
/** 上游 `[number, ColorString]`。 */
external interface GaugeColorStop

fun gaugeColorStop(offset: Double, color: String): GaugeColorStop =
    arrayOf<Any>(offset, color).unsafeCast<GaugeColorStop>()
```

- 类型保持不透明，只有工厂能造值；调用方不自己拼数组。
- 数组形式的 `DecalDashArrayX`、`ZRLineType` 等同样用不透明接口 + 数组工厂。

### `T | "none"`

`number[] | "none"`、`ColorString[] | "none"`、`DecalObject[] | "none"` 这类「数组或关键字」用不透明接口 + 一个数组工厂 + 一个 `String` 工厂：`DoubleArrayOrNone`、`ColorArrayOrNone`、`DecalArrayOrNone`。

### `boolean | "a" | "b"`

在字面量类型（第 4 节）之外补一个布尔工厂，把 `false` 也收进同一个类型，例如
`NodeClickBehavior(false)` / `FocusNodeAdjacency(true)`；需要 `false` 有名字时再给常量（`NodeClickBehaviors.none`）。

### 关键字字段要成组处理

上游同一批成员里的关键字字段（如 `layout` / `edgeShape` / `symbolPosition` / `axis.position`）往往是同一轮
书面里补的。补一个字段时顺手把同一文件里性质相同的字段一起补上，否则会出现「同一个 interface 里一个字段
是字面量、隔壁字段还是 `String`」的割裂；确实这轮不做的，写进 TODO 并说明原因。

### 上游真的只写了 `unknown`

仍然用 `JsAny`，并在 KDoc 里写明「上游类型是 `unknown`，因此这里是不透明的」（例如
`LegendDataItemOption.tooltip`、`CustomSeriesRenderItemParams.itemPayload` 的值类型）。这是第 8 节允许的用法，不是偷懒。

## 13. 落地检查清单

改完一个类型后逐条过：

1. 上游 `.d.ts` / `custom-elements.json` 里每个成员都有对应声明，没有 `JsAny` / `dynamic` 兜底；
2. 联合类型只有一个声明处，成员工厂齐全；
3. 字面量联合有常量对象，且测试逐条对照上游字符串；
4. CSS / canvas 值用了 kotlin-wrappers 的现成类型；
5. 对象字面量用 `unsafeJso`，没有自己造 builder、没有 `js("...")`；
6. 回调是函数类型，异步是 `Promise`，集合是 `Array` / `JsSet` / `Record`；
7. 没有 `org.w3c.dom.*` / `kotlinx.browser.*`（detekt `ForbiddenImport` 会拦）；
8. 上游的 `Omit<...> & { ... }`、`data` 收窄、元组、`T | "none"` 已按第 12 节处理，而不是退化；
9. `./gradlew build` 通过（含 detekt、浏览器测试），且测试真的调用了上游库验证字段值。

## 反模式速查

```kotlin
var type: String?                                  // ❌ 上游是字面量联合
var type: JsAny?                                   // ❌ 上游有具体类型
external interface Foo : JsAny { }                 // ❌ 多余的父类型
val opt = js("({})").unsafeCast<BarOption>()       // ❌ 用 unsafeJso
external interface Baz { fun render(): JsAny }      // ❌ 方法与返回类型都退化
var formatter: dynamic                             // ❌ 上游有签名
enum class AxisType { value, category }            // ❌ 运行时不是字符串
object AxisType { const val CATEGORY = "category" } // ❌ 字段仍是 String，拼错不报错
class OptionBuilder { fun build(): JsAny }         // ❌ 用 unsafeJso
external interface S : LineStyleOption { var color: Array<Stop>? } // ❌ 与基类成员类型冲突，见第 12 节
override var data: Array<NodeOption>?              // ❌ 基接口是 unknown，var 不能收窄
fun stop(o: Double, c: String): Stop = unsafeJso { offset = o; color = c } // ❌ 上游是数组元组
```
