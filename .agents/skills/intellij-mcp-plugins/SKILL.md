---
name: intellij-mcp-plugins
description: 用脚本或命令行调用 IntelliJ IDEA 的 MCP server（官方内置与 IDE Index MCP 插件），查类型继承树（父类链 + 子实现）、实现列表与覆盖方法；含通用 streamable-HTTP 客户端与继承树渲染脚本。
---

# IntelliJ MCP 脚本化查询

## 适用场景

- 从命令行或脚本查 IDEA 索引（符号、引用、继承、实现），不依赖 agent 客户端内置的 MCP 工具。
- 要 F4 的等价物：某个类型向上有哪些父类型、向下有哪些子类型，Java / Kotlin/JVM / Kotlin/JS 都要能查。
- 在 CI 或 agent 流程里做批量语义查询。

## 背景

- IDEA 有两个 MCP server：官方内置（稳定，工作流类工具）与 IDE Index MCP 插件（语义索引 / 层级 / 重构，
  迭代最快），端口不同，可以同时挂。
- 插件的层级三件套 `ide_type_hierarchy` / `ide_find_implementations` / `ide_find_super_methods`
  从 **5.20.4** 起对 Kotlin/JS（klib）可用：修复见
  <https://github.com/hechtcarmel/jetbrains-index-mcp-plugin/issues/456>（PR #457）。
  2026-11-12 在 IntelliJ 2026.3 + 插件 5.20.4 上实测：`CustomElementAdapter : HTMLElement()`
  一次给出 klib 里的父链（`HTMLElement` → `Element` → `Node` …，路径形如
  `kotlin-browser-js-2026.11.0.klib!/default/linkdata/...`）和 9 个子类型；`AttributeCodec` 的 5 个实现、
  `KtjsHero.connectedCallback` 的父方法也都有结果。
- 5.20.4 之前不要自己拼 `ide_symbol_info` + `ide_find_symbol` + `ide_find_references` 复刻继承树：
  那套在 klib 上既不完整又慢。先升级插件。
- `scope` 是最容易踩的坑：默认 `project_files` 会把库里的父类型过滤掉，Kotlin/JVM 与 Kotlin/JS 上都会
  看起来像「没有父类型」。查继承一律带 `project_and_libraries`。
- 反过来，能用语义工具就别用文本搜索；但工具的 `language` 枚举（Java / TypeScript / JavaScript / Python）
  还停在旧口径，`target: {qualifiedName, language: "Kotlin"}` 会被拒——Kotlin 用**顶层** `className`
  传全限定名，不要带 language。

## 前置条件

- IDE 已打开目标项目；多项目或 workspace 下调用必须带 `project_path`。
- 插件版本 ≥ 5.20.4。核对方式：
  `Get-ChildItem "$env:APPDATA\JetBrains\IntelliJIdea*\plugins\jetbrains-index-mcp-plugin\lib" -Filter *.jar`
  （文件名里带版本号）。
- Index MCP 插件默认地址：`http://127.0.0.1:29170/index-mcp/streamable-http`。
- 官方内置 server 的 URL 随 IDE 版本变化（常见 63342 / 64342），用 `--url` 覆盖；它还需要 header
  `IJ_MCP_SERVER_PROJECT_PATH`。
- 默认禁用的工具需在 `Settings → Tools → Index MCP Server → Exposed Tools` 打开，例如
  `ide_symbol_info`、`ide_read_file`、`ide_find_symbol`。
- `project_path` 要与 IDE 记录的大小写一致（Windows 上盘符是大写）：传 `s:/repo` 会得到
  `project_not_found` 并列出 `available_projects`，`S:/repo` 才对。`os.path.abspath` 的结果可直接用。

## scripts/mcp_client.py

通用 streamable-HTTP 客户端，`list` / `schema` / `call` 三个子命令：

```bash
# 列出工具
python scripts/mcp_client.py list --project /path/to/repo

# 查看工具参数
python scripts/mcp_client.py schema ide_type_hierarchy ide_find_symbol

# 调用任意工具
python scripts/mcp_client.py call ide_find_symbol '{"query":"Repository","language":"Kotlin"}' \
  --project /path/to/repo

# 连官方内置 server（需要 header）
python scripts/mcp_client.py --url http://127.0.0.1:64342/stream \
  --header IJ_MCP_SERVER_PROJECT_PATH=/path/to/repo list
```

