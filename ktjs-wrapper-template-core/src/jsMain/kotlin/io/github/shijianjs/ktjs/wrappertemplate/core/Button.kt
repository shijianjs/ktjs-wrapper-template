package io.github.shijianjs.ktjs.wrappertemplate.core

import web.components.CustomElementConstructor
import web.dom.TagName
import web.dom.document
import web.html.HTMLElement

/**
 * `<vaadin-button>`: an accessible and customizable button that runs an action.
 *
 * ```html
 * <vaadin-button>Press me</vaadin-button>
 * ```
 *
 * The label is the element's light-DOM text, not a property: upstream renders the children into the
 * `label` part of its shadow tree. Only what upstream declares as a property is mapped here.
 *
 * Styling goes through the shadow parts `label` / `prefix` / `suffix` and the `--vaadin-button-*`
 * custom properties.
 */
abstract external class ButtonElement : HTMLElement {
    /**
     * When disabled, the button is rendered as "dimmed".
     *
     * A disabled button is neither focusable nor clickable. Upstream hides it from assistive
     * technologies unless the `accessibleDisabledButtons` feature flag is enabled.
     */
    var disabled: Boolean
}

/** Tag names of every element this module registers, in one place. */
object Tags {
    /** `<vaadin-button>` */
    inline val button: TagName<ButtonElement> get() = TagName("vaadin-button")
}

/**
 * The upstream module.
 *
 * Importing it is what registers the custom element, which is why the element class is only reachable
 * through [registerButton] instead of being exported on its own.
 */
@JsModule("@vaadin/button/src/vaadin-button.js")
private external object ButtonModule {
    val Button: CustomElementConstructor<ButtonElement>
}

/** Imports the upstream module and returns the constructor it registers. */
fun registerButton(): CustomElementConstructor<ButtonElement> = ButtonModule.Button

/**
 * Creates a `<vaadin-button>` whose label is [label].
 *
 * The custom element is registered on first use, so there is no separate registration step; [configure]
 * runs before the element is returned, which makes it safe to use in a `document.body.append(...)` chain.
 */
fun createButton(label: String, configure: ButtonElement.() -> Unit = {}): ButtonElement {
    registerButton()
    return document.createElement(Tags.button).apply {
        textContent = label
        configure()
    }
}
