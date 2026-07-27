package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.storage.PreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: PreferencesDataStore
) {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                ApiClient.initialize(serverUrl)
                com.songloft.tv.data.api.UrlHelper.initialize(serverUrl)
                val response = ApiClient.getApi().login(
                    mapOf("username" to username, "password" to password)
                )
                ApiClient.authInterceptor.accessToken = response.accessToken
                ApiClient.authInterceptor.refreshToken = response.refreshToken
                dataStore.setServerUrl(serverUrl)
                dataStore.setTokens(response.accessToken, response.refreshToken)
                true
            }
        }

    suspend fun tryAutoLogin(): Boolean {
        return withContext(Dispatchers.IO) {
            val token = dataStore.accessToken.first()
            val url = dataStore.serverUrl.first()

            if (token.isNullOrEmpty() || url.isNullOrEmpty()) return@withContext false

            runCatching {
                ApiClient.initialize(url)
                com.songloft.tv.data.api.UrlHelper.initialize(url)
                ApiClient.authInterceptor.accessToken = token
                ApiClient.getApi().health()
                true
            }.getOrDefault(false)
        }
    }

    suspend fun logout() {
        dataStore.clearTokens()
        ApiClient.authInterceptor.accessToken = null
        ApiClient.authInterceptor.refreshToken = null
    }
}
