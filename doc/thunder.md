## 这个 App 实现流畅播放磁力的技术总结

通过反编译分析（包名 `syxzq.app` / `com.bclsapp.download`），这个 App 实现"磁力秒开、流畅播放"的核心技术是**"迅雷完整播放版 SDK + 边下边播 + P2SP/DCDN 云端加速"**的组合方案。具体拆解如下：

### 1. 混合架构（Hybrid App）
- 外层是 **WebView 壳**（JS 注入 + 原生桥接），UI 用网页实现，播放和下载走原生。
- 播放器：**APlayer** + **IJKPlayer**（FFmpeg 软解/硬解），并集成 **乐播投屏 hpplay** 支持投屏。

### 2. 迅雷"完整播放版" SDK（核心）
这是与普通 TV 版最大的区别。App 的 dex 里暴露了一整套**播放专用 API**（`SDK_` 前缀），这些 API 在 H:\TV 用的"精简下载版" .so 里**根本不存在**：

| 类别 | 关键 API | 作用 |
|------|---------|------|
| 边下边播 | `SDK_openUrlPlay`、`SDK_openTorrent2Play`、`SDK_openTorrentPlayByIndex` | 直接以"播放模式"打开磁力/URL，边下边播 |
| 本地代理 | `SDK_getProxyLocalUrl` | 返回一个本地 HTTP 代理地址，播放器从这个地址拉流 |
| 加速 | `SDK_getP2PSpeed`、`SDK_getP2SSpeed`、`SDK_getExtraSpeed`、`SDK_getOriginSpeed` | 实时获取 P2P/P2S/额外/原始速度 |
| 任务控制 | `SDK_pauseTask`、`SDK_setTaskMaxSpeed`、`SDK_getState`、`SDK_getTaskInfo(Ex)` | 暂停、限速、状态查询 |
| 文件信息 | `SDK_getCid`、`SDK_getGcid`、`SDK_getTorrentHash`、`SDK_getFileSize`、`SDK_getDownloadedSize` | 获取文件唯一标识与大小 |

### 3. 流畅播放的三大支柱

**① 边下边播（Play-While-Downloading）**
- 迅雷 SDK 以"播放模式"打开磁力后，会**优先下载文件头部/当前播放位置的数据块**（顺序下载），而不是随机乱序。
- 通过 `SDK_getProxyLocalUrl` 返回一个**本地 HTTP 代理地址**，播放器（APlayer/IJKPlayer）从这个地址发起 **HTTP Range 请求（206 Partial Content）** 拉流。
- 播放器请求到哪，SDK 就优先下到哪，实现"秒开 + 边下边播"。

**② P2SP + DCDN 云端加速**
- 迅雷的 **P2SP（Peer to Server & Peer）** 网络：同一资源同时从**多个 HTTP 服务器 + 多个 P2P 节点**并行拉取，速度远超单一 BT 源。
- **DCDN（迅雷云加速）**：当 P2P 节点不足时，自动从迅雷**云端缓存服务器**补速，保证冷门资源也能满速。
- 这就是为什么"同样的磁力链接，App 能流畅播放，而 TV 几乎放不出来"——TV 版只有纯 BT 下载（libtorrent4j / 精简迅雷），没有迅雷的 P2SP 服务器资源和 DCDN 云加速。

**③ 顺序预取 + 智能调度**
- SDK 内部对数据块做**优先级排序**，播放位置附近的数据块优先下载，非播放区域延后。
- 多线程并发下载 + 断点续传 + 磁盘缓存，保证播放不卡顿。

### 4. 与 H:\TV 的对比（为什么 TV 放不出来）

| 维度 | 这个 App | H:\TV |
|------|---------|-------|
| 迅雷 SDK 版本 | **完整播放版**（含 openUrlPlay/getProxyLocalUrl） | 精简下载版（无播放 API） |
| 边下边播 | ✅ 原生支持 | ❌ 只能下完再播 |
| P2SP 服务器加速 | ✅ 迅雷云端 | ❌ 无 |
| DCDN 云加速 | ✅ 迅雷云端 | ❌ 无 |
| 播放器 | APlayer/IJKPlayer | ExoPlayer |

### 5. 关键结论
App 流畅播放磁力的本质 = **迅雷"完整播放版" SDK 的边下边播能力 + 迅雷 P2SP/DCDN 云端加速网络**。这两者都是**迅雷闭源商业 SDK + 云端服务**，无法通过反编译 .so 单独复刻——因为：
- 播放 API（`SDK_openUrlPlay` 等）只存在于 dex 的 Java 层，对应的 native 实现和**迅雷服务器端**都在云端。
- 你提供的所有 .so（arm64-v8a 10.6MB、armeabi-v7a 8MB、3.5MB 精简版）都只是**下载型**，不含播放 API。

### 6. 我们能做的替代方案（已部分集成到 H:\TV）
既然拿不到完整播放版 SDK，只能**自己实现边下边播**：
1. ✅ **已集成**：激活了 H:\TV 迅雷 .so 里休眠的 P2SP 加速方法（`enterPrefetchMode` 顺序预取 + `requeryIndex` 刷新节点），让下载优先下播放位置的数据块。
2. 📋 **待实现**：自己写一个**本地 HTTP 代理服务器**（`http://127.0.0.1:PORT`），支持 Range 请求（206），把正在下载的文件通过 HTTP 暴露给 ExoPlayer，实现边下边播。H:\TV 已有 JianPian/ZLive/Force 的本地代理模式可参考，`Players.isLocalProxyUrl()` 也已支持 `127.0.0.1` 地址。

**一句话总结**：这个 App 靠的是**迅雷闭源"完整播放版" SDK 的边下边播 + P2SP/DCDN 云端加速**，这是商业级闭源能力，无法直接移植；但可以通过"本地 HTTP 代理 + Range 请求 + 顺序预取"在 H:\TV 上自建一套边下边播方案来逼近效果。