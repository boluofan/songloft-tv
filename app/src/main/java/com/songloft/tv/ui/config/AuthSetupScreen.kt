package com.songloft.tv.ui.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songloft.tv.ui.components.generateQrBitmap
import com.songloft.tv.ui.search.TvKeyboard
import com.songloft.tv.ui.search.TvKeyboardMode

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
    val useCustomKeyboard by viewModel.useCustomKeyboard.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startConfigServer() }

    var activeField by remember { mutableStateOf(ActiveField.NONE) }
    var passwordVisible by remember { mutableStateOf(false) }
    val showKeyboard = activeField != ActiveField.NONE
    val keyboardFocus = remember { FocusRequester() }
    val serverUrlFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    // 键盘收起后把焦点还给之前激活的输入框
    var lastActiveField by remember { mutableStateOf(ActiveField.NONE) }

    val windowInfo = LocalWindowInfo.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        // 冷启动时窗口已聚焦但 AndroidComposeView 尚无 view 焦点，
        // 此时 Compose 的 requestOwnerFocus() 会失败导致焦点请求被静默丢弃，需先补 view 焦点
        repeat(60) {
            withFrameNanos { }
            if (windowInfo.isWindowFocused) {
                view.requestFocus()
                serverUrlFocus.requestFocus()
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            lastActiveField = activeField
            runCatching { keyboardFocus.requestFocus() }
        } else {
            runCatching {
                when (lastActiveField) {
                    ActiveField.SERVER_URL -> serverUrlFocus.requestFocus()
                    ActiveField.USERNAME -> usernameFocus.requestFocus()
                    ActiveField.PASSWORD -> passwordFocus.requestFocus()
                    ActiveField.NONE -> {}
                }
            }
        }
    }

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
                focusRequester = serverUrlFocus,
                isActive = activeField == ActiveField.SERVER_URL,
                useSystemKeyboard = !useCustomKeyboard,
                keyboardType = KeyboardType.Uri,
                onTextChange = viewModel::onServerUrlChanged,
                onActivate = { activeField = ActiveField.SERVER_URL }
            )

            Spacer(Modifier.height(16.dp))

            InputField(
                label = "账号",
                value = username,
                placeholder = "admin",
                focusRequester = usernameFocus,
                isActive = activeField == ActiveField.USERNAME,
                useSystemKeyboard = !useCustomKeyboard,
                onTextChange = viewModel::onUsernameChanged,
                onActivate = { activeField = ActiveField.USERNAME }
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    InputField(
                        label = "密码",
                        value = password,
                        placeholder = "输入密码",
                        focusRequester = passwordFocus,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        isActive = activeField == ActiveField.PASSWORD,
                        useSystemKeyboard = !useCustomKeyboard,
                        onTextChange = viewModel::onPasswordChanged,
                        onActivate = { activeField = ActiveField.PASSWORD }
                    )
                }
                Spacer(Modifier.width(8.dp))
                var eyeFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (eyeFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .onFocusChanged { eyeFocused = it.isFocused }
                        .clickable { passwordVisible = !passwordVisible },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.Visibility
                                      else Icons.Rounded.VisibilityOff,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = if (eyeFocused) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

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

        // 键盘区域（仅自定义键盘模式；系统键盘模式由输入框直接弹出 IME）
        if (useCustomKeyboard && showKeyboard) {
            // 回显栏：键盘可能遮挡表单，这里实时显示当前字段内容（始终明文，便于核对输入）
            val echoLabel = when (activeField) {
                ActiveField.SERVER_URL -> "服务器地址"
                ActiveField.USERNAME -> "账号"
                ActiveField.PASSWORD -> "密码"
                else -> ""
            }
            val echoText = when (activeField) {
                ActiveField.SERVER_URL -> serverUrl
                ActiveField.USERNAME -> username
                ActiveField.PASSWORD -> password
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$echoLabel：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    if (echoText.isEmpty()) "（未输入）" else echoText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (echoText.isEmpty())
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TvKeyboard(
                mode = TvKeyboardMode.LOGIN,
                firstKeyFocusRequester = keyboardFocus,
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

@Composable
private fun InputField(
    label: String,
    value: String,
    placeholder: String,
    focusRequester: FocusRequester,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    isActive: Boolean = false,
    useSystemKeyboard: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChange: (String) -> Unit = {},
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
            .then(
                if (focused) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                ) else Modifier
            )
            // 仅确认键（点击）激活自定义键盘，焦点经过不弹出
            .onFocusChanged { focused = it.isFocused }
            .clickable {
                if (useSystemKeyboard) runCatching { focusRequester.requestFocus() }
                else onActivate()
            }
            // 自定义键盘模式的焦点落在外层框上，系统键盘模式落在内部输入框上
            .then(
                if (!useSystemKeyboard) Modifier.focusRequester(focusRequester) else Modifier
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (useSystemKeyboard) {
            BasicTextField(
                value = value,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (isPassword && !passwordVisible)
                    PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            Text(
                text = when {
                    value.isNotEmpty() && isPassword && !passwordVisible -> "●".repeat(value.length)
                    value.isNotEmpty() -> value
                    else -> placeholder
                },
                fontSize = 16.sp,
                color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
