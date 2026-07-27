package com.songloft.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.songloft.tv.data.api.ApiClient
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class SongloftTvApp : Application(), ImageLoaderFactory {

    // 封面接口位于 JWT 鉴权路径下，Coil 需复用 AuthInterceptor 携带 token，否则 401 无法加载
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiClient.authInterceptor)
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
