package com.songloft.tv.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.config.ConfigWebServer
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.util.LogStore
import com.songloft.tv.util.PinyinEntry
import com.songloft.tv.util.PinyinMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val hotTags: List<String> = emptyList(),
    val candidates: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            }, logsDir = LogStore.dir(context))
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

    private var pinyinIndex: List<PinyinEntry> = emptyList()

    init {
        viewModelScope.launch {
            songRepository.getFacets("artist", limit = 1000).onSuccess { facets ->
                val values = facets.map { it.value }.filter { it.isNotBlank() }
                _uiState.value = _uiState.value.copy(hotTags = values.take(10))
                pinyinIndex = withContext(Dispatchers.Default) { PinyinMatcher.index(values) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        val candidates = if (query.length >= 2 && query.all { it in 'a'..'z' || it in 'A'..'Z' }) {
            PinyinMatcher.match(query, pinyinIndex)
        } else {
            emptyList()
        }
        _uiState.value = _uiState.value.copy(query = query, candidates = candidates)
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
