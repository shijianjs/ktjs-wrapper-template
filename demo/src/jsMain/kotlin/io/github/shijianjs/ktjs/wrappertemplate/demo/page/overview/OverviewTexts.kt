package io.github.shijianjs.ktjs.wrappertemplate.demo.page.overview

import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke

/** Every string this page shows, in one language. */
internal class OverviewTexts(
    val title: String,
    val summary: String,
    val whatHeading: String,
    val whatBody: String,
    val startHeading: String,
    val startHint: String,
    /** One line per card, keyed by the page id the card opens. */
    val cardSummaries: Map<String, String>,
)
