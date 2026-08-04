package com.songloft.tv.data.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "songloft_tv_settings")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        private val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        private val USE_CUSTOM_KEYBOARD = booleanPreferencesKey("use_custom_keyboard")
        private val IGNORED_VERSION_CODE = intPreferencesKey("ignored_version_code")
        private val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val EQ_PRESET = stringPreferencesKey("eq_preset")
        private val EQ_BANDS = stringPreferencesKey("eq_bands")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: 0 }
    val themeColor: Flow<String> = context.dataStore.data.map { it[THEME_COLOR] ?: "indigo" }
    val audioQuality: Flow<String?> = context.dataStore.data.map { it[AUDIO_QUALITY] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val backgroundPlayback: Flow<Boolean> = context.dataStore.data.map { it[BACKGROUND_PLAYBACK] ?: true }
    val useCustomKeyboard: Flow<Boolean> = context.dataStore.data.map { it[USE_CUSTOM_KEYBOARD] ?: true }
    val ignoredVersionCode: Flow<Int> = context.dataStore.data.map { it[IGNORED_VERSION_CODE] ?: 0 }
    // 均衡器配置：开关 / 预设 key（"flat"/"rock"/...，"custom" = 自定义曲线）/ 频段增益 dB（逗号分隔）
    val eqEnabled: Flow<Boolean> = context.dataStore.data.map { it[EQ_ENABLED] ?: false }
    val eqPreset: Flow<String> = context.dataStore.data.map { it[EQ_PRESET] ?: "flat" }
    val eqBands: Flow<String> = context.dataStore.data.map { it[EQ_BANDS] ?: "" }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setThemeColor(name: String) {
        context.dataStore.edit { it[THEME_COLOR] = name }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { it[AUDIO_QUALITY] = quality }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setUseCustomKeyboard(enabled: Boolean) {
        context.dataStore.edit { it[USE_CUSTOM_KEYBOARD] = enabled }
    }

    suspend fun setIgnoredVersionCode(code: Int) {
        context.dataStore.edit { it[IGNORED_VERSION_CODE] = code }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[EQ_ENABLED] = enabled }
    }

    suspend fun setEqPreset(preset: String) {
        context.dataStore.edit { it[EQ_PRESET] = preset }
    }

    suspend fun setEqBands(bands: String) {
        context.dataStore.edit { it[EQ_BANDS] = bands }
    }

    suspend fun setTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN] = access
            it[REFRESH_TOKEN] = refresh
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(REFRESH_TOKEN)
        }
    }
}
