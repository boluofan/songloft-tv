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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.songloft.tv.data.api.UrlHelper
import com.songloft.tv.ui.components.CoverImage
import com.songloft.tv.ui.theme.PlayerColors
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
    val eqPanelFocus = remember { FocusRequester() }

    BackHandler {
        when {
            uiState.showQueueDrawer -> viewModel.closeQueueDrawer()
            uiState.showEqPanel -> viewModel.closeEqPanel()
            uiState.showControls -> viewModel.hideControls()
            else -> onBack()
        }
    }

    LaunchedEffect(uiState.isPlaying, uiState.showControls, interactionCount) {
        if (uiState.isPlaying && uiState.showControls) {
            delay(10_000)
            viewModel.hideControls()
        }
    }

    LaunchedEffect(uiState.showControls, uiState.showQueueDrawer, uiState.showEqPanel) {
        // 等待 AnimatedVisibility 完成组合后再请求焦点
        delay(100)
        runCatching {
            when {
                uiState.showQueueDrawer -> queueDrawerFocus.requestFocus()
                uiState.showEqPanel -> eqPanelFocus.requestFocus()
                uiState.showControls -> controlBarFocus.requestFocus()
            }
        }
    }

    var didSeekDuringPress by remember { mutableStateOf(false) }
    val seekStepMs = 10_000L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .pointerInput(uiState.showControls, uiState.showEqPanel) {
                // 控制栏/均衡器面板弹出时点击其他区域关闭；子节点消费的事件不会触发此回调
                if (uiState.showEqPanel) {
                    detectTapGestures { viewModel.closeEqPanel() }
                } else if (uiState.showControls) {
                    detectTapGestures { viewModel.hideControls() }
                }
            }
            .onPreviewKeyEvent { event ->
                val controlsHidden = !uiState.showControls && !uiState.showQueueDrawer && !uiState.showEqPanel
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        interactionCount++
                        when (event.key) {
                            // 优先于焦点链处理，保证抽屉/面板/工具栏一次返回即关闭
                            Key.Back -> {
                                when {
                                    uiState.showQueueDrawer -> {
                                        viewModel.closeQueueDrawer()
                                        true
                                    }
                                    uiState.showEqPanel -> {
                                        viewModel.closeEqPanel()
                                        true
                                    }
                                    uiState.showControls -> {
                                        viewModel.hideControls()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                                viewModel.togglePlay(); true
                            }
                            Key.MediaNext, Key.MediaSkipForward -> {
                                viewModel.nextTrack(); true
                            }
                            Key.MediaPrevious, Key.MediaSkipBackward -> {
                                viewModel.previousTrack(); true
                            }
                            Key.DirectionLeft, Key.DirectionRight -> {
                                if (controlsHidden) {
                                    if (event.nativeKeyEvent.repeatCount > 0) {
                                        didSeekDuringPress = true
                                        viewModel.seekBy(
                                            if (event.key == Key.DirectionLeft) -seekStepMs else seekStepMs
                                        )
                                    }
                                    true
                                } else false
                            }
                            Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter -> {
                                if (controlsHidden) {
                                    viewModel.showControls()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    }
                    KeyEventType.KeyUp -> {
                        when (event.key) {
                            Key.DirectionLeft, Key.DirectionRight -> {
                                if (controlsHidden) {
                                    if (!didSeekDuringPress) {
                                        if (event.key == Key.DirectionLeft) viewModel.previousTrack()
                                        else viewModel.nextTrack()
                                    }
                                    didSeekDuringPress = false
                                    true
                                } else false
                            }
                            else -> false
                        }
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
            } else {
                UrlHelper.resolve(uiState.currentSong?.coverUrl)?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(60.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PlayerColors.Scrim)
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
                            CoverImage(
                                url = uiState.currentSong?.coverUrl,
                                contentDescription = uiState.currentSong?.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = uiState.currentSong?.title ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayerColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = uiState.currentSong?.artist ?: "",
                            fontSize = 14.sp,
                            color = PlayerColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                Text("未选择歌曲", color = PlayerColors.TextTertiary, fontSize = 20.sp)
            }
        }

        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) { detectTapGestures { } } // 消费控制栏区域点击，避免触发外部关闭
        ) {
            ControlBar(
                uiState = uiState,
                onPlayPause = { viewModel.togglePlay() },
                onNext = { viewModel.nextTrack() },
                onPrevious = { viewModel.previousTrack() },
                onSeek = { viewModel.seekTo(it) },
                onCyclePlayMode = { viewModel.cyclePlayMode() },
                onToggleQueue = { viewModel.toggleQueueDrawer() },
                onToggleEq = if (uiState.eqEnabled) ({ viewModel.toggleEqPanel() }) else null,
                onToggleFavorite = { viewModel.toggleFavorite() },
                onCycleAudioTrack = { viewModel.cycleAudioTrack() },
                onRefreshLyrics = { viewModel.refreshLyrics() },
                playPauseFocusRequester = controlBarFocus,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 控制栏隐藏时的触屏入口：用 pointerInput 而非 clickable，避免进入遥控器焦点链
        AnimatedVisibility(
            visible = !uiState.showControls && !uiState.showQueueDrawer && !uiState.showEqPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PlayerColors.TouchEntryBg)
                    .pointerInput(Unit) { detectTapGestures { viewModel.showControls() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "显示控制栏",
                    tint = PlayerColors.TouchEntryIcon,
                    modifier = Modifier.size(26.dp)
                )
            }
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
                onSongClick = { viewModel.playAt(it) },
                initialFocusRequester = queueDrawerFocus,
                modifier = Modifier.fillMaxHeight().width(400.dp)
            )
        }

        // 均衡器面板从右侧滑入，与左侧播放队列抽屉对称
        AnimatedVisibility(
            visible = uiState.showEqPanel,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            EqPanel(
                supported = uiState.eqSupported,
                preset = uiState.eqPreset,
                bands = uiState.eqBands,
                bandFrequencies = uiState.eqBandFrequencies,
                bandLevelMin = uiState.eqBandLevelMin,
                bandLevelMax = uiState.eqBandLevelMax,
                presetKeys = uiState.eqPresetKeys,
                presetNames = uiState.eqPresetNames,
                onSetPreset = { viewModel.setEqualizerPreset(it) },
                onSetBand = { index, level -> viewModel.setEqualizerBand(index, level) },
                onClose = { viewModel.closeEqPanel() },
                initialFocusRequester = eqPanelFocus,
                modifier = Modifier.fillMaxHeight().width(440.dp)
            )
        }
    }
}

