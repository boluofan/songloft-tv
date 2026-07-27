package com.songloft.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songloft.tv.data.model.Song

@Composable
fun QueueDrawer(
    queue: List<Song>,
    currentIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xE6111827))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放列表",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            var closeFocused by remember { mutableStateOf(false) }
            Text(
                text = "✕",
                fontSize = 20.sp,
                color = if (closeFocused) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (closeFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .focusable()
                    .onFocusChanged { closeFocused = it.isFocused }
                    .clickable { onClose() }
                    .padding(8.dp)
            )
        }

        Text(
            text = "共 ${queue.size} 首",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(queue) { index, song ->
                val isCurrent = index == currentIndex
                var isFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isCurrent -> Color.White.copy(alpha = 0.15f)
                                isFocused -> Color.White.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            }
                        )
                        .focusable()
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable { onClose() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) {
                        Text("♪", fontSize = 16.sp, color = Color(0xFF415F91), modifier = Modifier.padding(end = 8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title, fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.8f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (song.artist != null) {
                            Text(
                                song.artist, fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
