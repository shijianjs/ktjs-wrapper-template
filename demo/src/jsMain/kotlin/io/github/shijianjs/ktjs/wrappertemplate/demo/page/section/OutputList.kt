package io.github.shijianjs.ktjs.wrappertemplate.demo.page.section

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.number
import web.cssom.px
import web.html.HTMLElement
import web.html.HtmlTagName

/** The list's own class names: only this file selects and carries them. */
private val OUTPUT_CLASS: ClassName = ClassName("output")

private val OUTPUT_ITEM_CLASS: ClassName = ClassName("output-item")

/**
 * A list of plain lines, which is the whole body of the sections that only say something.
 *
 * Every line of such a list comes from one language, so a language switch replaces the collection
 * wholesale — that is the case `replaceChildren` is for. The nodes are the section's own; nothing above
 * them is touched.
 */
internal class OutputList {
    val element: HTMLElement = HtmlTagName.ul.createElement {
        classList.add(OUTPUT_CLASS)
    }

    fun setLines(lines: List<String>) {
        val items = lines.map { line ->
            HtmlTagName.li.createElement {
                classList.add(OUTPUT_ITEM_CLASS)
                textContent = line
            }
        }
        element.replaceChildren(*items.toTypedArray())
    }
}

/** The rules of an output list, adopted by the section whose body is one. */
internal val OUTPUT_SHEET: CSSStyleSheet = cssSheet {
    OUTPUT_CLASS {
        margin = Margin(0.px, 0.px)
        padding = Padding(0.px, 0.px, 0.px, 18.px)
        fontSize = 13.px
        lineHeight = number(1.7)
    }

    OUTPUT_ITEM_CLASS {
        marginBottom = 4.px
    }
}
