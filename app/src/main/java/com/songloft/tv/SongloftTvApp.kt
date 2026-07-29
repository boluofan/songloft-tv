package com.songloft.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.TlsCompat
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class SongloftTvApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        TlsCompat.initialize(this)
    }

    // 封面接口位于 JWT 鉴权路径下，Coil 需复用 AuthInterceptor 携带 token，否则 401 无法加载
    override fun newImageLoader(): ImageLoader {
        val client = TlsCompat.apply(
            OkHttpClient.Builder()
                .addInterceptor(ApiClient.authInterceptor)
        ).build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
