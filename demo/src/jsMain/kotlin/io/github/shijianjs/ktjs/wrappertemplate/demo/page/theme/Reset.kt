package io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme

import io.github.shijianjs.ktjs.dom.css.cssSheet
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.Display

/**
 * The one sheet every component adopts before its own.
 *
 * Both rules exist for the same reason — a shadow tree is a separate box tree and inherits neither the
 * host page's `box-sizing` nor a sane `display` for a custom element:
 *
 * - `:host { display: block }`: a custom element defaults to `inline`, which makes every margin and
 *   padding of the component behave like an inline box.
 * - `box-sizing` is not inherited, so `* { box-sizing: border-box }` has to be redeclared inside every
 *   shadow root. Without it, padded boxes measure against `content-box` and layouts shift.
 *
 * It is adopted per shadow root, never on `document`: nothing in the demo paints the host page from a
 * global stylesheet.
 */
internal val RESET: CSSStyleSheet = cssSheet {
    ":host" { display = Display.block }

    "*, *::before, *::after" { boxSizing = BoxSizing.borderBox }
}
