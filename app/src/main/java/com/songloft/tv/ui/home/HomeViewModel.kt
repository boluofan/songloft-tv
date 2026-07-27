package com.songloft.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.repository.PlaylistRepository
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalSongs: Int = 0,
    val localSongs: Int = 0,
    val totalDuration: String = "",
    val totalSize: String = "",
    val topArtists: List<FacetItem> = emptyList(),
    val topAlbums: List<FacetItem> = emptyList(),
    val topYears: List<FacetItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val artistsDeferred = async { songRepository.getFacets("artist") }
            val albumsDeferred = async { songRepository.getFacets("album") }
            val yearsDeferred = async { songRepository.getFacets("year") }
            val statsDeferred = async { songRepository.getLibraryStats() }
            val playlistsDeferred = async { playlistRepository.getPlaylists() }

            val artists = artistsDeferred.await().getOrDefault(emptyList())
            val albums = albumsDeferred.await().getOrDefault(emptyList())
            val years = yearsDeferred.await().getOrDefault(emptyList())
            val statsResult = statsDeferred.await()
            val playlists = playlistsDeferred.await().getOrDefault(emptyList())

            if (artists.isEmpty() && albums.isEmpty() && statsResult.isFailure) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = statsResult.exceptionOrNull()?.message ?: "无法连接到服务器"
                )
                return@launch
            }

            val stats = statsResult.getOrNull()

            _uiState.value = HomeUiState(
                totalSongs = stats?.totalSongs ?: 0,
                localSongs = stats?.localSongs ?: 0,
                totalDuration = stats?.let { formatDuration(it.totalDurationSec) } ?: "",
                totalSize = stats?.let { formatSize(it.totalSizeBytes) } ?: "",
                topArtists = artists.take(6),
                topAlbums = albums.take(6),
                topYears = years.take(8),
                playlists = playlists.take(8),
                isLoading = false
            )
        }
    }

    private fun formatDuration(seconds: Double): String {
        val hours = seconds / 3600
        return if (hours >= 1) "%.1f 小时".format(hours) else "${(seconds / 60).toInt()} 分钟"
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1e9)
        bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1e6)
        else -> "${bytes / 1000} KB"
    }
}
