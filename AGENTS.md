# AGENTS.md

Songloft TV — [Songloft](https://github.com/songloft-org/songloft) 音乐服务器的 Android TV 客户端（Kotlin + Jetpack Compose for TV）。

## 常用命令

```bash
./gradlew assembleDebug      # 构建 Debug APK
./gradlew assembleRelease    # 构建 Release APK（签名走环境变量，缺省用 debug 签名）
./gradlew :app:compileDebugKotlin   # 快速编译检查
```

- JDK 17；Gradle 8.10（用 wrapper，勿升级）
- 无单元测试目录，验证以编译通过 + 真机/模拟器运行为准
- release lint 已关闭（CI 上 lint 崩溃，见 app/build.gradle.kts 注释），勿重新开启

## 架构

单模块 `:app`，包根 `com.songloft.tv`，标准三层：

- `data/api/` — Retrofit 接口（`SongloftApi`）、`AuthInterceptor`（附加 access token）、`TokenAuthenticator`（401 时用 refresh token 刷新）、`UrlHelper`（拼接服务器资源 URL）
- `data/cache/` — 播放缓存（`PlaybackCache` 目录管理 + `RoutingDataSource` 按 URL 路由 m3u8 清单走纯流式）；缓存大小存 DataStore，MusicService 启动时建 `SimpleCache`（LRU 淘汰）
- `data/repository/` — 各业务仓库，UI 层不直接碰 API
- `data/storage/PreferencesDataStore.kt` — DataStore 持久化（服务器地址、Token、设置项）
- `data/config/ConfigWebServer.kt` — NanoHTTPD 内置 HTTP 服务，配合 ZXing 二维码实现手机扫码配置
- `domain/PlayerController.kt` — 播放队列/模式/双音轨切换的唯一入口；内部经 Media3 `MediaController` 连接 MusicService，**ExoPlayer 实例只在 MusicService 中创建**
- `MusicService.kt` — MediaSessionService，ExoPlayer 宿主，后台播放；缓存初始化与 eq/sfx/cache 自定义命令
- `ui/<feature>/` — 每个功能目录含 Screen + ViewModel（Hilt 注入，StateFlow 暴露状态）
- 主界面**不用 Navigation Compose**：`MainActivity.TvApp` 用 `mutableStateOf<Screen>` 手写状态导航（路由定义在 `ui/navigation/Screen.kt`）；全屏播放器是独立 `PlayerActivity`；遥控器返回键走 `ui/navigation/BackToTop.kt` 三段式（回顶 → 聚焦顶部 → 底部 Tab 栏）

实现细节详见 `doc/implementation.md`（API 清单、认证刷新流程、双音轨机制、各页面实现、已知遗留问题）。

## 约定

- 依赖统一在 `gradle/libs.versions.toml` 声明，KSP 处理 Hilt
- UI 优先使用 `androidx.tv` 组件；所有可交互元素必须支持 D-Pad 焦点（参考 `ui/components/TvFocusable.kt`）
- API 响应用具体数据类（`SongListResponse` 等，定义在 `SongloftApi.kt`）；Retrofit 请求体也用具体数据类，不要用 Map/通配符泛型（曾引发运行时异常，见 16a910f）
- 电台/流媒体播放用服务端返回的 m3u8 地址（HLS），依赖 `media3-exoplayer-hls`
- 提交信息为中文，格式 `feat:` / `fix:` / `chore:` 前缀（参考 git log）；冒号必须半角，否则 CI changelog 无法识别。克隆仓库后执行 `git config core.hooksPath .githooks` 启用 commit-msg hook 自动纠正全角冒号
- 文档（doc/、README）用中文维护

## 版本发布

- 版本号只改 `app/build.gradle.kts` 的 `versionName` / `versionCode`，用 `./scripts/bump-version.sh [release|major|minor|patch]` 完成改版 + tag + push
- push `v*` tag 触发 `.github/workflows/build-and-release.yml` 构建并发布 Release；push main 自动发 dev 预发布包
- 不要手动 push tag 或修改 CI，除非用户明确要求
