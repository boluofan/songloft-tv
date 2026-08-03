package com.songloft.tv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songloft.tv.ui.navigation.LocalTabBarBridge
import com.songloft.tv.ui.theme.SelectedFocusBorder
import com.songloft.tv.ui.theme.seedColorFor
import com.songloft.tv.ui.update.UpdateViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onConfigureServer: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    var backButtonHasFocus by remember { mutableStateOf(false) }
    val bridge = LocalTabBarBridge.current
    val scope = rememberCoroutineScope()

    // 焦点已在返回按钮且页面在顶部时禁用，返回键穿透到外层 BackHandler 直接返回上一级
    BackHandler(
        enabled = bridge?.hasFocus != true && !(backButtonHasFocus && scrollState.value == 0)
    ) {
        if (scrollState.value > 0) {
            scope.launch {
                runCatching { topFocus.requestFocus() }
                scrollState.animateScrollTo(0)
            }
        } else {
            scope.launch {
                runCatching { topFocus.requestFocus() }
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { topFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack, focusRequester = topFocus, onFocusChanged = { backButtonHasFocus = it })
            Spacer(Modifier.width(16.dp))
            Text(
                text = "设置",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("主题模式") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOption("跟随系统", 0, uiState.themeMode) { viewModel.setThemeMode(0) }
                ThemeOption("浅色", 1, uiState.themeMode) { viewModel.setThemeMode(1) }
                ThemeOption("深色", 2, uiState.themeMode) { viewModel.setThemeMode(2) }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("主题色调") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeColorOption("黛青蓝", "indigo", uiState.themeColor) { viewModel.setThemeColor("indigo") }
                ThemeColorOption("薄荷绿", "emerald", uiState.themeColor) { viewModel.setThemeColor("emerald") }
                ThemeColorOption("珊瑚粉", "sakura", uiState.themeColor) { viewModel.setThemeColor("sakura") }
                ThemeColorOption("蜜橘橙", "honey", uiState.themeColor) { viewModel.setThemeColor("honey") }
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

        SettingsSection("音频格式（服务端转码，视频/多音轨文件不受影响）") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QualityOption("原始", "", uiState.audioQuality) { viewModel.setAudioQuality("") }
                QualityOption("MP3", "mp3", uiState.audioQuality) { viewModel.setAudioQuality("mp3") }
                QualityOption("FLAC", "flac", uiState.audioQuality) { viewModel.setAudioQuality("flac") }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("背景播放（退出应用后继续播放）") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionChip("是", uiState.backgroundPlayback) { viewModel.setBackgroundPlayback(true) }
                OptionChip("否", !uiState.backgroundPlayback) { viewModel.setBackgroundPlayback(false) }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("使用自定义键盘（关闭后使用系统键盘输入）") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionChip("是", uiState.useCustomKeyboard) { viewModel.setUseCustomKeyboard(true) }
                OptionChip("否", !uiState.useCustomKeyboard) { viewModel.setUseCustomKeyboard(false) }
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
                    OptionChip("播完本首", uiState.sleepAfterSongs == 1) { viewModel.setSleepAfterSongs(1) }
                    OptionChip("播完 3 首", uiState.sleepAfterSongs == 3) { viewModel.setSleepAfterSongs(3) }
                    OptionChip("播完 5 首", uiState.sleepAfterSongs == 5) { viewModel.setSleepAfterSongs(5) }
                    OptionChip("播完 10 首", uiState.sleepAfterSongs == 10) { viewModel.setSleepAfterSongs(10) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("日志") {
            SettingsItem(
                label = "导出日志",
                value = uiState.logExportStatus.ifEmpty { "导出运行日志用于排查问题" },
                onClick = { viewModel.exportLogs() }
            )
        }

        Spacer(Modifier.height(24.dp))

        var showHelpDialog by remember { mutableStateOf(false) }
        SettingsSection("帮助") {
            SettingsItem(
                label = "操作说明",
                value = "操作及按键说明",
                onClick = { showHelpDialog = true }
            )
        }
        if (showHelpDialog) {
            HelpDialog(onDismiss = { showHelpDialog = false })
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection("关于") {
            val context = LocalContext.current
            val updateViewModel: UpdateViewModel = hiltViewModel()
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "未知"
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var checkUpdateFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (checkUpdateFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("版本", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(12.dp))
                        Text(versionName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text(
                        text = "检查更新",
                        fontSize = 14.sp,
                        fontWeight = if (checkUpdateFocused) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (checkUpdateFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .then(
                                if (checkUpdateFocused) Modifier.border(
                                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .onFocusChanged { checkUpdateFocused = it.isFocused }
                            .clickable { updateViewModel.manualCheck() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                SettingsItem(label = "项目地址", value = "github.com/boluofan/songloft-tv")
                SettingsItem(
                    label = "开源组件",
                    value = "Jetpack Compose · Media3 · Retrofit · Coil · Hilt"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        var clearFocused by remember { mutableStateOf(false) }
        Text(
            text = "清除配置",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (clearFocused) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .then(
                    if (clearFocused) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)
                    ) else Modifier
                )
                .onFocusChanged { clearFocused = it.isFocused }
                .clickable {
                    viewModel.clearServerConfig()
                    onConfigureServer()
                }
                .padding(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        var logoutFocused by remember { mutableStateOf(false) }
        Text(
            text = "退出登录",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (logoutFocused) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .then(
                    if (logoutFocused) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)
                    ) else Modifier
                )
                .onFocusChanged { logoutFocused = it.isFocused }
                .clickable { onLogout() }
                .padding(12.dp)
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val closeFocus = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 36.dp, vertical = 28.dp)
        ) {
            Text(
                text = "操作说明",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HelpBlock(
                        title = "首页",
                        lines = listOf(
                            "列表已滚动时按返回键：快速回到顶部并聚焦顶部按钮",
                            "已在顶部时按返回键：焦点跳到底部 Tab 栏",
                            "焦点在底部 Tab 栏时按返回键：弹出退出应用确认"
                        )
                    )
                    HelpBlock(
                        title = "其他一级界面（搜索 / 歌单 / 我的）",
                        lines = listOf(
                            "列表已滚动时按返回键：快速回到顶部并聚焦顶部按钮",
                            "在顶部但焦点不在顶部按钮时按返回键：焦点跳到顶部按钮",
                            "焦点已在顶部按钮或底部 Tab 栏时按返回键：回到首页",
                            "回到首页后继续按返回键：弹出退出应用确认"
                        )
                    )
                    HelpBlock(
                        title = "二级界面（歌单详情 / 筛选歌曲 / 设置等）",
                        lines = listOf(
                            "进入时焦点默认在左上角【返回】按钮，直接按返回键即回上一级",
                            "列表已滚动时按返回键：先回到顶部并聚焦【返回】按钮",
                            "在顶部但焦点不在【返回】按钮时按返回键：焦点跳到【返回】按钮",
                            "焦点已在【返回】按钮时按返回键：直接回上一级",
                            "焦点在底部 Tab 栏时按返回键：直接回上一级"
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HelpBlock(
                        title = "播放器",
                        lines = listOf(
                            "控制栏隐藏时：左/右键单击切上一首/下一首，长按快退/快进",
                            "控制栏隐藏时：按上/下/确认键唤出控制栏",
                            "控制栏 10 秒无操作自动隐藏",
                            "播放列表侧边栏打开时按返回键：关闭侧边栏",
                            "其余情况按返回键：退出播放器（音乐可后台继续播放）"
                        )
                    )
                    HelpBlock(
                        title = "快速聚焦技巧",
                        lines = listOf(
                            "长列表中任意位置按返回键 = 一键回顶，无需长按方向键",
                            "首页在顶部时按返回键：焦点直达底部 Tab 栏",
                            "任意界面连按返回键最终都会回到首页，再按弹出退出确认"
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            var closeFocused by remember { mutableStateOf(false) }
            Text(
                text = "我知道了",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (closeFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (closeFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    .focusRequester(closeFocus)
                    .onFocusChanged { closeFocused = it.isFocused }
                    .clickable { onDismiss() }
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            )
        }
    }

    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }
}

@Composable
private fun HelpBlock(title: String, lines: List<String>) {
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        lines.forEach { line ->
            Text(
                text = "· $line",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
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
            .then(
                if (isFocused) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                ) else Modifier
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
private fun BackButton(onClick: () -> Unit, focusRequester: FocusRequester? = null, onFocusChanged: ((Boolean) -> Unit)? = null) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged?.invoke(it.isFocused)
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            tint = if (isFocused) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ThemeOption(label: String, mode: Int, currentMode: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OptionChip(label, mode == currentMode, modifier, onClick)
}

@Composable
private fun ThemeColorOption(label: String, name: String, currentName: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val isSelected = name == currentName
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(120),
        label = "themeColorScale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .then(
                if (isFocused) Modifier.border(
                    3.dp,
                    if (isSelected) SelectedFocusBorder else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(50))
                .background(seedColorFor(name))
                .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
        )
        Text(
            text = if (isSelected) "✓ $label" else label,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isFocused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun QualityOption(label: String, value: String, currentValue: String, onClick: () -> Unit) {
    OptionChip(label, value == currentValue, Modifier, onClick)
}

@Composable
private fun OptionChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(120),
        label = "optionChipScale"
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
                    // 选中项聚焦：白色粗描边与 ✓ 同色但更粗更亮，配合缩放一眼可辨
                    3.dp,
                    if (isSelected) SelectedFocusBorder else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
