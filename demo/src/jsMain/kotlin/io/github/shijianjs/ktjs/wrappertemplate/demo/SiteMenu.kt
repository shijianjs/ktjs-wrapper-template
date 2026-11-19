package io.github.shijianjs.ktjs.wrappertemplate.demo

import io.github.shijianjs.ktjs.demo.nav.DemoMenu
import io.github.shijianjs.ktjs.demo.nav.Entry
import io.github.shijianjs.ktjs.demo.nav.Group
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.api.newApiPage
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.button.newButtonPage
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.conventions.newConventionsPage
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.overview.newOverviewPage

/** The Kotlin version this template ships with; the navbar shows it next to the title. */
internal const val SITE_VERSION: String = "Kotlin/JS 2.4.21"

/** The documentation site of this template. */
internal const val DOCS_URL: String = "https://shijianjs.github.io/ktjs-wrapper-template/"

/** Dokka reference assembled by pagesSite next to the demo. */
internal const val API_URL: String = "https://shijianjs.github.io/ktjs-wrapper-template/api/"

/** Source repository, linked from the navbar's link menu. */
internal const val SOURCE_URL: String = "https://github.com/shijianjs/ktjs-wrapper-template"

/**
 * Groups are the drawer's sections; [icon] is an Iconify name drawn before the group title.
 *
 * A group with a single page is lifted to the top level by the layout, so `START` keeps its mark without
 * becoming a row that only exists to be opened again.
 */
internal val START: Group = Group("start", "lucide:sparkles")

internal val ELEMENT: Group = Group("element", "lucide:component")

internal val REFERENCE: Group = Group("reference", "lucide:book-open")

/**
 * The site: one entry per page, in navigation order.
 *
 * Adding a page is one entry here plus its titles in [siteTexts] — the drawer, the routes, the
 * breadcrumbs and the document title all derive from this list.
 */
internal fun siteMenu(): DemoMenu = DemoMenu(
    listOf(
        Entry("overview", START) { newOverviewPage() },
        Entry("button", ELEMENT) { newButtonPage() },
        Entry("api", REFERENCE) { newApiPage() },
        Entry("conventions", REFERENCE) { newConventionsPage() },
    ),
)
