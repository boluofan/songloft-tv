package com.songloft.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearching = true, hasSearched = true)
            try {
                val response = ApiClient.getApi().getSongs(keyword = query, limit = 50)
                _uiState.value = _uiState.value.copy(
                    results = response.songs,
                    isSearching = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
        searchJob?.cancel()
    }
}
