# __PROJECT__

[`__NPM_PACKAGE__`](https://www.npmjs.com/package/__NPM_PACKAGE__) 生态的 Kotlin/JS wrapper。

[English](README.md) | [简体中文](README.zh-CN.md)

**[在线演示](https://__OWNER__.github.io/__REPO__/)** ·
**[API 文档](https://__OWNER__.github.io/__REPO__/api/)**

仓库按 npm 包（或一组共享上游版本与发版节奏的包）划分 `__PREFIX__-*` Gradle 模块。终端应用打包时
本来就会摇树，把相关包合并到一个模块，比每个包各建一个模块更省发布额度和版本噪音。

## 模块

| 模块 | npm 包 | wrapper 版本 | Maven Central |
| --- | --- | --- | --- |
| [__PREFIX__-__MODULE__](__PREFIX__-__MODULE__/README.zh-CN.md) | `__NPM_PACKAGE__` | `__NPM_VERSION__-kt.1` | [![Maven Central](https://img.shields.io/maven-central/v/io.github.__OWNER__/__PREFIX__-__MODULE__?label=Maven%20Central&logo=apachemaven&logoColor=white)](https://central.sonatype.com/artifact/io.github.__OWNER__/__PREFIX__-__MODULE__) |

## 版本约定

每个模块的版本为 `{上游 npm 版本}-kt.{修订号}`。上游 npm 版本只在根 [`package.json`](package.json)
中声明；每个模块的修订号在 [`wrapper-revisions.properties`](wrapper-revisions.properties) 中维护。
Gradle 从这两份文件推导基础版本和 `npm(...)` 依赖：

- 只发布带 `-kt.N` 后缀的版本，不存在不带后缀的版本。
- 上游升级：改 `package.json` 里的版本，基础版本自动跟随，修订号保持不变。
- 仅 wrapper 修复：只把对应模块的修订号加一（`kt.2`、`kt.3`……）。
- 修订号按模块维护，只增不减，永不重置为 `kt.1`。发布时只发修订号变化的模块。

## 环境要求

- JDK 17 或更高（构建使用 Gradle 9.4.1）。
- Node.js 由 Kotlin Gradle 插件自动下载；`gradle.properties` 关闭了 Yarn，统一用 npm。
- 首次跑浏览器测试时由 Playwright 下载 Chromium。
- demo 的端到端测试是独立的 Node 脚本，因此 `PATH` 上还要有 `node`。

## 构建与测试

```bash
./gradlew build

# 单个模块的测试
./gradlew :__PREFIX__-__MODULE__:jsBrowserTest

# demo 打包页面的端到端检查（Playwright，需要 PATH 上有 node）
./gradlew :demo:e2eTest

# GitHub Pages 站点（根目录是 demo，API 文档在 /api/）
./gradlew pagesSite
```

新增或升级模块的 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```

## 安装

发布到 Maven Central，group 为 `io.github.__OWNER__`。把版本换成需要的那一个
（`{上游版本}-kt.{修订号}`）：

```kotlin
dependencies {
    implementation("io.github.__OWNER__:__PREFIX__-__MODULE__:__NPM_VERSION__-kt.1")
}
```

POM 的 `url` 与 `scm.url` 指向本仓库，每个模块都是 Kotlin Multiplatform 项目并发布
`kotlin-tooling-metadata.json`。这是 [klibs.io](https://klibs.io/) 自动收录的条件。

## 发布

发布配置统一在根 [`build.gradle.kts`](build.gradle.kts) 中。发布由
`.github/workflows/release.yml` 执行：签名构件、为每个 Maven 坐标打一个归档，并注册到共享的
Maven Central hub；hub 之后把待发布坐标合并成一次 Central deployment。一次性配置见
[AGENTS.md](AGENTS.md) 的发布章节。

## 许可证

以 [MIT License](LICENSE) 发布。
