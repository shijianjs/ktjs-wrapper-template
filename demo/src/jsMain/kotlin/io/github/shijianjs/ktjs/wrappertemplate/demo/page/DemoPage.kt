package io.github.shijianjs.ktjs.wrappertemplate.demo.page

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.header.headerBarTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Lang
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Translation
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.translationOf
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.apiSectionTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.buttonDemoSectionTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.conventionsSectionTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.bodyText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.pageBackground
import web.components.CustomElementReference
import web.cssom.Auto
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.FontFamily
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.number
import web.cssom.px
import web.cssom.rem
import web.dom.TagName
import web.html.HTMLElement

/**
 * A block the page mounts and, on a language switch, re-labels in place.
 *
 * Every implementation is a custom element, so the block *is* its node: [node] saves calling sites
 * from holding a second, parallel list of elements. Implementations only say how they change their
 * own text — the page decides when.
 */
internal interface PageBlock {
    /** The block as the node the page appends; a component is its own element. */
    val node: HTMLElement get() = this as HTMLElement

    fun applyTranslation(translation: Translation)
}

/**
 * `<ktjs-demo-page>`: one header, three sections and the language switch that re-labels them in place.
 *
 * Every component is a custom element with its own shadow root and its own sheet; this element only
 * composes them, owns the current language, and walks [blocks] when it changes. Nothing is recreated
 * and no tag is re-registered on a switch.
 */
internal class DemoPage : CustomElementAdapter() {
    private val header = headerBarTag().createElement()

    private val buttonDemo = buttonDemoSectionTag().createElement()

    private val api = apiSectionTag().createElement()

    private val conventions = conventionsSectionTag().createElement()

    /** The blocks in render order; adding the next section is one field plus one line here. */
    private val blocks: List<PageBlock> = listOf(header, buttonDemo, api, conventions)

    /** The page's language; named `currentLang` because `HTMLElement` already owns `lang`. */
    private var currentLang: Lang = Lang.EN

    init {
        // The header reports the click; the page owns what it means. Handing over the callback keeps
        // the component unaware of its parent.
        header.onSwitch = ::toggleLanguage

        val shadow = attachStyledShadow(RESET, PAGE_SHEET)
        blocks.forEach { block -> shadow.appendChild(block.node) }
    }

    override fun connectedCallback() {
        applyTranslation(translationOf(currentLang))
    }

    private fun toggleLanguage() {
        currentLang = if (currentLang == Lang.EN) Lang.ZH else Lang.EN
        applyTranslation(translationOf(currentLang))
    }

    private fun applyTranslation(translation: Translation) {
        blocks.forEach { block -> block.applyTranslation(translation) }
    }

    companion object : CustomElementReference<DemoPage>(DemoPage::class.js) {
        val NAME: TagName<DemoPage> = TagName("ktjs-demo-page")

        /**
         * The page's own rules.
         *
         * All on `:host`, because a shadow tree is not laid out by the host page's light-DOM rules: the
         * host is the box the document sees, and the inherited properties (`color`, font, size) reach
         * into the shadow trees from it.
         */
        private val PAGE_SHEET: CSSStyleSheet = cssSheet {
            ":host" {
                display = Display.block
                maxWidth = 60.rem
                margin = Margin(0.px, Auto.auto)
                padding = Padding(32.px, 24.px, 64.px)
                backgroundColor = pageBackground
                color = bodyText
                fontFamily = FontFamily.systemUi
                fontSize = 15.px
                lineHeight = number(1.6)
            }
        }
    }
}

/** Defines `<ktjs-demo-page>` once and returns its tag. */
internal fun demoPageTag(): TagName<DemoPage> {
    defineIfMissing(DemoPage.NAME, DemoPage.Companion)
    return DemoPage.NAME
}
