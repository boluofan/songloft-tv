package com.songloft.tv.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyUiState(
    val favoriteSongs: List<Song> = emptyList(),
    val favoriteRadios: List<Song> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MyViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
        // 其他页面收藏/取消收藏时，收藏 id 集变化，静默刷新列表保持同步
        viewModelScope.launch {
            favoriteRepository.favoriteIds
                .filterNotNull()
                .distinctUntilChanged()
                .drop(1)
                .collect { loadFavorites(showLoading = false) }
        }
    }

    fun loadFavorites(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
            favoriteRepository.getFavorites()
                .onSuccess { songs ->
                    val (radios, normals) = songs.partition { it.type == "radio" }
                    // 收藏歌单按加入顺序追加，倒序使最新收藏排在最前
                    _uiState.update {
                        it.copy(
                            favoriteSongs = normals.reversed(),
                            favoriteRadios = radios.reversed(),
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    if (showLoading) _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun removeFavorite(song: Song) {
        val prevSongs = _uiState.value.favoriteSongs
        val prevRadios = _uiState.value.favoriteRadios
        _uiState.update {
            if (song.type == "radio") it.copy(favoriteRadios = it.favoriteRadios - song)
            else it.copy(favoriteSongs = it.favoriteSongs - song)
        }
        viewModelScope.launch {
            favoriteRepository.removeFavorite(song).onFailure {
                _uiState.update { it.copy(favoriteSongs = prevSongs, favoriteRadios = prevRadios) }
            }
        }
    }
}
