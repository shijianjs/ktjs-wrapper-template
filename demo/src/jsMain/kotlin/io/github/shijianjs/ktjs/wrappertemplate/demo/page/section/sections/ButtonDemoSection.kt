package io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.Pseudos
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.core.createButton
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.PageBlock
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Translation
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.english
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sectionShellTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.accent
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.accentStrong
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.mutedText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.onAccent
import web.components.CustomElementReference
import web.cssom.AlignItems
import web.cssom.Border
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.Cursor
import web.cssom.Display
import web.cssom.FlexWrap
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.cssom.LineStyle
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.integer
import web.cssom.px
import web.dom.TagName
import web.events.EventHandler
import web.html.HTMLDivElement
import web.html.HTMLParagraphElement
import web.html.HtmlTagName

/** This section's own class names: no other file selects or carries them. */
private val ROW_CLASS: ClassName = ClassName("button-row")

private val BUTTON_CLASS: ClassName = ClassName("wrapped-button")

private val STATUS_CLASS: ClassName = ClassName("status")

/**
 * `<ktjs-button-demo>`: the one thing the wrapper exists for — a `<vaadin-button>` created by Kotlin.
 *
 * Two buttons: one counts its own clicks, the other flips the first one's mapped `disabled` property.
 * The labels depend on state, so [applyTranslation] derives them from the current count instead of
 * each handler writing its own sentence.
 *
 * The buttons are styled through their *host*: a rule on `.wrapped-button` reaches the element from this
 * shadow tree, while the element's own shadow stays untouched — which is what "wrapping" means here.
 */
internal class ButtonDemoSection : CustomElementAdapter(), PageBlock {
    private val shell = sectionShellTag().createElement()

    private val row: HTMLDivElement = HtmlTagName.div.createElement {
        classList.add(ROW_CLASS)
    }

    private val statusElement: HTMLParagraphElement = HtmlTagName.p.createElement {
        classList.add(STATUS_CLASS)
    }

    private val counter = createButton(english.clickMe)

    private val toggler = createButton(english.disableOther)

    private var clicks = 0

    private var translation: Translation = english

    init {
        counter.classList.add(BUTTON_CLASS)
        toggler.classList.add(BUTTON_CLASS)
        counter.onclick = EventHandler {
            clicks += 1
            applyTranslation(translation)
        }
        toggler.onclick = EventHandler {
            counter.disabled = !counter.disabled
            applyTranslation(translation)
        }

        row.appendChild(counter)
        row.appendChild(toggler)

        val shadow = attachStyledShadow(RESET, BUTTON_DEMO_SHEET)
        shadow.appendChild(shell)
        shell.appendChild(row)
        shell.appendChild(statusElement)

        applyTranslation(english)
    }

    /** Labels follow the click count and the disabled flag, so they are derived, never accumulated. */
    override fun applyTranslation(translation: Translation) {
        this.translation = translation
        shell.setText(translation.demoHeading, translation.demoHint)
        counter.textContent = when (clicks) {
            0 -> translation.clickMe
            1 -> translation.clickedOnce
            else -> translation.clickedTimes(clicks)
        }
        toggler.textContent = if (counter.disabled) translation.enableOther else translation.disableOther
        statusElement.textContent = "${translation.statusPrefix}: $clicks"
    }

    companion object : CustomElementReference<ButtonDemoSection>(ButtonDemoSection::class.js) {
        val NAME: TagName<ButtonDemoSection> = TagName("ktjs-button-demo")

        /**
         * The section's rules: the row's layout, the status line, and the host of the wrapped element.
         *
         * `.wrapped-button` colours the `<vaadin-button>` element itself. Its background, border and text
         * colour change on hover, so they live in the sheet — an inline colour would win over `:hover`
         * and freeze the button.
         */
        private val BUTTON_DEMO_SHEET: CSSStyleSheet = cssSheet {
            ROW_CLASS {
                display = Display.flex
                alignItems = AlignItems.center
                columnGap = 12.px
                rowGap = 12.px
                flexWrap = FlexWrap.wrap
            }

            BUTTON_CLASS {
                padding = Padding(9.px, 18.px)
                backgroundColor = accent
                border = Border(1.px, LineStyle.solid, accent)
                borderRadius = 8.px
                color = onAccent
                fontFamily = FontFamily.systemUi
                fontSize = 13.px
                fontWeight = integer(600)
                cursor = Cursor.pointer

                Pseudos.hover {
                    backgroundColor = accentStrong
                    borderColor = accentStrong
                }
            }

            STATUS_CLASS {
                margin = Margin(14.px, 0.px, 0.px)
                color = mutedText
                fontSize = 13.px
            }
        }
    }
}

/** Defines `<ktjs-button-demo>` once and returns its tag. */
internal fun buttonDemoSectionTag(): TagName<ButtonDemoSection> {
    defineIfMissing(ButtonDemoSection.NAME, ButtonDemoSection.Companion)
    return ButtonDemoSection.NAME
}
