package com.songloft.tv.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songloft.tv.domain.PlayMode
import com.songloft.tv.ui.theme.PlayerColors

private const val SEEK_STEP_MS = 10_000L

@Composable
private fun SeekBar(
    progress: Float,
    onSeekBy: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(20.dp)
            .padding(horizontal = 12.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onSeekBy(-SEEK_STEP_MS); true }
                    Key.DirectionRight -> { onSeekBy(SEEK_STEP_MS); true }
                    else -> false
                }
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 6.dp else 4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isFocused) PlayerColors.TrackBgFocused else PlayerColors.TrackBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlayerColors.TextPrimary)
                )
            }
        }
    }
}

@Composable
fun ControlBar(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlayMode: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleEq: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit = {},
    onCycleAudioTrack: () -> Unit = {},
    onRefreshLyrics: () -> Unit = {},
    isLyricRefreshing: Boolean = false,
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PlayerColors.BarBackground)
            .padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        val progress = if (uiState.duration > 0) {
            uiState.currentPosition.toFloat() / uiState.duration.toFloat()
        } else 0f

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(uiState.currentPosition),
                fontSize = 12.sp,
                color = PlayerColors.TextTertiary
            )

            SeekBar(
                progress = progress,
                onSeekBy = { delta ->
                    val target = (uiState.currentPosition + delta)
                        .coerceIn(0L, uiState.duration.coerceAtLeast(0L))
                    onSeek(target)
                },
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatTime(uiState.duration),
                fontSize = 12.sp,
                color = PlayerColors.TextTertiary
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "上一曲", onPrevious)
            val playIcon = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
            TransportButton(
                playIcon,
                if (uiState.isPlaying) "暂停" else "播放",
                onPlayPause,
                isLarge = true,
                focusRequester = playPauseFocusRequester
            )
            TransportButton(Icons.Rounded.SkipNext, "下一曲", onNext)
            // 电台是持续流媒体，没有队列顺序/循环概念，隐藏播放模式键
            if (uiState.currentSong?.type != "radio") {
                val modeIcon = when (uiState.playMode) {
                    PlayMode.ORDER -> Icons.AutoMirrored.Rounded.PlaylistPlay
                    PlayMode.LOOP -> Icons.Rounded.Repeat
                    PlayMode.SINGLE -> Icons.Rounded.RepeatOne
                    PlayMode.RANDOM -> Icons.Rounded.Shuffle
                }
                TransportButton(modeIcon, "播放模式", onCyclePlayMode)
            }
            TransportButton(
                if (uiState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "收藏",
                onToggleFavorite
            )
            TransportButton(
                Icons.Rounded.Refresh,
                "重新获取歌词",
                onRefreshLyrics,
                loading = isLyricRefreshing
            )
            if (uiState.availableTracks.size > 1) {
                // 第 1 条音轨视为原唱（Mic），其余视为伴唱（MicOff），与播放/暂停图标同样随状态切换
                val trackIndex = uiState.availableTracks
                    .indexOfFirst { it.id == uiState.currentTrack?.id }
                val isOriginal = trackIndex <= 0
                TransportButton(
                    if (isOriginal) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    if (isOriginal) "原唱" else "伴唱",
                    onCycleAudioTrack
                )
            }
            if (onToggleEq != null) {
                TransportButton(Icons.Rounded.Equalizer, "均衡器", onToggleEq)
            }
            TransportButton(Icons.AutoMirrored.Rounded.QueueMusic, "播放队列", onToggleQueue)
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isLarge: Boolean = false,
    focusRequester: FocusRequester? = null,
    loading: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = tween(120),
        label = "transportScale"
    )

    val size = if (isLarge) 64.dp else 48.dp
    val iconSize = if (isLarge) 32.dp else 24.dp

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(
                if (isFocused) PlayerColors.ControlBgFocused
                else PlayerColors.ControlBg
            )
            .then(
                if (isFocused) Modifier.border(2.dp, PlayerColors.ControlBorder, RoundedCornerShape(50))
                else Modifier
            )
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { if (!loading) onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.dp,
                color = PlayerColors.TextPrimary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = PlayerColors.TextPrimary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
