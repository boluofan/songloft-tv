package com.songloft.tv.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        accessToken?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
