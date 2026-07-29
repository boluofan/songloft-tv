package com.songloft.tv.ui.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val letters = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M")
)

private val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

private val symbols = listOf(".", ",", "'", "!", "@", "-", "_", ":", "/", "?", "&", "#")

private val loginTokens = listOf("http://", "https://", "192.168.", ":58091", ".com", ".cn", ".net", ".top")

enum class TvKeyboardMode { SEARCH, LOGIN }

@Composable
fun TvKeyboard(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    mode: TvKeyboardMode = TvKeyboardMode.SEARCH,
    firstKeyFocusRequester: FocusRequester? = null
) {
    var isShifted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 数字行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            digits.forEachIndexed { index, digit ->
                KeyboardKey(
                    key = digit,
                    onClick = { onKeyPress(digit) },
                    width = 56.dp,
                    modifier = if (index == 0 && firstKeyFocusRequester != null)
                        Modifier.focusRequester(firstKeyFocusRequester) else Modifier
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        // 字母行
        letters.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { letter ->
                    val display = if (isShifted) letter else letter.lowercase()
                    KeyboardKey(
                        key = display,
                        onClick = {
                            onKeyPress(if (isShifted) letter else letter.lowercase())
                            isShifted = false
                        },
                        width = 56.dp,
                        isActionKey = false
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        // 特殊符号行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            symbols.forEach { sym ->
                KeyboardKey(key = sym, onClick = { onKeyPress(sym) }, width = 56.dp)
                Spacer(Modifier.width(8.dp))
            }
        }

        // 登录页网络快捷符号行
        if (mode == TvKeyboardMode.LOGIN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                loginTokens.forEach { token ->
                    KeyboardKey(
                        key = token,
                        onClick = { onKeyPress(token) },
                        width = if (token.length > 5) 96.dp else 68.dp,
                        isActionKey = true
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        // 操作键行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKey("空格", onClick = { onKeyPress(" ") }, width = 136.dp)
            Spacer(Modifier.width(8.dp))
            KeyboardKey(
                "退格",
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = { onKeyPress("←退格") },
                width = 100.dp,
                isActionKey = true
            )
            Spacer(Modifier.width(8.dp))
            KeyboardKey(
                "大小写",
                icon = Icons.Rounded.KeyboardCapslock,
                onClick = { isShifted = !isShifted },
                width = 72.dp,
                isActionKey = true,
                isHighlighted = isShifted
            )
            Spacer(Modifier.width(8.dp))
            KeyboardKey("确定", onClick = { onKeyPress("确定") }, width = 100.dp, isActionKey = true)
            Spacer(Modifier.width(8.dp))
            KeyboardKey("清空", onClick = { onKeyPress("清空") }, width = 100.dp, isActionKey = true)
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
