package com.songloft.tv.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.model.LyricLine
import com.songloft.tv.data.model.Song
import com.songloft.tv.data.model.Track
import com.songloft.tv.data.repository.SongRepository
import com.songloft.tv.domain.LyricParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayMode { ORDER, LOOP, SINGLE, RANDOM }

data class PlayerUiState(
    val currentSong: Song? = null,
    val currentTrack: Track? = null,
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
    val isBuffering: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun playSong(song: Song, queue: List<Song> = listOf(song), index: Int = 0) {
        _uiState.value = PlayerUiState(
            currentSong = song,
            currentTrack = song.tracks?.firstOrNull(),
            queue = queue,
            currentIndex = index,
            isPlaying = true,
            isVideoMode = song.isVideo,
            duration = (song.duration * 1000L).toLong()
        )
        loadLyrics(song.id)
    }

    fun togglePlay() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun seekTo(position: Long) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun nextTrack() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex < state.queue.size) {
            playSong(state.queue[nextIndex], state.queue, nextIndex)
        }
    }

    fun previousTrack() {
        val state = _uiState.value
        val prevIndex = state.currentIndex - 1
        if (prevIndex >= 0) {
            playSong(state.queue[prevIndex], state.queue, prevIndex)
        }
    }

    fun cyclePlayMode() {
        _uiState.value = _uiState.value.copy(
            playMode = when (_uiState.value.playMode) {
                PlayMode.ORDER -> PlayMode.LOOP
                PlayMode.LOOP -> PlayMode.SINGLE
                PlayMode.SINGLE -> PlayMode.RANDOM
                PlayMode.RANDOM -> PlayMode.ORDER
            }
        )
    }

    fun switchTrack(track: Track) {
        _uiState.value = _uiState.value.copy(currentTrack = track)
    }

    fun toggleQueueDrawer() {
        _uiState.value = _uiState.value.copy(showQueueDrawer = !_uiState.value.showQueueDrawer)
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun updatePosition(position: Long) {
        val lyrics = _uiState.value.lyrics
        val index = lyrics.indexOfLast { it.time <= position }
        _uiState.value = _uiState.value.copy(
            currentPosition = position,
            currentLyricIndex = index
        )
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    fun showControls() {
        _uiState.value = _uiState.value.copy(showControls = true)
    }

    private fun loadLyrics(songId: Long) {
        viewModelScope.launch {
            songRepository.getSongLyric(songId).onSuccess { lrcText ->
                val parsed = LyricParser.parse(lrcText)
                _uiState.value = _uiState.value.copy(lyrics = parsed)
            }
        }
    }
}
