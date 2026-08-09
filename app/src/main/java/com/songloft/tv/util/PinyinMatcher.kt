package com.songloft.tv.util

import com.github.promeg.pinyinhelper.Pinyin

data class PinyinEntry(val text: String, val fullPinyin: String, val initials: String)

object PinyinMatcher {

    fun index(items: List<String>): List<PinyinEntry> =
        items.distinct().mapNotNull { text ->
            val full = StringBuilder()
            val initials = StringBuilder()
            var hasChinese = false
            text.forEach { c ->
                when {
                    Pinyin.isChinese(c) -> {
                        hasChinese = true
                        val py = Pinyin.toPinyin(c).lowercase()
                        full.append(py)
                        py.firstOrNull()?.let { initials.append(it) }
                    }
                    c in 'a'..'z' || c in 'A'..'Z' -> {
                        val lower = c.lowercaseChar()
                        full.append(lower)
                        initials.append(lower)
                    }
                }
            }
            // 纯英文/拼音名字无需拼音索引（全拼即原名，服务器按名字直接可搜）
            if (!hasChinese) return@mapNotNull null
            if (full.isEmpty()) null else PinyinEntry(text, full.toString(), initials.toString())
        }

    fun match(query: String, entries: List<PinyinEntry>, limit: Int = 8): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val prefix = entries.filter { it.fullPinyin.startsWith(q) || it.initials.startsWith(q) }
        val contains = entries.filter { it !in prefix && it.fullPinyin.contains(q) }
        return (prefix + contains).take(limit).map { it.text }
    }
}
