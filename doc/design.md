# Songloft TV — 设计文档

> 版本：v1.0  
> 日期：2026-07-21  
> 状态：草稿  

---

## 1. 项目概述

### 1.1 项目定位

Songloft TV 是 [Songloft](https://github.com/songloft-org/songloft) 音乐服务器的 Android TV 客户端。面向电视大屏场景，提供简洁、沉浸的音乐和 MV 播放体验。

### 1.2 核心原则

- **简洁**：只保留 TV 端核心功能，去除非必要模块
- **沉浸**：全屏播放器 + 毛玻璃背景 + 自动隐藏控制栏
- **TV 原生**：完整的 D-Pad 焦点控制，遥控器操作流畅
- **复用后端**：对接 Songloft Go 后端 API，不重复实现业务逻辑

### 1.3 参考项目

| 项目 | 参考内容                     |
|------|--------------------------|
| [songloft-player](https://github.com/songloft-org/songloft-player) | API 接口定义、数据模型、功能逻辑       |
| [music-tv](https://github.com/GanHuaLin/rouroumusic-tv) | TV 原生 UI 布局、焦点交互、沉浸播放器模式 |
| [songloft-library-plus-main](https://github.com/charce526/songloft-library-plus) | 首页功能、布局参考                |

---

## 2. 技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| 语言 | Kotlin | 原生 Android 开发 |
| UI 框架 | Jetpack Compose for TV | `androidx.tv:tv-material` + `tv-foundation` |
| 最小 SDK | 23 (Android 6.0) | 覆盖绝大多数 TV 设备 |
| 目标 SDK | 35+ | |
| 播放器 | ExoPlayer (Media3) | 视频/音频播放 |
| 后台播放 | MediaSessionService | 支持通知栏控制 |
| 网络 | Retrofit + OkHttp | REST API 通信 |
| 图片加载 | Coil (Compose 原生) | 支持模糊变换 |
| 状态管理 | ViewModel + StateFlow | Compose 原生集成 |
| DI | Hilt | 依赖注入 |
| 导航 | Compose 导航 / Activity | TV 场景适用 |

---

## 3. 功能清单

| 模块 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| **服务器配置** | 首次配置页 | P0 | 手动输入 URL / 扫码登录 |
| | JWT 认证 | P0 | access + refresh 双 Token |
| | 服务器切换 | P1 | 保存多台服务器，快速切换 |
| **首页** | 音乐库概览（Dashboard） | P0 | 参考 songloft-library-plus 概览布局 |
| | 统计卡片 | P0 | 全部歌曲/本地歌曲/总时长/文件大小 |
| | (洞察行已移除) | — | 与下方歌手/专辑区域功能重叠 |
| | 我的歌单网格 | P0 | 最多 8 个歌单卡片 |
| | 主要歌手/主要专辑 | P0 | 前 6 名，两列布局 |
| | 年份速览 | P1 | 前 8 个年份 |
| **搜索** | D-Pad 自定义键盘 | P0 | 全屏键盘，字母+符号 |
| | 搜索结果列表 | P0 | 歌曲列表，可播放/收藏 |
| **歌单浏览** | 6 列网格歌单 | P0 | 分页加载，支持 type 过滤 |
| **我的** | 收藏歌曲 | P0 | 基于 built_in 歌单 |
| | 收藏电台 | P1 | radio 类型收藏 |
| | 设置入口 | P0 | 右上角齿轮图标 |
| **全屏播放器** | 视频播放 (MV) | **P0** | 全屏视频渲染 + 控制栏覆盖 |
| | 音频播放 | P0 | 封面+歌词模式 |
| | **原伴唱双音轨切换** | **P0** | 原唱/伴奏/两轨切换 |
| | 同步歌词 | P0 | LRC 滚动显示 |
| | 毛玻璃背景 | P0 | 封面模糊 + 暗色叠加 |
| | 底部控制栏（自动隐藏） | P0 | 播放/暂停、上/下一首、进度条 |
| | 播放模式切换 | P0 | 顺序→列表循环→单曲循环→随机 |
| | 播放队列抽屉 | P0 | 左侧滑出，当前队列 |
| | 收藏/取消收藏 | P0 | |
| **悬浮迷你播放器** | 圆形旋转封面 + 歌名 | P0 | 全页面悬浮 |
| **设置** | 主题模式 | P0 | 亮色 / 暗色 / 跟随系统 |
| | 音质选择 | P1 | 原画 / MP3 / FLAC |
| | 背景播放开关 | P1 | |
| | 睡眠定时器 | P1 | 倒计时 / 指定歌曲数 |
| | 关于页面 | P1 | 版本号、开源许可 |
| **睡眠定时器** | 倒计时模式 | P1 | 30/60/90 分钟后停止 |
| | 歌曲数模式 | P1 | 播完 N 首后停止 |

### 移除功能（不进入 TV 客户端）

| 功能 | 移除原因 |
|------|---------|
| JS 插件管理 | TV 端不需要 |
| DLNA 投射 | TV 是播放终端 |
| 均衡器 | 遥控器操作不便 |
| 文件管理/本地扫描 | 依赖后端能力 |
| Bundle 本地模式 | TV 存算资源有限 |
| 歌词刮削 | songloft 后端已有歌词 |
| 多服务器管理 | 精简为单服务器切换 |

---

## 4. 系统架构

### 4.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                     UI 层 (Compose)                      │
│  HomeScreen │ SearchScreen │ LibraryScreen │ MyScreen    │
│  PlaylistDetailScreen │ PlayerActivity │ SettingsScreen │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    ViewModel 层                          │
│  HomeViewModel │ SearchViewModel │ PlayerViewModel       │
│  LibraryViewModel │ PlaylistViewModel │ SettingsViewModel │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Domain 层                             │
│  PlayerController │ LyricParser │ PlaybackState          │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Data 层                               │
│  Repository │ Retrofit API │ Data Model                  │
│  LocalStorage (DataStore)                               │
└─────────────────────────────────────────────────────────┘
```

### 4.2 播放器架构

```
┌────────────────────────────────────────────────────┐
│                  PlayerActivity                     │
│  ┌──────────────────────────────────────────────┐  │
│  │         Compose UI Layer                      │  │
│  │  VideoView / CoverImage │ LyricsPanel         │  │
│  │  ControlBar │ TrackSwitcher │ QueueDrawer     │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │           PlayerViewModel                     │  │
│  │  play/pause/seek/next/prev/mode/queue        │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │          PlayerController                     │  │
│  │  ExoPlayer 实例管理 │ TrackSelector          │  │
│  │  音轨切换 │ 视频/音频模式切换               │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │          MusicService (MediaSessionService)   │  │
│  │  后台播放 │ 通知栏控制 │ 焦点获取            │  │
│  └─────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

---

## 5. 页面设计

### 5.1 导航结构

```
底部 Tab 栏（4 个标签页）
├── 🏠 首页       → 音乐库概览（统计 + 洞察 + 歌单 + 歌手/专辑 + 年份）
├── 🔍 搜索       → D-Pad 键盘 + 搜索结果
├── 📋 歌单       → 全部歌单，支持 type 过滤（全部/普通/电台）
└── ❤️ 我的       → 收藏歌曲 + 收藏电台 + ⚙️ 设置入口

页面栈：
首页/搜索/歌单/我的
  └── 点击歌单 → PlaylistDetailScreen
        └── 点击歌曲 → PlayerActivity(全屏)
              └── 返回 → 原页面 + 悬浮迷你播放器
```

### 5.2 页面布局

#### 首页（音乐库概览风格）

参考 `songloft-library-plus` 插件的概览（Dashboard）布局，适配 TV D-Pad 交互。

```
┌──────────────────────────────────────────────┐
│  音乐库概览                        上次更新   │
│                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  │ 全部歌曲  │ │ 本地歌曲  │ │ 总时长    │ │ 文件大小  │  ← 统计卡片行
│  │  1,234   │ │  1,000   │ │ 12 小时   │ │ 12.5 GB  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘
│                                          │
│  我的歌单                                   │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐              ← 4 列网格
│  │封面 │ │封面 │ │封面 │ │封面 │             (最多 8 个)
│  │名称 │ │名称 │ │名称 │ │名称 │
│  │ 12首│ │ 8首 │ │ 25首│ │ 5首 │
│  └────┘ └────┘ └────┘ └────┘
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐
│  │封面 │ │封面 │ │封面 │ │封面 │
│  │名称 │ │名称 │ │名称 │ │名称 │
│  │ 42首│ │ 3首 │ │ 18首│ │ 9首 │
│  └────┘ └────┘ └────┘ └────┘
│                                          │
│  ┌─ 主要歌手 ──────┐ ┌─ 主要专辑 ──────┐  │  ← 两列布局
│  │ ┌────────────┐  │ │ ┌────────────┐  │  │
│  │ │ 歌手A 42首 │  │ │ │ 专辑A·歌手 │  │  │
│  │ │ 歌手B 36首 │  │ │ │ 专辑B·歌手 │  │  │
│  │ │ 歌手C 28首 │  │ │ │ 专辑C·歌手 │  │  │
│  │ │ ...共6位   │  │ │ │ ...共6张   │  │  │
│  │ [查看全部]   │  │ │ [查看全部]   │  │  │
│  └──────────────┘  └──────────────┘  │  │
│                                          │
│  年份速览                                  │
│  [2024·42首] [2023·36首] [2022·28首] ...  │  ← 4 列
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘
```

**数据来源**（songloft 后端 API）：
| 区域 | API | 说明 |
|------|-----|------|
| 统计卡片 | `GET /api/v1/songs` + `GET /api/v1/songs/facets` | 汇总计算 |
| (无洞察行) | — | TV 端不需要，下方已有歌手/专辑区域 |
| 歌单网格 | `GET /api/v1/playlists` | 最多 8 个 |
| 主要歌手 | `GET /api/v1/songs/facets?field=artist` | 按 count 排序前 6 |
| 主要专辑 | `GET /api/v1/songs/facets?field=album` | 按 count 排序前 6 |
| 年份速览 | `GET /api/v1/songs/facets?field=year` | 按 year desc 前 8 |

**D-Pad 焦点流**：
- 统计卡片行：横向焦点移动，不可点击（纯展示）
- 歌单网格：先横向后纵向，点击进入歌单详情
- 歌手/专辑列：先纵向后横向，点击跳转分类筛选
- 年份行：横向焦点移动，点击跳转年份筛选


#### 搜索

```
┌──────────────────────────────────────────┐
│  ← 搜索音乐                              │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │  搜索框 (D-Pad 聚焦打开键盘)      │    │
│  └──────────────────────────────────┘    │
│                                          │
│  热门搜索 (横向滚动标签)                 │
│  [周杰伦] [林俊杰] [陈奕迅] ...          │
│                                          │
│  搜索结果 "共 42 首"                     │
│  ┌──────────────────────────────────┐    │
│  │ 歌名1 - 歌手             ▶      │    │
│  │ 歌名2 - 歌手             ▶      │    │
│  │ 歌名3 - 歌手             ▶      │    │
│  │ ...                              │    │
│  └──────────────────────────────────┘    │
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘

D-Pad 键盘弹出时：
┌──────────────────────────────────────────┐
│  ┌──────────────────────────────────┐    │
│  │  A  B  C  D  E  F  G  H  I  J   │    │
│  │  K  L  M  N  O  P  Q  R  S  T   │    │
│  │  U  V  W  X  Y  Z  .  @  -  _   │    │
│  │  空格  ←退格  清空  确定         │    │
│  └──────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

#### 歌单

```
┌──────────────────────────────────────────┐
│  歌单                 [全部 ▾ 普通 电台]  │
│                                          │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐
│  │    │ │    │ │    │ │    │ │    │ │    │
│  │封面 │ │封面 │ │封面 │ │封面 │ │封面 │ │封面 │
│  │名称 │ │名称 │ │名称 │ │名称 │ │名称 │ │名称 │
│  │ 42首│ │ 12首│ │ 8首 │ │ 25首│ │ 36首│ │ 3首 │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐
│  │    │ │    │ │    │ │    │ │    │ │    │
│  │    │ │    │ │    │ │    │ │    │ │    │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘
│  ... (6 列网格，纵向滚动，分页加载)        │
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘

说明：歌单页面复用 songloft 的 playlist API，
通过 type 过滤（全部/normal/radio）。
无"歌单广场"概念——songloft 所有歌单归属同一用户。
```

#### 我的

```
┌──────────────────────────────────────────┐
│  我的                              ⚙️ 设置│
│                                          │
│  [收藏歌曲]  [收藏电台]                   │
│  (songloft 收藏基于 built_in 歌单实现)    │
│                                          │
│  收藏歌曲                                │
│  ┌──────────────────────────────────┐    │
│  │ 1. 歌名1 - 歌手             ▶   │    │
│  │ 2. 歌名2 - 歌手             ▶   │    │
│  │ 3. 歌名3 - 歌手             ▶   │    │
│  └──────────────────────────────────┘    │
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘

说明："我的"页面聚焦收藏内容（songloft 中收藏以 built_in 标签
歌单实现）。所有歌单统一在首页/歌单 Tab 浏览，
此处不再重复展示。"我的"仅展示收藏歌曲/电台 + 设置入口。
```

#### 歌单详情

```
┌──────────────────────────────────────────┐
│  ← 歌单                                  │
│                                          │
│  ┌──────────┐                            │
│  │          │  歌单名称                  │
│  │   封面    │  歌曲数: 42               │
│  │          │  创建时间: 2026-01-01      │
│  └──────────┘  描述: ...                │
│                                          │
│  [▶ 播放全部]  [🔀 随机播放]  [☆ 收藏]  │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │ 1. 歌名1 - 歌手             ▶   │    │
│  │ 2. 歌名2 - 歌手             ▶   │    │
│  │ 3. 歌名3 - 歌手             ▶   │    │
│  │ ...                              │    │
│  └──────────────────────────────────┘    │
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘
```

#### 全屏播放器（核心页面）

```
┌──────────────────────────────────────────┐
│  (毛玻璃背景 - 封面模糊20px + 暗色叠加)   │
│                                          │
│  ┌─────────────┐  ┌──────────────────┐   │
│  │             │  │                  │   │
│  │  视频画面    │  │  歌词区域        │   │
│  │  (MV模式)   │  │  (音频模式)      │   │
│  │             │  │                  │   │
│  │  或          │  │  ...前一句      │   │
│  │             │  │  → 当前歌词      │   │
│  │  圆角封面    │  │  ...后一句      │   │
│  │  (音频模式)  │  │                  │   │
│  │             │  │  渐隐遮罩        │   │
│  └─────────────┘  └──────────────────┘   │
│                                          │
│  ┌──────────────────────────────────────┐│
│  │  ─────────●─────────── 02:30/04:00  ││
│  │  ⏮   ⏸   ⏭   🔁   🎤原唱(动态) ☰队列││
│  └──────────────────────────────────────┘│
│      🎤原唱按钮：仅 tracks.size > 1 时显示 │
│            (控制栏，10s自动隐藏)         │
└──────────────────────────────────────────┘

播放队列抽屉 (左侧滑出)：
┌──────────────┐
│ 播放列表     │ 60% 宽度
│ ─────────── │
│ 🎵 当前歌曲  │
│ 下一首       │
│ 下下一首     │
│ ...          │
│              │
│ 共 N 首      │
└──────────────┘

音轨切换菜单（点击 🎤 按钮弹出）：
```
┌──────────────┐
│ 音轨选择     │
│ ● 原唱       │
│ ○ 伴奏       │
└──────────────┘
```
菜单内容根据 song.tracks 动态生成，每个 Track 对应一个选项。
tracks.size ≤ 1 时 🎤 按钮隐藏，视频/音频均适用此规则。
```

#### 悬浮迷你播放器（全局覆盖）

```
┌──────────────────────────────────────────┐
│                                  ┌────┐  │
│                                  │ ◉  │  │ 右下角悬浮
│                                  │ 歌名 │  │ 聚焦展开到 300dp
│                                  └────┘  │
│                                          │
│  (其他页面内容)                           │
│                                          │
├──────────────────────────────────────────┤
│  🏠 首页   🔍 搜索   📋 歌单   ❤️ 我的   │
└──────────────────────────────────────────┘
```

---

## 6. 数据模型

### 6.1 Song（参考 songloft-player API）

```kotlin
data class Song(
    val id: Long,
    val type: String,        // "local", "remote", "radio"
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Double,    // 秒
    val coverUrl: String?,
    val isVideo: Boolean,    // 是否有 MV
    // 音轨列表，songloft 后端返回
    // 示例：tracks=[{id:"vocal",name:"原唱"},{id:"accompaniment",name:"伴奏"}]
    val tracks: List<Track>?,
    // 是否有多个音轨（根据 tracks 列表长度动态判断）
    val hasMultiTrack: Boolean get() = (tracks?.size ?: 0) > 1
)

data class Track(
    val id: String,          // "vocal", "accompaniment", "dual"
    val name: String,        // "原唱", "伴奏", "双音轨"
    val url: String,         // 该音轨的播放 URL
    val quality: String?
)
```

### 6.2 Playlist

```kotlin
data class Playlist(
    val id: Long,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val songCount: Int,
    val type: String,        // "normal", "radio"
    val labels: List<String>, // "built_in", "auto_created", "hidden"
    val createdAt: String,
    val updatedAt: String
)
```

### 6.3 LyricLine

```kotlin
data class LyricLine(
    val time: Long,          // 毫秒
    val text: String,
    val translation: String? = null, // tlyric
    val romaji: String? = null       // rlyric
)
```

### 6.4 PlaybackState

```kotlin
data class PlaybackState(
    val currentSong: Song?,
    val queue: List<Song>,
    val currentIndex: Int,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val currentPosition: Long,  // 毫秒
    val duration: Long,         // 毫秒
    val playMode: PlayMode,     // ORDER, LOOP, SINGLE, RANDOM
    val volume: Float,          // 0.0 - 1.0
    val sleepTimer: SleepTimer?,
    val isVideoMode: Boolean,
    val currentTrack: String?   // "vocal", "accompaniment", "dual"
)

enum class PlayMode { ORDER, LOOP, SINGLE, RANDOM }

data class SleepTimer(
    val mode: SleepMode,    // DURATION, AFTER_SONGS
    val remaining: Long?,   // 毫秒 (DURATION 模式)
    val remainingSongs: Int? // (AFTER_SONGS 模式)
)

enum class SleepMode { DURATION, AFTER_SONGS }
```

---

## 7. API 接口

对接 Songloft Go 后端 API（参考 songloft-player `lib/core/network/` 和 `lib/shared/models/`）：

| 接口 | 方法 | 用途 |
|------|------|------|
| `POST /api/v1/auth/login` | 登录 | 获取 JWT Token |
| `POST /api/v1/auth/refresh` | 刷新 Token | |
| `GET /api/v1/songs?limit=&offset=` | 歌曲列表 | 分页 |
| `GET /api/v1/songs/{id}` | 歌曲详情 | |
| `GET /api/v1/songs/{id}/play?quality= ` | 播放 URL | 音频流 |
| `GET /api/v1/songs/{id}/cover` | 封面 | |
| `GET /api/v1/songs/{id}/lyric` | 歌词 | LRC 格式 |
| `POST /api/v1/songs/{id}/played` | 播放上报 | type=play/finish/skip |
| `POST /api/v1/songs/{id}/activate` | 激活歌曲 | 取消旧预缓存 |
| `GET /api/v1/playlists` | 歌单列表 | |
| `GET /api/v1/playlists/{id}` | 歌单详情 | |
| `GET /api/v1/playlists/{id}/songs` | 歌单歌曲 | 分页 |
| `POST /api/v1/playlists` | 新建歌单 | |
| `POST /api/v1/playlists/{id}/songs` | 添加歌曲 | |
| `DELETE /api/v1/playlists/{id}/songs/{songId}` | 移除歌曲 | |
| `PUT /api/v1/playlists/{id}/songs/reorder` | 排序 | |
| `DELETE /api/v1/playlists/{id}` | 删除歌单 | |
| `GET /api/v1/songs/favorites` | 收藏歌曲 | |
| `GET /api/v1/config/{key}` | 配置读取 | |
| `PUT /api/v1/config/{key}` | 配置写入 | |
| `GET /api/v1/health` | 健康检查 | |

---

## 8. 播放器详细设计

### 8.1 视频/音频模式切换

```
歌曲数据有 isVideo=true 时：
  → 视频模式：ExoPlayer 渲染视频到 SurfaceView，封面区域变为视频画面
  → 音频模式时：显示圆角封面，右侧歌词，毛玻璃背景

音轨切换按钮不依赖视频/音频模式，独立判断：
  → 歌曲 tracks 列表长度 > 1 时：显示音轨切换按钮
  → 歌曲 tracks 列表长度 ≤ 1 时：隐藏音轨切换按钮

即：音频歌曲如果有原伴唱两轨，同样显示音轨切换按钮；
    视频 MV 如果只有单音轨，则不显示音轨切换按钮。
```

### 8.2 双音轨切换实现

```kotlin
// ExoPlayer TrackSelector 配置
val trackSelector = DefaultTrackSelector(context).apply {
    // 按音轨组选择，传入 trackId 参数
    setParameters(buildUponParameters {
        setPreferredAudioLanguage("vocal") // 或 "accompaniment"、"dual"
    })
}

// 切换方式：重新构建 MediaItem 时选择不同音轨 URL
// 方案 A：同一 MediaItem 多音轨（songloft 返回的 tracks 列表中每个都有独立 URL）
// 方案 B：通过 ExoPlayer TrackSelector 切换
// 
// 推荐方案 A：songloft 后端的 tracks 字段包含不同音轨的 URL，
// 切换时重新设置 MediaItem
```

### 8.3 控制栏自动隐藏

```kotlin
// 使用 LaunchedEffect + 计时器
var isControlsVisible by remember { mutableStateOf(true) }

LaunchedEffect(isPlaying) {
    if (isPlaying) {
        delay(10_000) // 10 秒后隐藏
        isControlsVisible = false
    }
}

// D-Pad 下键按下时重新显示
// D-Pad 左/右长按：快进/快退
// D-Pad 左/右单击：上一首/下一首
```

### 8.4 歌曲音轨切换时序

```
用户切换音轨 ↓
PlayerViewModel.setTrack("vocal" | "accompaniment" | "dual")
  ↓
暂停播放（不丢失位置）
  ↓
重新构建 MediaItem，使用新音轨 URL
  ↓
seekTo(currentPosition)
  ↓
继续播放
  ↓
更新 UI 音轨指示器
```

---

## 9. TV 焦点交互设计

### 9.1 焦点动画（参考 music-tv FocusAnimationHelper）

```kotlin
// Compose Modifier 扩展
fun Modifier.tvFocusAnimation(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(120, easing = DecelerateEasing)
    )
    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 8f else 0f,
        animationSpec = tween(100, easing = AccelerateEasing)
    )
    this
        .scale(scale)
        .shadow(elevation)
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        )
        .onFocusChanged { isFocused = it.isFocused }
}
```

### 9.2 D-Pad 按键映射

| 按键 | 播放器内行为 | 其他页面 |
|------|-------------|---------|
| DPAD_CENTER / ENTER | 确认 | 选中/确定 |
| DPAD_UP | 控制栏显示（如果隐藏）→ 焦点上移 | 焦点上移 |
| DPAD_DOWN | 焦点下移 / 控制栏隐藏 | 焦点下移 |
| DPAD_LEFT | 单击：上一首；长按：快退 | 焦点左移 |
| DPAD_RIGHT | 单击：下一首；长按：快进 | 焦点右移 |
| MENU | 打开音轨切换菜单 | 打开侧边菜单 |
| BACK | 关闭抽屉/菜单 → 退出播放器 | 返回上级 |
| MEDIA_PLAY_PAUSE | 播放/暂停 | 全局播放/暂停 |
| MEDIA_NEXT | 下一首 | 全局下一首 |
| MEDIA_PREVIOUS | 上一首 | 全局上一首 |

### 9.3 焦点管理关键场景

- **播放队列抽屉打开时**：主内容 `focusable = false`，焦点锁定在抽屉内
- **音轨切换菜单打开时**：背景内容 `focusable = false`
- **控制栏隐藏时**：DPAD_DOWN 显示控制栏并将焦点定位到进度条
- **悬浮迷你播放器**：焦点移到屏幕右下角时自动聚焦到迷你播放器

---

## 10. 主题与样式

### 10.1 配色方案（对齐 songloft-player）

Seed Color 沿用 songloft-player 的 **`#415F91`**（M3 Blue / indigo-500），所有颜色由 `ColorScheme.fromSeed` 自动生成，**不硬编码具体色值**。

```kotlin
// 主题构建（参考 songloft-player app_theme.dart）
// 亮色 / 暗色 由同一 seed 生成，仅 brightness 不同
val seedColor = Color(0xFF415F91)

val lightColorScheme = ColorScheme.fromSeed(
    seedColor = seedColor,
    brightness = Brightness.light
)

val darkColorScheme = ColorScheme.fromSeed(
    seedColor = seedColor,
    brightness = Brightness.dark
)
```

### 10.2 夜间模式切换

- 支持 **亮色 / 暗色 / 跟随系统** 三种模式
- 选择存储在 `DataStore` 中持久化
- 通过 `CompositionLocal` 或 ViewModel 驱动全局主题切换

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }

