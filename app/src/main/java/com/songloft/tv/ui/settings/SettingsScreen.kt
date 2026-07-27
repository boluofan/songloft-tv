package com.songloft.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onConfigureServer: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Text(
            text = "设置",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        SettingsSection("主题模式") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOption("跟随系统", 0, uiState.themeMode) { viewModel.setThemeMode(0) }
                ThemeOption("浅色", 1, uiState.themeMode) { viewModel.setThemeMode(1) }
                ThemeOption("深色", 2, uiState.themeMode) { viewModel.setThemeMode(2) }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("服务器") {
            SettingsItem(
                label = "当前服务器",
                value = uiState.serverUrl.ifEmpty { "未配置" },
                onClick = onConfigureServer
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("音质") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QualityOption("原始", "", uiState.audioQuality) { viewModel.setAudioQuality("") }
                QualityOption("MP3", "mp3", uiState.audioQuality) { viewModel.setAudioQuality("mp3") }
                QualityOption("FLAC", "flac", uiState.audioQuality) { viewModel.setAudioQuality("flac") }
            }
        }

        Spacer(Modifier.height(24.dp))

        val sleepSuffix = when {
            uiState.sleepTimerRemaining > 0 -> "（剩余 ${uiState.sleepTimerRemaining} 分钟）"
            uiState.sleepAfterSongsRemaining > 0 -> "（剩余 ${uiState.sleepAfterSongsRemaining} 首）"
            else -> ""
        }
        SettingsSection("睡眠定时$sleepSuffix") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionChip("关闭", uiState.sleepTimerMinutes == 0 && uiState.sleepAfterSongs == 0) { viewModel.setSleepTimer(0) }
                    OptionChip("30 分钟", uiState.sleepTimerMinutes == 30) { viewModel.setSleepTimer(30) }
                    OptionChip("60 分钟", uiState.sleepTimerMinutes == 60) { viewModel.setSleepTimer(60) }
                    OptionChip("90 分钟", uiState.sleepTimerMinutes == 90) { viewModel.setSleepTimer(90) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionChip("播完 3 首", uiState.sleepAfterSongs == 3) { viewModel.setSleepAfterSongs(3) }
                    OptionChip("播完 5 首", uiState.sleepAfterSongs == 5) { viewModel.setSleepAfterSongs(5) }
                    OptionChip("播完 10 首", uiState.sleepAfterSongs == 10) { viewModel.setSleepAfterSongs(10) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("关于") {
            SettingsItem(label = "版本", value = "1.0.0")
        }

        Spacer(Modifier.height(24.dp))

        var clearFocused by remember { mutableStateOf(false) }
        Text(
            text = "清除服务器配置",
            fontSize = 14.sp,
            fontWeight = if (clearFocused) FontWeight.Bold else FontWeight.Normal,
            color = if (clearFocused) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (clearFocused) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
                .onFocusChanged { clearFocused = it.isFocused }
                .clickable { viewModel.clearServerConfig() }
                .padding(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        var logoutFocused by remember { mutableStateOf(false) }
        Text(
            text = "退出登录",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (logoutFocused) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (logoutFocused) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
                .onFocusChanged { logoutFocused = it.isFocused }
                .clickable { onLogout() }
                .padding(12.dp)
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .clickable { onClick() }
            else Modifier)
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun ThemeOption(label: String, mode: Int, currentMode: Int, onClick: () -> Unit) {
    OptionChip(label, mode == currentMode, onClick)
}

@Composable
private fun QualityOption(label: String, value: String, currentValue: String, onClick: () -> Unit) {
    OptionChip(label, value == currentValue, onClick)
}

@Composable
private fun OptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
        color = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
