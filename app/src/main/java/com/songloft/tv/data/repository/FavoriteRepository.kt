package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    private var favoritePlaylistId: Long? = null

    suspend fun getFavorites(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching { api.getFavorites().songs }
    }

    suspend fun addFavorite(songId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistId = resolveFavoritePlaylistId()
                ?: throw IllegalStateException("未找到收藏歌单")
            api.addSongsToPlaylist(playlistId, mapOf("song_ids" to listOf(songId)))
        }
    }

    suspend fun removeFavorite(songId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistId = resolveFavoritePlaylistId()
                ?: throw IllegalStateException("未找到收藏歌单")
            api.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // songloft 的收藏以 built_in 标签歌单实现
    private suspend fun resolveFavoritePlaylistId(): Long? {
        favoritePlaylistId?.let { return it }
        return runCatching {
            api.getPlaylists(null, 100).playlists.firstOrNull { it.isBuiltIn }?.id
        }.getOrNull()?.also { favoritePlaylistId = it }
    }
}
