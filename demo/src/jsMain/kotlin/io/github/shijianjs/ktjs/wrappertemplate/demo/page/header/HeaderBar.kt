package io.github.shijianjs.ktjs.wrappertemplate.demo.page.header

import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.Pseudos
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.PageBlock
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.lang.Translation
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.RESET
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.accent
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.bodyText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.hairline
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.mutedText
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme.panel
import web.components.CustomElementReference
import web.cssom.AlignItems
import web.cssom.Auto
import web.cssom.Border
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.Cursor
import web.cssom.Display
import web.cssom.FlexWrap
import web.cssom.FontWeight
import web.cssom.LineStyle
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.em
import web.cssom.number
import web.cssom.px
import web.cssom.rem
import web.dom.TagName
import web.events.EventHandler
import web.html.ButtonType
import web.html.HTMLButtonElement
import web.html.HTMLHeadingElement
import web.html.HTMLImageElement
import web.html.HTMLParagraphElement
import web.html.HTMLElement
import web.html.HtmlTagName
import web.html.button

/** The header's own class names: no other file selects or carries them, so they live here. */
private val LOGO_CLASS: ClassName = ClassName("logo")

private val TITLE_CLASS: ClassName = ClassName("title")

private val SUBTITLE_CLASS: ClassName = ClassName("subtitle")

private val LANG_CLASS: ClassName = ClassName("lang-switch")

/**
 * `<ktjs-demo-header>`: the project logo, the title, one line of description and the language button.
 *
 * The element renders itself into its shadow root; the page only hands it a click callback and asks it
 * to apply the current language. The button's label says which language it switches *to*, which is why
 * it is one of the nodes [applyTranslation] rewrites.
 */
internal class HeaderBar : CustomElementAdapter(), PageBlock {
    /** What a click on the language button means to the page; the component does not know the page. */
    var onSwitch: (() -> Unit)? = null

    /** The logo is copied in from the site's static files by the root build's `brandAssets` task. */
    private val logoElement: HTMLImageElement = HtmlTagName.img.createElement {
        classList.add(LOGO_CLASS)
        alt = "ktjs-wrapper-template logo"
        src = "img/ktjs-wrapper-template-logo.png"
    }

    private val titleElement: HTMLHeadingElement = HtmlTagName.h1.createElement {
        classList.add(TITLE_CLASS)
    }

    private val subtitleElement: HTMLParagraphElement = HtmlTagName.p.createElement {
        classList.add(SUBTITLE_CLASS)
    }

    private val switchButton: HTMLButtonElement = HtmlTagName.button.createElement {
        classList.add(LANG_CLASS)
        type = ButtonType.button
        onclick = EventHandler { onSwitch?.invoke() }
    }

    private val copy: HTMLElement = HtmlTagName.div.createElement {
        appendChild(titleElement)
        appendChild(subtitleElement)
    }

    init {
        // One sheet per element class, adopted by every instance: the rules are static, so parsing
        // them is paid once, not once per header.
        val shadow = attachStyledShadow(RESET, HEADER_SHEET)
        shadow.appendChild(logoElement)
        shadow.appendChild(copy)
        shadow.appendChild(switchButton)
    }

    override fun applyTranslation(translation: Translation) {
        titleElement.textContent = translation.title
        subtitleElement.textContent = translation.subtitle
        switchButton.textContent = translation.switchLabel
    }

    companion object : CustomElementReference<HeaderBar>(HeaderBar::class.js) {
        val NAME: TagName<HeaderBar> = TagName("ktjs-demo-header")

        /**
         * The header's own rules. The host is the flex row that puts the logo, the copy and the button
         * apart. The button's colours live here rather than inline: a `<button>` otherwise keeps the UA's
         * `buttontext` colour, and an inline colour would win over the `:hover` rule below.
         */
        private val HEADER_SHEET: CSSStyleSheet = cssSheet {
            ":host" {
                display = Display.flex
                alignItems = AlignItems.center
                columnGap = 16.px
                rowGap = 12.px
                flexWrap = FlexWrap.wrap
                paddingBottom = 20.px
                borderBottomWidth = 1.px
                borderBottomStyle = LineStyle.solid
                borderBottomColor = hairline
            }

            LOGO_CLASS {
                width = 56.px
                height = 56.px
                borderRadius = 12.px
            }

            TITLE_CLASS {
                margin = Margin(0.px, 0.px)
                fontSize = 30.px
                letterSpacing = (-0.02).em
                color = bodyText
            }

            SUBTITLE_CLASS {
                margin = Margin(6.px, 0.px, 0.px)
                color = mutedText
                fontSize = 14.px
                lineHeight = number(1.55)
                maxWidth = 46.rem
            }

            LANG_CLASS {
                marginLeft = Auto.auto
                padding = Padding(6.px, 14.px)
                backgroundColor = panel
                border = Border(1.px, LineStyle.solid, hairline)
                borderRadius = 999.px
                color = mutedText
                fontSize = 12.px
                fontWeight = FontWeight.bold
                cursor = Cursor.pointer

                Pseudos.hover {
                    borderColor = accent
                    color = accent
                }
            }
        }
    }
}

/** Defines `<ktjs-demo-header>` once and returns its tag, ready to `createElement()`. */
internal fun headerBarTag(): TagName<HeaderBar> {
    defineIfMissing(HeaderBar.NAME, HeaderBar.Companion)
    return HeaderBar.NAME
}
