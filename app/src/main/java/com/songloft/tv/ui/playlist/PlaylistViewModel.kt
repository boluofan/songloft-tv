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
    val isLoading: Boolean = true,
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

    val favoriteIds: StateFlow<Set<Long>> = favoriteRepository.favoriteIds
        .map { it ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadPlaylists()
        viewModelScope.launch { favoriteRepository.ensureFavoriteIdsLoaded() }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(song) }
    }

    fun loadPlaylists(type: String? = null) {
        _listState.value = _listState.value.copy(isLoading = true, error = null, selectedType = type)
        viewModelScope.launch {
            playlistRepository.getPlaylists(type = type).fold(
                onSuccess = { playlists ->
                    _listState.value = _listState.value.copy(
                        playlists = playlists,
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

    fun loadPlaylistDetail(id: Long) {
        detailJob?.cancel()
        _detailState.value = PlaylistDetailUiState(isLoading = true)
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
    }
}
