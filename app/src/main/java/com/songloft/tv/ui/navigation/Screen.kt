package com.songloft.tv.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "首页")
    object Search : Screen("search", "搜索")
    object Playlists : Screen("playlists", "歌单")
    object My : Screen("my", "我的")
    data class PlaylistDetail(val playlistId: Long) : Screen("playlist_detail", "歌单详情")

    companion object {
        val all = listOf<Screen>(Home, Search, Playlists, My)
    }
}
