package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.SongListResponse
import com.songloft.tv.data.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getPlaylists(type: String? = null, limit: Int = 20): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        runCatching { api.getPlaylists(type, limit).playlists }
    }

    suspend fun getPlaylistDetail(id: Long): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching { api.getPlaylistDetail(id) }
    }

    suspend fun getPlaylistSongs(id: Long, limit: Int = 50, offset: Int = 0): Result<SongListResponse> =
        withContext(Dispatchers.IO) { runCatching { api.getPlaylistSongs(id, limit, offset) } }
}
