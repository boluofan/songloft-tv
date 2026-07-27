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

    // songloft 的收藏以 built_in 标签歌单实现：normal=收藏歌曲，radio=收藏电台
    private val builtInPlaylistIds = mutableMapOf<String, Long>()

    suspend fun getFavorites(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            resolveBuiltInPlaylists().values.flatMap { playlistId ->
                api.getPlaylistSongs(playlistId, limit = 200).songs
            }
        }
    }

    suspend fun addFavorite(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistId = resolvePlaylistIdFor(song)
                ?: throw IllegalStateException("未找到收藏歌单")
            api.addSongsToPlaylist(playlistId, mapOf("song_ids" to listOf(song.id)))
        }
    }

    suspend fun removeFavorite(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistId = resolvePlaylistIdFor(song)
                ?: throw IllegalStateException("未找到收藏歌单")
            api.removeSongFromPlaylist(playlistId, song.id)
        }
    }

    private suspend fun resolvePlaylistIdFor(song: Song): Long? {
        val type = if (song.type == "radio") "radio" else "normal"
        return resolveBuiltInPlaylists()[type]
    }

    private suspend fun resolveBuiltInPlaylists(): Map<String, Long> {
        if (builtInPlaylistIds.isNotEmpty()) return builtInPlaylistIds
        api.getPlaylists(null, 100).playlists
            .filter { it.isBuiltIn }
            .forEach { builtInPlaylistIds[it.type] = it.id }
        return builtInPlaylistIds
    }
}
