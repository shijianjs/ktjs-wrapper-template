plugins {
    kotlin("multiplatform")
}

// The demo shell (`ktjs-demo-kit`) is built from Vaadin's published wrapper, which ships the JavaScript
// side of `@vaadin/vaadin-lumo-styles` but not the CSS: that is a page-level `<link>`, taken from the
// npm workspace the Kotlin/JS plugin installs at the root.
val lumoStyles = rootProject.layout.buildDirectory.dir("js/node_modules/@vaadin/vaadin-lumo-styles/dist")

val copyLumoStyles = tasks.register<Copy>("copyLumoStyles") {
    description = "Copies the pre-built Lumo stylesheet into the demo resources."
    dependsOn(":kotlinNpmInstall")
    from(lumoStyles) {
        include("lumo.css")
        include("utility.css")
    }
    into(layout.buildDirectory.dir("generated/lumo"))
}

// The brand image lives with the site's static files and is copied in here, so the demo and the
// documentation cannot drift apart. `srcDir(task)` only contributes the directory, not the
// dependency, hence the explicit wiring below.
val brandAssets = tasks.register<Copy>("brandAssets") {
    from(rootProject.file("docs/src/jsMain/resources/static/img")) {
        // Keep the same `img/` layout the site uses, so both pages reference `img/<logo>.png`.
        include("*.png")
        into("img")
    }
    into(layout.buildDirectory.dir("generated/brand"))
}

kotlin {
    js {
        useEsModules()

        // The demo's own components are real custom elements (`class X extends HTMLElement`). At the
        // default ES5 target Kotlin emits `function X() { HTMLElement.call(this) }`, and a DOM
        // constructor may only be invoked with `new` — the browser throws "Please use the 'new'
        // operator" and the tag never upgrades. ES2015 is the first target that can express a class.
        compilerOptions {
            target = "es2015"
        }

        // The demo is a standalone page, so the JS target needs an executable webpack bundle.
        binaries.executable()
        browser()
    }

    // The JVM target carries only the Playwright end-to-end suite in `src/jvmTest`; the demo itself
    // stays a JS page.
    jvm()

    sourceSets {
        jsMain {
            resources.srcDir(brandAssets)
            resources.srcDir(copyLumoStyles)
        }
        jsMain.dependencies {
            implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.2")
            // dom-kit is what the pages themselves use for typed nodes and sheets: `HtmlTagName.a { … }`,
            // the `cssSheet` DSL and the class names a page ships with its own rules. Detekt forbids mixing
            // `web.*` with `org.w3c.dom.*` / `kotlinx.browser.*`.
            implementation(
                "io.github.shijianjs:ktjs-dom-kit:" +
                    providers.gradleProperty("ktjs-dom-kit.version").get(),
            )
            // The shared shell: layout, menu, router, i18n and the page blocks. Published to
            // `mavenLocal` by a working tree of the `ktjs-dom-kit` repository until the release batch.
            implementation(
                "io.github.shijianjs:ktjs-demo-kit-js:" +
                    providers.gradleProperty("ktjs-demo-kit.version").get(),
            )
            // The components the pages compose with (card, horizontal / vertical layout) and the icon
            // set the drawer marks come from.
            implementation(
                "io.github.shijianjs:ktjs-vaadin-web-components-js:" +
                    providers.gradleProperty("ktjs-vaadin-web-components.version").get(),
            )
            implementation(
                "io.github.shijianjs:ktjs-iconify-icon-js:" +
                    providers.gradleProperty("ktjs-iconify-icon.version").get(),
            )
            // The published module the demo demonstrates, one line per module.
            implementation(project(":ktjs-wrapper-template-core"))
        }

        jvmTest.dependencies {
            implementation("com.microsoft.playwright:playwright:1.61.0")
            implementation("cn.hutool:hutool-all:5.8.48")
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
            implementation("org.junit-pioneer:junit-pioneer:2.3.0")
            implementation(kotlin("test-junit5"))
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
        }
    }
}

// `srcDir(task)` contributes the directory but not the task dependency; resource processing is what
// reads that directory, so the edge belongs there.
tasks.named("jsProcessResources") {
    dependsOn(brandAssets, copyLumoStyles)
}

// ---------------------------------------------------------------------------------------------
// End-to-end suite
//
// The suite lives in `src/jvmTest`, so the demo stays one KMP module instead of carrying a second
// Gradle project. It uses Playwright's official JUnit 5 integration plus Hutool's SimpleServer for
// the static bundle, and reuses the Chromium that `kotlinInstallPlaywrightBrowsers` installs for
// the Kotlin/JS browser tests (hence the pinned Playwright version in the dependencies).
// `jvmTest` itself is disabled to keep `build`/`check` browser-free; this dedicated task runs the
// same test compilation on demand.
// ---------------------------------------------------------------------------------------------
val jvmE2eCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Runs the Kotlin/JVM Playwright suite against the bundled demo page."
    useJUnitPlatform()
    dependsOn("jsBrowserDistribution")
    // The Chromium is installed for the Kotlin/JS browser tests of the wrapper module, and this suite
    // reuses it. A derived project points this at its own first published module.
    dependsOn(":ktjs-wrapper-template-core:kotlinInstallPlaywrightBrowsers")

    testClassesDirs = jvmE2eCompilation.output.classesDirs
    classpath = files(jvmE2eCompilation.output.allOutputs, jvmE2eCompilation.runtimeDependencyFiles)
    systemProperty("demo.dist", layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile.absolutePath)
    systemProperty("e2e.output", layout.buildDirectory.dir("e2e-results").get().asFile.absolutePath)
    systemProperty("junit.jupiter.execution.timeout.default", "30 s")

    outputs.upToDateWhen { false }
}

tasks.named<Test>("jvmTest") { enabled = false }
