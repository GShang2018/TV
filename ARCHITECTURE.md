# 影视 (FongMi TV) 架构级介绍

> 本文档从架构层面剖析本项目（基于 CatVod 的影视点播/直播聚合应用），涵盖整体框架、模块划分、核心运行机制与数据流，并以 Mermaid 图直观呈现。

---

## 1. 项目定位

本项目是一个 **Android 端影视聚合播放器**，核心能力：

- **点播（VOD）**：聚合多个站点，通过爬虫（Jar / JS / Python）抓取分类、详情、搜索、播放地址。
- **直播（Live）**：聚合直播源，支持 EPG 节目单、回看、台标、免密等。
- **多播放内核**：系统播放器 / IJK / ExoPlayer 三选一，支持软硬解、弹幕、字幕、倍速、投屏（DLNA/Cast）。
- **多解析引擎**：嗅探、JSON 解析、磁力（迅雷 / libtorrent）、TVBus、Youtube、ZLive、简片 P2P、强制（forcetech）等。
- **本地 HTTP 服务**：内置 NanoHTTPD 服务器（默认端口 9978），提供代理、缓存、媒体转发、远程控制 API。

---

## 2. 顶层模块架构

项目采用 **Gradle 多模块** 结构，`app` 为主应用，其余为功能/引擎模块。

```mermaid
graph TB
    subgraph APP["app 主应用 (com.fongmi.android.tv)"]
        UI["UI 层<br/>leanback / mobile 双 flavor"]
        API["API 层<br/>config / loader / Decoder"]
        PLAYER["播放器层<br/>Players / Source / ParseJob"]
        SERVER["本地服务器<br/>NanoHTTPD :9978"]
        DB["数据层<br/>Room AppDatabase"]
        EVENT["事件层<br/>EventBus"]
    end

    subgraph ENGINE["引擎 / 功能模块"]
        CATVOD["catvod<br/>Spider 抽象 / OkHttp / 工具"]
        QUICKJS["quickjs<br/>JS 爬虫引擎 (QuickJS)"]
        PYRAMID["pyramid<br/>Python 爬虫引擎 (Chaquopy)"]
        BT["btengine<br/>libtorrent4j 磁力引擎"]
        THUNDER["thunder<br/>迅雷下载 SDK"]
        TVBUS["tvbus<br/>TVBus 直播引擎"]
        ZLIVE["zlive<br/>ZLive 直播"]
        YT["youtube<br/>YouTube 解析"]
        JP["jianpian<br/>简片 P2P"]
        FORCE["forcetech<br/>强制直播 (mitv)"]
        IJK["ijkplayer<br/>IJK 播放内核"]
        HOOK["hook<br/>运行时 Hook"]
    end

    APP --> CATVOD
    APP --> QUICKJS
    APP --> PYRAMID
    APP --> BT
    APP --> THUNDER
    APP --> TVBUS
    APP --> ZLIVE
    APP --> YT
    APP --> JP
    APP --> FORCE
    APP --> IJK
    APP --> HOOK
```

### 2.1 模块职责一览

| 模块 | 职责 | 关键类 |
|------|------|--------|
| `app` | 主应用：UI、配置、播放、服务器、数据库 | [`App.java`](app/src/main/java/com/fongmi/android/tv/App.java) |
| `catvod` | 爬虫抽象基类、网络层（OkHttp）、通用工具 | [`Spider.java`](catvod/src/main/java/com/github/catvod/crawler/Spider.java) |
| `quickjs` | 以 QuickJS 运行 JS 爬虫脚本 | [`Spider.java`](quickjs/src/main/java/com/fongmi/quickjs/crawler/Spider.java) |
| `pyramid` | 以 Chaquopy 运行 Python 爬虫 | [`app.py`](pyramid/src/main/python/app.py) |
| `btengine` | libtorrent4j 原生磁力引擎（进程内） | [`LibtorrentEngine.java`](btengine/src/main/java/com/fongmi/btengine/LibtorrentEngine.java) |
| `thunder` | 迅雷下载 SDK（边下边播） | [`XLTaskHelper.java`](thunder/src/main/java/com/xunlei/downloadlib/XLTaskHelper.java) |
| `tvbus` / `zlive` | 直播引擎 | — |
| `youtube` / `jianpian` / `forcetech` | 特定源解析 | — |
| `ijkplayer` | IJK 播放内核（含 ffmpeg so） | — |
| `hook` | 运行时 Hook（如 TVBus 鉴权） | [`Hook.java`](hook/src/main/java/com/fongmi/hook/Hook.java) |

