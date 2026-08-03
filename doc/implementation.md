# Songloft TV — 实现文档

> 基于当前代码整理（v1.0.0），描述实际实现，区别于 [design.md](design.md) 的设计稿。

---

## 1. 总体架构

单模块 `:app`，包根 `com.songloft.tv`，分层：

```
UI (Compose Screen + ViewModel, Hilt + StateFlow)
   │
domain/PlayerController ── MediaController ──> MusicService (ExoPlayer + MediaSession)
   │
data/repository ──> data/api (Retrofit + OkHttp) ──> Songloft 后端 /api/v1
   │
data/storage/PreferencesDataStore (DataStore)
```

关键决策：

- **ExoPlayer 实例只存在于 `MusicService`**（MediaSessionService）。UI 层全部通过单例 `PlayerController` 内的 Media3 `MediaController` 与之交互，因此退出播放器页面或主界面后播放可继续。
- **主界面不使用 Navigation Compose**：`MainActivity.TvApp` 用 `mutableStateOf<Screen>` 手写状态导航；全屏播放器是独立 `PlayerActivity`。
- 依赖注入用 Hilt（KSP），仓库层方法统一 `withContext(Dispatchers.IO) { runCatching {...} }` 返回 `Result`。

## 2. 数据层

### 2.1 API 接口（`data/api/SongloftApi.kt`）

baseUrl 为 `{serverUrl}/api/v1/`，全部 suspend 方法：

| 方法 | HTTP | 路径 | 说明 |
|---|---|---|---|
| `login` | POST | `auth/login` | 返回 `LoginResponse`（access_token/refresh_token） |
| `getSongs` | GET | `songs` | limit/offset/keyword/artist/album/year |
| `getSongPlayUrl` | GET | `songs/{id}/play` | 可选 quality |
| `getSongLyric` | GET | `songs/{id}/lyric` | lyric/tlyric/rlyric/lxlyric |
| `getFacets` | GET | `songs/facets` | field=artist/album/year |
| `getSongNames` | GET | `songs/names` | field=title/artist，去重全量名字，供拼音检索 |
| `reportPlayed` | POST | `songs/{id}/played` | type=play/finish/skip；source 固定 `tv`；play 时带 context_type/context_key（歌单或分面，写入服务端播放历史） |
| `getPlaylists` | GET | `playlists` | 可选 type |
| `getPlaylistDetail` | GET | `playlists/{id}` | |
| `getPlaylistSongs` | GET | `playlists/{id}/songs` | limit/offset |
| `addSongsToPlaylist` | POST | `playlists/{id}/songs` | Body `AddSongsRequest(song_ids)` |
| `removeSongFromPlaylist` | DELETE | `playlists/{id}/songs/{songId}` | |
| `getConfig`/`setConfig` | GET/PUT | `config/{key}` | |
| `health` | GET | `health` | 连通性探测 |
| `getStatsSummary` | GET | `jsplugin/stats/api/stats/summary` | 播放统计插件：概览汇总，可选 `from`/`to`（毫秒时间戳，`[from, to)` 区间） |
| `getStatsTrends` | GET | `jsplugin/stats/api/stats/trends` | 播放统计插件：最近 N 天播放趋势，`days` 默认 7（上限 90） |
| `getStatsHourly` | GET | `jsplugin/stats/api/stats/hourly` | 播放统计插件：时段分布（凌晨/上午/下午/晚上） |
| `getStatsHistory` | GET | `jsplugin/stats/api/history/raw` | 播放统计插件：原始播放记录分页，`limit` 默认 20（上限 100）/`offset`，响应含 `hasMore` |

注意：各方法直接返回具体响应类（`SongListResponse` 等），无统一包装类。统计插件接口响应为 `{ success, data, error? }` 包装（`StatsSummaryResponse` 等），Repository 层统一校验 `success` 后解包，失败返回 `Result.failure`。

### 2.2 认证机制

- **`ApiClient`**（object 单例）：`initialize(url)` 幂等；OkHttp 挂 `AuthInterceptor` + BODY 日志 + `TokenAuthenticator`，超时 30s；暴露 `onTokensRefreshed` 回调供持久化。
- **`AuthInterceptor`**：内存持有 access/refresh token（运行时唯一来源），为请求添加 `Authorization: Bearer <token>`。
- **`TokenAuthenticator`**（401 触发）：
  1. 无 refresh token 或重试 ≥2 次即放弃；
  2. `synchronized` 内并发去重——若内存 token 已与失败请求头不同，说明他人已刷新，直接重试；
  3. 否则用**独立无拦截器 OkHttpClient** POST `{baseUrl}/api/v1/auth/refresh`（body `{"refresh_token": ...}`），成功则更新内存 token → 触发回调写回 DataStore → 重试原请求。
