---
name: ktjs-type-mapping
description: 把上游 npm 包的 TypeScript 声明翻译成 Kotlin/JS external 声明时的建模规范（kotlin-wrappers 风格）。在 ktjs-* 这类 wrapper 仓库新增或修改 external 声明、决定联合类型 / 字符串字面量 / 回调 / 集合怎么建模、判断 JsAny 与 dynamic 的边界、处理上游 Omit、data 收窄、元组、T | "none" 等 Kotlin 表达不了的写法时使用。
---

# ktjs-type-mapping

把上游 `.d.ts` 的每个类型/成员落到**被编译器检查的** Kotlin 类型上。这份技能是主线流程与硬约束，
完整对照表、范例和边界案例在 `references/type-mapping.md`，动手前按需查阅对应小节。

## 适用场景

- 给 `ktjs-*` 仓库新增一个上游 npm 包的声明。
- 上游升级后对齐 `.d.ts`，补齐缺失成员。
- 把已有的 `JsAny` / `dynamic` / `String` 字段收紧成具体类型。
- 遇到上游写法「Kotlin 表达不出来」，要确定固定处理方式而不是直接退化。

## 核心原则

上游 `.d.ts` 里写了类型，Kotlin 侧就必须是被编译器检查的类型。

- `JsAny` 只用于上游写 `any` / `unknown` / `object` / 索引签名值缺失，且要在 KDoc 里写明原因。
- `dynamic` 只用于上游自己只写了裸 `Function` / `any`、实现也无法推断的情况；公开 API 里不用。
- 两者都是最后手段，不是省事手段。

## 执行步骤

1. **读上游，不猜**：先读本地克隆的上游源码 / `.d.ts`（没有克隆就按项目约定克隆），确认成员的声明原文，
   不要凭记忆写。manifest（如 `custom-elements.json`）里成员缺类型时同样以源码为准。
2. **逐成员过对照表**：按下面对照表决定类型，细节见 `references/type-mapping.md` §1。
3. **处理 Kotlin 表达不了的写法**：`Omit<X,'k'> & {...}`、继承来的 `var` 需要收窄、`data?: unknown` 被
   各子类型收窄、元组、`T | "none"`、`number | callback`、泛型 mixin —— 一律按 §12 的固定写法落地，
   不要因为「写不出来」就退回 `JsAny`。
4. **写声明**：结构类型用 `external interface`（不写 `: JsAny`），联合用不透明接口 + 同名工厂，
   字面量用 `sealed external interface` + 常量对象，对象字面量用 `js.objects.unsafeJso`。
5. **补 KDoc**：每个公开声明都要有；上游有说明就翻译，没有就按源码行为写，字面量常量写清对应字符串。
6. **加测试**：字面量逐条断言 `constant.unsafeCast<String>() == "上游字面量"`；`@JsName` 同名字段用探针
   接口验证「两个 Kotlin 属性写的是同一个 JS 属性」；测试要真的调用上游库。
7. **过检查清单**（见下），`./gradlew build` 通过（含 detekt、浏览器测试）。

## 最小对照表

| 上游 TypeScript | Kotlin |
| --- | --- |
| `interface X` / `type X = {...}` | `external interface X`（不写 `: JsAny`） |
| `A \| B` | `external interface AOrB` + 每支一个同名工厂（§3） |
| `"a" \| "b"` | `sealed external interface X` + `X(String)` 工厂 + `Xs` 常量对象（§4） |
| `string` / `number` / `boolean` | `String` / `Double` / `Boolean`（JS 没有整数类型，`int` 也是 `Double`） |
| `string \| number` | 自定义联合 `StringOrDouble`，不要退化成 `Any` |
| `T[]` / `Set<T>` / `Record<K,V>` | `Array<T>` / `js.collections.JsSet<T>` / `js.objects.Record<K,V>` |
| `Date` / `Promise<T>` / `(...) => R` | `kotlin.js.Date` / `kotlin.js.Promise<T>` / `(A) -> R` |
| `any` / `unknown` / `object` / 缺类型 | `kotlin.js.JsAny`（可空 `JsAny?`），KDoc 注明 |
| `HTMLElement` 等 DOM 类型 | `web.html.*` / `web.dom.*`，禁止 `org.w3c.dom.*` |
| CSS 关键字 / 长度 / 颜色 | `web.cssom.*`、`web.canvas.*`（§5） |

可选性用 `var name: T?` 表达（「未设置」即 `undefined`，读回是 `null`）；只读字段用 `val`。

## 四条最容易做错的

1. **联合类型**：一个成员一个同名工厂函数（`inline` + `unsafeCast`），联合收在**一个**不透明接口里，
   按包去重、同语义在所有字段复用；联合里有 `JsAny` / `any` 分支时整个联合收敛为 `JsAny`（§3）。
2. **字符串字面量**：`sealed external interface` + `X(String)` 工厂 + `object Xs { inline val ... get() }`。
   不要 `enum class`（运行时不是字符串），不要 `const val` 字符串常量（拼错不报错）；保留 `X(String)`
   工厂作为 JS 返回值的唯一入口（§4）。
3. **对象字面量**：`unsafeJso<T> { ... }`（kotlin-js-plain-objects）。不要自己写 builder，不要
   `js("({})").unsafeCast<T>()`；「不设置」就留空，赋 `null` 会真的写入 `null`（§6）。
4. **继承来的 `var` 收窄/加宽**：子接口不能改类型地重声明，改为基接口保留原字段、子接口加
   `@JsName("原名") var typedXxx`。命名统一 `typed<原字段名>`（§12）。

## 禁止事项

- 不要用 `external interface Foo : JsAny`（多余父类型）。
- 不要让公开字段是 `String` / `JsAny` / `dynamic` 来顶替上游已有的字面量、联合、回调、集合类型。
- 不要用 `enum class` 或 `String` 常量建模字面量联合。
- 不要在 `external interface` 里写方法体、构造器、companion（除非确实是上游静态成员）。
- 不要用 `org.w3c.dom.*`、`kotlinx.browser.*`（detekt `ForbiddenImport` 会拦）。
- 不要为了「省事」新增近似类型：同一语义的联合在所有字段上用同一个类型。
- 不要只补一个新字段：同一批性质相同的关键字字段一起补，本轮确实不做的写进 TODO 说明原因。

## 落地检查清单

1. 上游每个成员都有对应声明，没有 `JsAny` / `dynamic` 兜底（确属 `unknown` 的已注明原因）；
2. 联合类型只有一个声明处，成员工厂齐全；
3. 字面量联合有常量对象，测试逐条对照上游字符串；
4. CSS / canvas 语义字段用了 kotlin-wrappers 现成类型；
5. 对象字面量用 `unsafeJso`，没有自造 builder、没有 `js("...")`；
6. 回调是函数类型、异步是 `Promise`、集合是 `Array` / `JsSet` / `Record`；
7. 上游 `Omit<...> & {...}`、`data` 收窄、元组、`T | "none"` 已按 §12 处理；
8. `./gradlew build` 通过，且测试真的调用上游库验证字段值。

## 完整规范

- `references/type-mapping.md`：上游类型 → Kotlin 的完整对照表、联合/字面量/mixin/包划分的范例、
  第 12 节「上游写法与 Kotlin 表达力不匹配时」的全部固定处理方式、反模式速查。

文中示例的类名（`ZRColor`、`SeriesOption`、`EChartsOption` …）取自 ktjs-echarts，换项目时替换成本项目
对应的类型名与包名，规则本身不变。
