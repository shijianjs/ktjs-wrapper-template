---
sidebar_position: 1
---

# 快速开始

## 环境要求

- JDK 21 或更高；Gradle wrapper 已入库（9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件下载并驱动——没有需要手动执行的 `npm install`，`kotlin.js.yarn=false`
  保证包管理器是 npm。
- 第一次跑浏览器测试时会下载 Chromium（Playwright）。

## 构建、测试、发布

```bash
./gradlew build                    # 编译、Detekt、浏览器测试
./gradlew :ktjs-wrapper-template-core:jsBrowserTest
./gradlew :demo:e2eTest            # 用 Playwright 打打包后的 demo 页面
./gradlew :docs:docusaurusBuild    # 文档站
./gradlew pagesSite                # 根目录是文档，demo 在 /demo/，Dokka 在 /api/
```

`build` 不含 demo 的端到端套件，也不组装 Pages 站点：两者都要浏览器，在 CI 里是单独的步骤。

## 开发

```bash
./gradlew :docs:docusaurusStart    # 文档站的开发服务器
./gradlew :demo:jsBrowserRun       # demo 页面的开发服务器
```

Docusaurus 监听站点目录，改 Markdown 即时刷新；改 Kotlin 要先 `./gradlew :docs:docusaurusSite`，因为
站点 import 的是编译产物。

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `package.json` | npm 版本的唯一来源（Dependabot 维护） |
| `wrapper-revisions.properties` | 每个模块的 `-kt.N` 修订号 |
| `ktjs-wrapper-template-core/src/jsMain/kotlin` | wrapper 声明 |
| `ktjs-wrapper-template-core/src/jsTest/kotlin` | 对应的浏览器测试 |
| `demo/src/jsMain` | demo 页面与它的 Kotlin 入口 |
| `docs/src/jsMain/kotlin` | 站点配置与侧边栏 |
| `docs/src/jsMain/resources` | Markdown 页面、MDX 首页、样式、译文 |

## 锁文件

整个 workspace 的 npm 依赖只解析一次，钉在 `kotlin-js-store/package-lock.json`。新增或升级 npm 依赖、
或者改了模块版本之后要重新生成：

```bash
./gradlew kotlinUpgradePackageLock
```

CI 会校验提交的锁文件，忘了重新生成会直接构建失败，而不是悄悄漂移。
