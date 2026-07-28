package com.songloft.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.songloft.tv.ui.components.tvFocusable
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
                        },
                        onExit = { finish() }
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
    onOpenPlayer: () -> Unit,
    onExit: () -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        is AuthState.LoggedIn -> TvApp(authViewModel, playerController, onPlaySongs, onShufflePlay, onOpenPlayer, onExit)
        else -> AuthSetupScreen(authViewModel)
    }
}

@Composable
fun TvApp(
    authViewModel: AuthViewModel,
    playerController: PlayerController,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onOpenPlayer: () -> Unit,
    onExit: () -> Unit
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.last()
    val stateHolder = rememberSaveableStateHolder()
    val tabBarBridge = remember { TabBarBridge() }
    var showExitDialog by remember { mutableStateOf(false) }

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler {
        when {
            backStack.size > 1 -> goBack()
            currentScreen != Screen.Home -> backStack[0] = Screen.Home
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = onExit,
            onDismiss = { showExitDialog = false }
        )
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

@Composable
private fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "确定退出吗？",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(24.dp))
            Row {
                ExitDialogButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(cancelFocusRequester)
                )
                Spacer(Modifier.width(16.dp))
                ExitDialogButton(
                    text = "退出",
                    onClick = onConfirm
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }
}

@Composable
private fun ExitDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .tvFocusable(cornerRadius = 8.dp, onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 28.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