// Compose Material3 的 MaterialTheme 包裹
MaterialTheme(
    colorScheme = when (currentThemeMode) {
        ThemeMode.LIGHT -> lightColorScheme
        ThemeMode.DARK -> darkColorScheme
        ThemeMode.SYSTEM -> if (isSystemDark) darkColorScheme else lightColorScheme
    },
    typography = TvTypography,
    shapes = TvShapes,
    content = { ... }
)
```

### 10.3 TV 专属样式常量（参考 songloft-player tv_theme.dart）

```kotlin
object TvTheme {
    // 间距 (8px base grid)
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingXLarge = 48.dp

    // 卡片网格
    val gridColumns = 4              // songloft TV 使用 4 列
    val gridSpacing = 24.dp
    val contentPadding = 48.dp

    // 封面大小
    val largeCoverSize = 300.dp      // 播放器封面
    val mediumCoverSize = 200.dp     // 卡片封面

    // 圆角
    val cardRadius = 16.dp           // TV 卡片圆角（songloft TV 用 16）
    val defaultRadius = 12.dp        // 通用圆角

    // 焦点动画
    val focusBorderWidth = 4.dp
    val focusScale = 1.05f
    val focusedScaleLarge = 1.1f
    val focusShadowBlurRadius = 20.dp
    val focusGlowSpreadRadius = 4.dp
    val focusGlowOpacity = 0.4f
    val focusAnimationDuration = 150
    val focusAnimationCurve = Curves.easeOutCubic

