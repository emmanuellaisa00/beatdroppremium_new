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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.specularHighlight
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
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            android.content.Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return
                // The clipboard watcher will detect this URL and show the dialog
            }
            android.content.Intent.ACTION_VIEW -> {
                val data = intent.data ?: return
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
    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null

    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) { SplashScreen(onDone = { showSplash = false }); return }

    LaunchedEffect(Unit) { vm.connect() }
    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            vm.loadLibrary()
            if (notifPerm != null && !notifPerm.status.isGranted) notifPerm.launchPermissionRequest()
        }
    }

    // Read versionCode at runtime via PackageManager — avoids needing
    // buildFeatures.buildConfig = true in Gradle (AGP 8+ default).
    val currentVersionCode = remember {
        runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
            else @Suppress("DEPRECATION") info.versionCode
        }.getOrDefault(0)
    }

    var onboarded by rememberSaveable { mutableStateOf(false) }
    if (!onboarded && !perm.status.isGranted) {
        OnboardingScreen(onGetStarted = {
            onboarded = true
            perm.launchPermissionRequest()
            // Fresh install — mark the *current* version as 'seen' so the
            // What's New sheet doesn't show after onboarding. Existing
            // users have lastSeen < currentVersionCode and will see the
            // sheet on first launch after the update.
            vm.markWhatsNewSeen(currentVersionCode)
        }); return
    }
    if (!perm.status.isGranted) {
        PermissionPrompt(onRequest = { perm.launchPermissionRequest() })
        return
    }

    // ── What's New sheet (post-update, pre-content) ─────────────────────
    // Show once per version bump for users who already onboarded. The sheet
    // dismisses itself by writing currentVersionCode back to prefs.
    var showWhatsNew by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        if (showWhatsNew == null) {
            val seen = vm.lastSeenWhatsNew()
            // Only show when we have a real prior install (seen >= 0) and
            // we've actually bumped the version since.
            showWhatsNew = seen in 0 until currentVersionCode
        }
    }
    if (showWhatsNew == true) {
        com.beatdrop.kt.ui.components.WhatsNewSheet(
            onDismiss = {
                vm.markWhatsNewSeen(currentVersionCode)
                showWhatsNew = false
            },
        )
    }

    // Clipboard URL detection
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
    // ✅ UX1 Fixed: Active state uses accent tint (design choice — Lucide icons are outline-only). No separate filled icons required.
    TabSpec2("library",  "Library",  Ic.Library, Ic.Library),
    TabSpec2("discover", "Discover", Ic.Discover,      Ic.Discover),
    TabSpec2("search",   "Search",   Ic.Search,     Ic.Search),
    TabSpec2("radio",    "Radio",    Ic.Podcast,     Ic.Podcast),
    TabSpec2("settings", "Settings", Ic.Settings,     Ic.Settings),
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
    data object Downloads : Dest
    data object Trending : Dest
    data object Browser : Dest
    data object Storage : Dest
    data object PrivateFolder : Dest
    data class VideoPlayer(val path: String, val title: String) : Dest
    data class Channel(val channelId: String, val name: String, val thumb: String?) : Dest
    data class ClipUrl(val url: String) : Dest
    data class PlaylistDownload(val playlistId: String) : Dest
    /** YT-Music online album detail screen (Spotify-style). */
    data class OnlineAlbum(val album: com.beatdrop.kt.youtube.OnlineAlbum) : Dest
}

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Main Scaffold
// Background: #050505 + ambient glow (rgba(30,80,200,.12))
// Accent: #21FF6B (Spotify Green)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun MainScaffold(vm: PlayerViewModel) {
    val C = LocalAppColors.current
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("library") }
    val stack = remember { mutableStateListOf<Dest>() }
    val currentDest: Dest = stack.lastOrNull() ?: Dest.Tabs
    fun push(d: Dest) { stack.add(d) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) } // ✅ UX3: pop() handled by AnimatedContent (improved transition spec)
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    // ── Privacy / Terms acceptance gate ───────────────────────────────
    // First-time prompt the moment the user lands on Discover / Search /
    // Radio (the network-touching tabs). Local-only tabs (Library,
    // Settings) don't trigger it. Acceptance is persisted as the
    // currentVersionCode in Prefs.termsAcceptedVersion so this shows
    // exactly once per app version that changes the policy.
    val currentVersionCode = remember {
        runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
            else @Suppress("DEPRECATION") info.versionCode
        }.getOrDefault(0)
    }
    var termsLoaded by rememberSaveable { mutableStateOf(false) }
    var needsTerms  by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!termsLoaded) {
            val accepted = vm.termsAcceptedVersion()
            needsTerms = accepted < currentVersionCode
            termsLoaded = true
        }
    }
    val touchedNetworkTab = tab == "discover" || tab == "search" || tab == "radio"
    if (needsTerms && touchedNetworkTab) {
        com.beatdrop.kt.ui.components.TermsSheet(
            onAccept = {
                vm.acceptTerms(currentVersionCode)
                needsTerms = false
            },
        )
    }

    val current    by vm.current.collectAsState()
    val isPlaying  by vm.isPlaying.collectAsState()
    val pos        by vm.position.collectAsState()
    val dur        by vm.duration.collectAsState()

    val artColor = com.beatdrop.kt.ui.components.rememberArtworkColor(current?.artworkUri)
    val bgColors = if (C.isDark) {
        listOf(
            artColor.copy(alpha = 0.32f),
            Color(0xFF0B0B0B),
            Color(0xFF050505),
        )
    } else {
        listOf(
            artColor.copy(alpha = 0.18f),
            Color(0xFFF8F8FC),
            Color(0xFFF0EDF5),
        )
    }

    val tilt = com.beatdrop.kt.ui.components.rememberDeviceTilt()

    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(Modifier.fillMaxSize()) {
            // ── Global blurred artwork background ────────────────────────────
            if (current != null) {
                AsyncImage(
                    model  = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(current?.artworkUri)
                        .crossfade(true)
                        .size(512)
                        .build(),
                    contentDescription = null,
                    contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect
                                    .createBlurEffect(150f, 150f, android.graphics.Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                            alpha = if (C.isDark) 0.55f else 0.38f
                        },
                )
            } else {
                // Deep background with ambient glow (Spotify style)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(bgColors)
                        )
                        .drawWithContent {
                            drawContent()
                            // Ambient background glow — adapts to theme (Spotify blue=cyan in dark, purple in light)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (C.isDark) Color(0x1E1E5080) else Color(0x141E50C8),  // dark: rgba(30,80,200,.12), light: rgba(30,80,200,.08)
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                                    radius = size.width * 0.8f,
                                ),
                            )
                        },
                )
            }

            // ── Global translucent glass tint & specular reflection ──────────
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (C.isDark) Color(0xE0050505)
                        else Color(0xE8FFFFFF),
                    )
                    .specularHighlight(
                        tilt,
                        intensity = if (C.isDark) 0.06f else 0.04f,
                        radius    = 1000f,
                    ),
            )

            // ── Animated screen transitions ──────────────────────────────────
            AnimatedContent(
                targetState = currentDest,
                transitionSpec = {
                    val isPush = targetState != Dest.Tabs && initialState == Dest.Tabs
                    if (targetState == Dest.Tabs && initialState == Dest.Tabs) {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(200)) // Spotify-smooth tab transition // ✅ UX2 Fixed
                    } else if (isPush) {
                        (slideInHorizontally(tween(340)) { it / 3 } + fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.96f)) togetherWith (fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.98f)) // Premium Spotify-like push
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
                        Dest.Search          -> SearchScreen(
                            vm,
                            onExpandPlayer = { push(Dest.NowPlaying) },
                            onOpenOnlineAlbum = { push(Dest.OnlineAlbum(it)) },
                            // Entered via Discover → online-only catalog.
                            mode = com.beatdrop.kt.ui.screens.SearchMode.ONLINE_ONLY,
                        )
                        Dest.NowPlaying      -> NowPlayingScreen(vm, onCollapse = { pop() }, onOpenQueue = { push(Dest.Queue) })
                        Dest.Queue           -> QueueScreen(vm, onClose = { pop() })
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
                        is Dest.OnlineAlbum -> com.beatdrop.kt.ui.screens.OnlineAlbumScreen(
                            vm = vm,
                            album = dest.album,
                            onBack = { pop() },
                            onExpandPlayer = { push(Dest.NowPlaying) },
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
                    "search"   -> SearchScreen(
                        vm,
                        onExpandPlayer = onExpandPlayer,
                        // Bottom tab → hybrid (local library + YT catalog).
                        mode = com.beatdrop.kt.ui.screens.SearchMode.HYBRID,
                    )
                    "radio"    -> RadioScreen(vm)
                    "settings" -> SettingsScreen(vm, onBack = {}, onOpenEq = onOpenEq, onOpenDebug = onOpenDebug)
                }
            }
        }
        // ── Bottom dock backdrop scrim ──────────────────────────────────────
        // The page scrollable above ends at fillMaxSize → its bottom rows
        // scroll under the MiniPlayer + GlassTabBar. The dock surfaces
        // sample THAT through their backdrop blur, smearing list text into
        // the glass. A short vertical-gradient scrim (transparent at top,
        // bg0 at bottom) sits BEHIND the dock so haze samples the scrim,
        // not the page content. Doesn't touch MiniPlayer/TabBar internals.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .drawBehind {
                    val scrim = C.bg0
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f    to Color.Transparent,
                            0.25f to scrim.copy(alpha = 0.55f),
                            1f    to scrim,
                        ),
                    )
                }
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().background(Color.Transparent)) {
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
        StatusBarGlassOverlay()
    }
}

@Composable private fun PlaylistsScreenHosted(vm: PlayerViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) = PlaylistsScreen(vm, onBack = onBack, onOpen = onOpen)
@Composable private fun StatsHosted(vm: PlayerViewModel, onBack: () -> Unit) = StatsScreen(vm, onBack = onBack)

// ═══════════════════════════════════════════════════════════════════════════════
// Status Bar Glass Overlay — Backdrop blur + rim light
// ═══════════════════════════════════════════════════════════════════════════════

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
                if (C.isDark) Color(0x30050505) else Color(0x30EEEEEE)
            )
            .drawWithContent {
                drawContent()
                // Bottom-edge rim light
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (C.isDark) Color(0x10FFFFFF) else Color(0x0A000000),
                        ),
                        startY = size.height * 0.6f,
                        endY = size.height,
                    ),
                )
            },
    )
}