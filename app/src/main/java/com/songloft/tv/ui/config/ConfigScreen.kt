package com.songloft.tv.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
    onConnected: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "Songloft TV",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "连接到 Songloft 服务器以开始使用",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text = "服务器地址",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ConfigTextField(
            value = uiState.serverUrl,
            placeholder = "http://192.168.1.100:58091",
            onValueChange = { viewModel.onServerUrlChanged(it) }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "用户名",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ConfigTextField(
            value = uiState.username,
            placeholder = "admin",
            onValueChange = { viewModel.onUsernameChanged(it) }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "密码",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ConfigTextField(
            value = uiState.password,
            placeholder = "********",
            onValueChange = { viewModel.onPasswordChanged(it) }
        )

        Spacer(Modifier.height(32.dp))

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (uiState.isConnected) {
            Text(
                text = "✓ 连接成功！",
                fontSize = 16.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        var isFocused by remember { mutableStateOf(false) }
        Text(
            text = if (uiState.isTesting) "正在连接..." else "连接服务器",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                .focusable()
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(enabled = !uiState.isTesting) { viewModel.testConnection() }
                .padding(horizontal = 48.dp, vertical = 16.dp)
        )

        if (uiState.isConnected) {
            Spacer(Modifier.height(16.dp))
            var enterFocused by remember { mutableStateOf(false) }
            Text(
                text = "开始使用 →",
                fontSize = 16.sp,
                color = if (enterFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (enterFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    .focusable()
                    .onFocusChanged { enterFocused = it.isFocused }
                    .clickable { onConnected() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ConfigTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (isFocused) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (value.isNotEmpty()) value else placeholder,
            fontSize = 16.sp,
            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
