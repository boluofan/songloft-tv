package com.songloft.tv.data.model

import com.google.gson.annotations.SerializedName

data class Playlist(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("song_count") val songCount: Int = 0,
    val type: String = "normal",
    val labels: List<String> = emptyList()
) {
    val isBuiltIn: Boolean get() = labels.contains("built_in")
    val isHidden: Boolean get() = labels.contains("hidden")
}
