package com.songloft.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.config.ConfigWebServer
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val hotTags: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private var remoteServer: ConfigWebServer? = null

    private val _remoteUrl = MutableStateFlow<String?>(null)
    val remoteUrl: StateFlow<String?> = _remoteUrl.asStateFlow()

    private val _remoteSubmitEvents = MutableSharedFlow<Unit>()
    val remoteSubmitEvents: SharedFlow<Unit> = _remoteSubmitEvents.asSharedFlow()

    fun startRemoteInput() {
        if (remoteServer != null) return
        val ip = ConfigWebServer.localIpAddress() ?: return
        for (port in REMOTE_PORTS) {
            val server = ConfigWebServer(port, onSearch = { keyword ->
                viewModelScope.launch {
                    onQueryChanged(keyword)
                    _remoteSubmitEvents.emit(Unit)
                }
            })
            if (runCatching { server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }.isSuccess) {
                remoteServer = server
                _remoteUrl.value = "http://$ip:$port/#search"
                return
            }
        }
    }

    fun stopRemoteInput() {
        remoteServer?.stop()
        remoteServer = null
        _remoteUrl.value = null
    }

    override fun onCleared() {
        stopRemoteInput()
    }

    init {
        viewModelScope.launch {
            songRepository.getFacets("artist").onSuccess { facets ->
                _uiState.value = _uiState.value.copy(
                    hotTags = facets.map { it.value }.filter { it.isNotBlank() }.take(10)
                )
            }
        }
    }

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
        _uiState.value = SearchUiState(hotTags = _uiState.value.hotTags)
        searchJob?.cancel()
    }

    companion object {
        private val REMOTE_PORTS = intArrayOf(18903, 18904, 18905, 18906)
    }
}
