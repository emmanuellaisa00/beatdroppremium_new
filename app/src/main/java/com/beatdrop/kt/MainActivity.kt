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
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.kt.ui.components.GlassTabBar2
import com.beatdrop.kt.ui.components.TabSpec2
import com.beatdrop.kt.ui.components.MiniPlayer
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
        // Immersive edge-to-edge system bars
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        cleanupWebViews = initHiddenYoutubeWebViews(this)
        setContent {
            val vm: PlayerViewModel = viewModel()
            val themePref by vm.theme.collectAsState()
            BeatDropTheme(themePref = themePref) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root(vm)
                }
            }
        }
        // Handle incoming share intent or deep link
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Handle URLs shared from other apps (YouTube, TikTok, etc.) or opened via deep link.
     */
    private fun handleIncomingIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            android.content.Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return
                val url = sharedText.trim()
                // The clipboard watcher will detect this URL and show the dialog
            }
            android.content.Intent.ACTION_VIEW -> {
                val data = intent.data ?: return
                val url = data.toString()
                // Will be handled by clipboard detection in Root composable
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val perm = rememberPermissionState(audioPermission)
    // POST_NOTIFICATIONS (Android 13+) — needed for the media-playback notification
    // with transport controls. Playback works without it, but the notification is
    // hidden and the foreground service is more likely to be killed when backgrounded.
    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null

    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) { SplashScreen(onDone = { showSplash = false }); return }

    LaunchedEffect(Unit) { vm.connect() }
    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            vm.loadLibrary()
            // Best-effort: ask for notification permission once audio is granted.
            if (notifPerm != null && !notifPerm.status.isGranted) notifPerm.launchPermissionRequest()
        }
    }

    var onboarded by rememberSaveable { mutableStateOf(false) }
    if (!onboarded && !perm.status.isGranted) {
        OnboardingScreen(onGetStarted = { onboarded = true; perm.launchPermissionRequest() }); return
    }
    if (!perm.status.isGranted) {
        PermissionPrompt(
            onRequest = { perm.launchPermissionRequest() }
        )
        return
    }

    // Clipboard URL detection — show dialog when user has a video URL copied
    var clipUrl by rememberSaveable { mutableStateOf<String?>(null) }
    if (perm.status.isGranted) {
        val detected = com.beatdrop.kt.util.ClipboardWatcher.checkClipboard(context)
        LaunchedEffect(detected) { clipUrl = detected?.url }
    }
    clipUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { clipUrl = null; com.beatdrop.kt.util.ClipboardWatcher.reset() },
            title = { Text("Video Link Detected") },
            text = { Text("A video URL was found on your clipboard. Do you want to play or download it?\n\n$url") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        vm.playOnlineByUrl(url)
                        clipUrl = null
                        com.beatdrop.kt.util.ClipboardWatcher.reset()
                    }) { Text("Play") }
                    TextButton(onClick = {
                        vm.downloadOnlineByUrl(url)
                        clipUrl = null
                        com.beatdrop.kt.util.ClipboardWatcher.reset()
                    }) { Text("Download") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipUrl = null
                    com.beatdrop.kt.util.ClipboardWatcher.reset()
                }) { Text("Dismiss") }
            }
        )
    }

    MainScaffold(vm)
}

