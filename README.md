# ktjs-wrapper-template

脚手架模板：用 Kotlin/JS 为 npm 包（尤其是 Web Component 生态，如 Vaadin）生成一套可构建、可测试、
可发布到 Maven Central 的多模块项目。

模板只提供通用基础设施：Gradle 多模块配置、Detekt 规则、npm 版本与 wrapper 修订号机制、
Playwright 浏览器测试、demo + GitHub Pages、Maven Central 发布流水线与文档骨架。领域内容（被包装的
API、示例页面、README 正文）由生成后的项目自己写。

## 快速开始

1. 复制仓库（或在 GitHub 上 **Use this template**）：

   ```bash
   git clone https://github.com/shijianjs/ktjs-wrapper-template.git ktjs-vaadin
   cd ktjs-vaadin
   ```

2. 运行初始化脚本，替换全部占位符并生成第一个模块：

   ```bash
   python3 scripts/init-wrapper-project.py \
     --prefix ktjs-vaadin \
     --owner shijianjs \
     --repo ktjs-vaadin \
     --package vaadin \
     --module core \
     --npm-package @vaadin/web-components \
     --npm-version 25.0.0
   ```

3. 生成锁文件并验证：

   ```bash
   ./gradlew kotlinNpmInstall
   ./gradlew kotlinUpgradePackageLock
   ./gradlew build
   ./gradlew pagesSite
   ./gradlew :demo:e2eTest
   ```

4. 按新项目重写 `demo`、各模块 README、根 README 模块表与 `AGENTS.md` 里的项目描述，然后提交。
   初始化脚本会删除自己，重复运行会被拒绝。

## 参数与占位符

| 参数 | 占位符 | 说明 |
| --- | --- | --- |
| `--prefix` | `__PREFIX__` | Gradle 模块前缀，例如 `ktjs-vaadin`；模块名为 `<prefix>-<module>` |
| `--owner` | `__OWNER__` | GitHub 用户/组织；group 为 `io.github.<owner>` |
| `--repo` | `__REPO__` | 仓库名，默认与 `--prefix` 相同 |
| `--package` | `__PACKAGE__` | Kotlin 包名，例如 `vaadin`；源码目录会同步重命名 |
| `--module` | `__MODULE__` | 第一个模块的后缀，例如 `core` |
| `--npm-package` | `__NPM_PACKAGE__` | 被包装的 npm 包名（scoped 包写全名） |
| `--npm-version` | `__NPM_VERSION__` | 上游 npm 版本，作为 `{版本}-kt.1` 的基础 |
| — | `__PROJECT__` | 项目显示名，默认等于 `--repo` |
| — | `__YEAR__` | 当前年份，用于 LICENSE |

`__MODULE__` 只用于 Gradle 模块名（`<prefix>-<module>`）与 `wrapper-revisions.properties` 的条目；
Kotlin 包名完全由 `--package` 决定，新增模块时自行选择子包并替换 `scaffold/module` 里的占位符。

## 脚本做了什么

- 遍历仓库文本文件替换占位符（跳过 `.git`、`build`、`node_modules` 与脚本自身）；
- 把 `scaffold/module` 复制为 `<prefix>-<module>`，并重命名 `src/**/kotlin/<package>` 目录；
- 在 `settings.gradle.kts` 追加 `include(":<prefix>-<module>")`；
- 在 `demo/build.gradle.kts` 追加该模块的 `implementation(project(...))`；
- 用 `scaffold/project/README*.md` 生成项目 README，然后删除模板说明与初始化脚本；
- 保留 `scaffold/module`，供以后新增模块时复制（做法见 `AGENTS.md`）。

## 初始化之后还需要手动做的事

- 重写 demo：示例目前只有一个标题，需要真正演示被包装的 API。
- 补 KDoc、模块 README（中英文）和测试：脚手架里只有一个占位符自检测试。
- 仓库 Settings：Secrets（签名密钥、`HUB_RELEASE_TOKEN`）、Pages Source 选 GitHub Actions。
- Central Portal 验证 `io.github.<owner>` namespace；第一次发布用 `release-1`。
- 若沿用 Maven Central hub：新增 `*-js` 坐标时先在 hub 的 `artifactOverrides` 里补 `jar: false`
  并关闭 `verifyPom`，否则整批失败。详见生成后的 `AGENTS.md`。

## 目录结构

```text
.
├── build.gradle.kts              # 插件、版本推导、发布、Dokka、Pages
├── settings.gradle.kts
├── package.json                  # npm 版本唯一来源
├── wrapper-revisions.properties  # 每个发布模块的 -kt.N 修订号
├── detekt.yml                    # 禁止 org.w3c.dom.* / kotlinx.browser.*
├── scripts/
│   ├── hub-release.py            # 归档 + hub 注册
│   └── init-wrapper-project.py   # 模板初始化（运行后删除）
├── scaffold/
│   ├── module/                   # 模块骨架，新增模块时复制
│   └── project/                  # 生成后的项目 README
├── demo/                         # Pages 演示页 + Playwright e2e
└── .github/workflows/            # CI、Pages、Release、Dependabot auto-merge
```

## 维护模板

- 模板里只放与具体上游无关的内容；某个包的用法、组件约定写进生成后的项目 `AGENTS.md`。
- 修改后在一个临时目录完整跑一次初始化 + `./gradlew build`，确认占位符没有遗漏。
- 生成后的项目从模板升级时用 `git diff` 人工 cherry-pick，不要把项目仓库反向覆盖模板。

## 环境要求

- JDK 17+（构建使用 Gradle 9.4.1）
- Python 3（初始化脚本与发布脚本只用标准库）
- Node.js 由 Kotlin Gradle 插件自动下载；Playwright 首次运行时下载 Chromium

## 许可证

[MIT](LICENSE)
