package io.github.shijianjs.ktjs.wrappertemplate.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the wrapper against the element the upstream package registers: the tag name the factory
 * builds, the label it sets, and the one property the wrapper maps.
 */
class ButtonTest {
    @Test
    fun createButtonBuildsTheRegisteredElement() {
        val button = createButton("Save")

        assertEquals("VAADIN-BUTTON", button.tagName)
        assertEquals("Save", button.textContent)
        assertFalse(button.disabled)
    }

    @Test
    fun configureRunsBeforeTheElementIsReturned() {
        val button = createButton("Save") { disabled = true }

        assertTrue(button.disabled)
    }

    @Test
    fun registeringTwiceIsHarmless() {
        // Every factory call imports the upstream module, so a second call must not throw
        // "the name has already been used with this registry".
        registerButton()
        val first = createButton("One")
        val second = createButton("Two")

        assertEquals("One", first.textContent)
        assertEquals("Two", second.textContent)
    }

    @Test
    fun disabledRoundTripsThroughTheProperty() {
        val button = createButton("Save").apply { disabled = true }
        button.disabled = false

        assertFalse(button.disabled, "the property is the source of truth for the wrapper")
    }
}
