package io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.PageBlock
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Translation
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.english
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sectionShellTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.hairline
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.mutedText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.panel
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.panelStrong
import web.components.CustomElementReference
import web.cssom.Border
import web.cssom.BorderCollapse
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.cssom.LineStyle
import web.cssom.Overflow
import web.cssom.Padding
import web.cssom.TextAlign
import web.cssom.VerticalAlign
import web.cssom.integer
import web.cssom.number
import web.cssom.pct
import web.cssom.px
import web.dom.TagName
import web.html.HTMLTableCellElement
import web.html.HTMLTableRowElement
import web.html.HTMLTableSectionElement
import web.html.HtmlTagName

/** This section's own class names: no other file selects or carries them. */
private val TABLE_CLASS: ClassName = ClassName("api-table")

private val CELL_CLASS: ClassName = ClassName("cell")

private val HEAD_CELL_CLASS: ClassName = ClassName("cell-head")

private val CODE_CLASS: ClassName = ClassName("code")

/** One row of the table, kept so a language switch rewrites its first cell instead of rebuilding it. */
private class ApiRow(val labelCell: HTMLTableCellElement, val valueCell: HTMLTableCellElement)

/**
 * `<ktjs-api>`: what the wrapper maps, as a two-column table.
 *
 * The rows are fixed, so they are built once and a language switch only rewrites the first column: the
 * values on the right are code, identical in both languages. A table is the right element here — this
 * is tabular data, not layout.
 */
internal class ApiSection : CustomElementAdapter(), PageBlock {
    private val shell = sectionShellTag().createElement()

    private val headRow: HTMLTableRowElement = HtmlTagName.tr.createElement()

    private val bodyRows: HTMLTableSectionElement = HtmlTagName.tbody.createElement()

    private val rows: List<ApiRow>

    init {
        rows = english.apiRows.map {
            val labelCell = cell()
            val valueCell = cell(monospace = true)
            val row = HtmlTagName.tr.createElement {
                appendChild(labelCell)
                appendChild(valueCell)
            }
            bodyRows.appendChild(row)
            ApiRow(labelCell, valueCell)
        }

        val table = HtmlTagName.table.createElement {
            classList.add(TABLE_CLASS)
            appendChild(HtmlTagName.thead.createElement { appendChild(headRow) })
            appendChild(bodyRows)
        }

        val shadow = attachStyledShadow(RESET, API_SHEET)
        shadow.appendChild(shell)
        shell.appendChild(table)

        applyTranslation(english)
    }

    override fun applyTranslation(translation: Translation) {
        shell.setText(translation.apiHeading, translation.apiHint)
        headRow.replaceChildren(
            headerCell(translation.apiRowHeading),
            headerCell(translation.apiValueHeading),
        )
        translation.apiRows.zip(rows).forEach { (row, cells) ->
            cells.labelCell.textContent = row.first
            cells.valueCell.textContent = row.second
        }
    }

    private fun headerCell(label: String): HTMLTableCellElement =
        HtmlTagName.th.createElement {
            classList.add(CELL_CLASS)
            classList.add(HEAD_CELL_CLASS)
            textContent = label
        }

    private fun cell(monospace: Boolean = false): HTMLTableCellElement =
        HtmlTagName.td.createElement {
            classList.add(CELL_CLASS)
            if (monospace) {
                classList.add(CODE_CLASS)
            }
        }

    companion object : CustomElementReference<ApiSection>(ApiSection::class.js) {
        val NAME: TagName<ApiSection> = TagName("ktjs-api")

        /**
         * The table's rules: one rule per node kind, so the table costs five rules whether it holds six
         * rows or sixty. Nothing here is inline, and nothing here can reach another component.
         */
        private val API_SHEET: CSSStyleSheet = cssSheet {
            TABLE_CLASS {
                width = 100.pct
                borderCollapse = BorderCollapse.collapse
                backgroundColor = panel
                border = Border(1.px, LineStyle.solid, hairline)
                borderRadius = 10.px
                overflow = Overflow.hidden
                fontSize = 13.px
            }

            CELL_CLASS {
                textAlign = TextAlign.left
                verticalAlign = VerticalAlign.top
                padding = Padding(9.px, 12.px)
                borderBottomWidth = 1.px
                borderBottomStyle = LineStyle.solid
                borderBottomColor = hairline
                lineHeight = number(1.5)
            }

            HEAD_CELL_CLASS {
                width = 32.pct
                backgroundColor = panelStrong
                color = mutedText
                fontWeight = integer(600)
            }

            CODE_CLASS {
                fontFamily = FontFamily.monospace
            }
        }
    }
}

/** Defines `<ktjs-api>` once and returns its tag. */
internal fun apiSectionTag(): TagName<ApiSection> {
    defineIfMissing(ApiSection.NAME, ApiSection.Companion)
    return ApiSection.NAME
}
