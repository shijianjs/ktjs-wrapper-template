---
sidebar_position: 1
title: 简介
---

# 简介

`__PROJECT__` 是 [`__NPM_PACKAGE__`](https://www.npmjs.com/package/__NPM_PACKAGE__) 的 Kotlin/JS
wrapper。

本站本身也是一个 Kotlin/JS 项目，没有任何手写的 npm 工程：

- 站点配置与侧边栏是 Kotlin（`src/jsMain/kotlin/__PACKAGE__/docs/Config.kt`），
- 页面是 `src/jsMain/resources/docs` 里的 Markdown，
- `./gradlew :docs:docusaurusBuild` 组装站点并调用 Docusaurus CLI。

把这一页换成真正的介绍。wrapper 模块的 API 文档另外发布在 `/api/` 下，第一版发布后从这里链过去。
