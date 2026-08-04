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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songloft.tv.ui.theme.PlayerColors
import com.songloft.tv.ui.theme.SelectedFocusBorder

private const val BAND_STEP_DB = 1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqPanel(
    supported: Boolean,
    preset: String,
    bands: List<Int>,
    bandFrequencies: List<Int>,
    bandLevelMin: Int,
    bandLevelMax: Int,
    presetKeys: List<String>,
    presetNames: List<String>,
    onSetPreset: (String) -> Unit,
    onSetBand: (Int, Int) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val focusedEdges = remember { mutableStateOf<Set<EqEdge>>(emptySet()) }
    Column(
        modifier = modifier
            .background(PlayerColors.QueueBackground)
            .padding(16.dp)
            .onKeyEvent { event ->
                // 子项未消费的方向键若落在面板边界，在此拦截，防止焦点移出面板
                if (event.type == KeyEventType.KeyDown && isEscapeKey(event.key, focusedEdges.value)) true else false
            }
    ) {
        Text(
            text = "均衡器",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PlayerColors.TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        if (!supported) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前设备不支持均衡器",
                    fontSize = 14.sp,
                    color = PlayerColors.TextMuted
                )
            }
            return
        }

        // 频段数据需音频会话就绪（播放中）后才有
        if (bandFrequencies.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "播放歌曲后可用",
                    fontSize = 14.sp,
                    color = PlayerColors.TextMuted
                )
            }
            return
        }

        if (presetNames.isNotEmpty()) {
            Text(
                text = "预设",
                fontSize = 13.sp,
                color = PlayerColors.TextMuted
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shownPresets = presetKeys.take(6)
                shownPresets.forEachIndexed { index, key ->
                    val edges = buildSet {
                        if (index == 0) add(EqEdge.LEFT)
                        if (index == shownPresets.lastIndex) add(EqEdge.RIGHT)
                    }
                    EqChip(
                        label = presetNames.getOrNull(index) ?: key,
                        isSelected = preset == key,
                        modifier = if (index == 0 && initialFocusRequester != null) {
                            Modifier.focusRequester(initialFocusRequester)
                        } else {
                            Modifier
                        },
                        onFocusChange = { focused -> focusedEdges.value = if (focused) edges else emptySet() },
                        onClick = { onSetPreset(key) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "频段增益",
            fontSize = 13.sp,
            color = PlayerColors.TextMuted
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(bands) { index, levelDb ->
                val edges = buildSet {
                    if (index == 0 && presetNames.isEmpty()) add(EqEdge.TOP)
                    if (index == bands.lastIndex) add(EqEdge.BOTTOM)
                }
                EqBandRow(
                    label = formatHz(bandFrequencies.getOrNull(index) ?: 0),
                    levelDb = levelDb,
                    levelMinDb = bandLevelMin / 100,
                    levelMaxDb = bandLevelMax / 100,
                    onFocusChange = { focused -> focusedEdges.value = if (focused) edges else emptySet() },
                    onStep = { delta ->
                        val next = (levelDb + delta)
                            .coerceIn(bandLevelMin / 100, bandLevelMax / 100)
                        onSetBand(index, next)
                    }
                )
            }
        }
    }
}

@Composable
private fun EqChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(120),
        label = "eqChipScale"
    )

    Text(
        text = if (isSelected) "✓ $label" else label,
        fontSize = 14.sp,
        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
        color = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (isFocused) Modifier.border(
                    3.dp,
                    if (isSelected) SelectedFocusBorder else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun EqBandRow(
    label: String,
    levelDb: Int,
    levelMinDb: Int,
    levelMaxDb: Int,
    onFocusChange: (Boolean) -> Unit = {},
    onStep: (Int) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = if (levelMaxDb > levelMinDb) {
        (levelDb - levelMinDb).toFloat() / (levelMaxDb - levelMinDb).toFloat()
    } else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isFocused) PlayerColors.TextPrimary else PlayerColors.TextSecondary
            )
            Text(
                text = "${if (levelDb > 0) "+" else ""}$levelDb dB",
                fontSize = 13.sp,
                color = PlayerColors.TextTertiary
            )
        }
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChange(it.isFocused)
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> { onStep(-BAND_STEP_DB); true }
                        Key.DirectionRight -> { onStep(BAND_STEP_DB); true }
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
                            .size(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PlayerColors.TextPrimary)
                    )
                }
            }
        }
    }
}

private fun formatHz(freq: Int): String = when {
    freq >= 1000 && freq % 1000 == 0 -> "${freq / 1000} kHz"
    freq >= 1000 -> "%.1f kHz".format(freq / 1000f)
    else -> "$freq Hz"
}

/** 面板焦点边界：记录当前焦点项所在的边界，用于拦截会把焦点带出面板的方向键 */
private enum class EqEdge { LEFT, RIGHT, TOP, BOTTOM }

private fun isEscapeKey(key: Key, edges: Set<EqEdge>): Boolean = when (key) {
    Key.DirectionLeft -> EqEdge.LEFT in edges
    Key.DirectionRight -> EqEdge.RIGHT in edges
    Key.DirectionUp -> EqEdge.TOP in edges
    Key.DirectionDown -> EqEdge.BOTTOM in edges
    else -> false
}
