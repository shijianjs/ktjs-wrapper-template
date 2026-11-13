# ktjs-wrapper-template-core

Kotlin/JS wrapper for [`@vaadin/button`](https://www.npmjs.com/package/@vaadin/button). This is the
sample module of [`ktjs-wrapper-template`](../README.md): one npm package, one Gradle module, one
Kotlin package. Copy it when a derived project adds a second package.

## Installation

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-wrapper-template-core:25.3.5-kt.1")
}
```

## Usage

```kotlin
import io.github.shijianjs.ktjs.wrappertemplate.core.createButton

val button = createButton("Save") { disabled = true }
button.onclick = { /* ... */ }
document.body.append(button)
```

`createButton` imports the upstream module, and that import is what registers `<vaadin-button>`.
Reach for `registerButton()` when the constructor itself is needed, for example to build the element
with `constructor()` instead of the factory.

## Build and test

```bash
./gradlew :ktjs-wrapper-template-core:jsBrowserTest
```

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```
