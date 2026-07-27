package com.songloft.tv.data.api

import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * 401 时用 refresh_token 换取新 token 并重试原请求，
 * 刷新失败则放弃，由上层退回登录页。
 */
class TokenAuthenticator(
    private val authInterceptor: AuthInterceptor,
    private val refreshUrlProvider: () -> String,
    private val onTokensRefreshed: (access: String, refresh: String) -> Unit
) : Authenticator {

    private val refreshClient = OkHttpClient()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        val refresh = authInterceptor.refreshToken ?: return null
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            // 其他请求可能已完成刷新，直接用新 token 重试
            val current = authInterceptor.accessToken
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (current != null && current != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val tokens = requestRefresh(refresh) ?: return null
            authInterceptor.accessToken = tokens.accessToken
            authInterceptor.refreshToken = tokens.refreshToken
            onTokensRefreshed(tokens.accessToken, tokens.refreshToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
        }
    }

    private fun requestRefresh(refreshToken: String): LoginResponse? = runCatching {
        val body = gson.toJson(mapOf("refresh_token" to refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(refreshUrlProvider()).post(body).build()
        refreshClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            gson.fromJson(resp.body?.string(), LoginResponse::class.java)
        }
    }.getOrNull()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
