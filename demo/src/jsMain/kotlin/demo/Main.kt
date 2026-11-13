package demo

import io.github.shijianjs.ktjs.wrappertemplate.core.createButton
import web.cssom.ClassName
import web.dom.document
import web.events.EventHandler
import web.html.HtmlTagName

private const val PROJECT_NAME = "ktjs-wrapper-template"

/**
 * Entry point of the demo page.
 *
 * Deliberately small: it creates two `<vaadin-button>` elements through the wrapper module and wires
 * them to a counter, which is the shape every wrapped package takes. Nodes are built with
 * `document.createElement` and mutated in place — no HTML strings (see the UI conventions in
 * AGENTS.md).
 */
fun main() {
    val root = document.createElement(HtmlTagName.div).apply { className = ClassName("page") }

    val title = document.createElement(HtmlTagName.h1).apply { textContent = "$PROJECT_NAME demo" }

    val hint = document.createElement(HtmlTagName.p).apply {
        className = ClassName("hint")
        textContent = "Both buttons below are <vaadin-button>, created by ktjs-wrapper-template-core " +
            "from the @vaadin/button package."
    }

    val status = document.createElement(HtmlTagName.p).apply {
        className = ClassName("status")
        textContent = "Clicks: 0"
    }

    var clicks = 0
    val counter = createButton("Click me")
    counter.onclick = EventHandler {
        clicks += 1
        status.textContent = "Clicks: $clicks"
        counter.textContent = if (clicks == 1) "Clicked once" else "Clicked $clicks times"
    }

    // The one property the wrapper maps: `disabled` reflects into the element's own state.
    val toggle = createButton("Disable the other button")
    toggle.onclick = EventHandler { counter.disabled = !counter.disabled }

    root.append(title, hint, counter, toggle, status)
    document.body.append(root)
}
