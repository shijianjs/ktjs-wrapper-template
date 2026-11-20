package io.github.shijianjs.ktjs.wrappertemplate.demo.page.api

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.Blocks.exampleBox
import io.github.shijianjs.ktjs.demo.page.SectionBlock
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.className
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import web.cssom.BorderCollapse
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.FontWeight
import web.cssom.Padding
import web.cssom.TextAlign
import web.cssom.integer
import web.cssom.pct
import web.cssom.px
import web.html.HTMLTableCellElement
import web.html.HTMLTableRowElement
import web.html.HTMLElement
import web.html.HtmlTagName

/** Every string this page shows, in one language. */
private class ApiTexts(
    val title: String,
    val summary: String,
    val heading: String,
    val hint: String,
    val upstreamHeading: String,
    val kotlinHeading: String,
    /** Row labels and the value each maps to; the values are code and identical in both languages. */
    val rows: List<Pair<String, String>>,
)

private val english: ApiTexts = ApiTexts(
    title = "What the wrapper maps",
    summary = "Only what upstream declares as an API is mapped; the label stays light-DOM text.",
    heading = "Property by property",
    hint = "The same table a new module is cut from: read the upstream declaration, then decide the Kotlin type.",
    upstreamHeading = "upstream",
    kotlinHeading = "in Kotlin",
    rows = listOf(
        "npm package" to "@vaadin/button",
        "tag" to "<vaadin-button>",
        "element class" to "ButtonElement : HTMLElement",
        "mapped property" to "disabled: Boolean",
        "label" to "light-DOM text (shadow part \"label\")",
        "styling" to "parts label / prefix / suffix, --vaadin-button-*",
    ),
)

private val chinese: ApiTexts = ApiTexts(
    title = "封装层映射了什么",
    summary = "只映射上游声明为 API 的东西；标签文本仍是 light DOM 文本。",
    heading = "逐条对照",
    hint = "新模块就照这张表切：先读上游声明，再决定 Kotlin 类型。",
    upstreamHeading = "上游",
    kotlinHeading = "Kotlin 侧",
    rows = listOf(
        "npm 包" to "@vaadin/button",
        "标签" to "<vaadin-button>",
        "元素类" to "ButtonElement : HTMLElement",
        "映射属性" to "disabled: Boolean",
        "标签文本" to "light DOM 文本（shadow part \"label\"）",
        "样式入口" to "parts label / prefix / suffix、--vaadin-button-*",
    ),
)

private val apiTexts: Translations<ApiTexts> = texts(
    Langs.EN to english,
    Langs.ZH_HANS to chinese,
)

/** The table's own class names: only this page selects and carries them. */
private val API_TABLE: ClassName = ClassName("api-table")

private val API_CELL: ClassName = ClassName("api-cell")

private val API_HEAD_CELL: ClassName = ClassName("api-cell-head")

/**
 * A static definition table has no Vaadin component counterpart — `vaadin-grid` is the element for data
 * a user navigates, and this is six fixed rows — so it stays a real `<table>`.
 *
 * The rules are the table's own: collapse the borders, one padding per cell, and the header weight. Colours
 * come from Lumo's tokens, so the table follows the theme like everything else on the page.
 */
private val TABLE_SHEET: CSSStyleSheet = cssSheet {
    className(API_TABLE).invoke {
        width = 100.pct
        borderCollapse = BorderCollapse.collapse
    }

    className(API_CELL).invoke {
        padding = Padding(6.px, 8.px)
    }

    className(API_HEAD_CELL).invoke {
        width = 32.pct
        fontWeight = integer(600)
        textAlign = TextAlign.left
    }
}

/** One row, kept so a language switch rewrites its first cell instead of rebuilding the table. */
private class ApiRow(val labelCell: HTMLTableCellElement, val valueCell: HTMLTableCellElement)

/**
 * The mapping table: what the module declares, and what it deliberately leaves to upstream.
 *
 * The right column is code, identical in both languages, so a switch only rewrites the left column.
 */
internal class ApiPage : DemoPage() {

    private val binder = Binder(apiTexts)

    private val headRow: HTMLTableRowElement = HtmlTagName.tr()

    private val bodyRows = HtmlTagName.tbody()

    private val rows: List<ApiRow>


    init {
        rows = english.rows.map {
            val labelCell = HtmlTagName.td { classList.add(API_CELL) }
            val valueCell = HtmlTagName.td { classList.add(API_CELL) }
            bodyRows.append(HtmlTagName.tr { append(labelCell, valueCell) })
            ApiRow(labelCell, valueCell)
        }

        val table = HtmlTagName.table {
            classList.add(API_TABLE)
            append(HtmlTagName.thead { append(headRow) }, bodyRows)
        }

        val section = SectionBlock.NAME { show() }
        binder.bind(section.heading) { it.heading }
        binder.bind(section.hint) { it.hint }
        section.body.append(exampleBox(null, table))

        addSheet(TABLE_SHEET)
        body.append(section.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = apiTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        binder.apply(lang)

        headRow.replaceChildren(
            HtmlTagName.th {
                classList.add(API_CELL)
                classList.add(API_HEAD_CELL)
                textContent = current.upstreamHeading
            },
            HtmlTagName.th {
                classList.add(API_CELL)
                classList.add(API_HEAD_CELL)
                textContent = current.kotlinHeading
            },
        )
        current.rows.zip(rows).forEach { (row, cells) ->
            cells.labelCell.textContent = row.first
            cells.valueCell.textContent = row.second
        }
    }

    companion object : CustomElementReferenceAdapter<ApiPage>(ApiPage::class.js, "wrapper-template-api-page")
}
