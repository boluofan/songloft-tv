package com.songloft.tv.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.FavoriteRepository
import com.songloft.tv.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistListUiState(
    val playlists: List<Playlist> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedType: String? = null
)

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(PlaylistListUiState())
    val listState: StateFlow<PlaylistListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(PlaylistDetailUiState())
    val detailState: StateFlow<PlaylistDetailUiState> = _detailState.asStateFlow()

    private var detailJob: Job? = null
    private var loadMoreJob: Job? = null

    val favoriteIds: StateFlow<Set<Long>> = favoriteRepository.favoriteIds
        .map { it ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadPlaylists()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(song) }
    }

    fun loadPlaylists(type: String? = null) {
        loadMoreJob?.cancel()
        _listState.value = PlaylistListUiState(isLoading = true, error = null, selectedType = type)
        viewModelScope.launch {
            playlistRepository.getPlaylists(type = type, limit = PLAYLIST_PAGE_SIZE).fold(
                onSuccess = { page ->
                    _listState.value = _listState.value.copy(
                        playlists = page.playlists,
                        total = page.total,
                        hasMore = page.playlists.size < page.total,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    /** 滚动到底时追加下一页（懒加载），offset 取当前已加载数量 */
    fun loadMorePlaylists() {
        val state = _listState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        if (loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            val offset = state.playlists.size
            playlistRepository.getPlaylists(
                type = state.selectedType,
                limit = PLAYLIST_PAGE_SIZE,
                offset = offset
            ).fold(
                onSuccess = { page ->
                    val current = _listState.value
                    val merged = (current.playlists + page.playlists).distinctBy { it.id }
                    _listState.value = current.copy(
                        playlists = merged,
                        total = page.total,
                        hasMore = offset + page.playlists.size < page.total,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    _listState.value = _listState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun loadPlaylistDetail(id: Long) {
        detailJob?.cancel()
        _detailState.value = PlaylistDetailUiState(isLoading = true)
        // 详情页歌曲需显示收藏状态，懒加载收藏 id（首次请求，之后走缓存），与歌曲列表并发加载
        viewModelScope.launch { favoriteRepository.ensureFavoriteIdsLoaded() }
        detailJob = viewModelScope.launch {
            playlistRepository.getPlaylistDetail(id).fold(
                onSuccess = { playlist ->
                    _detailState.value = _detailState.value.copy(playlist = playlist)
                    loadAllSongs(id)
                },
                onFailure = { e ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private suspend fun loadAllSongs(id: Long) {
        var offset = 0
        while (true) {
            val page = playlistRepository.getPlaylistSongs(id, limit = SONGS_PAGE_SIZE, offset = offset)
                .getOrNull() ?: break
            val current = _detailState.value
            _detailState.value = current.copy(
                songs = current.songs + page.songs,
                total = page.total,
                isLoading = false
            )
            offset += page.songs.size
            if (page.songs.isEmpty() || offset >= page.total) break
        }
        _detailState.value = _detailState.value.copy(isLoading = false)
    }

    fun clearDetail() {
        detailJob?.cancel()
        _detailState.value = PlaylistDetailUiState()
    }

    private companion object {
        const val SONGS_PAGE_SIZE = 500
        const val PLAYLIST_PAGE_SIZE = 100
    }
}