`--project` 会作为 `project_path` 注入每次 `tools/call`；也可用环境变量 `IDE_MCP_PROJECT`、
`IDE_MCP_URL`、`IDE_MCP_TIMEOUT`。

## scripts/hierarchy.py

`ide_type_hierarchy` 的薄封装：调工具 + 翻页合并 + 渲染成树，只补充工具本身没有的两件事——
按方向限深（工具只给整棵 BFS 闭包）和把 `files-2.1/<group>/<artifact>/<version>/<sha1>/` 压成
`artifact:version/file`。

```bash
# 位置形式，1-based，column 落在类型名首字母
python scripts/hierarchy.py ktjs-dom-kit/src/jsMain/kotlin/Store.kt 42 7 --project S:/repo

# 全限定名（Kotlin 也走 className，不要加 language）
python scripts/hierarchy.py --class io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter --project S:/repo

# 只看一层：直接父类型 + 直接子类型
python scripts/hierarchy.py --class io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter --up 1 --down 1
```

- `--up 0` / `--down 0` 关掉某个方向；`--scope` 默认 `project_and_libraries`，可选
  `project_files` / `project_production_files` / `project_test_files`。
- `--symbol sym_xxx` 复用上次调用拿到的 symbolId，省一次解析。
- 工具单页 `maxNodes` 上限 500，脚本自动跟 `cursor` 翻页；`hasMore` 却拿不到 cursor 时报错退出。

输出示例（`--up 3 --down 1`，实测节选）：

```text
io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter  (ktjs-dom-kit/src/jsMain/kotlin/…/CustomElementAdapter.kt)  [Kotlin CLASS]

UP (supertypes):
  web.html.HTMLElement  @ kotlin-browser-js:2026.11.0/kotlin-browser-js-2026.11.0.klib!/default/linkdata/package_web.html/027_html.knm
    web.dom.Element  @ kotlin-browser-js:2026.11.0/kotlin-browser-js-2026.11.0-sources.jar!/webMain/web/dom/Element.kt

DOWN (subtypes):
  io.github.shijianjs.ktjs.docusaurus.kit.browser.KtjsAction  @ …/KtjsAction.kt
  io.github.shijianjs.ktjs.docusaurus.kit.browser.KtjsHero  @ …/KtjsHero.kt
```

## 直接调 MCP 工具时

- 选目标有三种形态，`className` / `symbolId` / `file`+`line`+`column` 是**顶层**参数；
  嵌进 `target` 只接受 `symbolId` 与 `position`。
- `scope` 用 `project_and_libraries`，否则父类型为空（见上）。
- 返回是平铺的 BFS 列表（`nodeId` / `parentId` / `depth`），`maxNodes` 取 1–500，`hasMore` + `cursor`
  继续；`hasMore` 为真但没有 cursor 说明被截断，缩小查询范围再问。
- `ide_find_implementations` 对 Kotlin 匿名 object 的 `name` 是 `unknown`，按 `file:line` 定位即可。

## 故障排查

- `-32603 Could not initialize class com.jetbrains.python.parsing.FunctionParsing`：与本插件无关，是 IDE 侧
  Python 解析初始化失败（本机 2026-11-12 记到的根因是 Python 插件
  `IElementType TooManyElementTypesException: Out of (short) range`，由 Kubernetes 插件索引触发）。
  一旦发生，之后**所有**索引类工具调用都会连环失败，只能重启 IDE（`ide_restart` 或手动）。
- `project_not_found`：路径大小写不一致，或项目没在 IDEA 里打开。
- Kotlin/JS 报 `not found` / `No class/type found`：插件版本低于 5.20.4。
- 两个 server 都监听 `127.0.0.1` 且无鉴权，共享机器上注意。

## 禁止事项

- 不要用 `grep` / 文本搜索替代语义查询。
- 不要再用 `ide_symbol_info` + `ide_find_symbol` + `ide_find_references` 手工拼继承树。
- 不要把「工具列表里有 `ide_type_hierarchy`」当成「它对某语言可用」；也别拿 `ide_symbol_info` 的
  `signatureSource`（Kotlin 是 `element_text`）当判据——直接查一次看结果。