---

## 3. 核心运行机制

### 3.1 配置加载与爬虫分发

应用通过 **配置 URL** 拉取站点列表（点播/直播），并按 API 类型分发到不同的爬虫加载器。

```mermaid
flowchart TD
    A[用户输入配置 URL] --> B[VodConfig / LiveConfig]
    B --> C{BaseLoader.getSpider}
    C -->|api 含 .js| D[JsLoader → QuickJS Spider]
    C -->|api 含 .py| E[PyLoader → Chaquopy Spider]
    C -->|api 以 csp_ 开头| F[JarLoader → DexClassLoader 加载 Jar]
    C -->|其他| G[SpiderNull]

    D --> H[Spider 统一接口]
    E --> H
    F --> H
    G --> H

    H --> I[homeContent 首页]
    H --> J[categoryContent 分类]
    H --> K[detailContent 详情]
    H --> L[searchContent 搜索]
    H --> M[playerContent 播放]
```

- [`BaseLoader.java`](app/src/main/java/com/fongmi/android/tv/api/loader/BaseLoader.java) 是爬虫分发的门面，内部持有 `JarLoader` / `PyLoader` / `JsLoader` 三个加载器。
- 所有爬虫最终都实现 [`Spider.java`](catvod/src/main/java/com/github/catvod/crawler/Spider.java) 抽象类，向上层提供统一方法（首页、分类、详情、搜索、播放、代理）。
- Jar 爬虫通过 `DexClassLoader` 动态加载远程 Jar，并反射调用 `com.github.catvod.spider.Init` 初始化。

### 3.2 播放链路（点播）

播放是核心链路，涉及 **源解析（Source/Extractor）→ 解析任务（ParseJob）→ 播放内核（Players）**。

```mermaid
sequenceDiagram
    participant UI as UI (VodFragment)
    participant VM as SiteViewModel
    participant SP as Spider
    participant PJ as ParseJob
    participant SR as Source/Extractor
    participant PL as Players

    UI->>VM: 请求详情/播放
    VM->>SP: detailContent / playerContent
    SP-->>UI: 返回 Result (含 playUrl)
    UI->>PJ: start(result, useParse)
    PJ->>PJ: 判断解析类型 (嗅探/JSON/parse:)
    PJ->>SR: fetch(result)
    SR->>SR: 按 scheme/host 匹配 Extractor
    SR-->>PJ: 返回真实播放地址
    PJ-->>UI: onParseSuccess(url)
    UI->>PL: 创建播放器并 setSource
    PL->>PL: 选择 SYS/IJK/EXO 内核
    PL-->>UI: 播放状态回调 (EventBus)
```

#### 源解析器（Extractor）链

[`Source.java`](app/src/main/java/com/fongmi/android/tv/player/Source.java) 维护一组 `Extractor`，按 URL 的 scheme/host 匹配：

```mermaid
graph LR
    U[播放 URL] --> M{匹配 Extractor}
    M -->|forcetech| F[Force]
    M -->|jianpian| J[JianPian]
    M -->|proxy| P[Proxy]
    M -->|push| PU[Push]
    M -->|magnet/迅雷| T[Thunder]
    M -->|magnet/libtorrent| B[BtEngine]
    M -->|tvbus| TV[TVBus]
    M -->|video| V[Video]
    M -->|youtube| Y[Youtube]
    M -->|zlive| Z[ZLive]
```

