---
sidebar_position: 1
---

# Getting started

## Add the dependency

The modules are published to Maven Central under the `io.github.__OWNER__` group:

```kotlin
dependencies {
    implementation("io.github.__OWNER__:__PREFIX__-__MODULE__:__NPM_VERSION__-kt.1")
}
```

The klib records the npm package version it was built against, so the Kotlin/JS workspace installs
the matching `__NPM_PACKAGE__` automatically.

## Build this site

```bash
./gradlew :docs:docusaurusBuild
```

The static site is written to `build/js/docusaurus/docs/build`.

## Develop with a dev server

```bash
./gradlew :docs:docusaurusStart
```

Docusaurus watches the site directory, so Markdown edits reload the page. Kotlin changes need a new
build of the module first:

```bash
./gradlew :docs:docusaurusSite :docs:docusaurusStart
```

## Where things live

| Path | Contents |
| --- | --- |
| `src/jsMain/kotlin/__PACKAGE__/docs/Config.kt` | Site configuration and sidebar |
| `src/jsMain/resources/docs` | Markdown pages |
| `src/jsMain/resources/pages` | MDX pages (`index.mdx` is the landing page) |
| `src/jsMain/resources/css` | Extra stylesheets |
| `src/jsMain/resources/i18n` | Translations; English is the source language |
