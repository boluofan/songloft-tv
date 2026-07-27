package com.songloft.tv.data.api

object UrlHelper {
    private var baseUrl: String = ""

    fun initialize(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun songPlayUrl(songId: Long, quality: String? = null, track: String? = null): String {
        val sb = StringBuilder("${baseUrl}/api/v1/songs/$songId/play")
        val params = mutableListOf<String>()
        quality?.let { params.add("quality=$it") }
        track?.let { params.add("track=$it") }
        if (params.isNotEmpty()) sb.append("?").append(params.joinToString("&"))
        return sb.toString()
    }

    fun songCoverUrl(songId: Long): String = "${baseUrl}/api/v1/songs/$songId/cover"

    fun playlistCoverUrl(playlistId: Long): String = "${baseUrl}/api/v1/playlists/$playlistId/cover"

    /** 将后端返回的相对封面路径（如 /api/v1/songs/1/cover?v=..）解析为可加载的绝对 URL。 */
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
    }
}
