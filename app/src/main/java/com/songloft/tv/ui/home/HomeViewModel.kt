package com.songloft.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.StatsSummary
import com.songloft.tv.data.model.FacetItem
import com.songloft.tv.data.model.Playlist
import com.songloft.tv.data.repository.PlaylistRepository
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.data.repository.StatsRange
import com.songloft.tv.data.repository.StatsRepository
import com.songloft.tv.data.storage.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
    /** 用户自定义置顶歌单 id（不含内置收藏），用于首页卡片"置顶"角标判断 */
    val pinnedIds: Set<Long> = emptySet(),
    val statsSummaries: Map<StatsRange, StatsSummary> = emptyMap(),
    val statsAvailable: Boolean = false,
    val statsLoading: Boolean = true,
    val statsLoadingRanges: Set<StatsRange> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val statsRepository: StatsRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        // 置顶歌单变化后自动刷新首页前两排（跳过首次发射，避免与上面的加载重复）
        viewModelScope.launch {
            preferencesDataStore.pinnedPlaylistIds.drop(1).collect { loadDashboard() }
        }
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, statsLoading = true)

            val artistsDeferred = async { songRepository.getFacets("artist") }
            val albumsDeferred = async { songRepository.getFacets("album") }
            val yearsDeferred = async { songRepository.getFacets("year") }
            val statsDeferred = async { songRepository.getLibraryStats() }
            // 多拉取以保证内置收藏歌单（收藏/电台收藏）在响应中；用户置顶缺失时再按 id 单独兜底
            val playlistsDeferred = async { playlistRepository.getPlaylists(limit = 100) }
            // 首页只查概览汇总（全部区间，不带参数），其他区间在切换 Tab 时按需请求
            val statsSummaryDeferred = async { statsRepository.getSummary(StatsRange.ALL) }

            val artists = artistsDeferred.await().getOrDefault(emptyList())
            val albums = albumsDeferred.await().getOrDefault(emptyList())
            val years = yearsDeferred.await().getOrDefault(emptyList())
            val statsResult = statsDeferred.await()
            val all = playlistsDeferred.await().getOrNull()?.playlists.orEmpty()

            if (artists.isEmpty() && albums.isEmpty() && statsResult.isFailure) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    statsLoading = false,
                    error = statsResult.exceptionOrNull()?.message ?: "无法连接到服务器"
                )
                return@launch
            }

            // 置顶组合：内置收藏歌单/收藏电台 2 个 + 用户置顶（可能不在前 100 响应里，按 id 单独拉取兜底）+ 普通歌单填充，共 8 个
            val pinnedIds = preferencesDataStore.pinnedPlaylistIds.first()
            val builtIn = all.filter { it.isBuiltIn }.sortedBy { if (it.type == "normal") 0 else 1 }.take(2)
            val missingIds = pinnedIds.filterNot { id -> all.any { it.id == id } }
            val missingDeferred = missingIds.map { id -> async { playlistRepository.getPlaylistDetail(id).getOrNull() } }
            val fetchedMap = missingDeferred.mapNotNull { it.await() }.associateBy { it.id }
            val userPinned = pinnedIds.mapNotNull { id -> all.find { it.id == id } ?: fetchedMap[id] }
                .filterNot { it.isBuiltIn }
            val pinnedSet = userPinned.mapTo(mutableSetOf()) { it.id }
            val rest = all.filterNot { it.isBuiltIn || it.id in pinnedSet }
            val pinnedPlaylists = (builtIn + userPinned + rest).take(8)

            val stats = statsResult.getOrNull()
            val statsSummaryResult = statsSummaryDeferred.await()

            _uiState.value = HomeUiState(
                totalSongs = stats?.totalSongs ?: 0,
                localSongs = stats?.localSongs ?: 0,
                totalDuration = stats?.let { formatDuration(it.totalDurationSec) } ?: "",
                totalSize = stats?.let { formatSize(it.totalSizeBytes) } ?: "",
                topArtists = artists.take(6),
                topAlbums = albums.take(6),
                topYears = years.take(8),
                playlists = pinnedPlaylists,
                pinnedIds = userPinned.map { it.id }.toSet(),
                statsSummaries = statsSummaryResult.getOrNull()?.let { mapOf(StatsRange.ALL to it) } ?: emptyMap(),
                statsAvailable = statsSummaryResult.isSuccess,
                statsLoading = false,
                isLoading = false
            )
        }
    }

    /** 切换时间区间时按需拉取该区间概览（已加载过或加载中则跳过） */
    fun loadStatsRange(range: StatsRange) {
        val state = _uiState.value
        if (range == StatsRange.ALL || state.statsSummaries.containsKey(range)) return
        if (range in state.statsLoadingRanges) return
        _uiState.value = state.copy(statsLoadingRanges = state.statsLoadingRanges + range)
        viewModelScope.launch {
            val summary = statsRepository.getSummary(range).getOrNull()
            _uiState.value = _uiState.value.copy(
                statsLoadingRanges = _uiState.value.statsLoadingRanges - range,
                statsSummaries = summary?.let { _uiState.value.statsSummaries + (range to it) }
                    ?: _uiState.value.statsSummaries
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
