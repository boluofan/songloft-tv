package com.songloft.tv.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.songloft.tv.BuildConfig
import com.songloft.tv.data.api.TlsCompat
import com.songloft.tv.data.model.UpdateInfo
import com.songloft.tv.data.model.VersionJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

sealed interface DownloadState {
    data class Downloading(
        val bytesRead: Long,
        val totalBytes: Long,
        val mirrorLabel: String
    ) : DownloadState

    data class Success(val apkFile: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UpdateRepository"
        private const val VERSION_JSON_URL =
            "https://github.com/boluofan/songloft-tv/releases/latest/download/version.json"
        private const val APK_URL =
            "https://github.com/boluofan/songloft-tv/releases/latest/download/songloft-tv.apk"

        // 前缀代理池，按序回退；空串 = 直连
        private val MIRRORS = listOf(
            "" to "GitHub 直连",
            "https://ghfast.top/" to "ghfast.top",
            "https://gh-proxy.com/" to "gh-proxy.com",
            "https://ghproxy.net/" to "ghproxy.net"
        )
    }

    // 进程内启动自动检查只执行一次
    var autoCheckDone = false

    private val gson = Gson()

    private val checkClient: OkHttpClient = TlsCompat.apply(OkHttpClient.Builder())
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val downloadClient: OkHttpClient = TlsCompat.apply(OkHttpClient.Builder())
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun checkUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        for ((prefix, label) in MIRRORS) {
            currentCoroutineContext().ensureActive()
            try {
                val url = prefix + VERSION_JSON_URL
                Log.i(TAG, "检查更新: $url")
                val request = Request.Builder().url(url).build()
                checkClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val json = gson.fromJson(response.body!!.string(), VersionJson::class.java)
                    return@withContext evaluate(json)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "检查更新失败（$label）", e)
            }
        }
        UpdateCheckResult.Failed("检查更新失败，网络不可用！")
    }

    private fun evaluate(json: VersionJson): UpdateCheckResult {
        val remoteCode = json.versionCode
        if (remoteCode != null) {
            return if (remoteCode > BuildConfig.VERSION_CODE) {
                UpdateCheckResult.UpdateAvailable(
                    UpdateInfo(
                        versionCode = remoteCode,
                        versionName = json.version ?: remoteCode.toString(),
                        apkUrl = APK_URL
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        }

        // 旧 Release 无 version_code，回退到版本名（semver）比较
        val remoteName = json.version ?: return UpdateCheckResult.UpToDate
        val remote = parseSemver(remoteName) ?: return UpdateCheckResult.UpToDate
        val local = parseSemver(BuildConfig.VERSION_NAME) ?: return UpdateCheckResult.UpToDate
        return if (compareSemver(remote, local) > 0) {
            UpdateCheckResult.UpdateAvailable(
                UpdateInfo(
                    // 合成版本号，保证忽略过滤与缓存文件命名可用
                    versionCode = remote[0] * 1_000_000 + remote[1] * 1_000 + remote[2],
                    versionName = remoteName,
                    apkUrl = APK_URL
                )
            )
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    private fun parseSemver(name: String): List<Int>? {
        val parts = name.trim().removePrefix("v").split(".")
        if (parts.isEmpty() || parts.size > 3) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        return List(3) { nums.getOrElse(it) { 0 } }
    }

    private fun compareSemver(a: List<Int>, b: List<Int>): Int {
        for (i in 0..2) {
            if (a[i] != b[i]) return a[i] - b[i]
        }
        return 0
    }

    fun downloadApk(info: UpdateInfo): Flow<DownloadState> = flow {
        val dir = File(context.cacheDir, "updates")
        val target = File(dir, "songloft-tv-${info.versionCode}.apk")

        for ((prefix, label) in MIRRORS) {
            currentCoroutineContext().ensureActive()
            try {
                downloadFrom(prefix + info.apkUrl, label, dir, target)
                emit(DownloadState.Success(target))
                return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "下载失败（$label）", e)
            }
        }
        emit(DownloadState.Failed("网络不可用，请手动下载安装！"))
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<DownloadState>.downloadFrom(
        url: String,
        label: String,
        dir: File,
        target: File
    ) {
        val call = downloadClient.newCall(Request.Builder().url(url).build())
        Log.i(TAG, "下载 APK: $url")
        val handle = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("响应体为空")
                val total = body.contentLength()

                // 命中已完整下载的文件直接复用
                if (target.exists() && total > 0 && target.length() == total) {
                    return
                }

                dir.mkdirs()
                dir.listFiles()?.forEach { it.delete() }
                val tmp = File(dir, target.name + ".tmp")

                var read = 0L
                emit(DownloadState.Downloading(0L, total, label))
                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var lastEmit = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            // 每 256KB 上报一次进度，避免高频重组
                            if (read - lastEmit >= 256 * 1024) {
                                lastEmit = read
                                emit(DownloadState.Downloading(read, total, label))
                            }
                        }
                    }
                }
                if (total > 0 && read != total) throw IOException("下载不完整：$read/$total")
                if (!tmp.renameTo(target)) throw IOException("重命名失败")
                emit(DownloadState.Downloading(read, total, label))
            }
        } finally {
            handle.dispose()
        }
    }
}
