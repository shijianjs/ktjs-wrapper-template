package io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang

/** The two languages the demo page switches between, in place, without rebuilding the page. */
internal enum class Lang { EN, ZH }

/**
 * Every string the demo page shows.
 *
 * One instance per language; switching languages rewrites existing nodes instead of rebuilding the
 * page. Strings that embed a number are function types, so the count stays a value rather than a
 * hand-built sentence in each caller.
 */
internal class Translation(
    val title: String,
    val subtitle: String,
    val switchLabel: String,
    val demoHeading: String,
    val demoHint: String,
    val clickMe: String,
    val clickedOnce: String,
    val clickedTimes: (clicks: Int) -> String,
    val statusPrefix: String,
    val disableOther: String,
    val enableOther: String,
    val apiHeading: String,
    val apiHint: String,
    val apiRowHeading: String,
    val apiValueHeading: String,
    val apiRows: List<Pair<String, String>>,
    val conventionsHeading: String,
    val conventionsHint: String,
    val conventionLines: List<String>,
)

internal val english: Translation = Translation(
    title = "ktjs-wrapper-template",
    subtitle = "A Kotlin/JS wrapper around @vaadin/button, and the skeleton every ktjs-* wrapper starts from.",
    switchLabel = "中文",
    demoHeading = "1. The wrapped element",
    demoHint = "Both buttons are <vaadin-button>, created by ktjs-wrapper-template-core. The rules below " +
        "paint their host, not the element's own shadow.",
    clickMe = "Click me",
    clickedOnce = "Clicked once",
    clickedTimes = { clicks -> "Clicked $clicks times" },
    statusPrefix = "Clicks",
    disableOther = "Disable the other button",
    enableOther = "Enable the other button",
    apiHeading = "2. What the wrapper maps",
    apiHint = "Only what upstream declares as an API is mapped; the label stays light-DOM text.",
    apiRowHeading = "upstream",
    apiValueHeading = "in Kotlin",
    apiRows = listOf(
        "npm package" to "@vaadin/button",
        "tag" to "<vaadin-button>",
        "element class" to "ButtonElement : HTMLElement",
        "mapped property" to "disabled: Boolean",
        "label" to "light-DOM text (shadow part \"label\")",
        "styling" to "parts label / prefix / suffix, --vaadin-button-*",
    ),
    conventionsHeading = "3. Conventions this demo follows",
    conventionsHint = "The same rules as the library: every component is a custom element with its own " +
        "shadow root, and no stylesheet is global.",
    conventionLines = listOf(
        "One module per npm package, one Tags object holding a TagName for every element.",
        "Importing the module is what registers the element; createButton() does it on first use.",
        "No dynamic: upstream declarations are mapped to Kotlin types the compiler checks.",
        "A page is a set of custom elements; each sheet belongs to the component that owns it.",
    ),
)

internal val chinese: Translation = Translation(
    title = "ktjs-wrapper-template",
    subtitle = "围绕 @vaadin/button 的 Kotlin/JS 封装，也是所有 ktjs-* 封装项目的起点模板。",
    switchLabel = "EN",
    demoHeading = "1. 被封装出来的元素",
    demoHint = "两个按钮都是 ktjs-wrapper-template-core 创建的 <vaadin-button>。这里的规则只作用在宿主上，" +
        "没有伸进元素自己的 shadow。",
    clickMe = "点我",
    clickedOnce = "已点一次",
    clickedTimes = { clicks -> "已点 $clicks 次" },
    statusPrefix = "点击次数",
    disableOther = "禁用另一个按钮",
    enableOther = "启用另一个按钮",
    apiHeading = "2. 封装层映射了什么",
    apiHint = "只映射上游声明为 API 的东西；标签文本仍是 light DOM 文本。",
    apiRowHeading = "上游",
    apiValueHeading = "Kotlin 侧",
    apiRows = listOf(
        "npm 包" to "@vaadin/button",
        "标签" to "<vaadin-button>",
        "元素类" to "ButtonElement : HTMLElement",
        "映射属性" to "disabled: Boolean",
        "标签文本" to "light DOM 文本（shadow part \"label\"）",
        "样式入口" to "parts label / prefix / suffix、--vaadin-button-*",
    ),
    conventionsHeading = "3. 这个 demo 遵守的约定",
    conventionsHint = "和库本身同一套规矩：每个组件都是自带 shadow root 的自定义元素，没有全局样式表。",
    conventionLines = listOf(
        "一个 npm 包一个模块，一个 Tags 对象集中放每个元素的 TagName。",
        "导入模块本身即完成注册；createButton() 在第一次用到时触发。",
        "不用 dynamic：上游的声明都落到编译器能检查的 Kotlin 类型。",
        "页面是一组自定义元素；每份样式表都属于持有它的那个组件。",
    ),
)

internal fun translationOf(lang: Lang): Translation = if (lang == Lang.EN) english else chinese
