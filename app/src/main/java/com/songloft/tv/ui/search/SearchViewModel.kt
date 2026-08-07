package com.songloft.tv.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.config.ConfigWebServer
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.FavoriteRepository
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.data.storage.PreferencesDataStore
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val error: String? = null,
    /** 无关键词时浏览曲库（首次进入默认展示），滚动懒加载分页 */
    val browseSongs: List<Song> = emptyList(),
    val browseTotal: Int = 0,
    val hasMoreBrowse: Boolean = false,
    val isBrowseLoading: Boolean = false,
    val isLoadingMore: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
    private val favoriteRepository: FavoriteRepository,
    private val dataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val useCustomKeyboard: StateFlow<Boolean> = dataStore.useCustomKeyboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val favoriteIds: StateFlow<Set<Long>> = favoriteRepository.favoriteIds
        .map { it ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(song) }
    }

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

    private var loadMoreBrowseJob: Job? = null

    init {
        viewModelScope.launch { favoriteRepository.ensureFavoriteIdsLoaded() }
        viewModelScope.launch { loadSearchIndex() }
        viewModelScope.launch { loadBrowsePage() }
    }

    /** 首次进入无关键词：分页拉取曲库第一页作为默认内容 */
    private suspend fun loadBrowsePage() {
        _uiState.value = _uiState.value.copy(isBrowseLoading = true, error = null)
        songRepository.getSongs(limit = BROWSE_PAGE_SIZE, offset = 0).fold(
            onSuccess = { page ->
                _uiState.value = _uiState.value.copy(
                    browseSongs = page.songs,
                    browseTotal = page.total,
                    hasMoreBrowse = page.songs.size < page.total,
                    isBrowseLoading = false,
                    error = null
                )
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(isBrowseLoading = false, error = e.message)
            }
        )
    }

    /** 滚动接近底部时追加下一页曲库；有关键词时由 onQueryChanged 接管，不触发 */
    fun loadMoreBrowse() {
        val state = _uiState.value
        if (state.query.isNotBlank() || state.isBrowseLoading || state.isLoadingMore || !state.hasMoreBrowse) return
        if (loadMoreBrowseJob?.isActive == true) return
        loadMoreBrowseJob = viewModelScope.launch {
            val offset = state.browseSongs.size
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            songRepository.getSongs(limit = BROWSE_PAGE_SIZE, offset = offset).fold(
                onSuccess = { page ->
                    val current = _uiState.value
                    val merged = (current.browseSongs + page.songs).distinctBy { it.id }
                    _uiState.value = current.copy(
                        browseSongs = merged,
                        browseTotal = page.total,
                        hasMoreBrowse = merged.size < page.total,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    /** 热门搜索标签 + 拼音候选索引，首次进入即加载（曲库浏览区顶部展示热门标签） */
    private suspend fun loadSearchIndex() {
        val popularArtists = songRepository.getFacets("artist", limit = 1000).getOrNull()
            .orEmpty().map { it.value }.filter { it.isNotBlank() }
        if (popularArtists.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(hotTags = popularArtists.take(10))
        }
        val titles = songRepository.getSongNames("title").getOrDefault(emptyList())
        val artists = songRepository.getSongNames("artist").getOrDefault(emptyList())
        val names = (titles + artists).ifEmpty { popularArtists }
        pinyinIndex = withContext(Dispatchers.Default) { PinyinMatcher.index(names) }
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
            // 清空关键词回到曲库浏览：保留已加载的 browseSongs，仅清掉搜索结果
            _uiState.value = _uiState.value.copy(
                query = query,
                results = emptyList(),
                hasSearched = false,
                isSearching = false,
                error = null
            )
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
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = "",
            results = emptyList(),
            hasSearched = false,
            candidates = emptyList(),
            isSearching = false,
            error = null
        )
        val state = _uiState.value
        if (state.browseSongs.isEmpty() && !state.isBrowseLoading) {
            viewModelScope.launch { loadBrowsePage() }
        }
    }

    companion object {
        private val REMOTE_PORTS = intArrayOf(18903, 18904, 18905, 18906)
        private const val BROWSE_PAGE_SIZE = 50
    }
}
