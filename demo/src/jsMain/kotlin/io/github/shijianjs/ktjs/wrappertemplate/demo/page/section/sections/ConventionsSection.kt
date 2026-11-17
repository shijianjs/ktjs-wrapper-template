package io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.PageBlock
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Translation
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.english
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.OutputList
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.OUTPUT_SHEET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sectionShellTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import web.components.CustomElementReference
import web.dom.TagName

/**
 * `<ktjs-conventions>`: the rules the library and this demo are both written by.
 *
 * The body is an [OutputList], so the section adopts the list's sheet and adds nothing of its own.
 */
internal class ConventionsSection : CustomElementAdapter(), PageBlock {
    private val shell = sectionShellTag().createElement()

    private val list = OutputList()

    init {
        val shadow = attachStyledShadow(RESET, OUTPUT_SHEET)
        shadow.appendChild(shell)
        shell.appendChild(list.element)

        applyTranslation(english)
    }

    override fun applyTranslation(translation: Translation) {
        shell.setText(translation.conventionsHeading, translation.conventionsHint)
        list.setLines(translation.conventionLines)
    }

    companion object : CustomElementReference<ConventionsSection>(ConventionsSection::class.js) {
        val NAME: TagName<ConventionsSection> = TagName("ktjs-conventions")
    }
}

/** Defines `<ktjs-conventions>` once and returns its tag. */
internal fun conventionsSectionTag(): TagName<ConventionsSection> {
    defineIfMissing(ConventionsSection.NAME, ConventionsSection.Companion)
    return ConventionsSection.NAME
}
