package com.songloft.tv.ui.settings

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.storage.PreferencesDataStore
import com.songloft.tv.domain.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: Int = 0,
    val serverUrl: String = "",
    val audioQuality: String = "",
    val backgroundPlayback: Boolean = true,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemaining: Int = 0,
    val sleepAfterSongs: Int = 0,
    val sleepAfterSongsRemaining: Int = 0,
    val logExportStatus: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: PreferencesDataStore,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            dataStore.serverUrl.collect { url ->
                _uiState.value = _uiState.value.copy(serverUrl = url ?: "")
            }
        }
        viewModelScope.launch {
            dataStore.audioQuality.collect { q ->
                _uiState.value = _uiState.value.copy(audioQuality = q ?: "")
            }
        }
        viewModelScope.launch {
            dataStore.backgroundPlayback.collect { enabled ->
                _uiState.value = _uiState.value.copy(backgroundPlayback = enabled)
            }
        }
        viewModelScope.launch {
            playerController.state.collect { s ->
                _uiState.value = _uiState.value.copy(
                    sleepTimerMinutes = s.sleepTimerMinutes,
                    sleepTimerRemaining = s.sleepTimerRemaining,
                    sleepAfterSongs = s.sleepAfterSongs,
                    sleepAfterSongsRemaining = s.sleepAfterSongsRemaining
                )
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
    }

    fun setSleepAfterSongs(count: Int) {
        playerController.setSleepAfterSongs(count)
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { dataStore.setThemeMode(mode) }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch { dataStore.setAudioQuality(quality) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch { dataStore.setBackgroundPlayback(enabled) }
    }

    fun clearServerConfig() {
        viewModelScope.launch {
            dataStore.setServerUrl("")
            dataStore.clearTokens()
        }
    }

    fun exportLogs() {
        _uiState.value = _uiState.value.copy(logExportStatus = "正在导出…")
        viewModelScope.launch(Dispatchers.IO) {
            val status = runCatching {
                val fileName = "songloft-tv-log-" +
                    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
                val process = Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-v", "threadtime"))
                process.inputStream.bufferedReader().use { reader ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: throw IllegalStateException("无法创建下载文件")
                        resolver.openOutputStream(uri)!!.bufferedWriter().use { writer ->
                            reader.forEachLine { writer.appendLine(sanitizeLogLine(it)) }
                        }
                        "已导出到 下载/$fileName（已脱敏）"
                    } else {
                        val dir = context.getExternalFilesDir(null)
                            ?: throw IllegalStateException("外部存储不可用")
                        val file = File(dir, fileName)
                        file.bufferedWriter().use { writer ->
                            reader.forEachLine { writer.appendLine(sanitizeLogLine(it)) }
                        }
                        "已导出到 ${file.absolutePath}（已脱敏）"
                    }
                }
            }.getOrElse { e -> "导出失败：${e.message}" }
            _uiState.value = _uiState.value.copy(logExportStatus = status)
        }
    }

    private fun sanitizeLogLine(line: String): String {
        var s = line
        // HTTP 头：Authorization / Cookie / Set-Cookie
        s = s.replace(SENSITIVE_HEADER_REGEX, "$1: ***")
        // JSON 字段：token / password / secret 等
        s = s.replace(SENSITIVE_JSON_REGEX, "$1***$2")
        // URL 参数或 key=value 形式的 token / password
        s = s.replace(SENSITIVE_PARAM_REGEX, "$1***")
        // 裸 JWT
        s = s.replace(JWT_REGEX, "***.***.***")
        return s
    }

    private companion object {
        val SENSITIVE_HEADER_REGEX =
            Regex("(?i)\\b(authorization|cookie|set-cookie|x-api-key)\\s*:\\s*.*")
        val SENSITIVE_JSON_REGEX =
            Regex("(?i)(\"(?:access_token|refresh_token|token|password|secret)\"\\s*:\\s*\")[^\"]*(\")")
        val SENSITIVE_PARAM_REGEX =
            Regex("(?i)\\b((?:access_token|refresh_token|token|password|secret)=)[^&\\s\"']+")
        val JWT_REGEX =
            Regex("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b")
    }
}
