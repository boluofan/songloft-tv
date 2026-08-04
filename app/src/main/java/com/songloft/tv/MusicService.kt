package com.songloft.tv

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.ui.player.PlayerActivity

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 流媒体请求需携带 JWT，token 可能在运行期刷新，故每次创建数据源时读取
        val dataSourceFactory = DataSource.Factory {
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .createDataSource()
                .apply {
                    ApiClient.authInterceptor.accessToken?.let {
                        setRequestProperty("Authorization", "Bearer $it")
                    }
                }
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(playerListener) }

        val sessionActivityIntent = Intent(this, PlayerActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(mediaSessionCallback)
            .build()
    }

    // 均衡器按需创建：media3 在 ExoPlayer 构建时即分配 audioSessionId（AudioTrack 复用同一 id），
    // 无需等待播放开始；失败（如设备无 audiofx HAL）时每次命令到达都会重试
    private fun ensureEqualizer(): Equalizer? {
        equalizer?.let { return it }
        val sessionId = player?.audioSessionId ?: return null
        if (sessionId <= 0) return null
        return try {
            Equalizer(0, sessionId).also {
                equalizer = it
                Log.d(TAG, "均衡器创建成功：${it.numberOfBands} 段，${it.numberOfPresets} 个预设（session=$sessionId）")
            }
        } catch (e: Exception) {
            // 记录失败原因，便于区分设备不支持与权限/会话问题
            Log.w(TAG, "创建均衡器失败，audioSession=$sessionId", e)
            null
        }
    }

    // 音频会话 id 变化时旧均衡器失效，释放后按需重建（media3 中仅显式 setAudioSessionId 触发）
    private val playerListener = object : Player.Listener {
        @OptIn(UnstableApi::class)
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId <= 0) return
            equalizer?.release()
            equalizer = null
            Log.d(TAG, "音频会话变化：$audioSessionId，均衡器按需重建")
        }
    }

    @OptIn(UnstableApi::class)
    private val mediaSessionCallback = object : MediaSession.Callback {
        // 自定义命令必须在此授权，否则分发前被拒（ERROR_PERMISSION_DENIED）
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val available = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(EQ_APPLY, Bundle.EMPTY))
                .add(SessionCommand(EQ_INFO, Bundle.EMPTY))
                .add(SessionCommand(EQ_CHECK, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(available)
                .build()
        }

        @OptIn(UnstableApi::class)
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val eq = ensureEqualizer()
            if (eq == null) {
                Log.w(TAG, "收到 ${customCommand.customAction} 但均衡器不可用（未创建或创建失败）")
                return if (customCommand.customAction == EQ_INFO) {
                    Futures.immediateFuture(
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            Bundle().apply { putBoolean(EXTRA_SUPPORTED, false) }
                        )
                    )
                } else {
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                }
            }
            return Futures.immediateFuture(
                when (customCommand.customAction) {
                    EQ_APPLY -> {
                        val ok = runCatching {
                            eq.enabled = args.getBoolean(EXTRA_ENABLED, false)
                            args.getIntArray(EXTRA_BANDS)?.let { bands ->
                                val count = minOf(bands.size, eq.numberOfBands.toInt())
                                val range = eq.bandLevelRange
                                for (i in 0 until count) {
                                    val level = (bands[i] * 100)
                                        .coerceIn(range[0].toInt(), range[1].toInt())
                                        .toShort()
                                    eq.setBandLevel(i.toShort(), level)
                                }
                            }
                        }.isSuccess
                        Log.d(TAG, "eq/apply 结果：$ok（enabled=${args.getBoolean(EXTRA_ENABLED, false)}）")
                        if (ok) SessionResult(SessionResult.RESULT_SUCCESS)
                        else SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
                    }
                    EQ_INFO -> {
                        Log.d(TAG, "eq/info 返回：${eq.numberOfBands} 段")
                        val extras = Bundle().apply {
                            putBoolean(EXTRA_SUPPORTED, true)
                            val bandCount = eq.numberOfBands.toInt()
                            putInt(EXTRA_BAND_COUNT, bandCount)
                            putIntArray(
                                EXTRA_CENTER_FREQS,
                                IntArray(bandCount) { eq.getCenterFreq(it.toShort()) }
                            )
                            val range = eq.bandLevelRange
                            putInt(EXTRA_LEVEL_MIN, range[0].toInt())
                            putInt(EXTRA_LEVEL_MAX, range[1].toInt())
                            putBoolean(EXTRA_ENABLED, eq.enabled)
                            putIntArray(
                                EXTRA_BANDS,
                                IntArray(bandCount) { eq.getBandLevel(it.toShort()).toInt() / 100 }
                            )
                        }
                        SessionResult(SessionResult.RESULT_SUCCESS, extras)
                    }
                    // 能力校验：静态查询系统 audiofx HAL，不依赖音频会话，设置页开启时使用
                    EQ_CHECK -> {
                        val supported = AudioEffect.queryEffects()
                            ?.any { it.type == AudioEffect.EFFECT_TYPE_EQUALIZER } == true
                        Log.d(TAG, "eq/check：设备支持均衡器 = $supported")
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            Bundle().apply { putBoolean(EXTRA_SUPPORTED, supported) }
                        )
                    }
                    else -> SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
                }
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.stop()
            player?.release()
            release()
            mediaSession = null
            this@MusicService.player = null
        }
        equalizer?.release()
        equalizer = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MusicService"

        const val EQ_APPLY = "eq/apply"
        const val EQ_INFO = "eq/info"
        const val EQ_CHECK = "eq/check"

        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_BANDS = "bands"
        const val EXTRA_SUPPORTED = "supported"
        const val EXTRA_BAND_COUNT = "bandCount"
        const val EXTRA_CENTER_FREQS = "centerFreqs"
        const val EXTRA_LEVEL_MIN = "levelMin"
        const val EXTRA_LEVEL_MAX = "levelMax"
    }
}
