import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

// Wrapper version = {upstream npm version}-kt.{revision}. See the versioning section in AGENTS.md.
@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>
@Suppress("UNCHECKED_CAST")
val wrapperRevisions = rootProject.extra["wrapperRevisions"] as Map<String, Int>
val upstreamVersion = npmVersions.getValue("@vaadin/button")
val wrapperRevision = wrapperRevisions.getValue(project.name)
// Empty in a release build, `-SNAPSHOT` in a working tree: see the note in the root build script.
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String
version = "$upstreamVersion-kt.$wrapperRevision$versionSuffix"
description = "Kotlin/JS wrapper for the @vaadin/button package."

kotlin {
    js {
        useEsModules()

        compilerOptions {
            // ES2015 is the first target whose output can express a real class, which custom
            // elements need. Kotlin/JS accepts es5/es2015 only (es2020 fails).
            target = "es2015"
        }

        // The wrapped package is a custom element: it needs a DOM, so the tests run in Chromium
        // (Playwright) instead of Node. A Node-only package would use `nodejs()` and `jsNodeTest`.
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }

        binaries.library()
    }

    compilerOptions {
        // Kotlin/JS dead-code-eliminates a library's JS output down to the `@JsExport` roots, and a
        // wrapper module has none: the whole module would compile to an empty file, so any consumer
        // that imports it at runtime gets `undefined`. Keep the public API; a consumer's bundler
        // still shakes whatever it does not use. See the DCE note in AGENTS.md.
        freeCompilerArgs.add("-Xir-dce=false")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(npm("@vaadin/button", upstreamVersion))

            // The public surface exposes kotlin-browser types (`HTMLElement`, `TagName`), so this is
            // `api`: a consumer compiles against them through this module.
            api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
