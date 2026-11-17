package io.github.shijianjs.ktjs.wrappertemplate.demo.page.section

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.bodyText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.mutedText
import web.components.CustomElementReference
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.Display
import web.cssom.Margin
import web.cssom.number
import web.cssom.px
import web.dom.TagName
import web.html.HTMLHeadingElement
import web.html.HTMLParagraphElement
import web.html.HtmlTagName

/** The shell's own class names: only its shadow tree selects and carries them. */
private val SECTION_HEADING_CLASS: ClassName = ClassName("section-heading")

private val SECTION_HINT_CLASS: ClassName = ClassName("section-hint")

/**
 * `<ktjs-section>`: the box every section is built inside — a heading, one line of hint, then a slot
 * the section fills with its own content.
 *
 * This is the one-to-many of the page: three sections create one of these, none of them extends it.
 * The section owns what goes *in*; the shell owns the frame and the gap between sections, which is
 * why `:host` here carries the `margin-top` a section would otherwise carry itself.
 *
 * The content is appended to this element and lands in the slot, so it stays in the *section's* shadow
 * tree — the section's own sheet styles it, and it inherits the typography from the section's tree,
 * not from the shell's.
 */
internal class SectionShell : CustomElementAdapter() {
    private val headingElement: HTMLHeadingElement = HtmlTagName.h2.createElement {
        classList.add(SECTION_HEADING_CLASS)
    }

    private val hintElement: HTMLParagraphElement = HtmlTagName.p.createElement {
        classList.add(SECTION_HINT_CLASS)
    }

    init {
        // One sheet per element class, adopted by every instance: the rules are static.
        val shadow = attachStyledShadow(RESET, SHELL_SHEET)
        shadow.appendChild(headingElement)
        shadow.appendChild(hintElement)
        shadow.appendChild(HtmlTagName.slot.createElement())
    }

    /** Rewrites the two strings the shell owns; everything else in it belongs to the section. */
    fun setText(heading: String, hint: String) {
        headingElement.textContent = heading
        hintElement.textContent = hint
    }

    companion object : CustomElementReference<SectionShell>(SectionShell::class.js) {
        val NAME: TagName<SectionShell> = TagName("ktjs-section")

        /**
         * The frame's rules: the box the page lays out, and the heading/hint typography every section
         * shares. A section's body rules live in that section's sheet instead — no section adopts this
         * one.
         */
        private val SHELL_SHEET: CSSStyleSheet = cssSheet {
            ":host" {
                display = Display.block
                marginTop = 36.px
            }

            SECTION_HEADING_CLASS {
                margin = Margin(0.px, 0.px, 4.px)
                fontSize = 19.px
                color = bodyText
            }

            SECTION_HINT_CLASS {
                margin = Margin(0.px, 0.px, 14.px)
                color = mutedText
                fontSize = 13.px
                lineHeight = number(1.5)
            }
        }
    }
}

/** Defines `<ktjs-section>` once and returns its tag. */
internal fun sectionShellTag(): TagName<SectionShell> {
    defineIfMissing(SectionShell.NAME, SectionShell.Companion)
    return SectionShell.NAME
}
