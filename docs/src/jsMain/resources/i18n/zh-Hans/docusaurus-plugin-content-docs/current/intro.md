---
sidebar_position: 1
title: 简介
---

# 简介

`ktjs-wrapper-template` 是 Kotlin/JS wrapper 的起点，目标通常是 Web Component 生态的 npm 包。它刻意
**不是**填占位符的脚手架：这个仓库本身就是一个完整项目——能构建、能在真实浏览器里跑测试、有自己的文档站、
能部署到 GitHub Pages——所以复制一份就能在第一次 `./gradlew build` 跑起来。

## 里面有什么

| 部分 | 作用 |
| --- | --- |
| `ktjs-wrapper-template-core` | 示例模块：`@vaadin/button` 25.3.5。一个 npm 包、一个 Gradle 模块、一个 Kotlin 包；加下一个包时照它复制 |
| `demo` | 用 wrapper 造两个 `<vaadin-button>` 的页面，发布在 `/demo/` |
| `docs` | 本站：Kotlin 配置 + Markdown 页面，由 [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 的 Gradle 插件构建 |
| 其余 | 版本清单集中在一个 `package.json`、每模块独立的 `-kt.N` 修订号、Detekt、Playwright 浏览器测试、经 hub 发布到 Maven Central、Pages 工作流 |

`/api/` 下是发布模块的 Dokka 产物——和 `pagesSite` 组装本站时用的是同一次 Dokka。

## 接下来

- [快速开始](guide/getting-started.md)：构建、测试、开发服务器。
- [派生新项目](guide/deriving-a-project.md)：改名的清单。
