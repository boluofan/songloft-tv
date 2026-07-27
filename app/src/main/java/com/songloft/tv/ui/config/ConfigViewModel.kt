package com.songloft.tv.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.UrlHelper
import com.songloft.tv.data.storage.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isTesting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val dataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.serverUrl.collect { url ->
                if (!url.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(serverUrl = url)
                }
            }
        }
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun testConnection() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入服务器地址")
            return
        }

        _uiState.value = _uiState.value.copy(isTesting = true, error = null)

        viewModelScope.launch {
            try {
                ApiClient.initialize(url)
                UrlHelper.initialize(url)

                val health = ApiClient.getApi().health()
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    isConnected = true,
                    error = null
                )

                dataStore.setServerUrl(url)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    isConnected = false,
                    error = "连接失败: ${e.message}"
                )
            }
        }
    }
}
