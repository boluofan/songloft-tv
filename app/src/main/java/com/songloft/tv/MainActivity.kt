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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.songloft.tv.ui.library.FacetListScreen
import com.songloft.tv.ui.library.FilteredSongsScreen
import com.songloft.tv.ui.my.MyScreen
import com.songloft.tv.ui.navigation.LocalTabBarBridge
import com.songloft.tv.ui.navigation.Screen
import com.songloft.tv.ui.navigation.stateKey
import com.songloft.tv.ui.navigation.TabBarBridge
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
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.last()
    val stateHolder = rememberSaveableStateHolder()
    val tabBarBridge = remember { TabBarBridge() }

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1 || currentScreen != Screen.Home) {
        if (backStack.size > 1) {
            goBack()
        } else {
            backStack[0] = Screen.Home
        }
    }

    CompositionLocalProvider(LocalTabBarBridge provides tabBarBridge) {
        Scaffold(
            bottomBar = {
                TvBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { tab ->
                        if (backStack.size != 1 || backStack[0] != tab) {
                            backStack.clear()
                            backStack.add(tab)
                        }
                    }
                )
            }
        ) { padding ->
            val playbackState by playerController.state.collectAsStateWithLifecycle()

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                stateHolder.SaveableStateProvider(currentScreen.stateKey) {
                    when (val screen = currentScreen) {
                        Screen.Home -> HomeScreen(
                            onPlaylistClick = { id -> push(Screen.PlaylistDetail(id)) },
                            onArtistClick = { artist -> push(Screen.SongFilter("artist", artist)) },
                            onAlbumClick = { album -> push(Screen.SongFilter("album", album)) },
                            onYearClick = { year -> push(Screen.SongFilter("year", year.toString())) },
                            onViewAll = { field -> push(Screen.FacetList(field)) },
                            onManagePlaylists = { push(Screen.Playlists) }
                        )
                        Screen.Search -> SearchScreen(
                            onSongClick = onPlaySongs
                        )
                        Screen.Playlists -> PlaylistsScreen(
                            onPlaylistClick = { id -> push(Screen.PlaylistDetail(id)) }
                        )
                        is Screen.PlaylistDetail -> PlaylistDetailScreen(
                            playlistId = screen.playlistId,
                            onSongClick = onPlaySongs,
                            onShufflePlay = onShufflePlay,
                            onBack = { goBack() }
                        )
                        Screen.My -> MyScreen(
                            onSongClick = onPlaySongs,
                            onNavigateToSettings = { push(Screen.Settings) }
                        )
                        Screen.Settings -> SettingsScreen(
                            onBack = { goBack() },
                            onConfigureServer = { authViewModel.resetToConfig() },
                            onLogout = { authViewModel.logout() }
                        )
                        is Screen.SongFilter -> FilteredSongsScreen(
                            field = screen.field,
                            value = screen.value,
                            onSongClick = onPlaySongs,
                            onBack = { goBack() }
                        )
                        is Screen.FacetList -> FacetListScreen(
                            field = screen.field,
                            onItemClick = { value -> push(Screen.SongFilter(screen.field, value)) },
                            onBack = { goBack() }
                        )
                    }
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
}
