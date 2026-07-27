package com.songloft.tv.data.model

data class LyricLine(
    val time: Long,
    val text: String,
    val translation: String? = null,
    val romaji: String? = null
)
