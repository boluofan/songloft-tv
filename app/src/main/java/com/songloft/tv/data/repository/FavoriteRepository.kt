package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.AddSongsRequest
import com.songloft.tv.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    // songloft 的收藏以 built_in 标签歌单实现，官方迁移固定 id：normal=收藏歌曲(id=1)、radio=收藏电台(id=2)
    private val builtInPlaylistIds = mapOf("normal" to 1L, "radio" to 2L)

    // null 表示尚未加载
    private val _favoriteIds = MutableStateFlow<Set<Long>?>(null)
    val favoriteIds: StateFlow<Set<Long>?> = _favoriteIds.asStateFlow()

    private val loadMutex = Mutex()

    suspend fun ensureFavoriteIdsLoaded() {
        loadMutex.withLock {
            if (_favoriteIds.value == null) getFavorites()
        }
    }

    suspend fun getFavorites(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            builtInPlaylistIds.values.flatMap { playlistId ->
                api.getPlaylistSongs(playlistId, limit = 200).songs
            }
        }.onSuccess { songs ->
            _favoriteIds.value = songs.map { it.id }.toSet()
        }
    }

    suspend fun addFavorite(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.addSongsToPlaylist(playlistIdFor(song), AddSongsRequest(listOf(song.id)))
            Unit
        }.onSuccess {
            _favoriteIds.update { ids -> ids?.plus(song.id) }
        }
    }

    suspend fun removeFavorite(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.removeSongFromPlaylist(playlistIdFor(song), song.id)
            Unit
        }.onSuccess {
            _favoriteIds.update { ids -> ids?.minus(song.id) }
        }
    }

    /** 切换收藏状态：乐观更新 favoriteIds，失败回滚。返回操作后是否为收藏态 */
    suspend fun toggleFavorite(song: Song): Result<Boolean> {
        if (_favoriteIds.value == null) ensureFavoriteIdsLoaded()
        val current = _favoriteIds.value ?: emptySet()
        val wasFavorite = song.id in current
        _favoriteIds.value = if (wasFavorite) current - song.id else current + song.id
        val result = if (wasFavorite) removeFavorite(song) else addFavorite(song)
        return result.map { !wasFavorite }.onFailure {
            _favoriteIds.update { ids ->
                if (wasFavorite) ids?.plus(song.id) else ids?.minus(song.id)
            }
        }
    }

    private fun playlistIdFor(song: Song): Long =
        if (song.type == "radio") builtInPlaylistIds.getValue("radio")
        else builtInPlaylistIds.getValue("normal")
}
