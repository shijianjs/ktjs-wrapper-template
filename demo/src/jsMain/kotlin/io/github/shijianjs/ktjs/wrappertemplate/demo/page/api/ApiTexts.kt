package io.github.shijianjs.ktjs.wrappertemplate.demo.page.api

import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke

/** Every string this page shows, in one language. */
internal class ApiTexts(
    val title: String,
    val summary: String,
    val heading: String,
    val hint: String,
    val upstreamHeading: String,
    val kotlinHeading: String,
    /** Row labels and the value each maps to; the values are code and identical in both languages. */
    val rows: List<Pair<String, String>>,
)
