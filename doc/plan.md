# Songloft TV — 实施计划

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 创建 Songloft Android TV 客户端，从零搭建项目脚手架到完整功能实现

**架构：** 原生 Android + Jetpack Compose for TV，ExoPlayer(Media3) 播放器，Retrofit 网络层，Material3 主题

**技术栈：** Kotlin, Jetpack Compose for TV, ExoPlayer(Media3), Retrofit + OkHttp, Coil, Hilt, DataStore

---

## Phase 1：项目脚手架与基础设施

### Task 1.1：创建 Android 项目结构

**文件：**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1：创建根 build.gradle.kts**

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 2：创建 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "songloft-tv"
include(":app")
```

- [ ] **Step 3：创建版本目录 gradle/libs.versions.toml**

```toml
[versions]
agp = "8.7.0"
kotlin = "2.1.0"
compose-bom = "2025.01.00"
tv-compose = "1.0.0"
hilt = "2.51"
ksp = "2.1.0-1.0.29"
retrofit = "2.9.0"
okhttp = "4.12.0"
coil = "2.6.0"
media3 = "1.5.0"
datastore = "1.1.1"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

# TV Compose
tv-foundation = { group = "androidx.tv", name = "tv-foundation", version.ref = "tv-compose" }
tv-material = { group = "androidx.tv", name = "tv-material", version.ref = "tv-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Media3 (ExoPlayer)
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }

# Image
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# DataStore
datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Lifecycle
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.0" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4：创建 app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.songloft.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.songloft.tv"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    // TV Compose
    implementation(libs.tv.foundation)
    implementation(libs.tv.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Image
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
}
```

- [ ] **Step 5：创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    <uses-feature android:name="android.software.leanback" android:required="true" />

    <application
        android:name=".SongloftTvApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SongloftTv"
        android:networkSecurityConfig="@xml/network_security_config">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.player.PlayerActivity"
            android:exported="false"
            android:screenOrientation="landscape" />

        <service
            android:name=".MusicService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

- [ ] **Step 6：创建基础资源文件**

```xml
<!-- app/src/main/res/values/strings.xml -->
<resources>
    <string name="app_name">Songloft TV</string>
</resources>

<!-- app/src/main/res/values/themes.xml -->
<resources>
    <style name="Theme.SongloftTv" parent="android:Theme.NoTitleBar.Fullscreen">
        <item name="android:windowBackground">@color/background</item>
    </style>
</resources>

<!-- app/src/main/res/values/colors.xml -->
<resources>
    <color name="background">#FF111827</color>
</resources>

<!-- app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

- [ ] **Step 7：创建 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

---

### Task 1.2：应用入口 + 主题 + Hilt

**文件：**
- Create: `app/src/main/java/com/songloft/tv/SongloftTvApp.kt`
- Create: `app/src/main/java/com/songloft/tv/MainActivity.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/theme/TvTheme.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/theme/Color.kt`

- [ ] **Step 1：创建 Application 类**

```kotlin
// SongloftTvApp.kt
package com.songloft.tv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SongloftTvApp : Application()
```

- [ ] **Step 2：创建主题 Color.kt**

```kotlin
// Color.kt
package com.songloft.tv.ui.theme

import androidx.compose.ui.graphics.Color

// Seed color 对齐 songloft-player (#415F91)
val Seed = Color(0xFF415F91)
```

- [ ] **Step 3：创建 TvTheme.kt**

```kotlin
// TvTheme.kt
package com.songloft.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme.fromSeed(seed = Seed)
private val DarkColorScheme = darkColorScheme.fromSeed(seed = Seed)

@Composable
fun TvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 4：创建 MainActivity**

```kotlin
// MainActivity.kt
package com.songloft.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.Surface
import com.songloft.tv.ui.theme.TvTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TvApp()
                }
            }
        }
    }
}

@Composable
fun TvApp() {
    // Placeholder - will be replaced with navigation
}
```

---

### Task 1.3：数据模型

**文件：**
- Create: `app/src/main/java/com/songloft/tv/data/model/Song.kt`
- Create: `app/src/main/java/com/songloft/tv/data/model/Playlist.kt`
- Create: `app/src/main/java/com/songloft/tv/data/model/LyricLine.kt`
- Create: `app/src/main/java/com/songloft/tv/data/model/ApiResponse.kt`

- [ ] **Step 1：创建 Song 模型**

```kotlin
// Song.kt
package com.songloft.tv.data.model

import com.google.gson.annotations.SerializedName

data class Song(
    val id: Long,
    val type: String,         // "local", "remote", "radio"
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Double,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("is_video") val isVideo: Boolean,
    val tracks: List<Track>? = null
) {
    val hasMultiTrack: Boolean get() = (tracks?.size ?: 0) > 1
}

