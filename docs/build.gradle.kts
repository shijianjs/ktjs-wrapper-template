plugins {
    kotlin("multiplatform")
    id("io.github.shijianjs.ktjs.docusaurus")
}

/**
 * The project's documentation site: a Kotlin/JS module whose configuration and sidebars are Kotlin
 * and whose pages are Markdown under `src/jsMain/resources`.
 *
 * `./gradlew :docs:docusaurusBuild` assembles the site and runs the Docusaurus CLI on it;
 * `pagesSite` copies the result under `/docs/` of the GitHub Pages site, which is why the `baseUrl`
 * in [Config.kt] points there.
 *
 * The module is loaded by Node (Docusaurus imports `docusaurus.config.mjs` from its bundle), so it
 * must not define custom elements: those read `HTMLElement` at module load. Sites that need
 * browser-only behaviour publish a separate bundle through the `docusaurus { }` extension — see the
 * ktjs-docusaurus documentation.
 */
kotlin {
    js {
        useEsModules()
        nodejs()
        binaries.library()
    }

    compilerOptions {
        // `kotlin.js.JsAny` is the type of every upstream member declared as `unknown`.
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            // The wrapper brings its own `@docusaurus/*` npm dependencies: the klib records the
            // versions it was built against and Kotlin/JS installs them in the npm workspace.
            implementation(
                "io.github.shijianjs:ktjs-docusaurus:${providers.gradleProperty("ktjs-docusaurus.version").get()}",
            )
        }
    }
}

docusaurus {
    // Generates `sidebars.mjs` next to the site configuration from the exported `sidebars` function.
    sidebarsFunction = "sidebars"
}
