---
sidebar_position: 1
---

# 快速开始

## 添加依赖

模块发布在 Maven Central 的 `io.github.__OWNER__` 组下：

```kotlin
dependencies {
    implementation("io.github.__OWNER__:__PREFIX__-__MODULE__:__NPM_VERSION__-kt.1")
}
```

klib 记录了编译时使用的 npm 包版本，Kotlin/JS 工作区会自动装上对应的 `__NPM_PACKAGE__`。

## 构建本站

```bash
./gradlew :docs:docusaurusBuild
```

静态站点输出在 `build/js/docusaurus/docs/build`。

## 本地开发

```bash
./gradlew :docs:docusaurusStart
```

Docusaurus 监听站点目录，改 Markdown 会即时刷新；改 Kotlin 需要先重新构建模块：

```bash
./gradlew :docs:docusaurusSite :docs:docusaurusStart
```

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `src/jsMain/kotlin/__PACKAGE__/docs/Config.kt` | 站点配置与侧边栏 |
| `src/jsMain/resources/docs` | Markdown 页面 |
| `src/jsMain/resources/pages` | MDX 页面（`index.mdx` 是首页） |
| `src/jsMain/resources/css` | 额外样式 |
| `src/jsMain/resources/i18n` | 译文；英文是源语言 |
