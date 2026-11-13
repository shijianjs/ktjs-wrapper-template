package demo.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * Smoke checks for the demo page: it must load the bundle, apply its stylesheet, render the wrapped
 * custom element and produce no console errors or failed requests.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `page renders the project heading and styles`(page: Page) {
        openPage(page)

        assertTrue(page.title().contains("ktjs-wrapper-template"), "title: ${page.title()}")
        assertEquals("ktjs-wrapper-template demo", page.locator("h1").textContent())

        // The logo is copied in from the site's static files by the `brandAssets` task.
        assertTrue(page.locator("img.logo").isVisible(), "the demo serves the project logo")

        val display = page.locator(".page").evaluate("(node) => getComputedStyle(node).display") as String
        assertTrue(display != "contents", "the stylesheet applies to .page (display = $display)")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the wrapped element is upgraded and reacts to a click`(page: Page) {
        openPage(page)

        val counter = page.locator("vaadin-button").first()
        assertEquals("Click me", counter.textContent())
        assertEquals("Clicks: 0", page.locator(".status").textContent())

        counter.click()

        assertEquals("Clicked once", counter.textContent())
        assertEquals("Clicks: 1", page.locator(".status").textContent())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the mapped property reaches the element`(page: Page) {
        openPage(page)

        val counter = page.locator("vaadin-button").first()
        page.getByText("Disable the other button").click()

        assertTrue(
            counter.evaluate("(node) => node.disabled") as Boolean,
            "clicking the second button sets the property on the first",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `page logs no console errors or failed requests`(page: Page) {
        val consoleErrors = mutableListOf<String>()
        val failedRequests = mutableListOf<String>()
        page.onConsoleMessage { message ->
            if (message.type() == "error") consoleErrors += message.text()
        }
        page.onRequestFailed { request ->
            failedRequests += "${request.url()} ${request.failure()}"
        }

        openPage(page)

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun openPage(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
    }
}
