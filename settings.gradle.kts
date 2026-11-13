pluginManagement {
    repositories {
        // The docs site is built with the ktjs-docusaurus Gradle plugin. Released versions come from
        // Central; `mavenLocal` is what serves a SNAPSHOT one, which is how the site is developed
        // against an unreleased wrapper. An empty local repository falls through to the next
        // repository, so the order costs nothing in CI.
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }

    // The plugin's version lives in gradle.properties next to the wrapper's: both come from
    // ktjs-docusaurus, but each of its modules carries its own `-kt.N` revision.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.shijianjs.ktjs.docusaurus") {
                useVersion(providers.gradleProperty("ktjs-docusaurus-gradle-plugin.version").get())
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        // Same reasoning as above: the wrapper klib of a SNAPSHOT version comes from `mavenLocal`.
        mavenLocal()
        // Maven Central is the source of truth for kotlin-wrappers: the Aliyun mirror lags behind
        // and sometimes serves metadata without the corresponding klibs. Gradle does not fall back
        // to the next repository once metadata is found, so pin these modules to Central even when
        // a local mirror is active.
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("org.jetbrains.kotlin-wrappers")
                includeModule("org.jetbrains.kotlin", "kotlin-js-plain-objects")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "ktjs-wrapper-template"

// One Gradle project per published wrapper module. The sample module wraps a single npm package;
// a derived project adds one include per package (or per group of packages that release together).
include(":ktjs-wrapper-template-core")

// The demo page, published under /demo/ of the Pages site. Not a published module.
include(":demo")

// The documentation site: Kotlin configuration plus Markdown, assembled by the ktjs-docusaurus
// Gradle plugin into a static site that `pagesSite` publishes at the Pages root.
include(":docs")
