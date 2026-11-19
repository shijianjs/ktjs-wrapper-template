package io.github.shijianjs.ktjs.wrappertemplate.demo.page.overview

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.grid
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.child
import io.github.shijianjs.ktjs.dom.css.dsl.className
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.css.dsl.tag
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.vaadin.VaadinTagName
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import io.github.shijianjs.ktjs.vaadin.card.CardVariants
import io.github.shijianjs.ktjs.vaadin.card.createCard
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.wrappertemplate.demo.siteTexts
import web.cssom.ClassName
import web.cssom.Color
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.None
import web.cssom.TextDecorationLine
import web.cssom.number
import web.html.HTMLAnchorElement
import web.html.HTMLElement
import web.html.HtmlTagName

/** Every string this page shows, in one language. */
private class OverviewTexts(
    val title: String,
    val summary: String,
    val whatHeading: String,
    val whatBody: String,
    val startHeading: String,
    val startHint: String,
    /** One line per card, keyed by the page id the card opens. */
    val cardSummaries: Map<String, String>,
)

private val english: OverviewTexts = OverviewTexts(
    title = "Overview",
    summary = "A Kotlin/JS wrapper around @vaadin/button, and the skeleton every ktjs-* wrapper starts from.",
    whatHeading = "What this template is",
    whatBody = "One npm package, one module: `ktjs-wrapper-template-core` maps what upstream declares as " +
        "API onto Kotlin types the compiler checks. The demo is an ordinary consumer of that module — it " +
        "creates the element through `createButton` and never writes a tag name as a string.",
    startHeading = "Where to start",
    startHint = "Three pages, in the order a derived project needs them.",
    cardSummaries = mapOf(
        "button" to "The one thing the wrapper exists for: a <vaadin-button> created by Kotlin, with its " +
            "mapped properties exercised.",
        "api" to "What the wrapper maps, and what stays upstream's business — the table a new module is " +
            "cut from.",
        "conventions" to "The rules the library and this template are both written by: one module per " +
            "package, no dynamic, sheets that belong to their component.",
    ),
)

private val chinese: OverviewTexts = OverviewTexts(
    title = "概览",
    summary = "围绕 @vaadin/button 的 Kotlin/JS 封装，也是所有 ktjs-* 封装项目的起点模板。",
    whatHeading = "这个模板是什么",
    whatBody = "一个 npm 包对应一个模块：`ktjs-wrapper-template-core` 只把上游声明为 API 的东西映射成" +
        "编译器能检查的 Kotlin 类型。这个 demo 就是该模块的普通使用方——通过 `createButton` 创建元素，" +
        "任何地方都不把标签名写成字符串。",
    startHeading = "从这里开始",
    startHint = "三页，按派生项目需要的顺序排。",
    cardSummaries = mapOf(
        "button" to "封装存在的理由：用 Kotlin 创建 <vaadin-button>，并把映射过来的属性逐条跑一遍。",
        "api" to "封装映射了什么、哪些仍是上游的事——新模块照着切的那张表。",
        "conventions" to "库和这个模板都遵守的规矩：一个包一个模块、不用 dynamic、样式表属于它的组件。",
    ),
)

private val overviewTexts: Translations<OverviewTexts> = texts(
    Langs.EN to english,
    Langs.ZH_HANS to chinese,
)

/** The class the cards are found by in tests; it carries no style of its own. */
private val CARD_LINK: ClassName = ClassName("card-link")

/**
 * What the index needs from the wrapper — two declarations, each one caused by the anchor rather than by
 * the card: a link paints its text like a link and does not pass a row's height on to its children. The
 * card itself is left exactly as the component draws itself.
 */
private val CARD_SHEET: CSSStyleSheet = cssSheet {
    className(CARD_LINK).invoke {
        display = Display.flex
        color = Color("inherit")
        textDecorationLine = None.none
    }

    // The grid stretches the anchor to the tallest card of its row; inside a flex anchor the card grows
    // to that height, so the surfaces of one row line up whatever their summaries are.
    className(CARD_LINK).child(tag(VaadinTagName.card)).invoke {
        flexGrow = number(1.0)
    }
}

/** One card of the index, kept typed so a language switch rewrites its title and summary in place. */
private class CardLink(val id: String, val link: HTMLAnchorElement, val card: CardElement, val summary: HTMLElement)

/**
 * Landing page: what the template is, and the three pages a derived project reads first.
 *
 * A card is a `vaadin-card` inside a link: the whole surface is clickable, and middle-click, "copy link
 * address" and keyboard activation come from the anchor instead of from a click handler.
 */
internal class OverviewPage : DemoPage() {

    private val binder = Binder(overviewTexts)

    private val cards = mutableListOf<CardLink>()


    init {
        val what = sectionBlock()
        binder.bind(what.title) { it.whatHeading }
        what.body.append(binder.bind(paragraph()) { it.whatBody })

        val start = sectionBlock()
        binder.bind(start.title) { it.startHeading }
        binder.bind(start.hint) { it.startHint }
        start.body.append(grid(240, *buildCards().toTypedArray()))

        addSheet(CARD_SHEET)
        body.append(what.root, start.root)
    }

    private fun buildCards(): List<HTMLElement> = listOf("button", "api", "conventions").map { id ->
        val summary = HtmlTagName.div()
        // The title comes from the frame's table after the first language is applied: one place names
        // a page, and the card is another view of that name.
        val card = createCard {
            addTheme(CardVariants.elevated)
            titleHeadingLevel = 4.0
            append(summary)
        }
        val link = HtmlTagName.a {
            href = "#/$id"
            classList.add(CARD_LINK)
            dataset["route"] = id
            append(card)
        }
        cards += CardLink(id, link, card, summary)
        link
    }

    override fun applyTranslation(lang: Lang) {
        val current = overviewTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        binder.apply(lang)

        val site = siteTexts[lang]
        cards.forEach { entry ->
            entry.card.cardTitle = site.pageTitles[entry.id] ?: entry.id
            entry.summary.textContent = current.cardSummaries[entry.id].orEmpty()
        }
    }

    companion object : CustomElementReference<OverviewPage>(OverviewPage::class.js) {
        val NAME: TagName<OverviewPage> = TagName("wrapper-template-overview-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<wrapper-template-overview-page>`. */
internal fun newOverviewPage(): OverviewPage {
    OverviewPage.define()
    return OverviewPage.NAME()
}
