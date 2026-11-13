---
sidebar_position: 2
---

# Deriving a project

Everything below assumes you start from a copy of this repository (GitHub's **Use this template**, or
`git clone` and delete `.git`). There is no placeholder script to run: the workflow is a rename, and
the compiler and the tests tell you what is left.

## 1. Identity

| Where | What to change |
| --- | --- |
| `settings.gradle.kts` | `rootProject.name`, the `include(...)` of the module |
| module directory | rename it; the directory name, the `include(...)` and the `wrapper-revisions.properties` key must stay identical |
| `build.gradle.kts` | `publishedPrefix` (the published-module filter), POM `url` / `scm`, `inceptionYear` |
| `LICENSE`, READMEs | copyright line and links |
| Kotlin package | move `src/*/kotlin/io/github/shijianjs/...` and update the `package` lines |

## 2. The wrapped package

| Where | What to change |
| --- | --- |
| `package.json` | the npm package and its version — the only place a version is declared |
| module `build.gradle.kts` | `npmVersions.getValue("<package>")` |
| `src/jsMain/kotlin/...` | the wrapper declarations, following the `ktjs-type-mapping` skill |
| `src/jsTest/kotlin/...` | tests derived from the upstream tests, adapted to the pinned version |
| `demo/src/jsMain/kotlin/demo/Main.kt` | a page that actually exercises the wrapper |

## 3. The site

`docs/src/jsMain/kotlin/.../docs/Config.kt` holds every site-wide value: `title`, `tagline`, `url`,
`SITE_BASE_URL` (the repository name), `REPOSITORY`, and the two links to `/demo/` and `/api/`. The
pages under `docs/src/jsMain/resources` are content only; the translations mirror the same tree under
`i18n/zh-Hans/`.

The landing page uses the kit's `<ktjs-hero>` / `<ktjs-features>` / `<ktjs-next-steps>` sections. Keep
`ClientModule.kt` and the `browserBundle` block in `docs/build.gradle.kts` as they are; only the copy
changes. The logo has one source, `docs/src/jsMain/resources/static/img/`, and the demo's
`brandAssets` task copies it into its own resources — replace that file and update the three places
that name it (`Config.kt`, `demo/src/jsMain/resources/index.html`, `demo/.../Main.kt`).

## 4. Verify

```bash
./gradlew kotlinNpmInstall
./gradlew kotlinUpgradePackageLock   # the lock file records the module versions, so this is not optional
./gradlew build
./gradlew pagesSite
```

Then enable GitHub Pages with **GitHub Actions** as the source and run the `Deploy demo and API
reference` workflow once. Publishing is optional: `.github/workflows/release.yml` signs the artifacts
and hands them to the Maven Central hub, and needs the secrets listed in `AGENTS.md` before its first
run.
