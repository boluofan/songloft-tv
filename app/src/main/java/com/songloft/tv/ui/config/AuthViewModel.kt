package com.songloft.tv.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.UrlHelper
import com.songloft.tv.data.repository.AuthRepository
import com.songloft.tv.data.storage.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data object NotConfigured : AuthState()
    data object Configured : AuthState()
    data class LoggedIn(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: PreferencesDataStore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _serverUrl = MutableStateFlow("https://songloft.boluofan.top:23456/")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow("boluofan")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("Boluofan.123")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val storedUrl = dataStore.serverUrl.first()
            if (!storedUrl.isNullOrEmpty()) {
                _serverUrl.value = storedUrl
                tryAutoLogin()
            } else {
                _authState.value = AuthState.NotConfigured
            }
        }
    }

    private suspend fun tryAutoLogin() {
        val result = authRepository.tryAutoLogin()
        _authState.value = if (result) AuthState.LoggedIn("admin")
        else AuthState.Configured
    }

    fun onServerUrlChanged(url: String) {
        _serverUrl.value = url
        _error.value = null
    }

    fun onUsernameChanged(username: String) {
        _username.value = username
        _error.value = null
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
        _error.value = null
    }

    fun login() {
        val url = _serverUrl.value.trim()
        val username = _username.value.trim()
        val password = _password.value

        if (url.isBlank()) { _error.value = "请输入服务器地址"; return }
        if (username.isBlank()) { _error.value = "请输入账号"; return }
        if (password.isBlank()) { _error.value = "请输入密码"; return }

        _isLoggingIn.value = true
        _error.value = null

        viewModelScope.launch {
            authRepository.login(url, username, password)
                .onSuccess {
                    _isLoggingIn.value = false
                    _authState.value = AuthState.LoggedIn(username)
                }
                .onFailure { e ->
                    _isLoggingIn.value = false
                    _error.value = e.message ?: "登录失败"
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.NotConfigured
        }
    }

    fun resetToConfig() {
        _authState.value = AuthState.NotConfigured
        _serverUrl.value = ""
        _username.value = ""
        _password.value = ""
        _error.value = null
    }
}
