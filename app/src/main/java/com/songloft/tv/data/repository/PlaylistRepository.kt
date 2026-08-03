package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.PlaylistListResponse
import com.songloft.tv.data.api.SongListResponse
import com.songloft.tv.data.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getPlaylists(type: String? = null, limit: Int = 20, offset: Int = 0): Result<PlaylistListResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val page = api.getPlaylists(type, limit, offset)
                // 内置收藏歌单（收藏/电台收藏）置顶，仅第一页生效；后续分页由 distinctBy 去重
                if (offset > 0) page else page.withBuiltInFavoritesFirst(type)
            }
        }

    /** 内置收藏歌单固定到列表最前（收藏在前、电台收藏在后），普通歌单紧随其后，总数不变 */
    private fun PlaylistListResponse.withBuiltInFavoritesFirst(type: String?): PlaylistListResponse {
        val pinned = playlists
            .filter { it.isBuiltIn && (type == null || it.type == type) }
            .sortedBy { if (it.type == "normal") 0 else 1 }
            .take(2)
        if (pinned.isEmpty()) return this
        val regulars = playlists.filterNot { it.isBuiltIn }.take(playlists.size - pinned.size)
        return copy(playlists = pinned + regulars)
    }

    suspend fun getPlaylistDetail(id: Long): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching { api.getPlaylistDetail(id) }
    }

    suspend fun getPlaylistSongs(id: Long, limit: Int = 50, offset: Int = 0): Result<SongListResponse> =
        withContext(Dispatchers.IO) { runCatching { api.getPlaylistSongs(id, limit, offset) } }
}
