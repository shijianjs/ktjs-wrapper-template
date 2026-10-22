package demo

import web.cssom.ClassName
import web.dom.document
import web.html.HtmlTagName

private const val PROJECT_NAME = "__PROJECT__"

/**
 * Entry point of the demo page.
 *
 * The scaffold only renders a heading. Replace it with a real demonstration of the wrapped APIs:
 * build DOM nodes with `document.createElement` and mutate them directly, do not assemble HTML
 * strings (see the UI conventions in AGENTS.md).
 */
fun main() {
    val root = document.createElement(HtmlTagName.div)
    root.className = ClassName("page")

    val title = document.createElement(HtmlTagName.h1)
    title.textContent = "$PROJECT_NAME demo"

    val hint = document.createElement(HtmlTagName.p)
    hint.className = ClassName("hint")
    hint.textContent = "Replace this page with a demo of __PREFIX__-__MODULE__."

    root.append(title, hint)
    document.body.append(root)
}
