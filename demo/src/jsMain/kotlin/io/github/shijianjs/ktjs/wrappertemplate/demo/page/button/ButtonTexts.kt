package io.github.shijianjs.ktjs.wrappertemplate.demo.page.button

import io.github.shijianjs.ktjs.dom.element.invoke

/** Every string this page shows, in one language. */
internal class ButtonTexts(
    val title: String,
    val summary: String,
    val demoHeading: String,
    val demoHint: String,
    val clickMe: String,
    val clickedOnce: String,
    /** The count is a value, not a sentence: it stays a function so no call site builds prose. */
    val clickedTimes: (clicks: Int) -> String,
    val disableOther: String,
    val enableOther: String,
    val statusPrefix: String,
)
