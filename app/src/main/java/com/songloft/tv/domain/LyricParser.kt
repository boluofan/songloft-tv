package com.songloft.tv.domain

import com.songloft.tv.data.model.LyricLine

object LyricParser {

    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val lineRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        val multiTimeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]""")

        for (line in lrcText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:") || trimmed.startsWith("[total:")) continue

            val match = lineRegex.find(trimmed)
            if (match != null) {
                val minutes = match.groupValues[1].toIntOrNull() ?: 0
                val seconds = match.groupValues[2].toIntOrNull() ?: 0
                val millisStr = match.groupValues[3]
                val millis = if (millisStr.length == 2) millisStr.toInt() * 10 else millisStr.toInt()
                val text = match.groupValues[4].trim()

                val timeMs = (minutes * 60L + seconds) * 1000L + millis
                lines.add(LyricLine(time = timeMs, text = text))
            } else {
                val times = multiTimeRegex.findAll(trimmed).map { it.value }.toList()
                if (times.isNotEmpty()) {
                    val text = trimmed.replace(multiTimeRegex, "").trim()
                    if (text.isNotEmpty()) {
                        for (timeStr in times) {
                            val tMatch = multiTimeRegex.find(timeStr)
                            if (tMatch != null) {
                                val minutes = tMatch.groupValues[1].toIntOrNull() ?: 0
                                val seconds = tMatch.groupValues[2].toIntOrNull() ?: 0
                                val millisStr = tMatch.groupValues[3]
                                val millis = if (millisStr.length == 2) millisStr.toInt() * 10 else millisStr.toInt()
                                val timeMs = (minutes * 60L + seconds) * 1000L + millis
                                lines.add(LyricLine(time = timeMs, text = text))
                            }
                        }
                    }
                }
            }
        }

        return lines.sortedBy { it.time }
    }
}
