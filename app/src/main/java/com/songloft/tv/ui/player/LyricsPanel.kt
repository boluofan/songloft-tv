package com.songloft.tv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songloft.tv.data.model.LyricLine

private val ActiveColor = Color.White
private val InactiveColor = Color.White.copy(alpha = 0.75f)
private val LyricShadow = Shadow(
    color = Color.Black.copy(alpha = 0.8f),
    offset = Offset(0f, 2f),
    blurRadius = 8f
)

@Composable
fun LyricsPanel(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无歌词",
                fontSize = 18.sp,
                color = InactiveColor
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(vertical = 200.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == currentIndex
                val distance = kotlin.math.abs(index - currentIndex)
                val alpha = if (isActive) 1f else (0.85f - 0.08f * distance).coerceIn(0.45f, 0.85f)

                if (isActive && line.hasWords) {
                    KaraokeLine(line = line, position = currentPosition)
                } else {
                    Text(
                        text = line.text.ifEmpty { "···" },
                        fontSize = if (isActive) 30.sp else 22.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) ActiveColor else Color.White.copy(alpha = alpha),
                        style = TextStyle(shadow = LyricShadow),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }

                if (isActive && line.translation != null) {
                    Text(
                        text = line.translation,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        style = TextStyle(shadow = LyricShadow),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

/** 当前行逐字渐进高亮：已唱过的字为高亮色，正在唱的字按进度部分点亮。 */
@Composable
private fun KaraokeLine(line: LyricLine, position: Long) {
    val words = line.words ?: return
    val annotated = buildAnnotatedString {
        for (word in words) {
            val lit = when {
                position >= word.end -> 1f
                position <= word.start -> 0f
                word.end > word.start ->
                    ((position - word.start).toFloat() / (word.end - word.start)).coerceIn(0f, 1f)
                else -> 0f
            }
            val litChars = (word.text.length * lit).toInt().coerceIn(0, word.text.length)
            if (litChars > 0) {
                withStyle(SpanActive) { append(word.text.substring(0, litChars)) }
            }
            if (litChars < word.text.length) {
                withStyle(SpanInactive) { append(word.text.substring(litChars)) }
            }
        }
    }
    Text(
        text = annotated,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = LyricShadow),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

private val SpanActive = SpanStyle(color = ActiveColor)
private val SpanInactive = SpanStyle(color = InactiveColor)
