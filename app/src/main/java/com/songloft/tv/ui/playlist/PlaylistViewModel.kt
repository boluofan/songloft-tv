package com.songloft.tv.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(PlaylistListUiState())
    val listState: StateFlow<PlaylistListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(PlaylistDetailUiState())
    val detailState: StateFlow<PlaylistDetailUiState> = _detailState.asStateFlow()

    init {
        loadPlaylists()
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
        _detailState.value = PlaylistDetailUiState(isLoading = true)
        viewModelScope.launch {
            val detailResult = playlistRepository.getPlaylistDetail(id)
            val songsResult = playlistRepository.getPlaylistSongs(id)

            detailResult.fold(
                onSuccess = { playlist ->
                    _detailState.value = _detailState.value.copy(
                        playlist = playlist,
                        songs = songsResult.getOrDefault(emptyList()),
                        isLoading = false
                    )
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

    fun clearDetail() {
        _detailState.value = PlaylistDetailUiState()
    }
}