data class Track(
    val id: String,           // "vocal", "accompaniment", "dual"
    val name: String,         // "原唱", "伴奏", "双音轨"
    val url: String,
    val quality: String?
)

data class FacetItem(
    val value: String,
    val count: Int,
    @SerializedName("cover_url") val coverUrl: String?
)
```

- [ ] **Step 2：创建 Playlist 模型**

```kotlin
// Playlist.kt
package com.songloft.tv.data.model

import com.google.gson.annotations.SerializedName

data class Playlist(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("song_count") val songCount: Int,
    val type: String,
    val labels: List<String> = emptyList()
) {
    val isBuiltIn: Boolean get() = labels.contains("built_in")
}
```

- [ ] **Step 3：创建 LyricLine 模型**

```kotlin
// LyricLine.kt
package com.songloft.tv.data.model

data class LyricLine(
    val time: Long,      // 毫秒
    val text: String,
    val translation: String? = null,
    val romaji: String? = null
)
```

- [ ] **Step 4：创建 ApiResponse**

```kotlin
// ApiResponse.kt
package com.songloft.tv.data.model

data class ApiResponse<T>(
    val data: T?,
    val error: String?,
    val detail: String?
) {
    val isSuccess: Boolean get() = error == null && data != null
}

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
```

---

### Task 1.4：API 网络层

**文件：**
- Create: `app/src/main/java/com/songloft/tv/data/api/SongloftApi.kt`
- Create: `app/src/main/java/com/songloft/tv/data/api/ApiClient.kt`
- Create: `app/src/main/java/com/songloft/tv/data/api/AuthInterceptor.kt`
- Create: `app/src/main/java/com/songloft/tv/data/api/UrlHelper.kt`

- [ ] **Step 1：创建 Retrofit API 接口**

```kotlin
// SongloftApi.kt
package com.songloft.tv.data.api

import com.songloft.tv.data.model.*
import retrofit2.http.*

interface SongloftApi {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): ApiResponse<LoginData>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): ApiResponse<LoginData>

    // Songs
    @GET("songs")
    suspend fun getSongs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<PaginatedResponse<Song>>

    @GET("songs/{id}/play")
    suspend fun getSongPlayUrl(
        @Path("id") id: Long,
        @Query("quality") quality: String? = null
    ): ApiResponse<SongPlayInfo>

    @GET("songs/{id}/lyric")
    suspend fun getSongLyric(@Path("id") id: Long): ApiResponse<String>

    @GET("songs/facets")
    suspend fun getFacets(
        @Query("field") field: String  // "artist", "album", "genre", "year"
    ): ApiResponse<List<FacetItem>>

    @POST("songs/{id}/played")
    suspend fun reportPlayed(
        @Path("id") id: Long,
        @Query("type") type: String    // "play", "finish", "skip"
    ): ApiResponse<Unit>

    // Playlists
    @GET("playlists")
    suspend fun getPlaylists(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50
    ): ApiResponse<PaginatedResponse<Playlist>>

    @GET("playlists/{id}")
    suspend fun getPlaylistDetail(@Path("id") id: Long): ApiResponse<Playlist>

    @GET("playlists/{id}/songs")
    suspend fun getPlaylistSongs(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ApiResponse<PaginatedResponse<Song>>

    @POST("playlists/{id}/songs")
    suspend fun addSongsToPlaylist(
        @Path("id") id: Long,
        @Body body: Map<String, List<Long>>
    ): ApiResponse<Unit>

    @DELETE("playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") id: Long,
        @Path("songId") songId: Long
    ): ApiResponse<Unit>

    // Config
    @GET("config/{key}")
    suspend fun getConfig(@Path("key") key: String): ApiResponse<String>

    @PUT("config/{key}")
    suspend fun setConfig(
        @Path("key") key: String,
        @Body body: Map<String, String>
    ): ApiResponse<Unit>

    // Health
    @GET("health")
    suspend fun health(): Map<String, Any>
}

data class LoginData(
    val token: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class SongPlayInfo(
    val url: String,
    val quality: String? = null
)
```

- [ ] **Step 2：创建 AuthInterceptor**

```kotlin
// AuthInterceptor.kt
package com.songloft.tv.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    var accessToken: String? = null
    var refreshToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        accessToken?.let { token ->
            request.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(request.build())
    }
}
```

- [ ] **Step 3：创建 ApiClient**

```kotlin
// ApiClient.kt
package com.songloft.tv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var baseUrl: String = ""
    private var retrofit: Retrofit? = null
    private var api: SongloftApi? = null
    val authInterceptor = AuthInterceptor()

    fun init(url: String) {
        if (url == baseUrl && retrofit != null) return
        baseUrl = url

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val apiUrl = if (url.endsWith("/")) "${url}api/v1/" else "$url/api/v1/"

        retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit?.create(SongloftApi::class.java)
    }

    fun getApi(): SongloftApi = api ?: throw IllegalStateException("ApiClient not initialized")
}
```

- [ ] **Step 4：创建 UrlHelper**

```kotlin
// UrlHelper.kt
package com.songloft.tv.data.api

