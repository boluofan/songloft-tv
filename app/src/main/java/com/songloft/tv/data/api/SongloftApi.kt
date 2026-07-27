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
    suspend fun getFacets(@Query("field") field: String): FacetResponse

    @POST("songs/{id}/played")
    suspend fun reportPlayed(
        @Path("id") id: Long,
        @Query("type") type: String
    ): Unit

    @GET("playlists")
    suspend fun getPlaylists(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50
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
        @Body body: Map<String, List<Long>>
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
}

// ---- Response Models ----

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

data class PlaylistListResponse(
    val playlists: List<Playlist>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
