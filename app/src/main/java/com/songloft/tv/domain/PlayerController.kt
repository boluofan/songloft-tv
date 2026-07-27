package com.songloft.tv.domain

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayMode { ORDER, LOOP, SINGLE, RANDOM }

data class PlaybackState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val currentSong: Song? = null,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val duration: Long = 0L,
    val playMode: PlayMode = PlayMode.ORDER,
    val sleepTimerMinutes: Int = 0
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
    dataStore: PreferencesDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioQuality: String? = null

    init {
        scope.launch {
            dataStore.audioQuality.collect { audioQuality = it?.takeIf { q -> q.isNotBlank() } }
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
            _state.update {
                it.copy(
                    currentSong = song,
                    currentIndex = controller?.currentMediaItemIndex ?: -1,
                    currentTrack = song?.tracks?.firstOrNull(),
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
                }
                action(c)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(queue: List<Song>, index: Int) {
        val song = queue.getOrNull(index) ?: return
        _state.update {
            it.copy(
                queue = queue,
                currentIndex = index,
                currentSong = song,
                currentTrack = song.tracks?.firstOrNull(),
                duration = (song.duration * 1000).toLong()
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

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    fun duration(): Long = controller?.duration?.takeIf { it > 0 } ?: _state.value.duration

    fun withPlayer(action: (Player) -> Unit) = withController(action)

    private var sleepJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        _state.update { it.copy(sleepTimerMinutes = minutes) }
        if (minutes > 0) {
            sleepJob = scope.launch {
                delay(minutes * 60_000L)
                controller?.pause()
                _state.update { it.copy(sleepTimerMinutes = 0) }
            }
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
            song?.let { songRepository.reportPlayed(it.id, "play") }
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
        val uri = track?.url?.takeIf { it.isNotBlank() }
            ?: UrlHelper.songPlayUrl(song.id, quality = audioQuality, track = track?.id)
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(UrlHelper.resolve(song.coverUrl)?.let(Uri::parse))
                    .build()
            )
            .build()
    }
}