object UrlHelper {
    private var baseUrl: String = ""

    fun init(url: String) {
        baseUrl = url
    }

    fun songPlayUrl(songId: Long, quality: String? = null, track: String? = null): String {
        val sb = StringBuilder("${baseUrl}api/v1/songs/$songId/play")
        val params = mutableListOf<String>()
        quality?.let { params.add("quality=$it") }
        track?.let { params.add("track=$it") }
        if (params.isNotEmpty()) sb.append("?").append(params.joinToString("&"))
        return sb.toString()
    }

    fun songCoverUrl(songId: Long): String = "${baseUrl}api/v1/songs/$songId/cover"

    fun playlistCoverUrl(playlistId: Long): String = "${baseUrl}api/v1/playlists/$playlistId/cover"
}
```

---

### Task 1.5：本地持久化 (DataStore)

**文件：**
- Create: `app/src/main/java/com/songloft/tv/data/storage/PreferencesDataStore.kt`

- [ ] **Step 1：创建 DataStore 管理类**

```kotlin
// PreferencesDataStore.kt
package com.songloft.tv.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesDataStore(private val context: Context) {

    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val THEME_MODE = intPreferencesKey("theme_mode")  // 0=system, 1=light, 2=dark
        private val QUALITY = stringPreferencesKey("audio_quality")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: 0 }
    val quality: Flow<String?> = context.dataStore.data.map { it[QUALITY] }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setQuality(quality: String) {
        context.dataStore.edit { it[QUALITY] = quality }
    }
}
```

---

### Task 1.6：底部导航框架

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/navigation/TvBottomNav.kt`
- Modify: `app/src/main/java/com/songloft/tv/MainActivity.kt`（替换 TvApp）

- [ ] **Step 1：创建 Screen 路由定义**

```kotlin
// Screen.kt
package com.songloft.tv.ui.navigation

sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Home : Screen("home", "首页", "home")
    data object Search : Screen("search", "搜索", "search")
    data object Playlists : Screen("playlists", "歌单", "playlist")
    data object My : Screen("my", "我的", "favorite")
}
```

- [ ] **Step 2：创建底部导航栏组件**

```kotlin
// TvBottomNav.kt
package com.songloft.tv.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TvBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val screens = listOf(Screen.Home, Screen.Search, Screen.Playlists, Screen.My)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        screens.forEach { screen ->
            val isSelected = currentScreen == screen
            Text(
                text = screen.label,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .then(if (isSelected) Modifier.clip(RoundedCornerShape(8.dp)) else Modifier)
                    .focusable()
                    .onFocusChanged { /* focus animation */ }
            )
        }
    }
}
```

- [ ] **Step 3：更新 MainActivity TvApp**

```kotlin
// MainActivity.kt - 替换 TvApp()
@Composable
fun TvApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            TvBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen()
                Screen.Search -> SearchScreen()
                Screen.Playlists -> PlaylistsScreen()
                Screen.My -> MyScreen()
            }
        }
    }
}
```

---

## Phase 2：首页（音乐库概览）

### Task 2.1：首页 ViewModel + Repository

**文件：**
- Create: `app/src/main/java/com/songloft/tv/data/repository/SongRepository.kt`
- Create: `app/src/main/java/com/songloft/tv/data/repository/PlaylistRepository.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/home/HomeViewModel.kt`

- [ ] **Step 1：创建 SongRepository**

```kotlin
// SongRepository.kt
package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getFacets(field: String): Result<List<FacetItem>> = runCatching {
        val response = api.getFacets(field)
        if (response.isSuccess) response.data!!
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getSongs(limit: Int = 50, offset: Int = 0): Result<List<Song>> = runCatching {
        val response = api.getSongs(limit, offset)
        if (response.isSuccess) response.data!!.items
        else throw Exception(response.error ?: "Unknown error")
    }
}
```

- [ ] **Step 2：创建 PlaylistRepository**

```kotlin
// PlaylistRepository.kt
package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.model.Playlist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getPlaylists(type: String? = null, limit: Int = 20): Result<List<Playlist>> = runCatching {
        val response = api.getPlaylists(type, limit)
        if (response.isSuccess) response.data!!.items
        else throw Exception(response.error ?: "Unknown error")
    }
}
```

- [ ] **Step 3：创建 HomeViewModel**