private val TABS = listOf(
    TabSpec2("library",  "Library",  Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    TabSpec2("discover", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    TabSpec2("radio",    "Radio",    Icons.Filled.Podcasts, Icons.Outlined.Podcasts),
    TabSpec2("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
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
    data object Eq : Dest
    data object DebugLog : Dest
    data object Search : Dest
    data object NowPlaying : Dest
    data object Queue : Dest
    // New SnapTube-style screens
    data object Downloads : Dest
    data object Trending : Dest
    data object Browser : Dest
    data object Storage : Dest
    data object PrivateFolder : Dest
    data class VideoPlayer(val path: String, val title: String) : Dest
    data class Channel(val channelId: String, val name: String, val thumb: String?) : Dest
    data class ClipUrl(val url: String) : Dest
    data class PlaylistDownload(val playlistId: String) : Dest
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
                            onOpenDebug         = { push(Dest.DebugLog) },
                            onOpenDownloads     = { push(Dest.Downloads) },
                            onOpenTrending      = { push(Dest.Trending) },
                            onOpenBrowser       = { push(Dest.Browser) },
                            onOpenStorage       = { push(Dest.Storage) },
                            onOpenPrivateFolder = { push(Dest.PrivateFolder) },
                        )
                        is Dest.Album        -> AlbumScreen(vm, dest.name, dest.artist, onBack = { pop() })
                        is Dest.Artist       -> ArtistScreen(vm, dest.name, onBack = { pop() })
                        is Dest.Playlist     -> PlaylistDetailScreen(vm, dest.name, onBack = { pop() })
                        Dest.Playlists       -> PlaylistsScreenHosted(vm, onBack = { pop() }, onOpen = { push(Dest.Playlist(it)) })
                        Dest.Stats           -> StatsHosted(vm, onBack = { pop() })
                        Dest.Settings        -> SettingsScreen(vm, onBack = { pop() }, onOpenEq = { push(Dest.Eq) }, onOpenDebug = { push(Dest.DebugLog) })
                        Dest.LocalDiscover   -> LocalDiscoverScreen(vm, onBack = { pop() }, onOpenSearch = { push(Dest.Search) })
                        Dest.Eq              -> EqScreen(onBack = { pop() })
                        Dest.DebugLog        -> DebugLogScreen(vm, onBack = { pop() })
                        Dest.Search          -> SearchScreen(vm, onExpandPlayer = { push(Dest.NowPlaying) })
                        Dest.NowPlaying      -> NowPlayingScreen(vm, onCollapse = { pop() }, onOpenQueue = { push(Dest.Queue) })
                        Dest.Queue           -> QueueScreen(vm, onClose = { pop() })
                        // New screens
                        Dest.Downloads       -> com.beatdrop.kt.ui.screens.DownloadsScreen(vm, onBack = { pop() })
                        Dest.Trending        -> com.beatdrop.kt.ui.screens.TrendingScreen(vm, onExpandPlayer = { push(Dest.NowPlaying) }, onBack = { pop() })
                        Dest.Browser         -> com.beatdrop.kt.ui.screens.BrowserScreen(
                            onVideoDetected = { url, title ->
                                vm.playOnlineByUrl(url)
                                push(Dest.NowPlaying)
                            },
                            onBack = { pop() },
                        )
                        Dest.Storage         -> com.beatdrop.kt.ui.screens.StorageScreen(onBack = { pop() })
                        Dest.PrivateFolder   -> com.beatdrop.kt.ui.screens.PrivateFolderScreen(
                            savedPin = vm.privatePin.collectAsState().value,
                            onSetPin = { vm.setPrivatePin(it) },
                            onBack = { pop() },
                        )
                        is Dest.VideoPlayer  -> com.beatdrop.kt.ui.screens.VideoPlayerScreen(
                            vm = vm, videoPath = dest.path, title = dest.title, onBack = { pop() },
                        )
                        is Dest.Channel      -> com.beatdrop.kt.ui.screens.ChannelScreen(
                            vm = vm, channelId = dest.channelId, channelName = dest.name,
                            channelThumb = dest.thumb, onExpandPlayer = { push(Dest.NowPlaying) }, onBack = { pop() },
                        )
                        is Dest.ClipUrl      -> com.beatdrop.kt.ui.screens.ClipUrlScreen(
                            vm = vm, url = dest.url, onExpandPlayer = { push(Dest.NowPlaying) }, onBack = { pop() },
                        )
                        is Dest.PlaylistDownload -> com.beatdrop.kt.ui.screens.PlaylistDownloadScreen(
                            vm = vm, playlistId = dest.playlistId, onBack = { pop() },
                        )
                    }
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
    onOpenEq: () -> Unit, onOpenDebug: () -> Unit,
    onOpenDownloads: () -> Unit, onOpenTrending: () -> Unit,
    onOpenBrowser: () -> Unit, onOpenStorage: () -> Unit, onOpenPrivateFolder: () -> Unit,
) {
    val C = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "library"  -> LibraryScreen(vm, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist,
                        onOpenLocalDiscover = onOpenLocalDiscover, onOpenPlaylists = onOpenPlaylists, onOpenStats = onOpenStats)
                    "discover" -> DiscoverScreen(vm, onOpenSearch = onOpenSearch, onExpandPlayer = onExpandPlayer)
                    "radio"    -> RadioScreen(vm)
                    "settings" -> SettingsScreen(vm, onBack = {}, onOpenEq = onOpenEq, onOpenDebug = onOpenDebug)
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
            GlassTabBar2(TABS, tab) { onTab(it) }
        }

        // Premium status bar frosted glass blur overlay
        StatusBarGlassOverlay()
    }
}

@Composable private fun PlaylistsScreenHosted(vm: PlayerViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) = PlaylistsScreen(vm, onBack = onBack, onOpen = onOpen)
@Composable private fun StatsHosted(vm: PlayerViewModel, onBack: () -> Unit) = StatsScreen(vm, onBack = onBack)

/**
 * Liquid Glass status bar overlay — uses backdrop blur + saturation boost
 * with a subtle rim light at the bottom edge. Adapts to light/dark theme.
 * Pre-API-31 fallback uses a heavier translucent fill.
 */
@Composable
fun StatusBarGlassOverlay() {
    val C = LocalAppColors.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (topPadding <= 0.dp) return
    Box(
        Modifier
            .fillMaxWidth()
            .height(topPadding)
            .background(
                if (C.isDark) Color(0x330A0910) else Color(0x33EEEEEE)
            )
            .drawWithContent {
                drawContent()
                // Bottom-edge rim light — glass thickness indicator
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (C.isDark) Color(0x0CFFFFFF) else Color(0x08000000),
                        ),
                        startY = size.height * 0.6f,
                        endY = size.height,
                    ),
                )
            }
    )
}
