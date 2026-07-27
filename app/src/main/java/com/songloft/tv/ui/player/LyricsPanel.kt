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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songloft.tv.data.model.LyricLine

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
                color = Color.White.copy(alpha = 0.4f)
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
                val alpha = if (isActive) 1f else (0.08f / distance.coerceAtLeast(1)).coerceIn(0.08f, 0.5f)

                Text(
                    text = line.text.ifEmpty { "···" },
                    fontSize = if (isActive) 30.sp else 22.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = alpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                )

                if (isActive && line.translation != null) {
                    Text(
                        text = line.translation,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.5f),
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
