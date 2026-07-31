package com.songloft.tv.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.songloft.tv.BuildConfig
import com.songloft.tv.util.ApkInstaller
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onStartDownload: () -> Unit,
    onIgnore: () -> Unit,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state == UpdateUiState.Idle) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                UpdateUiState.Idle -> {}
                UpdateUiState.Checking -> CheckingPanel()
                UpdateUiState.UpToDate -> UpToDatePanel(onDismiss)
                is UpdateUiState.CheckFailed -> CheckFailedPanel(state.message, onRetryCheck, onDismiss)
                is UpdateUiState.UpdateAvailable -> AvailablePanel(state, onStartDownload, onIgnore, onDismiss)
                is UpdateUiState.Downloading -> DownloadingPanel(state, onDismiss)
                is UpdateUiState.DownloadFailed -> DownloadFailedPanel(state.message, onStartDownload, onDismiss)
                is UpdateUiState.ReadyToInstall -> ReadyToInstallPanel(state, onDismiss)
            }
        }
    }
}

@Composable
private fun CheckingPanel() {
    DialogTitle("检查更新")
    Spacer(Modifier.height(16.dp))
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    DialogBody("正在检查更新…")
}

@Composable
private fun UpToDatePanel(onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogTitle("已是最新版本")
    Spacer(Modifier.height(12.dp))
    DialogBody("当前版本 v${BuildConfig.VERSION_NAME}")
    Spacer(Modifier.height(24.dp))
    DialogButton("确定", onClick = onDismiss, modifier = Modifier.focusRequester(focus))
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun CheckFailedPanel(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogTitle("检查更新失败")
    Spacer(Modifier.height(12.dp))
    DialogBody(message)
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DialogButton("重试", onClick = onRetry, modifier = Modifier.focusRequester(focus))
        DialogButton("关闭", onClick = onDismiss)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun AvailablePanel(
    state: UpdateUiState.UpdateAvailable,
    onStartDownload: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val focus = remember { FocusRequester() }
    DialogTitle("发现新版本 v${state.info.versionName}")
    Spacer(Modifier.height(12.dp))
    DialogBody("当前版本 v${BuildConfig.VERSION_NAME} → 新版本 v${state.info.versionName}")
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DialogButton("立即更新", onClick = onStartDownload, modifier = Modifier.focusRequester(focus))
        DialogButton("稍后再说", onClick = onDismiss)
        if (state.fromAutoCheck) {
            DialogButton("忽略此版本", onClick = onIgnore)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun DownloadingPanel(state: UpdateUiState.Downloading, onCancel: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogTitle("正在下载 v${state.info.versionName}")
    Spacer(Modifier.height(16.dp))
    if (state.totalBytes > 0) {
        val fraction = (state.bytesRead.toFloat() / state.totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        DialogBody(
            "${(fraction * 100).toInt()}% · ${formatMb(state.bytesRead)}/${formatMb(state.totalBytes)}" +
                if (state.mirrorLabel.isNotEmpty()) " · ${state.mirrorLabel}" else ""
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        DialogBody(
            if (state.mirrorLabel.isEmpty()) "正在连接…"
            else "已下载 ${formatMb(state.bytesRead)} · ${state.mirrorLabel}"
        )
    }
    Spacer(Modifier.height(24.dp))
    DialogButton("取消", onClick = onCancel, modifier = Modifier.focusRequester(focus))
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun DownloadFailedPanel(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogTitle("下载失败")
    Spacer(Modifier.height(12.dp))
    DialogBody(message)
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DialogButton("重试", onClick = onRetry, modifier = Modifier.focusRequester(focus))
        DialogButton("取消", onClick = onDismiss)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun ReadyToInstallPanel(state: UpdateUiState.ReadyToInstall, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    var installFailed by remember { mutableStateOf(false) }

    DialogTitle("下载完成")
    Spacer(Modifier.height(12.dp))
    DialogBody(
        if (installFailed) "无法调起系统安装器，请在设置中允许安装未知应用后重试，或到项目主页手动下载"
        else "即将调起系统安装器安装 v${state.info.versionName}"
    )
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DialogButton(
            "重新安装",
            onClick = { installFailed = !ApkInstaller.install(context, state.apkFile) },
            modifier = Modifier.focusRequester(focus)
        )
        DialogButton("关闭", onClick = onDismiss)
    }
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        installFailed = !ApkInstaller.install(context, state.apkFile)
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun DialogBody(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    )
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = if (isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

private fun formatMb(bytes: Long): String =
    String.format(Locale.US, "%.1fMB", bytes / 1024f / 1024f)
