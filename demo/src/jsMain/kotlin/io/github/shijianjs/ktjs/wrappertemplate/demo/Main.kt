package io.github.shijianjs.ktjs.wrappertemplate.demo

import io.github.shijianjs.ktjs.demo.i18n.LanguageState
import io.github.shijianjs.ktjs.demo.layout.DemoLayout
import io.github.shijianjs.ktjs.demo.layout.LayoutConfig
import io.github.shijianjs.ktjs.demo.style.Theme
import web.dom.ElementId
import web.dom.document

/**
 * Entry point of the demo page.
 *
 * The shell is not built here: `index.html` names `<ktjs-demo-layout id="demo">`, this file defines the
 * tag and mounts the site into the element the browser already built. Everything the frame owns —
 * navbar, drawer, breadcrumbs, the language and theme menus, the scroll containers — belongs to
 * `ktjs-demo-kit`; the demo supplies the pages and the strings.
 *
 * Order matters: the language state is created and applied before the layout mounts, so the first
 * render already uses the stored language instead of flashing English and rewriting itself.
 */
public fun main() {
    val language = LanguageState(storageKey = "ktjs-wrapper-template-language")
    language.init()

    val theme = Theme()
    theme.applyStored()

    DemoLayout.define()
    val layout = document.getElementById(ElementId("demo")) as? DemoLayout
        ?: error("index.html has no <ktjs-demo-layout id=\"demo\">")

    layout.mount(
        LayoutConfig(
            menu = siteMenu(),
            language = language,
            theme = theme,
            texts = siteTexts,
            siteVersion = SITE_VERSION,
            docsUrl = DOCS_URL,
            apiUrl = API_URL,
            sourceUrl = SOURCE_URL,
            logoSrc = "img/ktjs-wrapper-template-logo.png",
        ),
    )
}
