plugins {
    kotlin("multiplatform")
    id("io.github.shijianjs.ktjs.docusaurus")
}

/**
 * The project's documentation site: a Kotlin/JS module whose configuration and sidebars are Kotlin
 * and whose pages are Markdown under `src/jsMain/resources`.
 *
 * `./gradlew :docs:docusaurusBuild` assembles the site and runs the Docusaurus CLI on it;
 * `pagesSite` copies the result to the GitHub Pages root, which is why the `baseUrl` in `Config.kt`
 * points there.
 *
 * The module is loaded by Node (Docusaurus imports `docusaurus.config.mjs` from its bundle), so it
 * must not define custom elements: those read `HTMLElement` at module load. The landing page's
 * `<ktjs-…>` sections therefore come from the kit's browser bundle, which is loaded by the browser
 * alone (see [ClientModule.kt] and the `docusaurus { }` block below).
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

// The kit's browser half — every `<ktjs-…>` element the landing page uses — is not a Maven artifact of
// its own: it ships as a klib resource (`ktjs-client/ktjs-docusaurus-kit-browser.js`), because it is
// UI, not an API anyone compiles against. `@klib` on the JS coordinate resolves the file without
// variant matching; the element bundle is then served as a browser-only script.
val kitBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    kitBundle(
        "io.github.shijianjs:ktjs-docusaurus-kit-js:" +
            "${providers.gradleProperty("ktjs-docusaurus-kit.version").get()}@klib",
    )
}

val kitBundleDir = layout.buildDirectory.dir("generated/kit-bundle/ktjs-client")

val extractKitBundle = tasks.register<Sync>("extractKitBundle") {
    from(zipTree(kitBundle.singleFile)) {
        include("ktjs-client/**")
    }
    into(layout.buildDirectory.dir("generated/kit-bundle"))
}

docusaurus {
    // Generates `sidebars.mjs` next to the site configuration from the exported `sidebars` function.
    sidebarsFunction = "sidebars"

    // Re-exported into `client-module.mjs`; the site loads the kit bundle from it after hydration.
    clientModuleFunction = "onRouteDidUpdate"

    // Copied to `static/ktjs-client/` and imported by the generated `index.mjs`. A file collection
    // cannot carry a task dependency, hence the explicit `map`.
    browserBundle = "ktjs-docusaurus-kit-browser.js"
    browserBundleFiles.from(extractKitBundle.map { kitBundleDir.get().asFile })
}