> 磁力播放采用 **双引擎互为备选**：优先迅雷 SDK（本地 HTTP 代理 Range 206 实现边下边播），失败则回退到 libtorrent4j 开源引擎。

### 3.3 播放内核（Players）

[`Players.java`](app/src/main/java/com/fongmi/android/tv/player/Players.java) 是播放器门面，封装三种内核：

```mermaid
graph TB
    P[Players] --> SYS[系统播放器 SYS=0]
    P --> IJK[IJK 内核 IJK=1<br/>IjkVideoView]
    P --> EXO[ExoPlayer 内核 EXO=2<br/>MediaSourceFactory]

    EXO --> MSF[MediaSourceFactory]
    MSF --> HLS[HLS 广告过滤]
    MSF --> CACHE[CacheManager 缓存]
    MSF --> DRM[DRM 支持]

    P --> DANMU[DanmakuView 弹幕]
    P --> SUB[字幕 Sub]
    P --> CAST[投屏 DLNA/Cast]
```

- 支持软解/硬解（`SOFT/HARD`）、倍速、重连、自动续播。
- 通过 `MediaSessionCompat` 与系统媒体会话集成。
- 弹幕使用 `DanmakuView`，字幕支持本地/远程。

### 3.4 本地 HTTP 服务器

[`Server.java`](app/src/main/java/com/fongmi/android/tv/server/Server.java) 启动 NanoHTTPD（默认 9978，冲突自动递增），提供多类处理：

```mermaid
flowchart LR
    REQ[HTTP 请求] --> N[Nano.serve]
    N --> A{路由匹配}
    A -->|/action| ACT[Action 远程控制]
    A -->|/cache| CACHE[Cache 缓存读写]
    A -->|/debug| LOG[DebugLogs 日志]
    A -->|/local| LOCAL[Local 本地资源]
    A -->|/media| MEDIA[Media 媒体转发]
    A -->|/parse| PARSE[Parse 解析代理]
    A -->|/proxy| PROXY[Proxy 爬虫代理]
    A -->|/go| GO[Go 代理]
    A -->|/tvbus| TVB[TVBus 直播]
    A -->|/device| DEV[Device 设备信息]
    A -->|其他| ASSET[静态资源]
```

- 服务器承担 **爬虫代理**（`/proxy`，供 JS/Python 脚本回源）、**媒体转发**（`/media`，磁力引擎产出的本地流）、**远程控制 API**（`/action`，如刷新详情/播放/直播、推送字幕/弹幕）。

### 3.5 数据层

使用 **Room** 持久化，实体包括：`Keep`（收藏）、`Site`（站点）、`Live`（直播）、`Track`（音轨）、`Config`（配置）、`Device`（设备）、`History`（历史）、`Download`（下载）。

```mermaid
graph LR
    DB[AppDatabase] --> DAO[DAO 层]
    DAO --> ConfigDao
    DAO --> DeviceDao
    DAO --> DownloadDao
    DAO --> HistoryDao
    DAO --> KeepDao
    DAO --> LiveDao
    DAO --> SiteDao
    DAO --> TrackDao
    DAO --> VM[ViewModel / Repository]
    VM --> UI[UI]
```

### 3.6 事件驱动

应用大量使用 **EventBus** 解耦模块间通信：

```mermaid
graph LR
    SRC[事件源] --> EB[EventBus]
    EB --> ActionEvent[ActionEvent]
    EB --> CastEvent[CastEvent]
    EB --> ErrorEvent[ErrorEvent]
    EB --> PlayerEvent[PlayerEvent]
    EB --> RefreshEvent[RefreshEvent]
    EB --> ScanEvent[ScanEvent]
    EB --> ServerEvent[ServerEvent]
    EB --> StateEvent[StateEvent]
```

---

## 4. 双 UI 风格（flavor）

`app` 通过 Gradle flavor 区分 **TV 端（leanback）** 与 **手机端（mobile）**，共享 `main` 下的核心逻辑：

