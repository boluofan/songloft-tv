package com.songloft.tv.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.repository.FavoriteRepository
import com.songloft.tv.data.repository.PlaylistRepository
import com.songloft.tv.data.storage.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistListUiState(
    val playlists: List<Playlist> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedType: String? = null
)

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _listState = MutableStateFlow(PlaylistListUiState())
    val listState: StateFlow<PlaylistListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(PlaylistDetailUiState())
    val detailState: StateFlow<PlaylistDetailUiState> = _detailState.asStateFlow()

    private var detailJob: Job? = null
    private var loadMoreJob: Job? = null

    /** 用户自定义置顶歌单 id，下标 0 最前（新置顶排最前）；内置收藏歌单不在此列 */
    val pinnedIds: StateFlow<List<Long>> = preferencesDataStore.pinnedPlaylistIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = favoriteRepository.favoriteIds
        .map { it ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadPlaylists()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(song) }
    }

    /** 长按歌单：已置顶则取消，未置顶则插入最前；超过上限自动替换最旧的置顶 */
    fun togglePin(id: Long) {
        viewModelScope.launch {
            val current = preferencesDataStore.pinnedPlaylistIds.first()
            android.util.Log.d("PlaylistPin", "[togglePin] id=$id current=$current")
            val next = if (id in current) current - id else listOf(id) + current.filterNot { it == id }
            preferencesDataStore.setPinnedPlaylistIds(next.take(MAX_PINNED))
            android.util.Log.d("PlaylistPin", "[togglePin] saved=${next.take(MAX_PINNED)}")
        }
    }

    fun loadPlaylists(type: String? = null) {
        loadMoreJob?.cancel()
        _listState.value = PlaylistListUiState(isLoading = true, error = null, selectedType = type)
        viewModelScope.launch {
            playlistRepository.getPlaylists(type = type, limit = PLAYLIST_PAGE_SIZE).fold(
                onSuccess = { page ->
                    _listState.value = _listState.value.copy(
                        playlists = page.playlists,
                        total = page.total,
                        hasMore = page.playlists.size < page.total,
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

    /** 滚动到底时追加下一页（懒加载），offset 取当前已加载数量 */
    fun loadMorePlaylists() {
        val state = _listState.value
        if (state.isLoading || state.isLoadingMore || state.isRefreshing || !state.hasMore) return
        if (loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            val offset = state.playlists.size
            playlistRepository.getPlaylists(
                type = state.selectedType,
                limit = PLAYLIST_PAGE_SIZE,
                offset = offset
            ).fold(
                onSuccess = { page ->
                    val current = _listState.value
                    val merged = (current.playlists + page.playlists).distinctBy { it.id }
                    _listState.value = current.copy(
                        playlists = merged,
                        total = page.total,
                        hasMore = offset + page.playlists.size < page.total,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    _listState.value = _listState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    /** 心跳/刷新按钮用：静默刷新当前筛选类型的歌单列表，数据无变化时不替换，避免打断浏览 */
    fun refreshPlaylists() {
        val state = _listState.value
        if (state.isLoading || state.isLoadingMore || state.isRefreshing) return
        val type = state.selectedType
        _listState.value = state.copy(isRefreshing = true)
        viewModelScope.launch {
            playlistRepository.getPlaylists(type = type, limit = PLAYLIST_PAGE_SIZE).fold(
                onSuccess = { page ->
                    val current = _listState.value
                    _listState.value = if (page.playlists.map { it.id } == current.playlists.map { it.id }) {
                        current.copy(isRefreshing = false)
                    } else {
                        current.copy(
                            playlists = page.playlists,
                            total = page.total,
                            hasMore = page.playlists.size < page.total,
                            isRefreshing = false,
                            error = null
                        )
                    }
                },
                onFailure = {
                    // 静默刷新失败：保留现有列表，不打断用户
                    _listState.value = _listState.value.copy(isRefreshing = false)
                }
            )
            prunePinnedPlaylists()
        }
    }

    /** 刷新后校验置顶歌单仍存在：已被服务端删除的置顶 id 从配置清理（首页监听 pinnedPlaylistIds 自动刷新） */
    private suspend fun prunePinnedPlaylists() {
        val pinned = preferencesDataStore.pinnedPlaylistIds.first()
        if (pinned.isEmpty()) return
        val existing = _listState.value.playlists.map { it.id }.toSet()
        val missing = pinned.filterNot { it in existing }
        if (missing.isEmpty()) return
        val removed = missing.filter { playlistRepository.getPlaylistDetail(it).isFailure }
        if (removed.isEmpty()) return
        preferencesDataStore.setPinnedPlaylistIds(pinned.filterNot { it in removed })
        android.util.Log.d("PlaylistPin", "[prune] 置顶歌单已失效，清理：$removed")
    }

    fun loadPlaylistDetail(id: Long) {
        detailJob?.cancel()
        _detailState.value = PlaylistDetailUiState(isLoading = true)
        // 详情页歌曲需显示收藏状态，懒加载收藏 id（首次请求，之后走缓存），与歌曲列表并发加载
        viewModelScope.launch { favoriteRepository.ensureFavoriteIdsLoaded() }
        detailJob = viewModelScope.launch {
            playlistRepository.getPlaylistDetail(id).fold(
                onSuccess = { playlist ->
                    _detailState.value = _detailState.value.copy(playlist = playlist)
                    loadAllSongs(id)
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

    private suspend fun loadAllSongs(id: Long) {
        var offset = 0
        while (true) {
            val page = playlistRepository.getPlaylistSongs(id, limit = SONGS_PAGE_SIZE, offset = offset)
                .getOrNull() ?: break
            val current = _detailState.value
            _detailState.value = current.copy(
                songs = current.songs + page.songs,
                total = page.total,
                isLoading = false
            )
            offset += page.songs.size
            if (page.songs.isEmpty() || offset >= page.total) break
        }
        _detailState.value = _detailState.value.copy(isLoading = false)
    }

    fun clearDetail() {
        detailJob?.cancel()
        _detailState.value = PlaylistDetailUiState()
    }

    companion object {
        const val SONGS_PAGE_SIZE = 500
        const val PLAYLIST_PAGE_SIZE = 100
        /** 用户自定义置顶歌单上限；加上内置收藏歌单/收藏电台共 8 个置顶槽位 */
        const val MAX_PINNED = 6
    }
}

/** 内置收藏歌单固定最前（收藏在前、电台收藏在后），随后用户置顶（新置顶最前），其余保持原顺序。
 *  内置歌单可能出现在任意分页，这里统一提到最前，不依赖 repository 第一页处理 */
fun orderWithPinnedFirst(playlists: List<Playlist>, pinnedIds: List<Long>): List<Playlist> {
    val byId = playlists.associateBy { it.id }
    val builtIn = playlists.filter { it.isBuiltIn }.sortedBy { if (it.type == "normal") 0 else 1 }.take(2)
    val pinned = pinnedIds.mapNotNull { byId[it] }.filterNot { it.isBuiltIn }
    val pinnedSet = pinned.mapTo(mutableSetOf()) { it.id }
    return builtIn + pinned + playlists.filterNot { it.isBuiltIn || it.id in pinnedSet }
}
