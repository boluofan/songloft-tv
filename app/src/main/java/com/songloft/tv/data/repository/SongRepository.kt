package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.FacetResponse
import com.songloft.tv.data.api.LyricResponse
import com.songloft.tv.data.api.SongListResponse
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getFacets(field: String, limit: Int = 20): Result<List<FacetItem>> = withContext(Dispatchers.IO) {
        runCatching { api.getFacets(field, limit).facets }
    }

    suspend fun getSongNames(field: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching { api.getSongNames(field).names }
    }

    suspend fun getSongs(
        limit: Int = 50,
        offset: Int = 0,
        keyword: String? = null,
        artist: String? = null,
        album: String? = null,
        year: Int? = null
    ): Result<SongListResponse> =
        withContext(Dispatchers.IO) { runCatching { api.getSongs(limit, offset, keyword, artist, album, year) } }

    /** 优先走汇总接口；旧服务器无 /songs/stats 或请求失败时回滚到分页遍历统计 */
    suspend fun getLibraryStats(): Result<LibraryStats> = withContext(Dispatchers.IO) {
        val fromApi = runCatching { api.getSongsStats() }
        if (fromApi.isSuccess) {
            val s = fromApi.getOrThrow()
            Result.success(LibraryStats(s.totalSongs, s.localSongs, s.totalDurationSec, s.totalFileSizeBytes))
        } else {
            fallbackLibraryStats()
        }
    }

    private suspend fun fallbackLibraryStats(): Result<LibraryStats> = withContext(Dispatchers.IO) {
        runCatching {
            var offset = 0
            var total = 0
            var local = 0
            var durationSec = 0.0
            var sizeBytes = 0L
            while (true) {
                val resp = api.getSongs(limit = 500, offset = offset)
                total = resp.total
                resp.songs.forEach { song ->
                    if (song.type == "local") local++
                    durationSec += song.duration
                    sizeBytes += song.fileSize
                }
                offset += resp.songs.size
                if (resp.songs.isEmpty() || offset >= total || offset >= MAX_STATS_SONGS) break
            }
            LibraryStats(total, local, durationSec, sizeBytes)
        }
    }

    suspend fun getSongLyric(songId: Long, refresh: Boolean = false): Result<LyricResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.getSongLyric(songId, refresh)
            if (resp.lyric.isNullOrBlank() && resp.lxlyric.isNullOrBlank()) throw Exception("无歌词")
            resp
        }
    }

    suspend fun reportPlayed(
        songId: Long,
        type: String,
        contextType: String? = null,
        contextKey: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.reportPlayed(songId, type, contextType = contextType, contextKey = contextKey) }
    }

    companion object {
        private const val MAX_STATS_SONGS = 5000
    }
}

data class LibraryStats(
    val totalSongs: Int,
    val localSongs: Int,
    val totalDurationSec: Double,
    val totalSizeBytes: Long
)
