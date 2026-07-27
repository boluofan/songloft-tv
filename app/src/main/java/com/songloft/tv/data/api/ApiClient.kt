package com.songloft.tv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var baseUrl: String = ""
    private var retrofit: Retrofit? = null
    private var api: SongloftApi? = null
    val authInterceptor = AuthInterceptor()

    // 由 AuthRepository 注册，用于持久化刷新后的 token
    @Volatile var onTokensRefreshed: ((access: String, refresh: String) -> Unit)? = null

    fun initialize(url: String) {
        if (url == baseUrl && retrofit != null) return
        baseUrl = url.trimEnd('/')

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(
                TokenAuthenticator(
                    authInterceptor = authInterceptor,
                    refreshUrlProvider = { "${baseUrl}/api/v1/auth/refresh" },
                    onTokensRefreshed = { access, refresh ->
                        onTokensRefreshed?.invoke(access, refresh)
                    }
                )
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val apiUrl = "${baseUrl}/api/v1/"

        retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit?.create(SongloftApi::class.java)
    }

    fun getApi(): SongloftApi {
        return api ?: throw IllegalStateException("ApiClient not initialized. Call initialize(url) first.")
    }

    fun isInitialized(): Boolean = api != null
}