- **Token 链路**：登录成功写内存 + DataStore；启动时 `AuthRepository.tryAutoLogin()` 从 DataStore 回填并调 `health()` 验证；登出两处同时清空。

### 2.3 数据模型（`data/model/`）

- `Song`：id/type("local"/"radio")/title/artist/album/duration(秒)/url/coverUrl/isVideo/fileSize/`tracks: List<Track>?`；`hasMultiTrack` = tracks>1（双音轨）。
- `Track`：id/name/url/quality。多文件音轨每轨独立 URL；内嵌音轨 id 为 `embedded:<groupIndex>`。
- `Playlist`：id/name/description/coverUrl/songCount/type("normal"/"radio")/labels；`isBuiltIn`（labels 含 `built_in`，收藏歌单）、`isHidden`。
- `LyricLine`：time/text/`words: List<LyricWord>?`（逐字）/translation/romaji。
- `FacetItem`：value/count/coverUrl。

### 2.4 Repository（均 `@Singleton`）

- **AuthRepository**：`login`（初始化 ApiClient/UrlHelper + 存 token）、`tryAutoLogin`、`logout`；init 中注册 token 刷新回调持久化。
- **FavoriteRepository**：收藏基于服务端 **`built_in` 标签歌单**实现——`type=normal` 歌单收藏歌曲、`type=radio` 歌单收藏电台；内部缓存 type→playlistId 映射；`getFavorites` 拉取所有内置歌单歌曲合并。
- **PlaylistRepository**：歌单列表/详情/歌曲（详情页按 500 首/页循环拉取直至全量，修复原先只显示前 50 首的问题）；列表第一页把内置收藏歌单（收藏/电台收藏）固定置顶，后续分页不变。
- **SongRepository**：`getSongs`、`getFacets`、`getSongLyric`（歌词全空抛异常）、`reportPlayed`、`getLibraryStats`（分页拉全库统计，上限 5000 首）。
- **StatsRepository**：播放统计插件（`jsplugin/stats`）数据源，`getSummary(range)`（range 由 `StatsRange` 枚举换算 from/to 时间戳：全部/今日/本周[周一起]/本月）、`getTrends(days)`、`getHourly()`、`getHistory(limit, offset)`；任一接口失败返回 Result.failure，UI 层据此回退。

### 2.5 存储（`data/storage/PreferencesDataStore.kt`）

DataStore 名 `songloft_tv_settings`，5 个 key：`server_url`、`theme_mode`(Int)、`audio_quality`、`access_token`、`refresh_token`。均以 Flow 暴露。

### 2.6 UrlHelper（`data/api/UrlHelper.kt`）

- `songPlayUrl(id, quality?, track?)` → `/api/v1/songs/{id}/play?quality=..&track=..`
- `songCoverUrl` / `playlistCoverUrl`
- `resolve(url?)`：后端相对路径 → 绝对 URL（已带 http(s) 原样返回）。

## 3. 播放体系

### 3.1 MusicService（`MusicService.kt`）

- ExoPlayer 真正的创建位置。自定义 `DataSource.Factory`：每次创建 `DefaultHttpDataSource` 时**动态**读取 `AuthInterceptor.accessToken` 附加 `Authorization` 头（JWT 会运行期刷新，不能固化）。
- AudioAttributes（music/media + 音频焦点）、`handleAudioBecomingNoisy`。
- MediaSession 的 sessionActivity 指向 `PlayerActivity`（点通知回播放器）。
- `onTaskRemoved`：仅在未播放或队列为空时 `stopSelf()` —— 播放中移除任务不停止，天然后台播放（**没有**用户可见的后台播放开关）。
- 通知使用 MediaSessionService 默认 MediaNotification，元数据来自 MediaItem 的 MediaMetadata。

### 3.2 PlayerController（`domain/PlayerController.kt`）

`@Singleton`，通过 `withController(action)` 懒建 `MediaController` 连接 MusicService。暴露 `state: StateFlow<PlaybackState>`（queue/currentIndex/currentSong/currentTrack/embeddedTracks/isPlaying/isBuffering/duration/playMode/睡眠定时器状态）。

