package com.songloft.tv.ui.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 参考 TV 端专业输入法（YouTube/Google TV）：6 列方阵布局，
// 相比 10 列 QWERTY 大幅缩短 D-Pad 平均移动距离
private val letterGrid = listOf(
    listOf("a", "b", "c", "d", "e", "f"),
    listOf("g", "h", "i", "j", "k", "l"),
    listOf("m", "n", "o", "p", "q", "r"),
    listOf("s", "t", "u", "v", "w", "x"),
    listOf("y", "z", "1", "2", "3", "4"),
    listOf("5", "6", "7", "8", "9", "0")
)

private val symbolGrid = listOf(
    listOf(".", ",", "'", "\"", "!", "?"),
    listOf("@", "#", "$", "%", "&", "*"),
    listOf("-", "_", "+", "=", "/", "\\"),
    listOf(":", ";", "(", ")", "[", "]"),
    listOf("{", "}", "<", ">", "|", "~"),
    listOf("^", "`", "·", "。", "！", "？")
)

private val loginTokens = listOf(".", "/", ":", "http://", "https://", "192.168.", ":58091", ".com", ".cn", ".net", ".top")

private val keySpacing = 8.dp
private val gridKeyWidth = 60.dp
private val actionKeyWidth = 108.dp

enum class TvKeyboardMode { SEARCH, LOGIN }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TvKeyboard(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    mode: TvKeyboardMode = TvKeyboardMode.SEARCH,
    firstKeyFocusRequester: FocusRequester? = null
) {
    var isShifted by remember { mutableStateOf(false) }
    var showSymbols by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // focusProperties 必须在 focusGroup() 之前，才能作用于焦点组自身的 exit
            // 登录页禁止 ↑ 跳出键盘（防止焦点跑到二维码等区域）；搜索页允许跳出，
            // 以便 D-Pad 上移回到搜索框/候选词区域
            .focusProperties {
                exit = { direction ->
                    if (direction == FocusDirection.Up && mode == TvKeyboardMode.LOGIN) FocusRequester.Cancel
                    else FocusRequester.Default
                }
            }
            .focusGroup()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(keySpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing * 2)) {
            // 字符方阵
            Column(verticalArrangement = Arrangement.spacedBy(keySpacing)) {
                val grid = if (showSymbols) symbolGrid else letterGrid
                grid.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
                        row.forEach { key ->
                            val display = if (isShifted && !showSymbols) key.uppercase() else key
                            KeyboardKey(
                                key = display,
                                onClick = {
                                    onKeyPress(display)
                                    isShifted = false
                                },
                                width = gridKeyWidth,
                                // 默认焦点放在方阵中心附近的 v，到任意键的平均距离最短
                                modifier = if (key == "v" && !showSymbols && firstKeyFocusRequester != null)
                                    Modifier.focusRequester(firstKeyFocusRequester) else Modifier
                            )
                        }
                    }
                }
            }

            // 功能键列：与方阵等高，任意行向右一步可达
            Column(verticalArrangement = Arrangement.spacedBy(keySpacing)) {
                KeyboardKey(
                    "退格",
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    onClick = { onKeyPress("←退格") },
                    width = actionKeyWidth,
                    isActionKey = true
                )
                KeyboardKey("清空", onClick = { onKeyPress("清空") }, width = actionKeyWidth, isActionKey = true)
                KeyboardKey("确定", onClick = { onKeyPress("确定") }, width = actionKeyWidth, isActionKey = true)
                KeyboardKey(
                    "大小写",
                    icon = Icons.Rounded.KeyboardCapslock,
                    onClick = { isShifted = !isShifted },
                    width = actionKeyWidth,
                    isActionKey = true,
                    isHighlighted = isShifted
                )
                KeyboardKey(
                    if (showSymbols) "abc" else "#+=",
                    onClick = { showSymbols = !showSymbols },
                    width = actionKeyWidth,
                    isActionKey = true,
                    isHighlighted = showSymbols
                )
                KeyboardKey("空格", onClick = { onKeyPress(" ") }, width = actionKeyWidth, isActionKey = true)
            }
        }

        // 登录页网络快捷符号行
        if (mode == TvKeyboardMode.LOGIN) {
            Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
                loginTokens.forEach { token ->
                    KeyboardKey(
                        key = token,
                        onClick = { onKeyPress(token) },
                        width = when {
                            token.length == 1 -> 48.dp
                            token.length > 5 -> 96.dp
                            else -> 68.dp
                        },
                        isActionKey = true
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    key: String,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActionKey: Boolean = false,
    isHighlighted: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(120),
        label = "keyScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .width(width)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> MaterialTheme.colorScheme.primary
                    isHighlighted -> MaterialTheme.colorScheme.tertiary
                    isActionKey -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .then(
                if (isFocused) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val contentColor = if (isFocused) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = key,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = key,
                fontSize = if (isActionKey) 14.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
