---
sidebar_position: 1
title: Introduction
---

# Introduction

`ktjs-wrapper-template` is a starting point for Kotlin/JS wrappers around npm packages, mostly Web
Components. It is deliberately **not** a scaffold full of placeholders: the repository is a complete
project that builds, tests in a real browser, documents itself and deploys to GitHub Pages, so copying
it gives you something that runs on the first `./gradlew build`.

## What is in it

| Part | Role |
| --- | --- |
| `ktjs-wrapper-template-core` | the sample wrapper: `@vaadin/button` 25.3.5. One npm package, one Gradle module, one Kotlin package — copy it for the next package |
| `demo` | a page that creates two `<vaadin-button>` elements through the wrapper; published under `/demo/` |
| `docs` | this site: Kotlin configuration, Markdown pages, built by the [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) Gradle plugin |
| the rest | npm versions in one `package.json`, per-module `-kt.N` revisions, Detekt, Playwright browser tests, Maven Central release through the hub, Pages workflow |

The API reference under `/api/` is Dokka output for the published modules — the same Dokka run that
`pagesSite` assembles next to this site.

## Where to go next

- [Getting started](guide/getting-started.md) — build it, test it, run the dev server.
- [Deriving a project](guide/deriving-a-project.md) — the rename checklist for turning this into your
  own wrapper.
