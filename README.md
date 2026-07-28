# Songloft TV

[Songloft](https://github.com/songloft-org/songloft) 音乐服务器的 Android TV 客户端。面向电视大屏场景，提供简洁、沉浸的音乐和 MV 播放体验。

## 功能特性

- **服务器配置**：手动输入地址或手机扫码配置登录（JWT 双 Token 认证）
- **首页概览**：统计卡片、我的歌单、主要歌手/专辑、年份速览
- **搜索**：D-Pad 全屏自定义键盘，搜索结果可直接播放/收藏
- **歌单浏览**：网格歌单，分页加载，支持类型过滤
- **我的**：收藏歌曲、收藏电台（HLS 播放）
- **全屏播放器**：
  - MV 视频播放 + 音频封面/歌词模式
  - 原唱/伴奏双音轨切换
  - LRC 同步歌词滚动
  - 毛玻璃封面背景、自动隐藏控制栏
  - 播放模式切换（顺序/列表循环/单曲循环/随机）、播放队列抽屉
- **悬浮迷你播放器**：旋转封面 + 歌名，全页面悬浮
- **设置**：主题模式、音质选择、后台播放、睡眠定时器、日志导出、关于

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

- minSdk 23（Android 6.0）/ targetSdk 35
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

## License

[Apache License 2.0](LICENSE)
