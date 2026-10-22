# __PROJECT__

Kotlin/JS wrappers for the [`__NPM_PACKAGE__`](https://www.npmjs.com/package/__NPM_PACKAGE__) ecosystem.

[English](README.md) | [简体中文](README.zh-CN.md)

**[Live demo](https://__OWNER__.github.io/__REPO__/)** ·
**[API reference](https://__OWNER__.github.io/__REPO__/api/)**

The repository keeps one `__PREFIX__-*` Gradle module per npm package, or per group of packages
that share an upstream version and release cadence. Tree shaking removes unused declarations from
the final application bundle, so grouping related packages into one module is cheaper than one
module per package.

## Modules

| Module | npm package | Wrapper version | Maven Central |
| --- | --- | --- | --- |
| [__PREFIX__-__MODULE__](__PREFIX__-__MODULE__/README.md) | `__NPM_PACKAGE__` | `__NPM_VERSION__-kt.1` | [![Maven Central](https://img.shields.io/maven-central/v/io.github.__OWNER__/__PREFIX__-__MODULE__?label=Maven%20Central&logo=apachemaven&logoColor=white)](https://central.sonatype.com/artifact/io.github.__OWNER__/__PREFIX__-__MODULE__) |

## Versioning

Each module is versioned as `{upstream npm version}-kt.{revision}`. The upstream npm versions are
declared once in the root [`package.json`](package.json); the revision is read per module from
[`wrapper-revisions.properties`](wrapper-revisions.properties). Gradle derives the base version and
the `npm(...)` dependency from these two files:

- Only `-kt.N` versions are published; a version without the suffix never exists.
- Upstream upgrade: change the version in `package.json`; the base version follows automatically
  while the revision stays, even when the wrapper code itself did not change.
- Wrapper-only fix: the revision increments (`kt.2`, `kt.3`, ...) while the base version stays.
- Revisions are tracked per module and only ever increase; they never reset to `kt.1`. A release
  bumps and publishes only the modules that changed.

## Requirements

- JDK 17 or newer (the build uses Gradle 9.4.1).
- Node.js is downloaded automatically by the Kotlin Gradle plugin. npm is used because Yarn is
  disabled in `gradle.properties`.
- Chromium is downloaded by Playwright the first time browser tests run.
- The demo's end-to-end test is a standalone Node script, so `node` must also be on `PATH`.

## Build and test

```bash
./gradlew build

# Per-module tests
./gradlew :__PREFIX__-__MODULE__:jsBrowserTest

# End-to-end checks for the bundled demo page (Playwright, needs node on PATH)
./gradlew :demo:e2eTest

# GitHub Pages site (demo at the root, Dokka output under /api/)
./gradlew pagesSite
```

After adding or upgrading an npm dependency in a module, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```

## Installation

Releases are published to Maven Central under the `io.github.__OWNER__` group. Replace the version
with the one you need (`{upstream version}-kt.{revision}`):

```kotlin
dependencies {
    implementation("io.github.__OWNER__:__PREFIX__-__MODULE__:__NPM_VERSION__-kt.1")
}
```

The POMs point at this GitHub repository (`url` and `scm.url`), and every module is a Kotlin
Multiplatform project publishing `kotlin-tooling-metadata.json`. Those are the requirements for
[klibs.io](https://klibs.io/) to discover the libraries automatically.

## Publishing

Publishing is configured once in the root [`build.gradle.kts`](build.gradle.kts) for all modules.
Releases run through `.github/workflows/release.yml`: it signs the artifacts, packages one archive
per Maven coordinate and registers them with the shared Maven Central hub. The hub later merges the
pending registrations into a single Central deployment. See the publishing section in
[AGENTS.md](AGENTS.md) for the one-time setup.

## License

Released under the [MIT License](LICENSE).