- **队列**：`play(queue, index, contextType?, contextKey?)` 先同步更新 state（UI 提前展示），再 `setMediaItems` + `prepare` + `play`；context 随 state 保存，仅在 `play` 事件上报时携带（歌单详情页传 `playlist`+歌单 ID，分面页传 `artist/album/year`+取值，搜索/收藏等扁平列表不传）。
- **播放模式**：`enum PlayMode { ORDER, LOOP, SINGLE, RANDOM }`，映射到 ExoPlayer 的 repeatMode + shuffleModeEnabled；`cyclePlayMode()` 轮转。
- **双音轨切换 `switchTrack(track)`**，两种机制：
  1. 服务端多文件音轨：记录进度 → `replaceMediaItem` 重建 MediaItem → `seekTo` 续播；`onMediaItemTransition` 中同曲 id 不重置 currentTrack；
  2. 内嵌音轨（如 MKV 多音轨，`onTracksChanged` 中检出多个音频 TrackGroup）：`TrackSelectionOverride` 无缝选轨，不重建。
- **URI 构建 `buildMediaItem`** 优先级：track.url → 电台 song.url（type=radio）→ `UrlHelper.songPlayUrl(id, quality, track)`；以 `.m3u8` 结尾时显式设 `APPLICATION_M3U8` MimeType 走 HLS。
- **播放上报**：转场时上一首按原因报 `finish`（自然播完）/`skip`（手动切），新歌报 `play`；source 固定 `tv`（来源统计），`play` 事件带当前播放上下文（见上）。
- **睡眠定时器**（两种互斥）：`setSleepTimer(minutes)` 协程每分钟递减；`setSleepAfterSongs(count)` 在自然转场时递减；归零 pause。UI 入口在设置页。

### 3.3 LyricParser（`domain/LyricParser.kt`）

`parsePayload(lyric, tlyric, rlyric, lxlyric)`：

- 优先级：lxlyric（洛雪逐字格式 `<偏移,时长>字`）→ lyric 含逐字标记 → 标准 LRC 逐行；
- 标准 LRC 支持一行多时间标签；逐字支持相对偏移和绝对 `[[mm:ss.xx]]` 两种，跨行修补末字结束时间；
- `mergeTranslations`：翻译/罗马音按时间最近邻匹配（容差 600ms）合并进主歌词行。

### 3.4 播放器 UI（`ui/player/`）

- **PlayerActivity**：独立 Activity，仅承载 `PlayerScreen`。
- **PlayerViewModel**：collect PlayerController.state 映射 UiState；进度轮询自适应——有逐字歌词时 60ms（卡拉 OK 平滑），否则 500ms；收藏乐观更新、失败回滚。
- **交互**：控制栏 10s 无操作自动隐藏；控制隐藏时——左右键长按连续 ±10s seek、短按切歌、上下/OK 唤出控制栏；媒体键直达。
- **两种模式**：视频（全屏 `VideoPlayer` = PlayerView 绑定 MediaController，多音轨时右上角 TrackChips）；音频（封面 blur(60dp) 毛玻璃背景 + 左封面/右 `LyricsPanel`）。
- **LyricsPanel**：自动滚动居中；逐字行渲染 KaraokeLine（按 word start/end 进度逐字点亮）；附带翻译行。
- **ControlBar**：SeekBar + 上一曲/播放暂停/下一曲/播放模式/收藏/重新获取歌词/队列按钮（重新获取歌词走 `refresh=1` 重跑服务端歌词插件搜索，请求中按钮显示加载圈）。
- **QueueDrawer**：左侧 400dp 抽屉，当前曲高亮，自动滚到当前位置，点击条目跳播（`PlayerController.playAt(index)`）。

## 4. UI 层

### 4.1 导航

`ui/navigation/Screen.kt`：sealed class，顶级 Tab 为 Home/Search/Playlists/My（`TvBottomNav` 底栏），二级页 Settings、Stats、PlaylistDetail(id)、SongFilter(field,value)、FacetList(field)。`MainActivity.TvApp` 用 `when(currentScreen)` 切换，`BackHandler` 定义回退链（PlaylistDetail→Playlists，Settings→My，其余→Home）。

启动流程：`MainApp` 观察 `AuthViewModel.authState`，`LoggedIn` 进 TvApp，否则显示 `AuthSetupScreen`。有播放时右下角悬浮 `FloatingPlayerBar`。

### 4.2 各页面

