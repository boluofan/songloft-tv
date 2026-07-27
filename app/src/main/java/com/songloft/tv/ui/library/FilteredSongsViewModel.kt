package com.songloft.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilteredSongsUiState())
    val uiState: StateFlow<FilteredSongsUiState> = _uiState.asStateFlow()

    private var loadedField: String? = null
    private var loadedValue: String? = null

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
