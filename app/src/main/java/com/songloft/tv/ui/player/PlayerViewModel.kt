package com.songloft.tv.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.LyricLine
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.model.Track
import com.songloft.tv.data.repository.FavoriteRepository
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.domain.LyricParser
import com.songloft.tv.domain.PlayMode
import com.songloft.tv.domain.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentSong: Song? = null,
    val currentTrack: Track? = null,
    val availableTracks: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playMode: PlayMode = PlayMode.ORDER,
    val lyrics: List<LyricLine> = emptyList(),
    val currentLyricIndex: Int = -1,
    val showControls: Boolean = true,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val showQueueDrawer: Boolean = false,
    val isVideoMode: Boolean = false,
    val isBuffering: Boolean = false,
    val isFavorite: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val songRepository: SongRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var lyricSongId: Long? = null
    private var favoriteIds: MutableSet<Long>? = null

    init {
        viewModelScope.launch {
            playerController.state.collect { s ->
                _uiState.update {
                    it.copy(
                        currentSong = s.currentSong,
                        currentTrack = s.currentTrack,
                        availableTracks = s.embeddedTracks.ifEmpty { s.currentSong?.tracks.orEmpty() },
                        isPlaying = s.isPlaying,
                        isBuffering = s.isBuffering,
                        duration = s.duration,
                        playMode = s.playMode,
                        queue = s.queue,
                        currentIndex = s.currentIndex,
                        isVideoMode = s.currentSong?.isVideo == true
                    )
                }
                val songId = s.currentSong?.id
                if (songId != null && songId != lyricSongId) {
                    lyricSongId = songId
                    loadLyrics(songId)
                    refreshFavoriteState(songId)
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isPlaying) {
                    updatePosition(playerController.currentPosition())
                    val duration = playerController.duration()
                    if (duration > 0 && duration != _uiState.value.duration) {
                        _uiState.update { it.copy(duration = duration) }
                    }
                }
                // 逐字歌词需要更高刷新率才能平滑高亮
                val hasWords = _uiState.value.lyrics.any { it.hasWords }
                delay(if (hasWords) 60L else 500L)
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), index: Int = 0) {
        playerController.play(queue, index)
    }

    fun togglePlay() = playerController.togglePlay()

    fun seekTo(position: Long) {
        playerController.seekTo(position)
        updatePosition(position)
    }

    fun seekBy(deltaMs: Long) {
        val duration = playerController.duration()
        val target = (playerController.currentPosition() + deltaMs)
            .coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        seekTo(target)
    }

    fun nextTrack() = playerController.next()

    fun previousTrack() = playerController.previous()

    fun cyclePlayMode() = playerController.cyclePlayMode()

    fun switchTrack(track: Track) = playerController.switchTrack(track)

    fun withPlayer(action: (androidx.media3.common.Player) -> Unit) = playerController.withPlayer(action)

    fun toggleFavorite() {
        val song = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            val ids = favoriteIds ?: favoriteRepository.getFavorites()
                .getOrNull()?.map { it.id }?.toMutableSet()
                ?.also { favoriteIds = it }
                ?: mutableSetOf<Long>().also { favoriteIds = it }

            val wasFavorite = song.id in ids
            // 乐观更新，失败时回滚
            _uiState.update { it.copy(isFavorite = !wasFavorite) }

            val result = if (wasFavorite) {
                favoriteRepository.removeFavorite(song).onSuccess { ids.remove(song.id) }
            } else {
                favoriteRepository.addFavorite(song).onSuccess { ids.add(song.id) }
            }
            result.onFailure { e ->
                Log.w("PlayerViewModel", "toggleFavorite failed for song ${song.id}", e)
                _uiState.update { it.copy(isFavorite = wasFavorite) }
            }
        }
    }

    private fun refreshFavoriteState(songId: Long) {
        viewModelScope.launch {
            val ids = favoriteIds ?: favoriteRepository.getFavorites()
                .getOrNull()?.map { it.id }?.toMutableSet()
                ?.also { favoriteIds = it }
            _uiState.update { it.copy(isFavorite = ids?.contains(songId) == true) }
        }
    }

    fun toggleQueueDrawer() {
        _uiState.update { it.copy(showQueueDrawer = !it.showQueueDrawer) }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun updatePosition(position: Long) {
        val lyrics = _uiState.value.lyrics
        val index = lyrics.indexOfLast { it.time <= position }
        _uiState.update {
            it.copy(currentPosition = position, currentLyricIndex = index)
        }
    }

    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
    }

    private fun loadLyrics(songId: Long) {
        _uiState.update { it.copy(lyrics = emptyList(), currentLyricIndex = -1) }
        viewModelScope.launch {
            songRepository.getSongLyric(songId).onSuccess { resp ->
                val parsed = LyricParser.parsePayload(
                    lyric = resp.lyric,
                    tlyric = resp.tlyric,
                    rlyric = resp.rlyric,
                    lxlyric = resp.lxlyric
                )
                _uiState.update { it.copy(lyrics = parsed) }
            }
        }
    }
}