```mermaid
graph TB
    MAIN[app/src/main<br/>核心逻辑] --> LB[app/src/leanback<br/>TV 遥控器 UI]
    MAIN --> MB[app/src/mobile<br/>触屏 UI]
    LB --> LBA[HomeActivity / VodActivity<br/>Leanback 组件]
    MB --> MBA[MainActivity / DetailActivity<br/>Material 组件]
```

- **leanback**：面向遥控器，使用 `CustomVerticalGridView`、`CustomHorizontalGridView`、Presenter 体系。
- **mobile**：面向触屏，使用 RecyclerView + Fragment + 底部导航。

---

## 5. 端到端运行流程总览

```mermaid
flowchart TB
    START[App 启动] --> INIT[App.onCreate<br/>初始化线程池/日志/Python]
    INIT --> CFG[加载配置 URL]
    CFG --> SITES[解析站点列表]
    SITES --> SRV[启动本地服务器 :9978]
    SRV --> HOME[进入首页]

    HOME -->|点播| VOD[VodFragment]
    HOME -->|直播| LIVE[LiveActivity]

    VOD --> CAT[Spider.categoryContent 分类]
    VOD --> DET[Spider.detailContent 详情]
    VOD --> SEA[Spider.searchContent 搜索]
    DET --> PLAY[播放链路]
    SEA --> PLAY
    PLAY --> EXTRACT[Source 匹配 Extractor]
    EXTRACT --> REAL[获取真实地址]
    REAL --> KERNEL[Players 选择内核播放]
    KERNEL --> DANMU2[弹幕/字幕/投屏]

    LIVE --> LCH[频道列表]
    LIVE --> EPG[EPG 节目单]
    LIVE --> LPLAY[直播播放]
```

---

## 6. 关键设计要点

1. **单例门面**：`App`、`VodConfig`、`LiveConfig`、`BaseLoader`、`Source`、`Server` 均为单例，通过静态 `get()` 访问，全局共享状态。
2. **线程模型**：`App` 提供固定线程池（`App.execute`）与主线程 Handler（`App.post`），耗时任务（配置加载、爬虫抓取、解析）统一在后台执行，结果通过回调/EventBus 回主线程。
3. **可插拔引擎**：Extractor 接口 + 加载器分发，使新增源/引擎只需实现接口并注册，无需改动上层。
4. **本地服务器中枢**：NanoHTTPD 承担代理、媒体转发与远程控制，是连接爬虫、磁力引擎与播放器的枢纽。
5. **多语言爬虫**：统一 `Spider` 抽象，屏蔽 Jar/JS/Python 三种实现差异，上层无感知。

---

## 7. 目录速查

| 路径 | 说明 |
|------|------|
| [`app/src/main/java/com/fongmi/android/tv/api`](app/src/main/java/com/fongmi/android/tv/api) | 配置加载与爬虫分发 |
| [`app/src/main/java/com/fongmi/android/tv/player`](app/src/main/java/com/fongmi/android/tv/player) | 播放器、源解析、解析任务 |
| [`app/src/main/java/com/fongmi/android/tv/server`](app/src/main/java/com/fongmi/android/tv/server) | 本地 HTTP 服务器 |
| [`app/src/main/java/com/fongmi/android/tv/db`](app/src/main/java/com/fongmi/android/tv/db) | Room 数据库 |
| [`app/src/main/java/com/fongmi/android/tv/bean`](app/src/main/java/com/fongmi/android/tv/bean) | 数据模型 |
| [`app/src/main/java/com/fongmi/android/tv/event`](app/src/main/java/com/fongmi/android/tv/event) | EventBus 事件 |
| [`catvod`](catvod) | 爬虫抽象与网络层 |
| [`quickjs`](quickjs) | JS 爬虫引擎 |
| [`pyramid`](pyramid) | Python 爬虫引擎 |
| [`btengine`](btengine) / [`thunder`](thunder) | 磁力引擎 |