```kotlin
// HomeViewModel.kt
package com.songloft.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.repository.PlaylistRepository
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalSongs: Int = 0,
    val localSongs: Int = 0,
    val totalDuration: String = "",
    val totalSize: String = "",
    val topArtists: List<FacetItem> = emptyList(),
    val topAlbums: List<FacetItem> = emptyList(),
    val topYears: List<FacetItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 并行加载
            val artistsDeferred = async { songRepository.getFacets("artist") }
            val albumsDeferred = async { songRepository.getFacets("album") }
            val genresDeferred = async { songRepository.getFacets("genre") }
            val yearsDeferred = async { songRepository.getFacets("year") }
            val songsDeferred = async { songRepository.getSongs(limit = 1) }
            val playlistsDeferred = async { playlistRepository.getPlaylists() }

            val artists = artistsDeferred.await().getOrDefault(emptyList())
            val albums = albumsDeferred.await().getOrDefault(emptyList())
            val years = yearsDeferred.await().getOrDefault(emptyList())
            val songs = songsDeferred.await().getOrDefault(emptyList())
            val playlists = playlistsDeferred.await().getOrDefault(emptyList())

            val totalSongs = songs.size // 需要实际 total 字段
            val localSongs = songs.count { it.type == "local" }

            _uiState.value = HomeUiState(
                totalSongs = totalSongs,
                localSongs = localSongs,
                topArtists = artists.take(6),
                topAlbums = albums.take(6),
                topYears = years.take(8),
                playlists = playlists.take(8),
                isLoading = false
            )
        }
    }
}
```

### Task 2.2：首页 HomeScreen

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/home/HomeScreen.kt`

- [ ] **Step 1：创建 HomeScreen**

```kotlin
// HomeScreen.kt
package com.songloft.tv.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlaylistClick: (Long) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onYearClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 统计卡片行
        item { StatsRow(uiState) }

        // 我的歌单
        item { PlaylistSection(uiState.playlists, onPlaylistClick) }

        // 主要歌手 + 主要专辑（两列）
        item { ArtistsAlbumsRow(uiState.topArtists, uiState.topAlbums, onArtistClick, onAlbumClick) }

        // 年份速览
        item { YearSection(uiState.topYears, onYearClick) }
    }
}
```

- [ ] **Step 2：创建统计卡片行 StatsRow（略，后续实现具体组件）**
- [ ] **Step 3：创建歌单网格 PlaylistSection（略）**
- [ ] **Step 4：创建歌手/专辑两列布局 ArtistsAlbumsRow（略）**
- [ ] **Step 5：创建年份速览 YearSection（略）**

---

## Phase 3：搜索 + D-Pad 键盘

### Task 3.1：搜索页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/search/SearchScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/search/TvKeyboard.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/search/SearchViewModel.kt`

（后续展开具体实现）

---

## Phase 4：歌单浏览 + 歌单详情

### Task 4.1：歌单列表页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/playlist/PlaylistsScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/playlist/PlaylistDetailScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/playlist/PlaylistViewModel.kt`

---

## Phase 5：播放器（核心）

### Task 5.1：MusicService (MediaSessionService)

**文件：**
- Create: `app/src/main/java/com/songloft/tv/MusicService.kt`
- Create: `app/src/main/java/com/songloft/tv/domain/PlayerController.kt`

### Task 5.2：播放器页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/player/PlayerActivity.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/PlayerScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/PlayerViewModel.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/ControlBar.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/LyricsPanel.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/VideoPlayer.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/player/QueueDrawer.kt`
- Create: `app/src/main/java/com/songloft/tv/domain/LyricParser.kt`

---

## Phase 6：我的页面 + 设置

### Task 6.1：我的页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/my/MyScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/my/MyViewModel.kt`

### Task 6.2：设置页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/settings/SettingsViewModel.kt`

---

## Phase 7：服务器配置

### Task 7.1：配置页面

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/config/ConfigScreen.kt`
- Create: `app/src/main/java/com/songloft/tv/ui/config/ConfigViewModel.kt`

---

## Phase 8：悬浮迷你播放器

### Task 8.1：FloatingPlayerBar

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/components/FloatingPlayerBar.kt`

---

## Phase 9：焦点动画 + 打磨

### Task 9.1：焦点效果

**文件：**
- Create: `app/src/main/java/com/songloft/tv/ui/components/TvFocusableCard.kt`

---

## 执行顺序

```
Phase 1 (项目脚手架) → Phase 2 (首页) → Phase 7 (服务器配置) →
Phase 5 (播放器核心) → Phase 3 (搜索) → Phase 4 (歌单) →
Phase 6 (我的+设置) → Phase 8 (迷你播放器) → Phase 9 (打磨)
```
