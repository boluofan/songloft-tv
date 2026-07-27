package com.songloft.tv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.storage.PreferencesDataStore
import com.songloft.tv.domain.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: Int = 0,
    val serverUrl: String = "",
    val audioQuality: String = "",
    val sleepTimerMinutes: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
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
            playerController.state.collect { s ->
                _uiState.value = _uiState.value.copy(sleepTimerMinutes = s.sleepTimerMinutes)
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { dataStore.setThemeMode(mode) }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch { dataStore.setAudioQuality(quality) }
    }

    fun clearServerConfig() {
        viewModelScope.launch {
            dataStore.setServerUrl("")
            dataStore.clearTokens()
        }
    }
}
