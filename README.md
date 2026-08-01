# Songloft TV

[Songloft](https://github.com/songloft-org/songloft) 音乐服务器的 Android TV 客户端。面向电视大屏场景，提供简洁、沉浸的音乐和 MV 播放体验。

> 系统要求：Android 5.0（API 21）及以上，兼容老款电视/盒子设备。

## 功能特性

- **服务器配置**：手动输入地址或手机扫码配置登录（JWT 双 Token 认证）
- **首页概览**：统计卡片、我的歌单、主要歌手/专辑、年份速览（装有[播放统计插件](https://github.com/songloft-org/songloft-plugin-stats)时自动切换为播放统计概览：全部/今日/本周/本月）
- **播放统计**：全部/今日/本周/本月概览卡、艺术家/歌曲/专辑排行、听歌趋势（7/30 天）、时段分布、来源分布、最近播放；数据来自服务器端[播放统计插件](https://github.com/songloft-org/songloft-plugin-stats)，未安装插件时自动隐藏
- **搜索**：6×6 方阵 TV 键盘（缩短遥控器移动距离），支持手机扫码远程输入关键字，热门搜索推荐
- **歌单浏览**：网格歌单，分页加载，支持类型过滤
- **我的**：收藏歌曲、收藏电台（HLS 播放）
- **全屏播放器**：
  - MV 视频播放 + 音频封面/歌词模式
  - 原唱/伴奏双音轨切换
  - LRC 同步歌词滚动，支持逐字卡拉OK高亮与翻译歌词
  - 毛玻璃封面背景、自动隐藏控制栏
  - 播放模式切换（顺序/列表循环/单曲循环/随机）、播放队列抽屉
- **悬浮迷你播放器**：旋转封面 + 歌名，全页面悬浮
- **设置**：主题模式、音质选择、后台播放、睡眠定时器、日志导出（网页下载）、问题反馈、操作说明、关于
- **遥控体验**：全局 D-Pad 焦点导航、退出二次确认、二级页面“从哪儿来回哪儿去”返回逻辑

设计与规划详见 [doc/design.md](doc/design.md) 和 [doc/plan.md](doc/plan.md)，代码实现细节详见 [doc/implementation.md](doc/implementation.md)。

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| 语言 | Kotlin 2.1 |
| UI | Jetpack Compose for TV（`androidx.tv:tv-material` / `tv-foundation`） |
| 播放器 | Media3 ExoPlayer（含 HLS），MediaSessionService 后台播放 |
| 网络 | Retrofit + OkHttp |
| 图片 | Coil |
| DI | Hilt（KSP） |
| 存储 | DataStore Preferences |
| 扫码配置 | ZXing 生成二维码 + NanoHTTPD 内置配置服务 |

- minSdk 21（Android 5.0）/ targetSdk 35
- JDK 17，Gradle 8.10

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（无签名环境变量时使用 debug 签名）
./gradlew assembleRelease
```

Release 签名通过环境变量注入：

| 变量 | 说明 |
|------|------|
| `ANDROID_KEYSTORE_PATH` | keystore 文件路径 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | 密钥密码 |

安装到 TV 设备：

```bash
adb connect <TV_IP>:5555
adb install app/build/outputs/apk/release/app-release.apk
```

## 发布

推送 `v*` tag 后由 GitHub Actions（`.github/workflows/build-and-release.yml`）自动构建 APK 并创建 Release；push 到 `main` 会自动发布 dev 预发布包。

```bash
./scripts/bump-version.sh patch    # 补丁版本
./scripts/bump-version.sh minor    # 次版本
./scripts/bump-version.sh release  # 去掉预发布后缀
```

## 项目结构

```
app/src/main/java/com/songloft/tv/
├── data/
│   ├── api/          # Retrofit API、鉴权拦截器、Token 刷新
│   ├── config/       # 扫码配置内置 Web 服务
│   ├── model/        # 数据模型
│   ├── repository/   # 数据仓库
│   └── storage/      # DataStore 持久化
├── domain/           # 播放控制、歌词解析
├── ui/
│   ├── home/         # 首页概览
│   ├── stats/        # 播放统计（服务器播放统计插件数据）
│   ├── search/       # 搜索 + TV 键盘
│   ├── library/      # 歌手/专辑/年份浏览
│   ├── playlist/     # 歌单列表与详情
│   ├── my/           # 我的收藏
│   ├── player/       # 全屏播放器
│   ├── settings/     # 设置
│   ├── config/       # 服务器配置/登录
│   ├── components/   # 通用组件（悬浮播放器等）
│   ├── navigation/   # 导航
│   └── theme/        # 主题
├── MainActivity.kt
├── MusicService.kt   # MediaSessionService 后台播放
└── SongloftTvApp.kt
```

## 致谢

本项目在设计与实现过程中参考了以下优秀项目（详见 [doc/design.md](doc/design.md)）：

| 项目 | 参考内容                     |
|------|--------------------------|
| [songloft](https://github.com/songloft-org/songloft) | 主程序，音乐服务器后端，提供全部 API 接口  |
| [songloft-player](https://github.com/songloft-org/songloft-player) | API 接口定义、数据模型、功能逻辑、主题与样式 |
| [music-tv](https://github.com/boluofan/music-tv) | TV 原生 UI 布局、焦点交互、沉浸播放器模式 |
| [songloft-library-plus](https://github.com/charce526/songloft-library-plus) | 【首页】概览功能与布局                |
| [songloft-plugin-stats](https://github.com/songloft-org/songloft-plugin-stats) | 【播放统计】统计接口定义、数据模型与统计页签布局 |

感谢以上项目的开源贡献。

## License

[Apache License 2.0](LICENSE)
