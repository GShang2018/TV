# AGENTS.md

> 本文件面向在此仓库上继续开发的 AI 智能体（Agent）。开始任何改动前请先阅读本文件与 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 项目概览

本项目是基于 [CatVod](https://github.com/CatVodTVOfficial/CatVodTVJarLoader) 的 Android 影视聚合播放器，同时支持 **Android TV（leanback）** 与 **手机（mobile）** 两套 UI。

- **包名**：`com.fongmi.android.tv`
- **核心能力**：点播（VOD）、直播（Live）、多播放内核（系统/IJK/ExoPlayer）、多解析引擎（嗅探/JSON/磁力/TVBus/Youtube/ZLive 等）、内置本地 HTTP 服务器（NanoHTTPD，默认端口 9978）
- **爬虫**：统一 `Spider` 抽象，支持 Java JAR（DexClassLoader）、JavaScript（QuickJS）、Python（Chaquopy）三种实现

## 构建与运行

### 环境要求

- **JDK 17**（本机路径见 [build.bat](build.bat)：`C:\Users\12209\.jdks\jdk-17.0.15+6`）
- Android SDK，`compileSdk 34`、`minSdk 21`（README 中写 minSdk 24，以 [app/build.gradle](app/build.gradle) 的 `minSdk 21` 为准）
- 源码用 Java 11（`sourceCompatibility/targetCompatibility = VERSION_11`）

### 构建命令

在仓库根目录，PowerShell 下执行：

```powershell
# 完整发布构建（等价于 build.bat 中的流程）
.\gradlew clean --no-daemon
.\gradlew assembleRelease --no-daemon
```

- 也可使用 [build.bat](build.bat)（会自动设置 `JAVA_HOME`）。
- 产出的 APK 命名规则：`{mode}-{api}-{abi}.apk`，例如 `mobile-java-arm64_v8a.apk`（见 [app/build.gradle](app/build.gradle) 的 `outputFileName`）。
- `release` 构建完成后会自动递增 [version.properties](version.properties) 中的 `VERSION_CODE` 与 `VERSION_PATCH`。

> ⚠️ **重要**：本仓库存在约定——**未经用户明确同意，禁止执行编译/构建命令**（见 [.roo/skills/comp/SKILL.md](.roo/skills/comp/SKILL.md)）。修改代码后先向用户确认是否需要编译。

## 模块结构

Gradle 多模块，根项目名 `TV`，见 [settings.gradle](settings.gradle)：

| 模块 | 职责 |
|------|------|
| `app` | 主应用：UI、配置、播放、本地服务器、Room 数据库 |
| `catvod` | 爬虫抽象基类、网络层（OkHttp）、通用工具 |
| `quickjs` | QuickJS 运行 JS 爬虫脚本 |
| `pyramid` | Chaquopy 运行 Python 爬虫 |
| `btengine` | libtorrent4j 磁力引擎 |
| `thunder` | 迅雷下载 SDK（边下边播） |
| `tvbus` / `zlive` | 直播引擎 |
| `youtube` / `jianpian` / `forcetech` | 特定源解析 |
| `ijkplayer` | IJK 播放内核（含 ffmpeg so） |
| `hook` | 运行时 Hook（如 TVBus 鉴权） |

### Flavor 三维度

`app` 模块通过 `flavorDimensions = ["mode", "api", "abi"]` 划分三个维度：

- **mode**：`leanback`（电视版）、`mobile`（手机版）
- **api**：`java`、`python`（`python` 会额外引入 `:pyramid` 模块）
- **abi**：`x86`、`arm64_v8a`、`armeabi_v7a`

源码目录对应关系：

- `app/src/main/` — 两套 UI 共享的核心逻辑
- `app/src/leanback/` — 电视版 UI（遥控器、Leanback、Presenter 体系）
- `app/src/mobile/` — 手机版 UI（触屏、RecyclerView + Fragment）

> ⚠️ **常见坑**：对 UI 的改动要确认是 `mobile` 还是 `leanback`，两套 UI 是独立实现、不共享布局文件。只改一处会导致另一端看不到效果。

## 核心包速查

根包 `com.fongmi.android.tv`（位于 `app/src/main/java`）：

| 包 | 说明 |
|----|------|
| `api/config` | 配置加载（`VodConfig` / `LiveConfig` / `WallConfig`） |
| `api/loader` | 爬虫分发门面（`BaseLoader` + `JarLoader`/`JsLoader`/`PyLoader`） |
| `player` | 播放器（`Players`）、源解析（`Source` + `extractor/*`）、解析任务（`ParseJob`） |
| `player/exo` | ExoPlayer 相关（`MediaSourceFactory`、HLS 广告过滤、缓存、DRM） |
| `server` | 本地 HTTP 服务器（`Server` + `process/*` 各路由处理器） |
| `db` | Room 数据库（`AppDatabase` + `dao/*`） |
| `bean` | 数据模型 |
| `event` | EventBus 事件 |
| `impl` | 各类 Callback 实现 |
| `utils` | 工具类 |

关键单例（通过静态 `get()` 访问）：`App`、`VodConfig`、`LiveConfig`、`BaseLoader`、`Source`、`Server`。

线程模型：耗时任务用 `App.execute(Runnable)`（固定线程池），回主线程用 `App.post(Runnable)`。

## 代码风格与约定

- **缩进**：使用 **Tab**，不用空格。
- **保留已有注释**：修改代码时不要删除原有注释。
- **import 置顶**：所有 `import` 语句保持在文件顶部，不散落。
- 命名遵循现有代码习惯（类名大驼峰、方法/字段小驼峰）。
- 布局资源、字符串资源需同时维护 `values` / `values-zh-rCN` / `values-zh-rTW`（简体/繁体）。

## 关键约束与踩坑（务必遵守）

这些是历史开发中踩过的坑，改动相关代码时必须保持一致：

### 封面比例

- 首页封面比例：**动态**（由站点配置决定）。
- 收藏 / 历史 / 搜索页封面比例：**固定 4:3**。
- 精确计算：`height = width * 3 / 4`，不要用 `width / 1.33f`（有精度问题）。
- 尺寸需扣除左右边距：`imageWidth = itemWidth - 16dp`（每边 8dp）。

### 广告过滤

- AI 去广告按 JSON 配置的 `rules` 过滤：`host` 包含指定字符串或符合 glob 模式（如 `*.v155p*`）时应用对应 regex。
- `ads` 黑名单：域名命中直接拦截。
- **M3U8 广告过滤拦截器**仅对 URL 含 `.m3u8` 或 `Content-Type` 为 `mpegurl`/`m3u8` 的请求生效，TS 片段/图片等不处理。
- M3U8 规则中 host 通配符 `*` 转正则 `[^.]*`（如 `*.v155p*` → `[^.]*\.v155p[^.]*`），且正则需加 `Pattern.DOTALL`。
- M3U8 拦截器启用状态由 `Setting.isRemoveAd()` 控制，用户切换时立即更新。

### 网络

- DoH（DNS over HTTPS，支持 Bootstrap IP）、HTTP/HTTPS/SOCKS4/SOCKS5 代理（按 host 正则动态选择）、Hosts DNS 解析覆盖（支持通配符 `*`）、CORS 注入（按 host 规则注入自定义头）。
- WebView 嗅探通过 `Sniffer` 以 regex 拦截媒体 URL，支持 UA 伪装。

### 磁力播放（libtorrent4j）

- `announce_to_all_trackers`、`announce_to_all_tiers` 设为 `true`。
- 磁力链接用 `load_torrent_parsed` 加载以保留自带 trackers 与 url seeds，**不要**用 `setTorrentInfo()`。
- metadata 获取后保存为 torrent 文件做缓存，避免重复获取。
- session 设置 `validate_https_trackers=false`。
- 需实现 `NEED_SAVE_RESUME` 标志与 fast resume。
- session 参数：`tickInterval=1000ms`、`alert_queue_size=5000`、`enable_ip_notifier=false`、`connectionsLimitPerTorrent=40`。
- **trackers 必须在 `add_torrent` 前通过 `add_torrent_params.setTrackers()` 一次性预设**，避免逐个 `add_tracker` 导致首次 announce 遗漏。
- `btengine` 不能反向引用 `app` 模块（依赖方向：`app` → `btengine` → `catvod`）；跨模块读取配置用 `com.github.catvod.utils.Prefers`。

### 其他

- RecyclerView 点击监听中**必须**用 `holder.getBindingAdapterPosition()` 获取位置，不要用 `position` 参数（刷新后会错位）。
- 本地服务器日志等场景：WebSocket 在 NanoHTTPD 中会导致连接关闭，改用 HTTP 轮询（如 500ms 间隔请求日志端点）更可靠。
- 详情页 tab 与直播页 tab 的切换机制需保持一致：点播详情 tab 需作为顶层 `NestedScrollView` 并设 `android:nestedScrollingEnabled="false"` 关闭嵌套滚动分发，避免手势冲突（目标为 mobile 端时注意两套 UI）。

## 文档索引

| 文档 | 说明 |
|------|------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构级介绍（模块、运行机制、数据流，含 Mermaid 图） |
| [README.md](README.md) | 开发者文档（功能总览） |
| [docs/CONFIG.md](docs/CONFIG.md) | Vod / Live 完整配置字段说明 |
| [docs/SPIDER.md](docs/SPIDER.md) | Spider 所有方法规格与返回格式 |
| [docs/LOCAL.md](docs/LOCAL.md) | 本地 HTTP API 全部端点说明 |
| [docs/LIVE.md](docs/LIVE.md) | 直播源格式说明 |
| [doc/THEME_COLOR_README.md](doc/THEME_COLOR_README.md) | 主题色配置说明 |
| [doc/thunder.md](doc/thunder.md) | 迅雷引擎说明 |
