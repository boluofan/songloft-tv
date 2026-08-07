package com.songloft.tv.data.cache

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.File

private const val TAG = "PlaybackCache"

/** 播放缓存目录与清理入口；目录专用，勿存放其他文件 */
object PlaybackCache {
    fun dir(context: Context): File = File(context.cacheDir, "play_cache")

    fun clear(context: Context) {
        dir(context).deleteRecursively()
    }

    fun usage(context: Context): Long =
        dir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

/**
 * 按 URL 路由的数据源：m3u8 直播清单走纯流式（缓存会让 live 清单永久命中旧版本导致播放卡死），
 * 其余资源（音频/视频/HLS 分片）走 CacheDataSource
 */
class RoutingDataSource(
    private val cacheSource: DataSource,
    private val streamSource: DataSource,
    private val onCacheLoadStarted: () -> Unit = {}
) : DataSource {
    private var current: DataSource = streamSource

    override fun open(dataSpec: DataSpec): Long {
        current = if (isPlaylist(dataSpec.uri)) {
            Log.i(TAG, "m3u8 清单不走缓存：${dataSpec.uri}")
            streamSource
        } else {
            Log.i(TAG, "开始加载（走缓存）：${dataSpec.uri}")
            onCacheLoadStarted()
            cacheSource
        }
        return current.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        current.read(buffer, offset, length)

    override fun close() = current.close()

    override fun getUri(): Uri? = current.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current.responseHeaders

    override fun addTransferListener(transferListener: TransferListener) {
        cacheSource.addTransferListener(transferListener)
        streamSource.addTransferListener(transferListener)
    }

    private fun isPlaylist(uri: Uri): Boolean =
        uri.lastPathSegment?.endsWith(".m3u8", ignoreCase = true) == true
}
