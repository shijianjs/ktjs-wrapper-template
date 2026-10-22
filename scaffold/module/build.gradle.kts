import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

// Wrapper version = {upstream npm version}-kt.{revision}. See the versioning section in AGENTS.md.
@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>
@Suppress("UNCHECKED_CAST")
val wrapperRevisions = rootProject.extra["wrapperRevisions"] as Map<String, Int>
val upstreamVersion = npmVersions.getValue("__NPM_PACKAGE__")
val wrapperRevision = wrapperRevisions.getValue(project.name)
version = "$upstreamVersion-kt.$wrapperRevision"
description = "Kotlin/JS wrapper for the __NPM_PACKAGE__ package."

kotlin {
    js {
        useEsModules()

        // Browser test runtime. Swap to `nodejs()` (and jsNodeTest) for a Node-only package.
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }
    }

    sourceSets {
        jsMain.dependencies {
            implementation(npm("__NPM_PACKAGE__", upstreamVersion))

            // Uncomment for packages whose API exposes DOM types. Public declarations must then use
            // kotlin-browser's web.* types, never org.w3c.dom.* or kotlinx.browser.*.
            // api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.10.1")
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
