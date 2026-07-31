package com.songloft.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.FavoriteRepository
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilteredSongsUiState(
    val title: String = "",
    val songs: List<Song> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FilteredSongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilteredSongsUiState())
    val uiState: StateFlow<FilteredSongsUiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = favoriteRepository.favoriteIds
        .map { it ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var loadedField: String? = null
    private var loadedValue: String? = null

    init {
        viewModelScope.launch { favoriteRepository.ensureFavoriteIdsLoaded() }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(song) }
    }

    fun load(field: String, value: String) {
        if (field == loadedField && value == loadedValue) return
        loadedField = field
        loadedValue = value
        _uiState.value = FilteredSongsUiState(title = value)
        viewModelScope.launch {
            songRepository.getSongs(
                limit = 500,
                artist = value.takeIf { field == "artist" },
                album = value.takeIf { field == "album" },
                year = if (field == "year") value.toIntOrNull() else null
            ).onSuccess { resp ->
                _uiState.update {
                    it.copy(songs = resp.songs, total = resp.total, isLoading = false)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
