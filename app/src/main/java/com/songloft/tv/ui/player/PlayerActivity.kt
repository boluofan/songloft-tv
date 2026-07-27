package com.songloft.tv.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.songloft.tv.data.model.Track
import com.songloft.tv.ui.theme.TvTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    var interactionCount by remember { mutableIntStateOf(0) }
    val controlBarFocus = remember { FocusRequester() }
    val queueDrawerFocus = remember { FocusRequester() }

    BackHandler {
        if (uiState.showQueueDrawer) {
            viewModel.toggleQueueDrawer()
        } else {
            onBack()
        }
    }

    LaunchedEffect(uiState.isPlaying, uiState.showControls, interactionCount) {
        if (uiState.isPlaying && uiState.showControls) {
            delay(10_000)
            viewModel.hideControls()
        }
    }

    LaunchedEffect(uiState.showControls, uiState.showQueueDrawer) {
        // 等待 AnimatedVisibility 完成组合后再请求焦点
        delay(100)
        runCatching {
            when {
                uiState.showQueueDrawer -> queueDrawerFocus.requestFocus()
                uiState.showControls -> controlBarFocus.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interactionCount++
                when (event.key) {
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        viewModel.togglePlay(); true
                    }
                    Key.MediaNext, Key.MediaSkipForward -> {
                        viewModel.nextTrack(); true
                    }
                    Key.MediaPrevious, Key.MediaSkipBackward -> {
                        viewModel.previousTrack(); true
                    }
                    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft,
                    Key.DirectionRight, Key.DirectionCenter, Key.Enter -> {
                        if (!uiState.showControls && !uiState.showQueueDrawer) {
                            viewModel.showControls()
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        if (uiState.currentSong != null) {
            if (uiState.isVideoMode) {
                VideoPlayer(
                    withPlayer = viewModel::withPlayer,
                    modifier = Modifier.fillMaxSize()
                )
                val tracks = uiState.currentSong?.tracks
                if (tracks != null && tracks.size > 1) {
                    AnimatedVisibility(
                        visible = uiState.showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrackChips(
                                tracks = tracks,
                                currentTrackId = uiState.currentTrack?.id,
                                onSwitch = { viewModel.switchTrack(it) }
                            )
                        }
                    }
                }
            } else {
                uiState.currentSong?.coverUrl?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(60.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                }
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
                                TrackChips(
                                    tracks = tracks,
                                    currentTrackId = uiState.currentTrack?.id,
                                    onSwitch = { viewModel.switchTrack(it) }
                                )
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
                onToggleFavorite = { viewModel.toggleFavorite() },
                playPauseFocusRequester = controlBarFocus,
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
                initialFocusRequester = queueDrawerFocus,
                modifier = Modifier.fillMaxHeight().width(400.dp)
            )
        }
    }
}

@Composable
private fun TrackChips(
    tracks: List<Track>,
    currentTrackId: String?,
    onSwitch: (Track) -> Unit
) {
    tracks.forEach { track ->
        val isActive = track.id == currentTrackId
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
                .onFocusChanged { trackFocused = it.isFocused }
                .clickable { onSwitch(track) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