| 页面 | 实现要点 |
|---|---|
| 首页 Home | 5 个 async 并发拉统计/歌手/专辑/年份/歌单；统计卡 ×4、歌单 4 列网格（≤8，内置收藏歌单置顶）、歌手/专辑两列（各 6）、年份胶囊行（8）；最下方「年份速览」动态切换：仅预取统计插件 summary（全部区间，不带参数），成功则展示「播放统计」概览（全部/今日/本周/本月 Tab，切换其他区间时按需请求该区间 summary，右上角查看全部进统计页），失败则回退年份速览；概览区「本月」Tab 与「查看全部」用 `focusProperties` 双向焦点跳转 |
| 统计 Stats | 播放统计插件（jsplugin/stats）子界面：全部/今日/本周/本月时间 Tab、概览卡 ×4（播放次数/听歌时长/不同歌曲/不同艺术家）、艺术家排行 top4、歌曲排行 top3、听歌趋势（7/30 天柱状图）、专辑排行 top3、时段分布、来源分布 top3、歌曲类型 top3、最近播放 top3；各卡片标题栏带「刷新」按钮 |
| 搜索 Search | 300ms 防抖搜索；自定义 `TvKeyboard`（左侧 8×4 字母方阵+功能键、右侧 4×4 数字/符号方阵可切换 + 一次性 Shift，特殊键用字符串协议"←退格/清空/确定"）；热门标签取 artist facet 前 10；拼音/首字母候选取 `songs/names`（title+artist 去重全量）经 `PinyinMatcher` 索引，输入 ≥2 个字母时匹配候选（旧服务器无该接口时回退 artist facet 值） |
| 分类 FacetList | 全部歌手/专辑/年份，3 列网格 → 点击进 SongFilter |
| 筛选 FilteredSongs | 按 artist/album/year 拉 500 首列表 |
| 歌单 Playlists | 全部/普通/电台 FilterChip 过滤，4 列网格（第一页内置收藏歌单置顶）；详情页有"播放全部/随机播放" |
| 我的 My | 收藏按 `song.type` partition 为歌曲/电台两个 Tab；右上角进设置 |
| 设置 Settings | 见 4.3 |
| 配置 AuthSetup | 见 4.4 |

### 4.3 设置页

- **主题**：跟随系统(0)/浅色(1)/深色(2) 写 DataStore `theme_mode`；`TvTheme` 直接订阅同一 key，即时全局换肤。
- **音质**：原始("")/mp3/flac 写 DataStore，PlayerController 取流时读取拼入 quality 参数。
- **睡眠定时**：直接调 PlayerController（不持久化），剩余量实时回显。
- **日志导出**：`logcat -d` 逐行脱敏（Authorization/Cookie 头、JSON token/password 字段、URL token 参数、裸 JWT 四个正则）后写系统下载目录（API 29+ 用 MediaStore）。
- **关于**：运行时读 versionName、项目地址、开源组件列表。
- **危险区**：清除服务器配置、退出登录。

### 4.4 配置/登录（`ui/config/`）

`AuthState`（sealed）：Loading/NotConfigured/Configured/LoggedIn/Error。启动时读 DataStore serverUrl，非空则 `tryAutoLogin`。两种配置方式并存于 `AuthSetupScreen`：

1. **遥控器手动输入**：三个 InputField（服务器/账号/密码）+ 共享 TvKeyboard，按 activeField 路由按键。
2. **手机扫码**：`startConfigServer()` 在候选端口 18899-18902 启动 `ConfigWebServer`（NanoHTTPD，`GET /` 返回移动端 HTML 表单、`POST /submit` 接收 server/username/password）；电视端 ZXing 生成 `http://<局域网IP>:<端口>` 二维码；手机提交后回调触发登录，成功后停服。

两种方式共用 `login()`：地址无协议前缀时按 `https://` → `http://` 顺序探测登录，仅连接层失败（IOException）才换协议重试，服务器有真实响应（如账号密码错误）直接报错；成功后以实际可用协议的完整 URL 持久化。

### 4.5 通用组件与主题

- **CoverImage**：`UrlHelper.resolve` + Coil AsyncImage，加载中/失败/无 URL 显示音符占位；Coil 的 OkHttpClient 在 `SongloftTvApp`（ImageLoaderFactory）中挂 AuthInterceptor（封面接口需 JWT）。
- **FloatingPlayerBar**：右下角迷你条，未聚焦为 96dp 圆形（仅封面），聚焦展开 300dp 露出标题；播放中封面 10s/圈旋转。
- **TvFocusable / D-Pad 规范**：统一"焦点 = 缩放 1.05-1.1x + primary 边框"模式；`Modifier.tvFocusable()` 是抽象，多数页面内联实现同一模式；无自定义 FocusOrder，依赖 Compose 默认焦点搜索。
- **主题 TvTheme**：单种子色 `0xFF415F91`，手写 Light/Dark ColorScheme；composable 内直接订阅 DataStore 的 `PreferencesDataStore.THEME_MODE` key。

## 5. 已知问题 / 遗留

| 问题 | 位置 |
|---|---|
| StateFlow 初始值硬编码了测试服务器地址/账号/密码（临时测试用，发布前需清理） | `ui/config/AuthViewModel.kt` |
| 设计稿中的"后台播放开关"、"服务器切换"未实现（后台播放为默认行为） | — |
