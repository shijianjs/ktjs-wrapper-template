---
sidebar_position: 1
title: Introduction
---

# Introduction

`__PROJECT__` is a Kotlin/JS wrapper for
[`__NPM_PACKAGE__`](https://www.npmjs.com/package/__NPM_PACKAGE__).

This site is a Kotlin/JS project as well — nothing here is a hand-written npm project:

- the site configuration and the sidebar are Kotlin
  (`src/jsMain/kotlin/__PACKAGE__/docs/Config.kt`),
- the pages are Markdown files under `src/jsMain/resources/docs`,
- `./gradlew :docs:docusaurusBuild` assembles the site and runs the Docusaurus CLI on it.

Replace this page with the real introduction. The API reference of the wrapper modules is published
separately under `/api/`; link to it from here once the first release exists.
