package com.songloft.tv.data.api

import android.os.Build

object UrlHelper {
    private var baseUrl: String = ""

    // Android 5（API 21/22）系统无 FLAC 解码器，强制 format=mp3 让服务端转码
    // （服务端对 mp3 源直接透传，FLAC/WAV 等转为 mp3，见 songloft GetSongPlay）
    // 注：官方保证系统自带 FLAC 解码器是 API 27 起，API 23~26 不保证（实际多数固件带了）；
    // 若此类设备反馈播不了 FLAC，把阈值改成 SDK_INT < 27 (O_MR1) 即可
    private val needsLegacyTranscode: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M

    fun initialize(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun songPlayUrl(
        songId: Long,
        transcodeFormat: String? = null,
        track: String? = null,
        isVideo: Boolean = false,
        sourceFormat: String? = null
    ): String {
        val sb = StringBuilder("${baseUrl}/api/v1/songs/$songId/play")
        val params = mutableListOf<String>()
        when {
            isVideo -> {
                // 视频文件只带 media=video：服务端据此原容器透传（不转码），
                // 保留视频轨与内嵌多音轨（原唱/伴奏），quality/format 会触发 ffmpeg -vn 丢轨
                params.add("media=video")
            }
            isMultiTrackContainer(sourceFormat) -> {
                // mka 多音轨容器不带任何转码参数直出原容器（对齐 songloft-player 原生端），
                // ExoPlayer 的 MatroskaExtractor 可枚举全部音轨供原唱/伴奏切换
            }
            else -> {
                track?.let { params.add("track=$it") }
                // 服务端 format= 才是容器格式参数（quality= 只认 128/192/320 比特率）；
                // Android 5 强制 mp3 覆盖用户选择（系统解不了 FLAC）
                val format = if (needsLegacyTranscode) "mp3"
                else transcodeFormat?.takeIf { it.isNotBlank() }
                format?.let { params.add("format=$it") }
            }
        }
        if (params.isNotEmpty()) sb.append("?").append(params.joinToString("&"))
        return sb.toString()
    }

    private fun isMultiTrackContainer(format: String?): Boolean =
        format?.lowercase() == "mka"

    fun songCoverUrl(songId: Long): String = "${baseUrl}/api/v1/songs/$songId/cover"

    fun playlistCoverUrl(playlistId: Long): String = "${baseUrl}/api/v1/playlists/$playlistId/cover"

    /** 将后端返回的相对封面路径（如 /api/v1/songs/1/cover?v=..）解析为可加载的绝对 URL。 */
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
    }
}
