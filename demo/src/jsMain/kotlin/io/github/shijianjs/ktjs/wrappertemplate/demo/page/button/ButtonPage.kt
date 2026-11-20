package io.github.shijianjs.ktjs.wrappertemplate.demo.page.button

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.newReadout
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.wrappertemplate.core.ButtonElement
import io.github.shijianjs.ktjs.wrappertemplate.core.createButton
import web.dom.clickEvent
import web.events.EventHandler
import web.events.addHandler
import web.html.HTMLElement

/** Every string this page shows, in one language. */
private class ButtonTexts(
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

private val english: ButtonTexts = ButtonTexts(
    title = "The wrapped element",
    summary = "Both buttons are <vaadin-button>, created by ktjs-wrapper-template-core.",
    demoHeading = "Created by Kotlin",
    demoHint = "One button counts its own clicks, the other flips the first one's mapped `disabled` " +
        "property. Labels derive from that state instead of accumulating text.",
    clickMe = "Click me",
    clickedOnce = "Clicked once",
    clickedTimes = { clicks -> "Clicked $clicks times" },
    disableOther = "Disable the other button",
    enableOther = "Enable the other button",
    statusPrefix = "Clicks",
)

private val chinese: ButtonTexts = ButtonTexts(
    title = "被封装出来的元素",
    summary = "两个按钮都是 ktjs-wrapper-template-core 创建的 <vaadin-button>。",
    demoHeading = "由 Kotlin 创建",
    demoHint = "一个按钮累计自己的点击次数，另一个翻转第一个按钮映射过来的 `disabled` 属性。" +
        "标签文本由状态推导，而不是一层层往上拼。",
    clickMe = "点我",
    clickedOnce = "已点一次",
    clickedTimes = { clicks -> "已点 $clicks 次" },
    disableOther = "禁用另一个按钮",
    enableOther = "启用另一个按钮",
    statusPrefix = "点击次数",
)

private val buttonTexts: Translations<ButtonTexts> = texts(
    Langs.EN to english,
    Langs.ZH_HANS to chinese,
)

/**
 * The one thing the wrapper exists for: a `<vaadin-button>` built from Kotlin.
 *
 * The buttons are Lumo's own — no padding, colour or radius is set here, because the component's default
 * appearance is the answer. The page only shows what the wrapper maps: `textContent` for the label and
 * `disabled` for the state.
 *
 * The demo keeps its own click count and rebuilds the three labels from it, so a language switch never
 * has to know what the reader has clicked.
 */
internal class ButtonPage : DemoPage() {

    private val binder = Binder(buttonTexts)

    private val counter: ButtonElement = createButton("")

    private val toggler: ButtonElement = createButton("")

    private val status = newReadout()

    private var clicks = 0

    private var current: ButtonTexts = english


    init {
        val section = sectionBlock()
        binder.bind(section.heading) { it.demoHeading }
        binder.bind(section.hint) { it.demoHint }

        counter.clickEvent.addHandler(EventHandler {
            clicks += 1
            writeLabels()
        })
        toggler.clickEvent.addHandler(EventHandler {
            counter.disabled = !counter.disabled
            writeLabels()
        })

        section.body.append(exampleBox(null, row(counter, toggler), status.root))
        body.append(section.root)
        writeLabels()
    }

    /** Labels follow the click count and the disabled flag, so they are derived, never accumulated. */
    private fun writeLabels() {
        counter.textContent = when (clicks) {
            0 -> current.clickMe
            1 -> current.clickedOnce
            else -> current.clickedTimes(clicks)
        }
        toggler.textContent = if (counter.disabled) current.enableOther else current.disableOther
        status.set("${current.statusPrefix}: $clicks")
    }

    override fun applyTranslation(lang: Lang) {
        current = buttonTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        binder.apply(lang)
        writeLabels()
    }

    companion object : CustomElementReference<ButtonPage>(ButtonPage::class.js) {
        val NAME: TagName<ButtonPage> = TagName("wrapper-template-button-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<wrapper-template-button-page>`. */
internal fun newButtonPage(): ButtonPage {
    ButtonPage.define()
    return ButtonPage.NAME()
}
