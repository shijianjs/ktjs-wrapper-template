# ktjs-wrapper-template-core

[`@vaadin/button`](https://www.npmjs.com/package/@vaadin/button) 的 Kotlin/JS wrapper。它同时是
[`ktjs-wrapper-template`](../README.zh-CN.md) 的示例模块：一个 npm 包、一个 Gradle 模块、一个 Kotlin 包。
派生项目加第二个包时，照着它复制即可。

## 安装

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-wrapper-template-core:25.3.5-kt.1")
}
```

## 用法

```kotlin
import io.github.shijianjs.ktjs.wrappertemplate.core.createButton

val button = createButton("Save") { disabled = true }
button.onclick = { /* ... */ }
document.body.append(button)
```

`createButton` 会 import 上游模块，而这个 import 就是注册 `<vaadin-button>` 的动作。需要构造函数本身
（例如用 `constructor()` 而不是工厂）时用 `registerButton()`。

## 构建与测试

```bash
./gradlew :ktjs-wrapper-template-core:jsBrowserTest
```

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```
