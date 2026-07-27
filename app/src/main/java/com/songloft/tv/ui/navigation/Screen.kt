package com.songloft.tv.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "首页")
    object Search : Screen("search", "搜索")
    object Playlists : Screen("playlists", "歌单")
    object My : Screen("my", "我的")
    object Settings : Screen("settings", "设置")
    data class PlaylistDetail(val playlistId: Long) : Screen("playlist_detail", "歌单详情")
    data class SongFilter(val field: String, val value: String) : Screen("song_filter", "歌曲筛选")
    data class FacetList(val field: String) : Screen("facet_list", "分类列表")

    companion object {
        val all = listOf<Screen>(Home, Search, Playlists, My)
    }
}
