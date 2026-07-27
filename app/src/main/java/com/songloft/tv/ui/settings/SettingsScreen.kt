package com.songloft.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
            SettingsItem(
                label = "播放音质",
                value = uiState.audioQuality.ifEmpty { "原始" }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("关于") {
            SettingsItem(label = "版本", value = "1.0.0")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "清除服务器配置",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.clearServerConfig() }
                .padding(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "退出登录",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .focusable()
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
                .focusable()
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
    var isFocused by remember { mutableStateOf(false) }
    val isSelected = mode == currentMode

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
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
