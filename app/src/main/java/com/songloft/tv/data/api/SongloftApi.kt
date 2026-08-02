package com.songloft.tv.data.api

import com.songloft.tv.data.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

interface SongloftApi {
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): LoginResponse

    @GET("songs")
    suspend fun getSongs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("keyword") keyword: String? = null,
        @Query("artist") artist: String? = null,
        @Query("album") album: String? = null,
        @Query("year") year: Int? = null
    ): SongListResponse

    @GET("songs/{id}/play")
    suspend fun getSongPlayUrl(
        @Path("id") id: Long,
        @Query("quality") quality: String? = null
    ): SongPlayInfo

    @GET("songs/{id}/lyric")
    suspend fun getSongLyric(@Path("id") id: Long): LyricResponse

    @GET("songs/facets")
    suspend fun getFacets(
        @Query("field") field: String,
        @Query("limit") limit: Int = 20
    ): FacetResponse

    @GET("songs/names")
    suspend fun getSongNames(
        @Query("field") field: String
    ): SongNamesResponse

    @POST("songs/{id}/played")
    suspend fun reportPlayed(
        @Path("id") id: Long,
        @Query("type") type: String,
        @Query("source") source: String = "tv",
        @Query("context_type") contextType: String? = null,
        @Query("context_key") contextKey: String? = null
    ): Unit

    @GET("playlists")
    suspend fun getPlaylists(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int? = null
    ): PlaylistListResponse

    @GET("playlists/{id}")
    suspend fun getPlaylistDetail(@Path("id") id: Long): Playlist

    @GET("playlists/{id}/songs")
    suspend fun getPlaylistSongs(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SongListResponse

    @POST("playlists/{id}/songs")
    suspend fun addSongsToPlaylist(
        @Path("id") id: Long,
        @Body body: AddSongsRequest
    ): Unit

    @DELETE("playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") id: Long,
        @Path("songId") songId: Long
    ): Unit

    @GET("config/{key}")
    suspend fun getConfig(@Path("key") key: String): Map<String, Any>

    @PUT("config/{key}")
    suspend fun setConfig(
        @Path("key") key: String,
        @Body body: Map<String, String>
    ): Unit

    @GET("health")
    suspend fun health(): Map<String, Any>

    // ── 播放统计插件（jsplugin/stats）─────────────────────────────────────

    @GET("jsplugin/stats/api/stats/summary")
    suspend fun getStatsSummary(
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null
    ): StatsSummaryResponse

    @GET("jsplugin/stats/api/stats/trends")
    suspend fun getStatsTrends(@Query("days") days: Int = 7): StatsTrendsResponse

    @GET("jsplugin/stats/api/stats/hourly")
    suspend fun getStatsHourly(): StatsHourlyResponse

    @GET("jsplugin/stats/api/history/raw")
    suspend fun getStatsHistory(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): StatsHistoryResponse
}

// ---- Response Models ----

data class AddSongsRequest(
    @SerializedName("song_ids") val songIds: List<Long>
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class SongPlayInfo(
    val url: String,
    val quality: String? = null
)

data class LyricResponse(
    val lyric: String? = null,
    val tlyric: String? = null,
    val rlyric: String? = null,
    val lxlyric: String? = null
)

data class SongListResponse(
    val songs: List<Song>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class FacetResponse(
    val facets: List<FacetItem>,
    val total: Int,
    val field: String? = null,
    val limit: Int = 0,
    val offset: Int = 0
)

data class SongNamesResponse(
    val field: String? = null,
    val names: List<String> = emptyList(),
    val total: Int = 0
)

data class PlaylistListResponse(
    val playlists: List<Playlist>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

// ---- 播放统计插件响应模型 ----

data class StatsSummaryResponse(
    val success: Boolean = false,
    val data: StatsSummary? = null,
    val error: String? = null
)

data class StatsSummary(
    val totalPlays: Int = 0,
    val totalDurationSec: Double = 0.0,
    val uniqueSongs: Int = 0,
    val uniqueArtists: Int = 0,
    val topArtists: List<StatsRankEntry> = emptyList(),
    val topSongs: List<StatsSongEntry> = emptyList(),
    val topAlbums: List<StatsRankEntry> = emptyList(),
    val bySource: Map<String, Int> = emptyMap(),
    val byMediaType: Map<String, Int> = emptyMap()
)

data class StatsRankEntry(
    val artist: String? = null,
    val album: String? = null,
    val plays: Int = 0
)

data class StatsSongEntry(
    val songId: Long = 0,
    val title: String? = null,
    val artist: String? = null,
    val plays: Int = 0
)

data class StatsTrendsResponse(
    val success: Boolean = false,
    val data: List<StatsTrendPoint>? = null
)

data class StatsTrendPoint(
    val date: String = "",
    val count: Int = 0
)

data class StatsHourlyResponse(
    val success: Boolean = false,
    val data: List<StatsHourlyPoint>? = null
)

data class StatsHourlyPoint(
    val label: String = "",
    val count: Int = 0
)

data class StatsHistoryResponse(
    val success: Boolean = false,
    val data: StatsHistoryPage? = null
)

data class StatsHistoryPage(
    val total: Int = 0,
    val records: List<StatsHistoryRecord> = emptyList(),
    val hasMore: Boolean = false
)

data class StatsHistoryRecord(
    val songId: Long = 0,
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val duration: Double? = null,
    val source: String = "",
    val type: String? = null,
    val timestamp: Long = 0
)
