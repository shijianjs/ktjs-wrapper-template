# ktjs-wrapper-template

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-wrapper-template-logo.png" width="120" alt="ktjs-wrapper-template"></p>

一个立刻能跑的 Kotlin/JS wrapper 项目：一个 Gradle 模块包装
[`@vaadin/button`](https://www.npmjs.com/package/@vaadin/button)，加上 demo 页面、用
[ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 构建的文档站，以及围绕它们的测试与发布
流水线。

[English](README.md) | [简体中文](README.zh-CN.md)

**[文档](https://shijianjs.github.io/ktjs-wrapper-template/)** ·
**[在线演示](https://shijianjs.github.io/ktjs-wrapper-template/demo/)** ·
**[API 文档](https://shijianjs.github.io/ktjs-wrapper-template/api/)**

复制仓库、改名即可——这就是全部「初始化」。这里没有等着脚本替换的占位符，所以复制出来的副本直接
`./gradlew build` 就能过。改名清单在文档里：
[派生新项目](https://shijianjs.github.io/ktjs-wrapper-template/zh-Hans/guide/deriving-a-project)。

## 模块

| 模块 | npm 包 | wrapper 版本 |
| --- | --- | --- |
| [ktjs-wrapper-template-core](ktjs-wrapper-template-core/README.zh-CN.md) | `@vaadin/button` | `25.3.5-kt.1` |

仓库按 npm 包（或一组共享上游版本与发版节奏的包）划分 `ktjs-wrapper-template-*` Gradle 模块。终端应用
打包时本来就会摇树，把相关包合并到一个模块，比每个包各建一个模块更省发布额度和版本噪音。

## 版本约定

每个模块的版本为 `{上游 npm 版本}-kt.{修订号}`。上游 npm 版本只在根 [`package.json`](package.json)
中声明；每个模块的修订号在 [`wrapper-revisions.properties`](wrapper-revisions.properties) 中维护。
Gradle 从这两份文件推导基础版本和 `npm(...)` 依赖：

- 只发布带 `-kt.N` 后缀的版本，不存在不带后缀的版本。
- 工作树里的版本一律再带 `-SNAPSHOT`：推导出的是「下一个要发的版本」，Central 上还没有，开发构建不冒用它。
  发布时才用 `-Pktjs.release=true` 去掉后缀。
- 上游升级：改 `package.json` 里的版本，基础版本自动跟随，修订号保持不变。
- 仅 wrapper 修复：只把对应模块的修订号加一（`kt.2`、`kt.3`……）。
- 修订号按模块维护，只增不减，永不重置为 `kt.1`。发布时只发修订号变化的模块。

## 环境要求

- JDK 21 或更高（构建使用 Gradle 9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件自动下载；`gradle.properties` 里的 `kotlin.js.yarn=false` 保证包管理器
  是 npm。
- 第一次跑浏览器测试时由 Playwright 下载 Chromium。

## 构建与测试

```bash
./gradlew build                    # 编译、Detekt、浏览器测试
./gradlew :ktjs-wrapper-template-core:jsBrowserTest

# demo 页面的端到端检查（Playwright，打打包产物）
./gradlew :demo:e2eTest

# 文档站：Kotlin 配置 + Markdown 页面，由 ktjs-docusaurus 构建
./gradlew :docs:docusaurusBuild

# GitHub Pages 站点（根目录是文档，demo 在 /demo/，API 文档在 /api/）
./gradlew pagesSite
```

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```

`docs` 模块同样是 Kotlin/JS 项目。三个 ktjs-docusaurus 版本都写在 `gradle.properties` 里：wrapper klib、
Gradle 插件、以及首页分区用到的工具箱——那个项目的每个模块各自带 `-kt.N` 修订号。写 SNAPSHOT 时从
`mavenLocal` 解析。工具箱的浏览器那一半不是独立的 Maven 产物，而是 kit klib 里的资源，由
`extractKitBundle` 解到站点的 `static/ktjs-client/`。

## 发布

发布配置统一在根 [`build.gradle.kts`](build.gradle.kts) 里，对所有模块生效。`.github/workflows/release.yml`
签名、按 Maven 坐标打包并注册到共享的 Maven Central hub，由 hub 合并成一次 deployment。派生项目如果不想
发布，删掉那条工作流和发布块即可，其它部分不依赖它。

## 许可证

[MIT](LICENSE)
