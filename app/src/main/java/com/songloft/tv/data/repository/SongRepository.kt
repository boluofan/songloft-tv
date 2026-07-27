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

    suspend fun getFacets(field: String): Result<List<FacetItem>> = withContext(Dispatchers.IO) {
        runCatching { api.getFacets(field).facets }
    }

    suspend fun getSongs(limit: Int = 50, offset: Int = 0, keyword: String? = null): Result<SongListResponse> =
        withContext(Dispatchers.IO) { runCatching { api.getSongs(limit, offset, keyword) } }

    suspend fun getSongLyric(songId: Long): Result<LyricResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.getSongLyric(songId)
            if (resp.lyric.isNullOrBlank() && resp.lxlyric.isNullOrBlank()) throw Exception("无歌词")
            resp
        }
    }

    suspend fun reportPlayed(songId: Long, type: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.reportPlayed(songId, type) }
    }
}
