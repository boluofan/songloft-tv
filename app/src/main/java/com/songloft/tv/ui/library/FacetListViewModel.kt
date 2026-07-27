package com.songloft.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacetListUiState(
    val items: List<FacetItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FacetListViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacetListUiState())
    val uiState: StateFlow<FacetListUiState> = _uiState.asStateFlow()

    private var loadedField: String? = null

    fun load(field: String) {
        if (field == loadedField) return
        loadedField = field
        _uiState.value = FacetListUiState()
        viewModelScope.launch {
            songRepository.getFacets(field)
                .onSuccess { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
