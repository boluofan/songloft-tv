package com.songloft.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Surface
import com.songloft.tv.data.model.Song
import com.songloft.tv.domain.PlayMode
import com.songloft.tv.domain.PlayerController
import com.songloft.tv.ui.config.AuthSetupScreen
import com.songloft.tv.ui.config.AuthState
import com.songloft.tv.ui.config.AuthViewModel
import com.songloft.tv.ui.components.FloatingPlayerBar
import com.songloft.tv.ui.home.HomeScreen
import com.songloft.tv.ui.my.MyScreen
import com.songloft.tv.ui.navigation.Screen
import com.songloft.tv.ui.navigation.TvBottomNav
import com.songloft.tv.ui.player.PlayerActivity
import com.songloft.tv.ui.playlist.PlaylistDetailScreen
import com.songloft.tv.ui.playlist.PlaylistsScreen
import com.songloft.tv.ui.search.SearchScreen
import com.songloft.tv.ui.settings.SettingsScreen
import com.songloft.tv.ui.theme.TvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp(
                        playerController = playerController,
                        onPlaySongs = { songs, index -> openPlayer(songs, index) },
                        onShufflePlay = { songs ->
                            playerController.setPlayMode(PlayMode.RANDOM)
                            openPlayer(songs, Random.nextInt(songs.size))
                        },
                        onOpenPlayer = {
                            startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun openPlayer(songs: List<Song>, index: Int) {
        playerController.play(songs, index)
        startActivity(Intent(this, PlayerActivity::class.java))
    }
}

@Composable
fun MainApp(
    playerController: PlayerController,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        is AuthState.LoggedIn -> TvApp(authViewModel, playerController, onPlaySongs, onShufflePlay, onOpenPlayer)
        else -> AuthSetupScreen(authViewModel)
    }
}

@Composable
fun TvApp(
    authViewModel: AuthViewModel,
    playerController: PlayerController,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onOpenPlayer: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = when (currentScreen) {
            is Screen.PlaylistDetail -> Screen.Playlists
            Screen.Settings -> Screen.My
            else -> Screen.Home
        }
    }

    Scaffold(
        bottomBar = {
            TvBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = when (screen) {
                        is Screen.Home -> Screen.Home
                        is Screen.Search -> Screen.Search
                        is Screen.Playlists -> Screen.Playlists
                        is Screen.My -> Screen.My
                        else -> Screen.Home
                    }
                }
            )
        }
    ) { padding ->
        val playbackState by playerController.state.collectAsStateWithLifecycle()

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val screen = currentScreen) {
                Screen.Home -> HomeScreen(
                    onPlaylistClick = { id -> currentScreen = Screen.PlaylistDetail(id) },
                    onArtistClick = { artist ->
                        currentScreen = Screen.Search
                    },
                    onAlbumClick = { album ->
                        currentScreen = Screen.Search
                    },
                    onYearClick = { year ->
                        currentScreen = Screen.Search
                    }
                )
                Screen.Search -> SearchScreen(
                    onSongClick = onPlaySongs
                )
                Screen.Playlists -> PlaylistsScreen(
                    onPlaylistClick = { id -> currentScreen = Screen.PlaylistDetail(id) }
                )
                is Screen.PlaylistDetail -> PlaylistDetailScreen(
                    playlistId = screen.playlistId,
                    onSongClick = onPlaySongs,
                    onShufflePlay = onShufflePlay,
                    onBack = { currentScreen = Screen.Playlists }
                )
                Screen.My -> MyScreen(
                    onSongClick = onPlaySongs,
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
                Screen.Settings -> SettingsScreen(
                    onBack = { currentScreen = Screen.My },
                    onConfigureServer = { authViewModel.resetToConfig() },
                    onLogout = { authViewModel.logout() }
                )
            }

            playbackState.currentSong?.let { song ->
                FloatingPlayerBar(
                    title = song.title,
                    artist = song.artist,
                    coverUrl = song.coverUrl,
                    isPlaying = playbackState.isPlaying,
                    onClick = onOpenPlayer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                )
            }
        }
    }
}
