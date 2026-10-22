# AGENTS.md

本文件记录跨会话需要保留的项目约定：构建方式、模块结构、发布流程和容易踩的坑。只与某个目录强相关的
说明放在该目录下的 `AGENTS.md`，这里保留指向它的简短索引。

## 项目概况与模块结构

`__PROJECT__` 为 [`__NPM_PACKAGE__`](https://www.npmjs.com/package/__NPM_PACKAGE__) 提供 Kotlin/JS wrapper。

- `__PREFIX__-__MODULE__`：发布模块，`version = {上游 npm 版本}-kt.{修订号}`。
- `demo`：GitHub Pages 演示站点，不发布、也不进入 Dokka。
- `scaffold/module`：新增模块时复制的骨架，不参与构建。

按「上游发版节奏」而不是「npm 包数量」划分子模块：上游版本号相同、总是一起发版的包放在同一个 Gradle
模块里，避免每个包都产生一个 Maven 版本和一次 Central 发布。终端应用打包时本来就会摇树，模块内多出的
声明不会进入最终产物。

### 新增模块

1. 复制 `scaffold/module` 为 `__PREFIX__-<name>`，替换文件里的所有占位符（包名、npm 包名、项目名）。
2. `settings.gradle.kts` 加 `include(":__PREFIX__-<name>")`。
3. `package.json` 的 `dependencies` 加 npm 包版本；`wrapper-revisions.properties` 加 `__PREFIX__-<name>=1`。
4. `demo/build.gradle.kts` 的 `jsMain.dependencies` 加 `implementation(project(":__PREFIX__-<name>"))`，
   并在 demo 里实际使用它。
5. 全局替换时同步检查 `detekt`、Dependabot 分组、README 模块表与 Dokka（Dokka 按前缀自动收集，无需改）。

模块名、`wrapper-revisions.properties` 的 key、目录名三者必须完全一致，否则配置阶段会失败。

## 浏览器 DOM 类型体系

统一使用 JetBrains `kotlin-browser`（`org.jetbrains.kotlin-wrappers:kotlin-browser`）的 `web.*` 类型，
不要在公开 API 中混用 `org.w3c.dom.*`（Kotlin/JS 自带）或 `kotlinx.browser.*`：

- DOM 元素：`web.html.HTMLElement`
- 全局 document：`web.dom.document`
- 创建元素：`document.createElement(HtmlTagName.xxx)`，无需 `unsafeCast`
- 自定义元素标签名参照 `web.html.HtmlTagName`，为每个 Web Component 提供
  `object XxxTagName { inline val xxx: TagName<XxxElement> }`，把标签名集中在一处

两套类型运行时指向同一个对象，但编译器不视为同一类型；wrapper 的公开 API 一旦用 `org.w3c.dom.*`，
kotlin-browser 项目就无法直接使用。

`build` / `check` 会执行 Detekt 的 `ForbiddenImport` 规则：根脚本为所有子模块应用 `dev.detekt` 并分析
`src`，根目录 `detekt.yml` 禁止 `org.w3c.dom.*` 与 `kotlinx.browser.*`。新增模块无需单独配置。该规则
只拦截 import，代码里直接写全限定名（如 `kotlinx.browser.window`）不会被发现。

Detekt 版本固定为 `2.0.0-alpha.6`：1.23.8 在 JDK 25 下直接失败，而 2.0 alpha 明确支持 Kotlin 2.4 /
Gradle 9 / JDK 25。升级时注意 2.0 的插件 ID 是 `dev.detekt`，规则配置项也已改名
（如 `imports` → `forbiddenImports`）。

## Kotlin/JS 模块格式与测试运行时

- 所有 `js` 目标统一 `useEsModules()`，新增模块沿用，不要退回 CommonJS/UMD。klib 不携带 moduleKind，
  消费方在链接时自行选择格式；这里的选择只影响本仓库的测试产物与 demo bundle。
- `@JsModule` 声明必须按 ESM 语义书写：`@file:JsModule` 的顶层函数/类会生成具名导入，安全；但顶层
  `external val` 配 `@JsName` 会生成 default import，对只有具名导出的包必须写成 `@JsModule` +
  `external object` + 具名属性。
- ESM-only 的 Node 包（`exports` 里没有 `require`/`default` 条件）要导入具体子路径，例如
  `@scope/package/lib/index.js`，不要写包名；子路径对 CommonJS 与 ESM 链接都可用。
- 浏览器测试使用新版测试 DSL：`@OptIn(ExperimentalJsTestDsl::class) browser { test { chromium() } }`，
  由 Playwright 驱动并按需安装浏览器。Karma 已废弃，不要新增 `testTask { useKarma { ... } }`。
- Node 侧的 `jsNodeTest` 同样按 ESM 产物运行。
- 根构建脚本用 `NpmExtension.override("webpack", ...)` 固定一份 webpack：npm workspace 会为
  `source-map-loader` 的 peer dependency 装第二份，测试包只引到其中一份时构建会报
  `The 'compilation' argument must be an instance of Compilation`。

## UI 编程约定

以下为维护者的通用 UI 编码偏好。新增任何 UI 代码（Web Component 包装、演示页）都按此执行。

### 保留模式：对原生 DOM 做面向对象封装

不要引入 React / Vue / Lit 的「状态 → render → 重建 DOM」模型。组件应表现为持有内部 DOM 对象的普通类，
按以下优先级裁决（上面的赢）：

1. 保留 DOM 节点：节点长期存在，不随状态重建。
2. 类/对象字段持有 DOM 引用。
3. 直接修改 DOM：property / attribute / text / class / 事件监听。
4. 只做局部更新。
5. 必要时才替换局部集合。
6. 禁止整组件 re-render。
7. 禁止用响应式 state 驱动 render。
8. 禁止引入 Lit / React / Vue 等 UI runtime 或其设计模式。

### 禁止拼 HTML 字符串

不许 `innerHTML = "..."`、`insertAdjacentHTML`，也不许任何「模板字符串生成标记再塞进 DOM」的写法。
节点用 `document.createElement`（Kotlin/JS 中优先用 kotlin-browser 的类型化 API）逐个构造并组装。

### DOM 用对象操作，字段持有引用

需要反复访问的节点一律存成字段，直接对字段调方法；不要用 `querySelector` 当组件内部的状态管理方式。
`querySelector` 只允许用于「从外部挂载点找目标」这类不属于组件自身结构的场景。

### 更新局部、直接、明确，不设通用刷新机制

状态变化只改相关的那几个节点，用命名的领域 setter 表达（如 `setResult` / `setLoading`），而不是把整个
UI 当成 data 的函数重算。用标准 DOM API 直说：`textContent`、`setAttribute` / `removeAttribute`、
`classList.add/remove/toggle`、`hidden`、`disabled`、`value`、`href` / `src` 赋值等。

禁止实现 `render()` / `rebuild()` / `rerender()` 这类通用刷新入口；禁止因为一个字段变化就重建整个子树；
禁止用「替换子树」模拟响应式更新：

```ts
set data(value) { this.#data = value; this.render(); }        // 禁止
update(data) { this.replaceChildren(); this.render(data); }   // 禁止
```

### `replaceChildren()` 分级使用

它本身不是禁品，禁的是把它当整组件的通用 render 机制：

- 允许：确实需要整体更新的局部集合（一次性静态组装、整批新条目、固定的「图标 + 文本」组合）。
- 禁止：清空组件自己的根子树再重建。

变长列表能增量就增量：按数据长度增删条目、复用已有条目对象，只有条目语义整体失效时才整批替换。

### 内容入口：命名的领域 setter

组件不接受整份 data blob，也不提供通用 `render` / `apply` / `update` 入口。每个组件暴露一组领域 setter，
一个方法只碰它负责的那部分节点：

- setter 只做 mutation；变长集合按新长度增删条目、复用已有对象。
- setter 只写自己持有的节点，因此在元素尚未连接、甚至尚未插入文档时调用也安全。
- 不做 attribute reflection：不把 setter 的值镜像回 attribute。
- 新增字段就加对应 setter，不要回头引入 `setData()` / `update()`。

**例外（attribute 种子）**：由构建期插件生成、没有挂载点可调 setter 的元素，可在 `connectedCallback()`
里从 attribute 读一次作初始种子；这与「读取用于初始化」不冲突，仍属保留模式。手写挂载的元素仍走 setter。

### 生命周期：构造时建树

- 静态结构优先在 `constructor()` 中创建并组装完毕，`connectedCallback()` 只做必要的一次性初始化，
  不要把 `connectedCallback() → rebuild() → build()` 当标准生命周期。
- Custom Elements 规范限制：构造函数里不得给 `this` 加属性或子节点（`this.append(...)`、
  `this.setAttribute(...)` 都不行）；`attachShadow()` 合法，所以结构挂进 shadow root，light DOM 保持为空。
- 监听器按归属决定挂在哪：挂在自有节点上的写在构造函数里，无需清理；挂在 `document` / `window` /
  `matchMedia` 等外部对象上的，必须在 `disconnectedCallback()` 里移除，注册用幂等标志
  （元素移动或框架重挂载会再次触发 `connectedCallback()`）。

### 组件用 class，但不建通用组件基类

一个组件一个类，自带字段、构造时组装、自己的 setter；不做 `create()` / `apply()` 之类的模板方法抽象，
那会把状态驱动刷新重新引进来。非元素的小部件（折叠控件、卡片等）同样是 class；函数式导出只留给真正的
纯工具。

### 默认 Shadow DOM

- 组件默认渲染进 `shadow root`（`mode: 'open'`），样式通过 `adoptedStyleSheets` 或 shadow 内样式注入，
  避免与宿主页面互相污染。
- 主题靠 CSS 自定义属性继承穿过 shadow 边界；这些变量必须声明在文档的 `:root` / `[data-theme]` 上。
  组件内部不要写 `[data-theme]` 选择器，也不要依赖宿主 class。
- 只有继承属性穿得过边界：`box-sizing` 不是继承属性，shadow 内必须在自己的作用域重新声明
  `box-sizing` 重置，不要依赖宿主的 `* { box-sizing: border-box }`。
- 万不得已才退回 light DOM（仅当必须直接复用宿主的 CSS 规则时），类名加项目前缀，新增例外要在评审里
  说明依赖了宿主的哪条规则。混合形态（部分 shadow、部分 light）要划清边界并记录原因。

### 无 document 环境安全

- 可能被 Node（SSR、`jsNodeTest`）加载的模块，类字段初始化器不要碰 `document` / `window` /
  `customElements`；这些入口要么带守卫，要么由调用方在浏览器环境触达。
- `CSSStyleSheet` 等浏览器对象必须惰性创建（在构造函数里），不要在模块级初始化。
- 浏览器专用的重依赖用动态 `import()` 按需加载，业务纯数据模块不要反向依赖重依赖。

### React 19 自定义元素

- 对象 prop 不会在 SSR/hydration 阶段设到自定义元素上，挂载用 callback ref 显式喂内容。
- `mount*()` 助手必须是纯 setter 调用（幂等）：StrictMode 双调用、元素移动后重挂载会重复喂同样的数据，
  重复执行必须无副作用。
- 预渲染 HTML 里自定义元素是空壳是已知取舍；light DOM 恒为空可以顺带消除 hydration mismatch。若控制台
  出现 hydration 报错指向自定义元素，说明有代码把节点挂回了 light DOM，修原因而不是用
  `suppressHydrationWarning` 掩盖。

## 版本约定

每个子模块独立版本，格式为 `{上游 npm 版本}-kt.{修订号}`。上游 npm 版本只在根目录 `package.json` 的
`dependencies` 中声明；每个模块的修订号在根目录 `wrapper-revisions.properties` 中维护。根
`build.gradle.kts` 解析这两份数据，分别放入 `extra["npmVersions"]` 与 `extra["wrapperRevisions"]`，
各子模块读取后声明 `npm(...)` 并推导版本。禁止在模块脚本里硬编码 npm 版本或 `kt.N`。

- 只发布带 `-kt.N` 后缀的版本，不存在不带后缀的版本。
- 每个模块的修订号独立递增，永不重置为 `kt.1`。它表示该模块的第 N 次 wrapper 发布，与上游 npm 版本
  各自独立演进。
- wrapper 代码或该模块的已发布元数据变化时，只把 `wrapper-revisions.properties` 中对应模块的修订号加一；
  其他模块保持原版本，发布时命令里不列它们，因此不会为它们产生新版本、新文件。
- 仅上游 npm 版本变化、wrapper 代码没变时，修订号保持不变；Dependabot 改 `package.json` 后，受影响模块的
  基础版本自动跟随（如 `3.0.4-kt.3` → `3.0.5-kt.3`），该模块作为新版本发布。
- 模块间的 `api(project(...))` 会解析到依赖模块当前的版本。一次提交同时变更多个模块时，必须在同一条发布
  命令里上传所有缺失版本；不要分多次只发布其中一个，否则 POM 可能指向尚未上传的依赖版本。
- 新增发布模块时，在 `wrapper-revisions.properties` 里从 `1` 开始加一行；缺少条目会在配置阶段直接失败。
- 消费方不必为了上游补丁等 wrapper 发版：在根构建脚本里用 `NpmExtension.override(...)` 覆盖 klib 记录的
  npm 版本即可（根 `overrides` 对 workspace 内所有包生效）。不要用第二个 `npm(...)` 声明去改版本：它只改
  声明它的模块的 `package.json`，其他模块仍固定 wrapper 的版本，npm 可能装出两份。
- `override` 改完必须重建锁文件：Gradle 用的是 npm 11，`overrides` 变化不会让旧锁失效，`npm install` 会
  认为 `up to date` 并继续装旧版本。删掉 `kotlin-js-store/package-lock.json` 后跑
  `./gradlew kotlinNpmInstall`，再用 `./gradlew kotlinUpgradePackageLock` 把新锁写回。
- `-kt.N` 在 SemVer 中是预发布标识，优先级低于同基础版本的无后缀版本；因为永不发布无后缀版本，发布序列
  内部不会冲突。

## 仓库与本地镜像

仓库里提交的构建配置只使用官方源：`settings.gradle.kts` 用 Gradle Plugin Portal 与 Maven Central，
`gradle/wrapper/gradle-wrapper.properties` 用 `services.gradle.org`。这样 GitHub Actions 和任何克隆仓库的
人行为一致。

国内加速只在本机生效，通过 `~/.gradle/init.d/aliyun-mirror.gradle` 注入阿里云镜像，不提交到仓库；不要在
`settings.gradle.kts` 或 wrapper 配置里写镜像地址。用 Groovy 而不是 `.gradle.kts`：IDEA 同步时会对
`init.d` 下的 Kotlin DSL 脚本做独立编译，`beforeSettings` 等隐式接收者解析不了，会报一堆
`Unresolved reference`（命令行构建却正常）。

`kotlin-wrappers` 同步滞后，且有时只有 POM 没有 klib，而 Gradle 一旦在某个仓库找到元数据就不会回退到
下一个仓库。`settings.gradle.kts` 因此用 `exclusiveContent` 把 `org.jetbrains.kotlin-wrappers` 和
`kotlin-js-plain-objects` 固定到 Maven Central；新增 wrapper 依赖时如遇 `Could not find ... .klib`，把
对应 group/module 加进去。

## CI 与依赖更新

- `.github/workflows/ci.yml` 只在**所有 PR** 和手动触发时运行 `./gradlew build`（JDK 21，Ubuntu），
  **不被 push 到 master 触发**：能进 `master` 的提交已经在 PR 上跑过同一套构建。需要验证 `master`
  现状时用 `gh workflow run ci.yml`。
- `.github/workflows/dependabot-auto-merge.yml` 在 CI 成功后自动 squash 合并 Dependabot 的 PR。它用
  `workflow_run` 而不是直接由 Dependabot PR 触发，因为后者拿到的 `GITHUB_TOKEN` 只读。
- `.github/dependabot.yml` 覆盖 `gradle`、`npm`（根目录 `package.json`）和 `github-actions` 三个生态。
  npm 版本由 Dependabot 改 `package.json`，Gradle 自动读取，不再手工改 `npm(...)`。
- Gradle wrapper 固定在 `9.4.1`，并在 Dependabot 里忽略 `gradle-wrapper`：IntelliJ IDEA 2026.2.3
  同步 Gradle 9.8.x 项目时会报 `ExternalSystemPartialResolutionException`（消息为空），回退到 9.4.1
  即恢复正常。等 IDEA 修复后再删除这条 ignore 并升级。
- Dependabot 的 npm PR 不会带 Gradle 生成的锁文件更新，所以 CI 对 Dependabot PR 先跑
  `kotlinUpgradePackageLock` 再构建，并把校验过的锁文件作为 `npm-lock` artifact 上传；自动合并 job
  下载该 artifact、提交到 PR 分支，然后 squash 合并。普通 PR 仍然严格校验锁文件。
- `kotlin-js-store` 里只有 Gradle 生成的锁文件，没有也不应该有 `package.json`；根目录的
  `package.json` 才是版本清单。
- 本地增量构建后，`jsPackageJson` / `rootPackageJson` 可能残留旧输出（模块版本变成
  `0.0.0-unspecified`、漏掉 `packages_imported/*`），此时 CI 会报 `Lock file was changed`。执行
  `./gradlew kotlinUpgradePackageLock --rerun-tasks` 重新生成锁文件并提交。
- 提升 `wrapper-revisions.properties` 的修订号也会改生成的 workspace 包版本，必须同时跑
  `./gradlew kotlinUpgradePackageLock` 并提交 `kotlin-js-store/package-lock.json`，否则 CI 在
  `:kotlinStorePackageLock` 报 `Lock file was changed`。

## 发布到 Maven Central

发布配置统一在根 `build.gradle.kts` 的 `subprojects {}` 里，使用 `com.vanniktech.maven.publish`，
group 固定为 `io.github.__OWNER__`。`subprojects` 的 group 必须单独设置，根项目的 `group` 不会自动继承。

发布是两段式，Central 只在第二段被触及：

1. 本仓库 `.github/workflows/release.yml`（只有 `workflow_dispatch`）：解析版本 → 用仓库里的签名密钥
   签名 → 发布到模块内的 `build/hub-staging` 文件仓库 → `scripts/hub-release.py` 为**每个 Maven 坐标**
   打一个 `.tar.xz` 并生成注册 → 创建/更新 GitHub Release 并上传归档 → 把注册推到 hub → 触发 hub 的
   `upload=false` dry run 并等待它结束。
2. hub（`__OWNER__/maven-central-publishing`）手动 dispatch 且勾选 `upload=true`，才真正上传 Central，
   并在成功后删除已满足的注册。

```bash
gh workflow run release.yml -f release=release-1 \
  -f modules=__PREFIX__-__MODULE__
```

- `release`：`release-<数字>`，tag 由工作流创建；工作流不会移动已有 tag。
- `modules`：逗号分隔；留空表示全部发布模块。只列本次修订号变化的模块。
- `register`（默认 true）：把注册推到 hub；关掉则只做构建和 GitHub Release。
- `verify`（默认 true）：触发 hub 的 dry run 并等待；关掉则到 Release 为止。

重跑同一 `release`：已存在的 Release 用 `gh release upload --clobber` 覆盖同名归档，注册按 sha256 覆盖
旧文件。归档时间戳固定为 1980-01-01、条目顺序固定，但 Gradle 产出的 klib/jar/module 本身不是逐字节可
复现的，所以重跑会得到新的 sha256 并改写注册；覆盖是幂等的，不要把归档 sha256 当成构建输入的指纹。

版本仍完全由 `package.json` 与 `wrapper-revisions.properties` 推导，工作流不重复 `-kt.N` 规则：
`./gradlew -q printPublishedVersions` 输出 `模块=版本`，只把 `modules` 选中的模块传给
`hub-release.py`。

### 一次性准备

1. 在 [Central Portal](https://central.sonatype.com/) 注册账号并验证 `io.github.__OWNER__` namespace
   （GitHub 方式）。
2. 生成 GPG 密钥并发布公钥到 `keys.openpgp.org` / `keyserver.ubuntu.com`。
3. 在本仓库 Settings → Secrets 配置 `MAVEN_CENTRAL_SIGNING_KEY`、`MAVEN_CENTRAL_SIGNING_KEY_PASSWORD`
   与 `HUB_RELEASE_TOKEN`（fine-grained PAT：hub 的 Contents + Actions 读写）。Central User Token 配在
   hub 仓库，不放在本仓库。
4. **若沿用 hub：新增 KMP 模块（产生新的 `*-js` 坐标）时，先在 hub 的 `artifactOverrides` 里补
   `jar: false` 并关闭 `verifyPom`**。JReleaser 的 PomChecker 不认 klib packaging，且它把 pom/aar/war/zip
   以外的 packaging 都当成必须有同名 `.jar`；漏配会让 hub 整批失败。
5. 第一次发布用 `release-1`，`modules` 留空（发布全部模块）；之后只列修订号变化的模块。

### 本地预演

```bash
ORG_GRADLE_PROJECT_signingInMemoryKey="$(< ~/central-signing-key.txt)" \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=... \
./gradlew :__PREFIX__-__MODULE__:publishAllPublicationsToHubStagingRepository

python3 scripts/hub-release.py --project __OWNER__/__REPO__ --release release-1 \
  --module __PREFIX__-__MODULE__=<version> --repository . \
  --archives build/hub-release/archives --registrations build/hub-release
```

每个归档只含一个 Maven 坐标、含 `.asc`、不含 `.md5` / `.sha1`。还可以在 hub 仓库执行
`python3 scripts/collect.py --local <本仓库>/build/hub-release` 再 `python3 scripts/stage.py` 做离线预检。

### 发布额度

Central 社区版按月限制 Release Count、File Count 与 Release Size：

- **Release Count 按 deployment 计**，不按模块或坐标计。所有待发布构件必须在同一次上传里；不要按模块
  循环调用 `publishToMavenCentral`，那会把 1 次 deployment 拆成 N 次。
- **只发变更模块**：未提升修订号的模块不要列进发布命令，减少 File Count、Release Size 和无意义的版本号。
- **javadoc 空包**：`configureBasedOnAppliedPlugins(javadocJar = JavadocJar.Empty())`，API 文档由 Pages
  统一部署，不在每个构件里重复 Dokka 的静态资源。
- **不传签名校验文件**：`excludeSignatureChecksums()` 保证不会为 `.asc` 上传 `.md5` / `.sha1`。

### klibs.io 自动发现

klibs.io 从 Maven Central 与 GitHub 自动抓取，无需提交元数据文件。条件：项目开源且托管在 GitHub；至少
一个构件发布到 Maven Central 且已同步到 Google 的镜像；至少一个构件是 KMP 并发布
`kotlin-tooling-metadata.json`；POM 的 `url` 或 `scm.url` 是有效的 GitHub 仓库链接。改仓库地址时要同步
修改根 `build.gradle.kts` 的 POM 配置。

## 演示页面与 GitHub Pages

- `demo` 只消费公共 API，不发布、也不进入 Dokka。根 `build.gradle.kts` 的发布与 Dokka 配置按
  `__PREFIX__-` 前缀过滤子模块，新增非发布模块时沿用该约定。
- 端到端测试是 `demo/e2e/demo-page.test.mjs`：Playwright 驱动打包后的
  `demo/build/dist/js/productionExecutable`，脚本自带静态服务器。运行 `./gradlew :demo:e2eTest`；
  它不在 `check` 里，CI 在 `pagesSite` 之后单独执行。Playwright 与 Chromium 复用 Kotlin/JS npm
  workspace（`build/js`）和第一个 wrapper 模块的 `kotlinInstallPlaywrightBrowsers`，脚本用 PATH 上的
  `node` 启动。
- `./gradlew pagesSite` 把 demo 与 `build/dokka/html` 组装到 `build/pages`：演示页在根目录，API 文档在
  `api/`。演示页内部链接使用相对路径。
- Dokka 2.2.0 同时应用到根项目和所有 wrapper 子模块；聚合入口是根项目的 `dependencies { dokka(...) }`，
  任务为 `dokkaGeneratePublicationHtml`（2.x 已移除旧的 `dokkaHtmlMultiModule`）。
- `.github/workflows/pages.yml` 只有 `workflow_dispatch`：站点更新在内容值得上线后手动执行
  `gh workflow run pages.yml`（或 Actions 页面点 Run workflow）。它每次都重新跑 `pagesSite`，所以线上
  内容始终对应触发时 `master` 的提交。仓库 Settings → Pages 的 Source 必须选 **GitHub Actions**。
- 本地预览：执行 `./gradlew pagesSite` 后给 `build/pages` 起任意静态服务器。直接开 `file://` 会因相对
  路径与 CORS 导致资源加载异常。

## 文档约定

- 需要跨会话、跨上下文感知的信息（环境约定、构建方式、模块结构、容易踩的坑）记录在本文件。
- 只与某个目录强相关、篇幅较长的说明，放在该目录下的 `AGENTS.md`，根文件只保留指向它的简短索引。
- 公开声明都要有 KDoc：上游源码或 README 有说明的，翻译并改写过来；没有的按实际行为补充。描述以源码
  为准，不要照搬过时或错误的理解。
- 每个模块维护 `README.md` 与 `README.zh-CN.md`；根 README 的模块表、版本与 Maven 徽章随发布更新。

## 模板来源与升级

本仓库由 [`shijianjs/ktjs-wrapper-template`](https://github.com/shijianjs/ktjs-wrapper-template) 生成。
模板只提供构建、发布与文档骨架；领域约定写在本文件，不要回灌模板。升级模板时从模板仓库 `git diff`
取变更后人工 cherry-pick，不要用项目仓库反向覆盖模板。
