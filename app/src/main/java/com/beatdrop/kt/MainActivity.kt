package com.beatdrop.kt

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.kt.ui.components.GlassTabBar
import com.beatdrop.kt.ui.components.MiniPlayer
import com.beatdrop.kt.ui.components.TabSpec
import com.beatdrop.kt.ui.screens.*
import com.beatdrop.kt.ui.theme.BeatDropTheme
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.youtube.YoutubeIFramePlayerHost
import com.beatdrop.kt.youtube.YoutubeStreamExtractorHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: PlayerViewModel = viewModel()
            val themePref by vm.theme.collectAsState()
            BeatDropTheme(themePref = themePref) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        Root(vm)

                        // Hidden YouTube WebViews — mounted ONCE, never unmounted.
                        // IFrame player: runs YouTube audio via YouTube's own JS engine.
                        // Stream extractor: loads embed pages to pull out direct stream URLs.
                        // Both live at pixel (−9999, −9999) so they're invisible but alive.
                        YoutubeIFramePlayerHost(
                            modifier = Modifier
                                .size(1.dp)
                                .offset(x = (-9999).dp, y = (-9999).dp)
                        )
                        YoutubeStreamExtractorHost(
                            modifier = Modifier
                                .size(1.dp)
                                .offset(x = (-9999).dp, y = (-9999).dp)
                        )
                    }
                }
            }
        }
    }
}

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Root(vm: PlayerViewModel = viewModel()) {
    val perm = rememberPermissionState(audioPermission)
    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) { SplashScreen(onDone = { showSplash = false }); return }

    LaunchedEffect(Unit) { vm.connect() }
    LaunchedEffect(perm.status.isGranted) { if (perm.status.isGranted) vm.loadLibrary() }

    var onboarded by rememberSaveable { mutableStateOf(false) }
    if (!onboarded && !perm.status.isGranted) {
        OnboardingScreen(onGetStarted = { onboarded = true; perm.launchPermissionRequest() }); return
    }
    if (!perm.status.isGranted) { PermissionPrompt(onRequest = { perm.launchPermissionRequest() }); return }
    MainScaffold(vm)
}

private val TABS = listOf(
    TabSpec("library",  "Library",  Icons.Filled.LibraryMusic),
    TabSpec("discover", "Discover", Icons.Outlined.Explore),
    TabSpec("radio",    "Radio",    Icons.Filled.Radio),
    TabSpec("dj",       "DJ Mode",  Icons.Filled.GraphicEq),
)

private sealed interface Dest {
    data object Tabs : Dest
    data class Album(val name: String, val artist: String) : Dest
    data class Artist(val name: String) : Dest
    data class Playlist(val name: String) : Dest
    data object Playlists : Dest
    data object Stats : Dest
    data object Settings : Dest
    data object Eq : Dest
    data object Search : Dest
    data object NowPlaying : Dest
    data object Queue : Dest
}

@Composable
fun MainScaffold(vm: PlayerViewModel) {
    val C = LocalAppColors.current
    var tab by rememberSaveable { mutableStateOf("library") }
    val stack = remember { mutableStateListOf<Dest>() }
    val currentDest: Dest = stack.lastOrNull() ?: Dest.Tabs
    fun push(d: Dest) { stack.add(d) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    val current    by vm.current.collectAsState()
    val isPlaying  by vm.isPlaying.collectAsState()
    val pos        by vm.position.collectAsState()
    val dur        by vm.duration.collectAsState()

    Surface(Modifier.fillMaxSize(), color = C.bg0) {
        AnimatedContent(
            targetState = currentDest,
            transitionSpec = {
                val isPush = targetState != Dest.Tabs && initialState == Dest.Tabs
                if (targetState == Dest.Tabs && initialState == Dest.Tabs) {
                    androidx.compose.animation.EnterTransition.None togetherWith
                        androidx.compose.animation.ExitTransition.None
                } else if (isPush) {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200))) togetherWith fadeOut(tween(120))
                } else {
                    fadeIn(tween(180)) togetherWith (slideOutHorizontally(tween(220)) { it } + fadeOut(tween(120)))
                }
            },
            label = "screen",
        ) { dest ->
            Box(Modifier.fillMaxSize().background(C.bg0)) {
                when (dest) {
                    Dest.Tabs -> TabsHost(
                        vm = vm, tab = tab, onTab = { tab = it },
                        current = current, isPlaying = isPlaying, pos = pos, dur = dur,
                        onOpenAlbum      = { a, ar -> push(Dest.Album(a, ar)) },
                        onOpenArtist     = { push(Dest.Artist(it)) },
                        onOpenSettings   = { push(Dest.Settings) },
                        onOpenPlaylists  = { push(Dest.Playlists) },
                        onOpenStats      = { push(Dest.Stats) },
                        onOpenSearch     = { push(Dest.Search) },
                        onExpandPlayer   = { push(Dest.NowPlaying) },
                    )
                    is Dest.Album    -> AlbumScreen(vm, dest.name, dest.artist, onBack = { pop() })
                    is Dest.Artist   -> ArtistScreen(vm, dest.name, onBack = { pop() })
                    is Dest.Playlist -> PlaylistDetailScreen(vm, dest.name, onBack = { pop() })
                    Dest.Playlists   -> PlaylistsScreenHosted(vm, onBack = { pop() }, onOpen = { push(Dest.Playlist(it)) })
                    Dest.Stats       -> StatsHosted(vm, onBack = { pop() })
                    Dest.Settings    -> SettingsScreen(vm, onBack = { pop() }, onOpenEq = { push(Dest.Eq) })
                    Dest.Eq          -> EqScreen(onBack = { pop() })
                    Dest.Search      -> SearchScreen(vm)
                    Dest.NowPlaying  -> NowPlayingScreen(vm, onCollapse = { pop() }, onOpenQueue = { push(Dest.Queue) })
                    Dest.Queue       -> QueueScreen(vm, onClose = { pop() })
                }
            }
        }
    }
}

@Composable
private fun TabsHost(
    vm: PlayerViewModel, tab: String, onTab: (String) -> Unit,
    current: com.beatdrop.kt.data.Track?, isPlaying: Boolean, pos: Long, dur: Long,
    onOpenAlbum: (String, String) -> Unit, onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit, onOpenPlaylists: () -> Unit,
    onOpenStats: () -> Unit, onOpenSearch: () -> Unit, onExpandPlayer: () -> Unit,
) {
    val C = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(C.bg0)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "library"  -> LibraryScreen(vm, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist,
                        onOpenSettings = onOpenSettings, onOpenPlaylists = onOpenPlaylists, onOpenStats = onOpenStats)
                    "discover" -> DiscoverScreen(vm, onOpenSearch = onOpenSearch)
                    "radio"    -> RadioScreen(vm)
                    "dj"       -> DJScreen(vm)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().background(if (C.isDark) Color(0xFF101018) else Color(0xFFF2F2F7))) {
                current?.let { t ->
                    MiniPlayer(
                        track = t, isPlaying = isPlaying,
                        progress = if (dur > 0) pos.toFloat() / dur else 0f,
                        onToggle = { vm.togglePlay() }, onNext = { vm.next() }, onPrev = { vm.prev() },
                        onExpand = onExpandPlayer,
                    )
                }
            }
            GlassTabBar(TABS, tab) { onTab(it) }
        }
    }
}

@Composable private fun PlaylistsScreenHosted(vm: PlayerViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) = PlaylistsScreen(vm, onBack = onBack, onOpen = onOpen)
@Composable private fun StatsHosted(vm: PlayerViewModel, onBack: () -> Unit) = StatsScreen(vm, onBack = onBack)
