package com.songloft.tv.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.songloft.tv.ui.theme.TvTheme
import kotlinx.coroutines.delay

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songId = intent.getLongExtra("song_id", -1L)
        val songTitle = intent.getStringExtra("song_title")
        val songArtist = intent.getStringExtra("song_artist")

        setContent {
            TvTheme {
                PlayerScreen(
                    viewModel = hiltViewModel(),
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var controlsTimer by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            controlsTimer = 0
            while (controlsTimer < 10) {
                delay(1000)
                controlsTimer++
            }
            viewModel.hideControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.currentSong != null) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(54.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val song = uiState.currentSong
                            if (song?.coverUrl != null) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("♫", fontSize = 48.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = uiState.currentSong?.title ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = uiState.currentSong?.artist ?: "",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val tracks = uiState.currentSong?.tracks
                        if (tracks != null && tracks.size > 1) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tracks.forEach { track ->
                                    val isActive = track.id == uiState.currentTrack?.id
                                    var trackFocused by remember { mutableStateOf(false) }
                                    Text(
                                        text = track.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            trackFocused -> MaterialTheme.colorScheme.onPrimary
                                            isActive -> MaterialTheme.colorScheme.primary
                                            else -> Color.White.copy(alpha = 0.6f)
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                when {
                                                    trackFocused -> MaterialTheme.colorScheme.primary
                                                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else -> Color.White.copy(alpha = 0.1f)
                                                }
                                            )
                                            .focusable()
                                            .onFocusChanged { trackFocused = it.isFocused }
                                            .clickable { viewModel.switchTrack(track) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LyricsPanel(
                        lyrics = uiState.lyrics,
                        currentIndex = uiState.currentLyricIndex,
                        currentPosition = uiState.currentPosition
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("未选择歌曲", color = Color.Gray, fontSize = 20.sp)
            }
        }

        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ControlBar(
                uiState = uiState,
                onPlayPause = { viewModel.togglePlay() },
                onNext = { viewModel.nextTrack() },
                onPrevious = { viewModel.previousTrack() },
                onSeek = { viewModel.seekTo(it) },
                onCyclePlayMode = { viewModel.cyclePlayMode() },
                onToggleQueue = { viewModel.toggleQueueDrawer() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = uiState.showQueueDrawer,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            QueueDrawer(
                queue = uiState.queue,
                currentIndex = uiState.currentIndex,
                onClose = { viewModel.toggleQueueDrawer() },
                modifier = Modifier.fillMaxHeight().width(400.dp)
            )
        }
    }
}