    // 按钮
    val minButtonSize = 80.dp
    val navBarHeight = 88.dp
}
```

### 10.4 字体比例

TV 屏幕距离远，字体比手机大（参考 songloft-player TvTheme 文字样式）：

| 用途 | 字号 | 字重 | Material3 Token |
|------|------|------|-----------------|
| 页面标题 | 24sp | w600 | `headlineSmall` |
| 正文/歌手名 | 20sp | normal | `bodyLarge` |
| 辅助文字 | 16sp | normal | `bodyMedium` (onSurfaceVariant) |
| 按钮文字 | 18sp | w500 | `labelLarge` |
| 歌词（活跃行） | 30sp | Bold | 自定义 |
| 歌词（非活跃行） | 22sp | Normal | 自定义 |
| 控制栏时间 | 12sp | Monospace | 自定义 |

### 10.5 间距

| 变量 | TV 值 | 用途 |
|------|-------|------|
| `spacingSmall` | 8dp | 元素间间距 |
| `spacingMedium` | 16dp | 列表项内边距 |
| `spacingLarge` | 24dp | 网格间距 |
| `spacingXLarge` | 48dp | 页面内容边距 |
| `cardRadius` | 16dp | 歌单卡片圆角 |
| `coverRadius` | 8dp（列表）/ 54dp（播放器） | 封面圆角 |

### 10.6 阴影效果（参考 songloft-player AppShadows）

```kotlin
object TvShadows {
    val light = ListStyle(
        color = Color(0x0F000000), blur = 8.dp, offsetY = 2.dp
    )
    val medium = ListStyle(
        color = Color(0x14000000), blur = 12.dp, offsetY = 4.dp
    )
    val heavy = ListStyle(
        color = Color(0x26000000), blur = 20.dp, offsetY = 10.dp
    )
}
```

---

## 11. 项目结构

```
songloft-tv/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/songloft/tv/
│       │   ├── SongloftTvApp.kt              // Application
│       │   ├── MainActivity.kt                // 入口 Activity
│       │   ├── MusicService.kt                // MediaSessionService
│       │   │
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── SongloftApi.kt         // Retrofit 接口
│       │   │   │   ├── ApiClient.kt           // Retrofit 实例
│       │   │   │   ├── AuthInterceptor.kt     // JWT 拦截器
│       │   │   │   └── UrlHelper.kt           // URL 构建
│       │   │   ├── model/
│       │   │   │   ├── Song.kt
│       │   │   │   ├── Playlist.kt
│       │   │   │   ├── LyricLine.kt
│       │   │   │   └── ApiResponse.kt
│       │   │   ├── repository/
│       │   │   │   ├── SongRepository.kt
│       │   │   │   ├── PlaylistRepository.kt
│       │   │   │   └── AuthRepository.kt
│       │   │   └── storage/
│       │   │       └── PreferencesDataStore.kt
│       │   │
│       │   ├── domain/
│       │   │   ├── PlayerController.kt
│       │   │   ├── PlaybackState.kt
│       │   │   └── LyricParser.kt
│       │   │
│       │   ├── ui/
│       │   │   ├── navigation/
│       │   │   │   ├── TvNavigation.kt
│       │   │   │   └── Screen.kt
│       │   │   ├── theme/
│       │   │   │   ├── TvTheme.kt
│       │   │   │   └── Color.kt
│       │   │   ├── home/
│       │   │   │   └── HomeScreen.kt
│       │   │   ├── search/
│       │   │   │   ├── SearchScreen.kt
│       │   │   │   └── TvKeyboard.kt
│       │   │   ├── library/
│       │   │   │   └── LibraryScreen.kt
│       │   │   ├── playlist/
│       │   │   │   └── PlaylistDetailScreen.kt
│       │   │   ├── my/
│       │   │   │   └── MyScreen.kt
│       │   │   ├── player/
│       │   │   │   ├── PlayerActivity.kt
│       │   │   │   ├── PlayerScreen.kt
│       │   │   │   ├── PlayerViewModel.kt
│       │   │   │   ├── VideoPlayer.kt
│       │   │   │   ├── LyricsPanel.kt
│       │   │   │   ├── ControlBar.kt
│       │   │   │   └── QueueDrawer.kt
│       │   │   ├── settings/
│       │   │   │   └── SettingsScreen.kt
│       │   │   ├── config/
│       │   │   │   └── ConfigScreen.kt
│       │   │   └── components/
│       │   │       ├── TvFocusableCard.kt
│       │   │       ├── TvButton.kt
│       │   │       ├── TvBottomNav.kt
│       │   │       └── FloatingPlayerBar.kt
│       │   │
│       │   └── util/
│       │       ├── Formatter.kt
│       │       └── Constants.kt
│       │
│       └── res/
│           ├── drawable/
│           │   ├── ic_*.xml
│           │   ├── selector_*.xml
│           │   └── bg_*.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── themes.xml
│           │   └── strings.xml
│           └── xml/
│               └── network_security_config.xml
│
├── build.gradle.kts (root)
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 12. 关键技术决策

