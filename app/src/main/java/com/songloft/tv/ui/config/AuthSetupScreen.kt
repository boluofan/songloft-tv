package com.songloft.tv.ui.config

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songloft.tv.ui.search.TvKeyboard

private enum class ActiveField { NONE, SERVER_URL, USERNAME, PASSWORD }

@Composable
fun AuthSetupScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        is AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        is AuthState.LoggedIn -> { /* 已登录 */ }
        else -> LoginForm(viewModel)
    }
}

@Composable
private fun LoginForm(viewModel: AuthViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoggingIn.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val configUrl by viewModel.configUrl.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startConfigServer() }

    var activeField by remember { mutableStateOf(ActiveField.NONE) }
    val showKeyboard = activeField != ActiveField.NONE

    BackHandler(enabled = showKeyboard) {
        activeField = ActiveField.NONE
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
        // 表单区域（可滚动）
        Column(
            modifier = Modifier
                .weight(0.6f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 56.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Songloft TV", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("连接到服务器并登录", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(32.dp))

            InputField(
                label = "服务器地址",
                value = serverUrl,
                placeholder = "http://192.168.1.100:58091",
                isActive = activeField == ActiveField.SERVER_URL,
                onActivate = { activeField = ActiveField.SERVER_URL }
            )

            Spacer(Modifier.height(16.dp))

            InputField(
                label = "账号",
                value = username,
                placeholder = "admin",
                isActive = activeField == ActiveField.USERNAME,
                onActivate = { activeField = ActiveField.USERNAME }
            )

            Spacer(Modifier.height(16.dp))

            InputField(
                label = "密码",
                value = password,
                placeholder = "输入密码",
                isPassword = true,
                isActive = activeField == ActiveField.PASSWORD,
                onActivate = { activeField = ActiveField.PASSWORD }
            )

            Spacer(Modifier.height(24.dp))

            val errorMsg = error
            if (!errorMsg.isNullOrEmpty()) {
                Text(
                    errorMsg, fontSize = 14.sp, color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            var btnFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                    .then(
                        if (btnFocused) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(12.dp)
                        ) else Modifier
                    )
                    .onFocusChanged { btnFocused = it.isFocused }
                    .clickable(enabled = !isLoading) { viewModel.login() }
                    .padding(horizontal = 56.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isLoading) "连接并登录中..." else "连接并登录",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        configUrl?.let { url ->
            QrPanel(
                url = url,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(end = 48.dp, top = 24.dp, bottom = 24.dp)
            )
        }
        }

        // 键盘区域（始终显示在底部，仅当有激活字段时响应输入）
        if (showKeyboard) {
            TvKeyboard(
                onKeyPress = { key ->
                    val current = when (activeField) {
                        ActiveField.SERVER_URL -> serverUrl
                        ActiveField.USERNAME -> username
                        ActiveField.PASSWORD -> password
                        else -> return@TvKeyboard
                    }
                    val newValue = when (key) {
                        "←退格" -> if (current.isNotEmpty()) current.substring(0, current.length - 1) else current
                        "清空" -> ""
                        "确定" -> { activeField = ActiveField.NONE; return@TvKeyboard }
                        "空格" -> "$current "
                        else -> "$current$key"
                    }
                    when (activeField) {
                        ActiveField.SERVER_URL -> viewModel.onServerUrlChanged(newValue)
                        ActiveField.USERNAME -> viewModel.onUsernameChanged(newValue)
                        ActiveField.PASSWORD -> viewModel.onPasswordChanged(newValue)
                        else -> {}
                    }
                }
            )
        }
    }
}

@Composable
private fun QrPanel(url: String, modifier: Modifier = Modifier) {
    val qrBitmap = remember(url) { generateQrBitmap(url) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "扫码配置",
                modifier = Modifier.size(180.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "手机扫码配置",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "同一局域网内扫码，在手机上填写\n服务器地址和账号密码",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = url,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(
        content, BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.MARGIN to 1)
    )
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) android.graphics.Color.BLACK
        else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.RGB_565)
}

@Composable
private fun InputField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    isActive: Boolean = false,
    onActivate: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Text(
        label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onActivate()
            }
            .clickable { onActivate() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = when {
                value.isNotEmpty() && isPassword -> "●●●●●●"
                value.isNotEmpty() -> value
                else -> placeholder
            },
            fontSize = 16.sp,
            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
