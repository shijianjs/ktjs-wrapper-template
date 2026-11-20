package io.github.shijianjs.ktjs.wrappertemplate.demo.page.conventions

import io.github.shijianjs.ktjs.dom.element.invoke

/** Every string this page shows, in one language. */
internal class ConventionsTexts(
    val title: String,
    val summary: String,
    val heading: String,
    val hint: String,
    val lines: List<String>,
)
