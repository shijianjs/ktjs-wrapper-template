package io.github.shijianjs.ktjs.wrappertemplate.demo.page.conventions

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.bulletList
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.dom.element.invoke
import web.html.HTMLElement
import web.html.HtmlTagName

/** Every string this page shows, in one language. */
private class ConventionsTexts(
    val title: String,
    val summary: String,
    val heading: String,
    val hint: String,
    val lines: List<String>,
)

private val english: ConventionsTexts = ConventionsTexts(
    title = "Conventions",
    summary = "The same rules as the library: every component is a custom element with its own shadow " +
        "root, and no stylesheet is global.",
    heading = "Rules this template follows",
    hint = "A derived project inherits these; the checklist is what a review looks for.",
    lines = listOf(
        "One module per npm package, one Tags object holding a TagName for every element.",
        "Importing the module is what registers the element; createButton() does it on first use.",
        "No dynamic: upstream declarations are mapped to Kotlin types the compiler checks.",
        "A page is a set of components; each sheet belongs to the component that owns it.",
        "The demo reuses ktjs-demo-kit for the frame, so no site here hand-rolls navigation or i18n.",
    ),
)

private val chinese: ConventionsTexts = ConventionsTexts(
    title = "约定",
    summary = "和库本身同一套规矩：每个组件都是自带 shadow root 的自定义元素，没有全局样式表。",
    heading = "这个模板遵守的规矩",
    hint = "派生项目直接继承这些规矩；评审就照这份清单看。",
    lines = listOf(
        "一个 npm 包一个模块，一个 Tags 对象集中放每个元素的 TagName。",
        "导入模块本身即完成注册；createButton() 在第一次用到时触发。",
        "不用 dynamic：上游的声明都落到编译器能检查的 Kotlin 类型。",
        "页面是一组组件；每份样式表都属于持有它的那个组件。",
        "demo 直接复用 ktjs-demo-kit 的外壳，不再自己搓导航与多语言。",
    ),
)

private val conventionsTexts: Translations<ConventionsTexts> = texts(
    Langs.EN to english,
    Langs.ZH_HANS to chinese,
)

/**
 * The rules the library and the demo are both written by, as a list.
 *
 * A list of whole lines is the one case where replacing the collection is right: each line comes from
 * one language, the nodes are this page's own, and nothing above them is touched.
 */
internal class ConventionsPage : DemoPage() {

    private val binder = Binder(conventionsTexts)

    private val list: HTMLElement = bulletList(emptyList())


    init {
        val section = sectionBlock()
        binder.bind(section.title) { it.heading }
        binder.bind(section.hint) { it.hint }
        section.body.append(list)

        body.append(section.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = conventionsTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        binder.apply(lang)

        list.replaceChildren(*current.lines.map { HtmlTagName.li { textContent = it } }.toTypedArray())
    }

    companion object : CustomElementReference<ConventionsPage>(ConventionsPage::class.js) {
        val NAME: TagName<ConventionsPage> = TagName("wrapper-template-conventions-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<wrapper-template-conventions-page>`. */
internal fun newConventionsPage(): ConventionsPage {
    ConventionsPage.define()
    return ConventionsPage.NAME()
}
