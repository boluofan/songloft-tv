package com.songloft.tv.domain

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.songloft.tv.MusicService
import com.songloft.tv.data.api.UrlHelper
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.model.Track
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.data.storage.PreferencesDataStore
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.math.ln
import kotlin.math.round
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

// 固定均衡器预设（借鉴 songloft-player 的 10 段曲线与中文名）：
// 不吃设备系统预设，名称恒定中文、听感跨设备一致；"custom" 表示手动调出的自定义曲线
private val EQ_PRESETS = linkedMapOf(
    "flat" to ("平坦" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    "rock" to ("摇滚" to listOf(5, 4, 2, 0, -1, 1, 3, 4, 5, 4)),
    "pop" to ("流行" to listOf(-1, 2, 4, 5, 4, 2, 0, -1, -1, -1)),
    "jazz" to ("爵士" to listOf(4, 3, 1, 2, -1, -1, 0, 2, 3, 4)),
    "classical" to ("古典" to listOf(5, 4, 3, 2, -1, -1, 0, 3, 4, 5)),
    "bass_boost" to ("低音增强" to listOf(6, 5, 4, 2, 0, 0, 0, 0, 0, 0)),
    "treble_boost" to ("高音增强" to listOf(0, 0, 0, 0, 0, 0, 2, 4, 5, 6)),
    "vocal" to ("人声" to listOf(-2, -1, 0, 3, 5, 5, 3, 1, 0, -2))
)
private val EQ_PRESET_KEYS = EQ_PRESETS.keys.toList() + "custom"
private val EQ_PRESET_NAMES = EQ_PRESETS.values.map { it.first } + "自定义"

// 预设曲线的参考频点（Hz），与 EQ_PRESETS 的增益一一对应
private val EQ_PRESET_FREQS = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

enum class PlayMode { ORDER, LOOP, SINGLE, RANDOM }

data class PlaybackState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val currentSong: Song? = null,
    val currentTrack: Track? = null,
    // 媒体文件内嵌的多条音轨（如 MKV 中的原唱/伴奏），由 onTracksChanged 检测
    val embeddedTracks: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val duration: Long = 0L,
    val playMode: PlayMode = PlayMode.ORDER,
    // 播放上下文（服务端播放历史）：playlist 传歌单 ID，分面传 artist/album/year 等
    val contextType: String? = null,
    val contextKey: String? = null,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemaining: Int = 0,
    val sleepAfterSongs: Int = 0,
    val sleepAfterSongsRemaining: Int = 0,
    // 均衡器（来自 MusicService 的 audiofx.Equalizer，频段增益单位 dB）
    val eqSupported: Boolean = false,
    val eqEnabled: Boolean = false,
    // 预设 key（"flat"/"rock"/...，"custom" = 自定义曲线），名称与增益见 EQ_PRESETS
    val eqPreset: String = "flat",
    val eqPresetKeys: List<String> = EQ_PRESET_KEYS,
    val eqPresetNames: List<String> = EQ_PRESET_NAMES,
    val eqBands: List<Int> = emptyList(),
    val eqBandFrequencies: List<Int> = emptyList(),
    val eqBandLevelMin: Int = -1500,
    val eqBandLevelMax: Int = 1500
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
    private val dataStore: PreferencesDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioQuality: String? = null

    private var eqEnabledCache: Boolean = false
    private var eqBandsCache: List<Int> = emptyList()

    init {
        scope.launch {
            dataStore.audioQuality.collect { audioQuality = it?.takeIf { q -> q.isNotBlank() } }
        }
        // 均衡器配置持久化闭环：UI 只写 DataStore，这里缓存并推送给 MusicService
        scope.launch {
            combine(dataStore.eqEnabled, dataStore.eqBands) { enabled, bands ->
                enabled to parseBands(bands)
            }.collect { (enabled, bands) ->
                eqEnabledCache = enabled
                eqBandsCache = bands
                _state.update { it.copy(eqEnabled = enabled, eqBands = bands) }
                if (controller != null) sendEqApply(enabled, bands)
            }
        }
        scope.launch {
            dataStore.eqPreset.collect { preset ->
                _state.update { it.copy(eqPreset = preset) }
            }
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previousSong = _state.value.currentSong
            val song = _state.value.queue.firstOrNull { it.id.toString() == mediaItem?.mediaId }
            reportTransition(previousSong, song, reason)
            countDownSleepAfterSongs(previousSong, song, reason)
            _state.update {
                it.copy(
                    currentSong = song,
                    currentIndex = controller?.currentMediaItemIndex ?: -1,
                    // 音轨切换会重建同一首歌的 MediaItem，此时保留已选音轨
                    currentTrack = if (song != null && song.id == previousSong?.id) {
                        it.currentTrack
                    } else {
                        song?.tracks?.firstOrNull()
                    },
                    embeddedTracks = if (song != null && song.id == previousSong?.id) {
                        it.embeddedTracks
                    } else {
                        emptyList()
                    },
                    duration = ((song?.duration ?: 0.0) * 1000).toLong()
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val duration = controller?.duration ?: 0L
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    duration = if (duration > 0) duration else it.duration
                )
            }
            // 音频会话在首次播放时才创建，均衡器可能晚于控制器连接就绪，播放就绪后重试一次
            if (playbackState == Player.STATE_READY && _state.value.eqBandFrequencies.isEmpty()) {
                retryEqSetup()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            if (audioGroups.size > 1) {
                val embedded = audioGroups.mapIndexed { index, group ->
                    val format = group.getTrackFormat(0)
                    Track(
                        id = "$EMBEDDED_TRACK_PREFIX$index",
                        name = format.label ?: "音轨 ${index + 1}",
                        url = ""
                    )
                }
                val selectedIndex = audioGroups.indexOfFirst { it.isSelected }
                _state.update {
                    it.copy(
                        embeddedTracks = embedded,
                        currentTrack = embedded.getOrNull(selectedIndex) ?: it.currentTrack
                    )
                }
            } else if (_state.value.embeddedTracks.isNotEmpty()) {
                _state.update { it.copy(embeddedTracks = emptyList()) }
            }
        }
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let { action(it); return }
        val future = controllerFuture ?: MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, MusicService::class.java))
        ).buildAsync().also { controllerFuture = it }

        future.addListener({
            runCatching {
                val c = future.get()
                if (controller == null) {
                    controller = c
                    c.addListener(listener)
                    // 先恢复已保存配置，再查询能力与频段数据，保证 info 反映应用后的状态
                    sendEqApply(eqEnabledCache, eqBandsCache)
                    checkEqSupport(c)
                    queryEqInfo(c)
                }
                action(c)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(
        queue: List<Song>,
        index: Int,
        contextType: String? = null,
        contextKey: String? = null
    ) {
        val song = queue.getOrNull(index) ?: return
        _state.update {
            it.copy(
                queue = queue,
                currentIndex = index,
                currentSong = song,
                currentTrack = song.tracks?.firstOrNull(),
                duration = (song.duration * 1000).toLong(),
                contextType = contextType,
                contextKey = contextKey
            )
        }
        withController { c ->
            c.setMediaItems(queue.map { buildMediaItem(it) }, index, 0L)
            applyPlayMode(c, _state.value.playMode)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = withController { it.seekToNextMediaItem() }

    fun previous() = withController { it.seekToPreviousMediaItem() }

    fun playAt(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
        }
    }

    fun seekTo(position: Long) = withController { it.seekTo(position) }

    fun setPlayMode(mode: PlayMode) {
        _state.update { it.copy(playMode = mode) }
        withController { applyPlayMode(it, mode) }
    }

    fun cyclePlayMode() {
        setPlayMode(
            when (_state.value.playMode) {
                PlayMode.ORDER -> PlayMode.LOOP
                PlayMode.LOOP -> PlayMode.SINGLE
                PlayMode.SINGLE -> PlayMode.RANDOM
                PlayMode.RANDOM -> PlayMode.ORDER
            }
        )
    }

    fun switchTrack(track: Track) {
        if (track.id.startsWith(EMBEDDED_TRACK_PREFIX)) {
            switchEmbeddedTrack(track)
            return
        }
        val song = _state.value.currentSong ?: return
        withController { c ->
            val position = c.currentPosition
            val index = c.currentMediaItemIndex
            c.replaceMediaItem(index, buildMediaItem(song, track))
            c.prepare()
            c.seekTo(index, position)
            c.play()
        }
        _state.update { it.copy(currentTrack = track) }
    }

    private fun switchEmbeddedTrack(track: Track) {
        val groupIndex = track.id.removePrefix(EMBEDDED_TRACK_PREFIX).toIntOrNull() ?: return
        withController { c ->
            val audioGroups = c.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val group = audioGroups.getOrNull(groupIndex) ?: return@withController
            c.trackSelectionParameters = c.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                .build()
            _state.update { it.copy(currentTrack = track) }
        }
    }

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    fun duration(): Long = controller?.duration?.takeIf { it > 0 } ?: _state.value.duration

    fun withPlayer(action: (Player) -> Unit) = withController(action)

    fun setEqualizerEnabled(enabled: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        if (!enabled) {
            scope.launch { dataStore.setEqEnabled(false) }
            _state.update { it.copy(eqEnabled = false) }
            onResult?.invoke(true)
            return
        }
        // 开启前校验设备能力，不支持则不写入配置
        scope.launch {
            val supported = checkEqualizerSupport()
            _state.update { it.copy(eqSupported = supported) }
            if (supported) {
                dataStore.setEqEnabled(true)
                _state.update { it.copy(eqEnabled = true) }
            }
            onResult?.invoke(supported)
        }
    }

    // 面板打开时刷新：能力 + 频段数据（数据需音频会话就绪后才有）
    fun refreshEqInfo() = withController { c ->
        checkEqSupport(c)
        queryEqInfo(c)
    }

    private suspend fun checkEqualizerSupport(): Boolean = withTimeoutOrNull(5_000) {
        suspendCancellableCoroutine { cont ->
            withController { c ->
                val future = c.sendCustomCommand(
                    SessionCommand(MusicService.EQ_CHECK, Bundle.EMPTY), Bundle.EMPTY
                )
                future.addListener({
                    val supported = runCatching {
                        val r = future.get()
                        r.resultCode == SessionResult.RESULT_SUCCESS &&
                            r.extras?.getBoolean(MusicService.EXTRA_SUPPORTED, false) == true
                    }.getOrDefault(false)
                    if (cont.isActive) cont.resume(supported)
                }, ContextCompat.getMainExecutor(context))
            }
        }
    } ?: false

    private fun checkEqSupport(c: MediaController) {
        val future = c.sendCustomCommand(
            SessionCommand(MusicService.EQ_CHECK, Bundle.EMPTY), Bundle.EMPTY
        )
        future.addListener({
            runCatching {
                val r = future.get()
                val supported = r.resultCode == SessionResult.RESULT_SUCCESS &&
                    r.extras?.getBoolean(MusicService.EXTRA_SUPPORTED, false) == true
                Log.d(TAG, "eq/check：设备支持均衡器 = $supported")
                _state.update { it.copy(eqSupported = supported) }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setEqualizerPreset(preset: String) {
        if (preset !in EQ_PRESET_KEYS) return
        val bands = resolvePresetBands(preset)
        scope.launch {
            dataStore.setEqPreset(preset)
            dataStore.setEqBands(formatBands(bands))
        }
        _state.update { it.copy(eqPreset = preset, eqBands = bands) }
    }

    fun setEqualizerBand(bandIndex: Int, levelDb: Int) {
        val bands = eqBandsCache.toMutableList()
        if (bandIndex !in bands.indices) return
        val min = _state.value.eqBandLevelMin / 100
        val max = _state.value.eqBandLevelMax / 100
        bands[bandIndex] = levelDb.coerceIn(min, max)
        scope.launch {
            dataStore.setEqBands(formatBands(bands))
            // 手动调频段即视为自定义曲线
            dataStore.setEqPreset("custom")
        }
        _state.update { it.copy(eqBands = bands, eqPreset = "custom") }
    }

    // 预设曲线（10 频点）按对数频率插值到设备实际频段；设备频段未知时按已知段数截取
    private fun resolvePresetBands(preset: String): List<Int> {
        val curve = EQ_PRESETS[preset]?.second ?: return eqBandsCache
        val deviceFreqs = _state.value.eqBandFrequencies
        if (deviceFreqs.isEmpty()) {
            val knownCount = eqBandsCache.size
            return if (knownCount == 0) curve else curve.take(knownCount)
        }
        return deviceFreqs.map { freq -> interpolateGain(freq, EQ_PRESET_FREQS, curve) }
    }

    // 对数频率线性插值（同 songloft-player 的 mpv EQ 映射方式）
    private fun interpolateGain(freq: Int, freqs: List<Int>, gains: List<Int>): Int {
        val logFreq = ln(freq.toDouble())
        val logLow = ln(freqs.first().toDouble())
        val logHigh = ln(freqs.last().toDouble())
        val gain = when {
            logFreq <= logLow -> gains.first().toDouble()
            logFreq >= logHigh -> gains.last().toDouble()
            else -> {
                var result = gains.last().toDouble()
                for (i in 0 until freqs.size - 1) {
                    val lo = ln(freqs[i].toDouble())
                    val hi = ln(freqs[i + 1].toDouble())
                    if (logFreq in lo..hi) {
                        val t = (logFreq - lo) / (hi - lo)
                        result = gains[i] + t * (gains[i + 1] - gains[i])
                        break
                    }
                }
                result
            }
        }
        return round(gain).toInt()
    }

    private fun sendEqApply(enabled: Boolean, bands: List<Int>) {
        val c = controller ?: return
        val args = Bundle().apply {
            putBoolean(MusicService.EXTRA_ENABLED, enabled)
            putIntArray(MusicService.EXTRA_BANDS, bands.toIntArray())
        }
        c.sendCustomCommand(SessionCommand(MusicService.EQ_APPLY, Bundle.EMPTY), args)
    }

    private fun retryEqSetup() {
        val c = controller ?: return
        Log.d(TAG, "播放就绪，重试均衡器（apply + check + query）")
        sendEqApply(eqEnabledCache, eqBandsCache)
        checkEqSupport(c)
        queryEqInfo(c)
    }

    private fun queryEqInfo(c: MediaController) {
        val future = c.sendCustomCommand(SessionCommand(MusicService.EQ_INFO, Bundle.EMPTY), Bundle.EMPTY)
        future.addListener({
            runCatching {
                val result = future.get()
                // 频段数据需音频会话就绪（播放中）才有；失败不影响能力判断（eq/check 负责）
                if (result.resultCode != SessionResult.RESULT_SUCCESS) {
                    Log.w(TAG, "eq/info 响应异常：code=${result.resultCode}")
                    return@addListener
                }
                val extras = result.extras ?: run {
                    Log.w(TAG, "eq/info 无返回数据")
                    return@addListener
                }
                if (!extras.getBoolean(MusicService.EXTRA_SUPPORTED, false)) {
                    Log.w(TAG, "eq/info 绑定失败（音频会话未就绪或设备无 audiofx）")
                    return@addListener
                }
                Log.d(TAG, "eq/info 成功：${extras.getInt(MusicService.EXTRA_BAND_COUNT)} 段")
                _state.update {
                    it.copy(
                        eqSupported = true,
                        eqBandFrequencies = extras.getIntArray(MusicService.EXTRA_CENTER_FREQS)?.toList() ?: emptyList(),
                        eqBandLevelMin = extras.getInt(MusicService.EXTRA_LEVEL_MIN),
                        eqBandLevelMax = extras.getInt(MusicService.EXTRA_LEVEL_MAX),
                        eqEnabled = extras.getBoolean(MusicService.EXTRA_ENABLED, false),
                        eqBands = extras.getIntArray(MusicService.EXTRA_BANDS)?.toList() ?: emptyList()
                    )
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun parseBands(s: String): List<Int> =
        s.split(',').mapNotNull { it.trim().toIntOrNull() }

    private fun formatBands(bands: List<Int>): String = bands.joinToString(",")

    private var sleepJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        _state.update {
            it.copy(
                sleepTimerMinutes = minutes,
                sleepTimerRemaining = minutes,
                sleepAfterSongs = 0,
                sleepAfterSongsRemaining = 0
            )
        }
        if (minutes > 0) {
            sleepJob = scope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60_000L)
                    remaining--
                    _state.update { it.copy(sleepTimerRemaining = remaining) }
                }
                controller?.pause()
                _state.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemaining = 0) }
            }
        }
    }

    fun setSleepAfterSongs(count: Int) {
        sleepJob?.cancel()
        sleepJob = null
        _state.update {
            it.copy(
                sleepTimerMinutes = 0,
                sleepTimerRemaining = 0,
                sleepAfterSongs = count,
                sleepAfterSongsRemaining = count
            )
        }
    }

    private fun countDownSleepAfterSongs(previousSong: Song?, song: Song?, reason: Int) {
        val remaining = _state.value.sleepAfterSongsRemaining
        if (remaining <= 0) return
        // 只统计自然播完的歌曲，音轨切换重建的同曲 MediaItem 不计数
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        if (previousSong == null || previousSong.id == song?.id) return
        val next = remaining - 1
        _state.update { it.copy(sleepAfterSongsRemaining = next) }
        if (next == 0) {
            controller?.pause()
            _state.update { it.copy(sleepAfterSongs = 0) }
        }
    }

    private fun reportTransition(previousSong: Song?, song: Song?, reason: Int) {
        // 音轨切换会重建同一首歌的 MediaItem，不重复上报
        if (previousSong?.id == song?.id) return
        scope.launch {
            previousSong?.let { prev ->
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ->
                        songRepository.reportPlayed(prev.id, "finish")
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ->
                        songRepository.reportPlayed(prev.id, "skip")
                    else -> Result.success(Unit)
                }
            }
            song?.let {
                songRepository.reportPlayed(
                    it.id,
                    "play",
                    contextType = _state.value.contextType,
                    contextKey = _state.value.contextKey
                )
            }
        }
    }

    private fun applyPlayMode(c: MediaController, mode: PlayMode) {
        when (mode) {
            PlayMode.ORDER -> {
                c.repeatMode = Player.REPEAT_MODE_OFF
                c.shuffleModeEnabled = false
            }
            PlayMode.LOOP -> {
                c.repeatMode = Player.REPEAT_MODE_ALL
                c.shuffleModeEnabled = false
            }
            PlayMode.SINGLE -> {
                c.repeatMode = Player.REPEAT_MODE_ONE
                c.shuffleModeEnabled = false
            }
            PlayMode.RANDOM -> {
                c.repeatMode = Player.REPEAT_MODE_ALL
                c.shuffleModeEnabled = true
            }
        }
    }

    private fun buildMediaItem(song: Song, track: Track? = null): MediaItem {
        val radioUrl = if (song.type == "radio") UrlHelper.resolve(song.url) else null
        val uri = UrlHelper.resolve(track?.url)
            ?: radioUrl
            ?: UrlHelper.songPlayUrl(
                song.id,
                transcodeFormat = audioQuality,
                track = track?.id,
                isVideo = song.isVideo,
                sourceFormat = song.format
            )
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .apply { if (uri.endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(UrlHelper.resolve(song.coverUrl)?.let(Uri::parse))
                    .build()
            )
            .build()
    }

    companion object {
        private const val TAG = "PlayerController"
        private const val EMBEDDED_TRACK_PREFIX = "embedded:"
    }
}
