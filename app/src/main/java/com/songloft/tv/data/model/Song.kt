package com.songloft.tv.data.model

import com.google.gson.annotations.SerializedName

data class Song(
    val id: Long,
    val type: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Double,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("is_video") val isVideo: Boolean = false,
    val tracks: List<Track>? = null
) {
    val hasMultiTrack: Boolean get() = (tracks?.size ?: 0) > 1
}

data class Track(
    val id: String,
    val name: String,
    val url: String,
    val quality: String? = null
)

data class FacetItem(
    val value: String,
    val count: Int,
    @SerializedName("cover_url") val coverUrl: String? = null
)
