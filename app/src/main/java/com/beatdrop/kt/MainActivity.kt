package com.beatdrop.kt

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.kt.ui.components.GlassTabBar
import com.beatdrop.kt.ui.components.MiniPlayer
import com.beatdrop.kt.ui.components.TabSpec
import com.beatdrop.kt.ui.screens.*
import com.beatdrop.kt.ui.theme.BeatDropTheme
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.youtube.initHiddenYoutubeWebViews
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private var cleanupWebViews: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() removed — was causing black screen on some devices
        cleanupWebViews = initHiddenYoutubeWebViews(this)
        setContent {
            val vm: PlayerViewModel = viewModel()
            val themePref by vm.theme.collectAsState()
            val isFetchingStream by vm.isFetchingStream.collectAsState()
            BeatDropTheme(themePref = themePref) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        Root(vm)
                        if (isFetchingStream) {
                            GlobalGlassLoader()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        cleanupWebViews?.invoke()
        super.onDestroy()
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
    TabSpec("activity", "Activity", Icons.Filled.Settings),
)

private sealed interface Dest {
    data object Tabs : Dest
    data class Album(val name: String, val artist: String) : Dest
    data class Artist(val name: String) : Dest
    data class Playlist(val name: String) : Dest
    data object Playlists : Dest
    data object Stats : Dest
    data object Settings : Dest
    data object LocalDiscover : Dest
    data object ManualDJ : Dest
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

    val artColor = com.beatdrop.kt.ui.components.rememberArtworkColor(current?.artworkUri)
    val bgColors = if (C.isDark) {
        listOf(artColor.copy(alpha = 0.28f), Color(0xFF100E17), Color(0xFF07060A))
    } else {
        listOf(artColor.copy(alpha = 0.15f), Color(0xFFF9F7FC), Color(0xFFF0EDF5))
    }

    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.linearGradient(bgColors))
        ) {
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
                Box(Modifier.fillMaxSize().background(Color.Transparent)) {
                    when (dest) {
                    Dest.Tabs -> TabsHost(
                        vm = vm, tab = tab, onTab = { tab = it },
                        current = current, isPlaying = isPlaying, pos = pos, dur = dur,
                        onOpenAlbum         = { a, ar -> push(Dest.Album(a, ar)) },
                        onOpenArtist        = { push(Dest.Artist(it)) },
                        onOpenLocalDiscover = { push(Dest.LocalDiscover) },
                        onOpenPlaylists     = { push(Dest.Playlists) },
                        onOpenStats         = { push(Dest.Stats) },
                        onOpenSearch        = { push(Dest.Search) },
                        onExpandPlayer      = { push(Dest.NowPlaying) },
                        onOpenEq            = { push(Dest.Eq) },
                        onOpenManualDJ      = { push(Dest.ManualDJ) },
                    )
                    is Dest.Album        -> AlbumScreen(vm, dest.name, dest.artist, onBack = { pop() })
                    is Dest.Artist       -> ArtistScreen(vm, dest.name, onBack = { pop() })
                    is Dest.Playlist     -> PlaylistDetailScreen(vm, dest.name, onBack = { pop() })
                    Dest.Playlists       -> PlaylistsScreenHosted(vm, onBack = { pop() }, onOpen = { push(Dest.Playlist(it)) })
                    Dest.Stats           -> StatsHosted(vm, onBack = { pop() })
                    Dest.Settings        -> SettingsScreen(vm, onBack = { pop() }, onOpenEq = { push(Dest.Eq) })
                    Dest.LocalDiscover   -> LocalDiscoverScreen(vm, onBack = { pop() }, onOpenSearch = { push(Dest.Search) })
                    Dest.ManualDJ        -> DJScreen(vm, onBack = { pop() })
                    Dest.Eq              -> EqScreen(onBack = { pop() })
                    Dest.Search          -> SearchScreen(vm)
                    Dest.NowPlaying      -> NowPlayingScreen(vm, onCollapse = { pop() }, onOpenQueue = { push(Dest.Queue) })
                    Dest.Queue           -> QueueScreen(vm, onClose = { pop() })
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
    onOpenLocalDiscover: () -> Unit, onOpenPlaylists: () -> Unit,
    onOpenStats: () -> Unit, onOpenSearch: () -> Unit, onExpandPlayer: () -> Unit,
    onOpenEq: () -> Unit, onOpenManualDJ: () -> Unit,
) {
    val C = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "library"  -> LibraryScreen(vm, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist,
                        onOpenLocalDiscover = onOpenLocalDiscover, onOpenPlaylists = onOpenPlaylists, onOpenStats = onOpenStats)
                    "discover" -> DiscoverScreen(vm, onOpenSearch = onOpenSearch)
                    "radio"    -> RadioScreen(vm)
                    "activity" -> ActivityScreen(vm, onOpenEq = onOpenEq, onOpenManualDJ = onOpenManualDJ)
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

@Composable
fun GlobalGlassLoader() {
    val C = LocalAppColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {}, // Consume taps to prevent background clicks during loading
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(240.dp, 160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (C.isDark) Color(0xFF151025).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f))
                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = C.accent, strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Connecting to stream...",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Resolving high-quality audio",
                    color = C.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
