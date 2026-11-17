package io.github.shijianjs.ktjs.wrappertemplate.demo.page.theme

import web.cssom.Color
import web.cssom.rgb

/**
 * The demo's palette, as typed colours.
 *
 * A token several components must paint with is exactly what a shared constant is for: the page, the
 * header, the sections and the wrapped element's host all read the same values, and a colour changes
 * in one place. It stays a Kotlin constant rather than a CSS variable because nothing outside the
 * bundle needs to override it.
 */
internal val pageBackground: Color = rgb(255, 255, 255)

internal val panel: Color = rgb(248, 250, 252)

internal val panelStrong: Color = rgb(241, 245, 249)

internal val bodyText: Color = rgb(31, 41, 51)

internal val mutedText: Color = rgb(82, 96, 109)

internal val accent: Color = rgb(37, 99, 235)

internal val accentStrong: Color = rgb(29, 78, 216)

internal val onAccent: Color = rgb(255, 255, 255)

internal val hairline: Color = rgb(226, 232, 240)