| 决策 | 方案 | 理由 |
|------|------|------|
| UI 框架 | Jetpack Compose for TV | Google 官方 TV 组件库，D-Pad 内建支持 |
| 播放器 | ExoPlayer (Media3) | 成熟稳定，视频/音频/音轨切换全支持 |
| 后台播放 | MediaSessionService | Android 标准方案，通知栏控制 |
| 图片加载 | Coil | Compose 原生集成，支持模糊变换 |
| 主题 | Material3 + `ColorScheme.fromSeed` | 复用 songloft-player 的 seed `#415F91` |
| 状态管理 | ViewModel + StateFlow | Compose 原生，生命周期感知 |
| 依赖注入 | Hilt | 官方推荐，编译期校验 |
| 歌词解析 | 自定义 LRC 解析器 | 轻量，songloft 返回标准 LRC |
| 网络层 | Retrofit + OkHttp | 稳定成熟，JWT 拦截器易实现 |

---

## 13. 实施路线图

| 阶段 | 内容 | 估算 |
|------|------|------|
| Phase 1 | 项目脚手架、主题、导航框架、服务器配置、首页歌单网格 | 1 周 |
| Phase 2 | 播放器核心（音频播放、控制栏、封面+歌词、毛玻璃背景） | 1 周 |
| Phase 3 | 视频播放、双音轨切换、播放队列 | 1 周 |
| Phase 4 | 搜索（D-Pad 键盘）、歌单详情、我的页面 | 1 周 |
| Phase 5 | 悬浮迷你播放器、设置、睡眠定时器、收藏 | 1 周 |
| Phase 6 | 焦点动画优化、edge case 处理、测试 | 1 周 |
