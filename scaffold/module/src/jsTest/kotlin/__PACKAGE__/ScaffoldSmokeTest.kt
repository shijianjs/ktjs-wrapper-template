package __PACKAGE__

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the scaffold: the init script must replace every placeholder before the first build.
 *
 * Replace this class with tests derived from the upstream package once the wrapper has a real API.
 */
class ScaffoldSmokeTest {
    @Test
    fun placeholdersWereReplaced() {
        assertTrue(UPSTREAM_PACKAGE.isNotBlank(), "the npm package name is blank")
        assertFalse(UPSTREAM_PACKAGE.contains("__"), "unreplaced placeholder in $UPSTREAM_PACKAGE")
    }
}
