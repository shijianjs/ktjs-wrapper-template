package demo.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * Smoke checks for the demo: the shared shell mounts, the drawer drives the router, the wrapped
 * `<vaadin-button>` is upgraded and reacts to clicks, the table and the list render their copy, and a
 * language switch rewrites the pages in place.
 *
 * The shell lives in a shadow tree, so the demo's own nodes are selected through `#demo` and the page
 * element the shell mounts — never through the document root.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `overview renders the shell and the three cards`(page: Page) {
        openPage(page)

        assertTrue(page.title().contains("ktjs-wrapper-template"), "title: ${page.title()}")
        assertTrue(page.locator("#demo img.layout__logo").isVisible(), "the shell serves the project logo")
        assertEquals("Overview", page.locator("#demo ktjs-demo-page h1").first().textContent())

        // One top-level row for the lifted start group, one group with the two reference pages.
        val rows = page.locator("#demo vaadin-side-nav-item").count()
        assertTrue(rows >= 4, "drawer rows: $rows")

        val cards = page.locator("#demo ktjs-demo-page a.card-link").count()
        assertEquals(3, cards, "one card per page behind the overview")
    }

    @RetryingTest(maxAttempts = 2)
    fun `a card opens its page`(page: Page) {
        openPage(page)
        page.locator("#demo ktjs-demo-page a.card-link[data-route='button']").first().click()
        page.waitForSelector("#demo ktjs-demo-page h1")

        assertEquals("The wrapped element", page.locator("#demo ktjs-demo-page h1").first().textContent())
        assertEquals(2, page.locator("#demo ktjs-demo-page vaadin-button").count())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the wrapped element is upgraded and reacts to a click`(page: Page) {
        openPage(page)
        openRoute(page, "button")

        val counter = page.locator("#demo ktjs-demo-page vaadin-button").first()
        assertEquals("Click me", counter.textContent())
        assertEquals("Clicks: 0", page.locator("#demo ktjs-demo-page .readout").first().textContent())

        counter.click()
        assertEquals("Clicked once", counter.textContent())
        assertEquals("Clicks: 1", page.locator("#demo ktjs-demo-page .readout").first().textContent())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the mapped property reaches the element`(page: Page) {
        openPage(page)
        openRoute(page, "button")

        val counter = page.locator("#demo ktjs-demo-page vaadin-button").first()
        page.locator("#demo ktjs-demo-page vaadin-button").nth(1).click()

        assertTrue(
            counter.evaluate("(node) => node.disabled") as Boolean,
            "the second button sets `disabled` on the first",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `the mapping table and the conventions list render`(page: Page) {
        openPage(page)
        openRoute(page, "api")
        assertEquals("What the wrapper maps", page.locator("#demo ktjs-demo-page h1").first().textContent())
        // Six rows of the mapping plus the header row.
        assertEquals(7, page.locator("#demo ktjs-demo-page table tr").count())

        openRoute(page, "conventions")
        assertEquals("Conventions", page.locator("#demo ktjs-demo-page h1").first().textContent())
        assertEquals(5, page.locator("#demo ktjs-demo-page ul li").count())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the language switch rewrites the page in place`(page: Page) {
        openPage(page)
        openRoute(page, "button")

        switchLanguage(page, "zh-Hans")
        assertEquals("被封装出来的元素", page.locator("#demo ktjs-demo-page h1").first().textContent())
        assertEquals("点我", page.locator("#demo ktjs-demo-page vaadin-button").first().textContent())
        assertEquals("点击次数: 0", page.locator("#demo ktjs-demo-page .readout").first().textContent())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the page keeps a single scrollbar`(page: Page) {
        openPage(page)
        openRoute(page, "api")

        val horizontal = page.evaluate(
            "() => document.documentElement.scrollWidth - document.documentElement.clientWidth",
        ) as Number
        assertEquals(0, horizontal.toInt(), "no horizontal overflow")
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
        openRoute(page, "button")

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun openPage(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
        page.waitForSelector("#demo ktjs-demo-page h1")
    }

    /** Clicks the drawer item for [route]; the router is also what the menu drives. */
    private fun openRoute(page: Page, route: String) {
        page.locator("#demo vaadin-side-nav-item[data-route='$route']").first().click()
        page.waitForSelector("#demo ktjs-demo-page h1")
    }

    /** Opens the layout's language menu and picks [code]. */
    private fun switchLanguage(page: Page, code: String) {
        page.locator("#demo [data-menu='lang']").first().click()
        page.locator("#demo [data-action='lang:$code']").first().click()
        page.waitForFunction("(expected) => document.documentElement.lang === expected", code)
    }
}
