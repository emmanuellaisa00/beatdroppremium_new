package com.beatdrop.kt.ui.screens

import androidx.activity.compose.BackHandler
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.ui.components.GlassLevel
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.premiumGlass
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.AppleLyrics
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.components.rememberArtworkColor
import com.beatdrop.kt.ui.components.specularHighlight
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing

// ═══════════════════════════════════════════════════════════════════════════════
// BeatDrop Glassmorphism Now Playing Screen
// Background: #050505 + ambient glow rgba(50,120,255,.18)
// Accent: #FA2D48 (Apple Music Pink)
// ═══════════════════════════════════════════════════════════════════════════════

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: PlayerViewModel,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit = {},
) {
    val C             = LocalAppColors.current
    val ctx           = LocalContext.current
    val track         by vm.current.collectAsState()
    val isPlaying     by vm.isPlaying.collectAsState()
    val smartShuffle  by vm.smartShuffle.collectAsState()
    val pos           by vm.position.collectAsState()
    val dur           by vm.duration.collectAsState()
    val lyrics        by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val activeLyric   by vm.activeLyric.collectAsState()
    val liked         by vm.liked.collectAsState()
    val volume        by vm.volume.collectAsState()
    val mixingNext    by vm.mixingNext.collectAsState()
    val onlineMessage by vm.onlineMessage.collectAsState()
    val lastFailed    by vm.lastFailedOnline.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLyrics    by remember { mutableStateOf(false) }
    var fullLyrics    by remember { mutableStateOf(false) }
    var showActions   by remember { mutableStateOf(false) }
    // Tap on any preview lyric line ⇒ expand to full-screen.
    // When `fullLyrics` is true, the existing onSeek wiring is what runs
    // (so taps in full mode = seek to that line, like Apple Music).

    val t = track ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Nothing playing", color = C.textSecondary)
        }
        return
    }

    // Hardware/system back should close the player sheet. Transport previous is
    // still handled by the on-screen skip-back button only.
    BackHandler { onCollapse() }


    LaunchedEffect(onlineMessage) {
        onlineMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (lastFailed != null) "Retry" else null,
            )
            if (result == SnackbarResult.ActionPerformed) vm.retryOnlinePlay()
            vm.clearOnlineMessage()
        }
    }

    if (showActions) {
        com.beatdrop.kt.ui.components.TrackActionsSheet(vm, t, onDismiss = { showActions = false })
    }

    // ── Art pulse animation ─────────────────────────────────────────────────
    // Resting: 1.0 ↔ 1.032 every 2.6s (calm breath while playing).
    // Fetching: 1.0 ↔ 1.05 every 1.1s — same motion vocabulary, just more
    //   urgent. Tells the user "we're working on it" without a spinner or
    //   "Loading…" text. Replaces the LinearProgressIndicator that used to
    //   sit under the title.
    val fetchingVideoId by vm.fetchingVideoId.collectAsState()
    val isFetching = fetchingVideoId != null && fetchingVideoId == t.sourceVideoId
    val infinite = rememberInfiniteTransition(label = "pulse")
    val restingPulse by infinite.animateFloat(
        1f, 1.032f,
        infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "resting-pulse",
    )
    val fetchPulse by infinite.animateFloat(
        1f, 1.05f,
        infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fetch-pulse",
    )
    val pulse = if (isFetching) fetchPulse else restingPulse
    val artScale by animateFloatAsState(
        if (isPlaying || isFetching) pulse else 0.88f,
        spring(stiffness = Spring.StiffnessLow),
        label = "art",
    )

    val artColor = rememberArtworkColor(t.artworkUri)
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.86f),
        label = "nowPlayingSwipeDown",
    )
    val dragProgress = (animatedDragOffset / 420f).coerceIn(0f, 1f)
    val tilt = com.beatdrop.kt.ui.components.rememberDeviceTilt()

    // ── Full-screen backdrop — calm radial wash from album color ───────────
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        artColor.copy(alpha = if (C.isDark) 0.38f else 0.22f),
                        if (C.isDark) Color(0xFF050507) else Color(0xFFEDEDF2),
                        if (C.isDark) Color(0xFF000000) else Color(0xFFF7F7F9),
                    ),
                )
            )
            .graphicsLayer {
                // Gesture-coupled collapse: as the user swipes down, the whole
                // player follows their finger, gently scales toward the dock,
                // then the MainScaffold transition finishes the morph into the
                // MiniPlayer.
                translationY = animatedDragOffset
                scaleX = 1f - dragProgress * 0.045f
                scaleY = 1f - dragProgress * 0.045f
                alpha = 1f - dragProgress * 0.10f
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 160f) onCollapse()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                ) { change, dy ->
                    if (dy > 0f) {
                        change.consume()
                        dragOffset = (dragOffset + dy).coerceIn(0f, 520f)
                    } else if (dragOffset > 0f) {
                        change.consume()
                        dragOffset = (dragOffset + dy * 0.55f).coerceAtLeast(0f)
                    }
                }
            },
    ) {
        // ── Blurred art backdrop ─────────────────────────────────────────────
        AsyncImage(
            model  = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(80f, 80f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    alpha = if (C.isDark) 0.40f else 0.30f
                },
        )

        // Light contrast overlay — keeps art subtle, lets pink ambient breathe
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (C.isDark) Color.Black.copy(alpha = 0.22f)
                    else Color.White.copy(alpha = 0.45f),
                )
        )

        // ── Ambient player glow — soft pink accent ──────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                C.accent.copy(alpha = if (C.isDark) 0.10f else 0.06f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.3f),
                            radius = size.width * 0.8f,
                        ),
                    )
                },
        )

        // ── Specular highlight (device tilt) ────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .specularHighlight(tilt, intensity = 0.05f, radius = 600f),
        )

        // ── Content column ────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Drag pill (glass style) ────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .pressableScale(onClick = onCollapse),
                )
            }

            // ── Auto-Mix "next up" glass pill ──────────────────────────────
            mixingNext?.let { upNext ->
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Ic.Sparkles,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Mixing in: ${upNext.title}",
                        color     = Color.White,
                        fontSize  = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.widthIn(max = 200.dp),
                    )
                }
            }

            // ── Art / Lyrics toggle pane ──────────────────────────────────────
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    // Faster lyrics-mode swap (Spotify-iOS feel):
                    //   in:  200/200 (was 320/320)
                    //   out: 150/150 (was 220/220)
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 12 }) togetherWith
                    (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 12 })
                },
                label      = "modeSwitch",
                modifier   = Modifier.weight(1f).fillMaxWidth(),
            ) { lyricsMode ->
                if (!lyricsMode) {
                    // ── ART MODE ─────────────────────────────────────────────
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                                .scale(artScale)
                                .shadow(36.dp, RoundedCornerShape(Radius.xxl), clip = false)
                                .clip(RoundedCornerShape(Radius.xxl))
                                .background(Color.Black.copy(alpha = 0.15f)),
                            Alignment.Center,
                        ) {
                            AsyncImage(
                                model  = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                    }
                } else {
                    // ── LYRICS MODE ───────────────────────────────────────────
                    Column(Modifier.fillMaxSize()) {
                        // Compact header: thumbnail + title + artist — glass strip
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(Radius.md))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(Radius.md)),
                            ) {
                                AsyncImage(
                                    model  = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize(),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title,  color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val isFav = liked.contains(t.id)
                            IconButton(onClick = { vm.toggleLike(t.id) }) {
                                Icon(
                                    Ic.Star,
                                    "Favourite",
                                    tint     = if (isFav) LocalAppColors.current.accent else Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(Ic.More, "More",
                                    tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(22.dp))
                            }
                        }

                        // Lyrics body
                        when {
                            lyricsLoading && lyrics.isEmpty() -> {
                                // Content-shaped ghost lines instead of a spinner.
                                // Crossfades into real AppleLyrics once the fetch lands.
                                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                    com.beatdrop.kt.ui.components.LyricsSilhouettes()
                                }
                            }
                            lyrics.isEmpty() -> {
                                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                    Text("No synced lyrics available.", color = Color.White.copy(alpha = 0.55f),
                                        textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            else -> AppleLyrics(
                                lines       = lyrics,
                                activeIndex = activeLyric,
                                modifier    = Modifier.weight(1f),
                                onSeek      = { vm.seekTo(it) },
                                // In the preview pane, swallow per-line taps
                                // and expand to full-screen lyrics instead.
                                // (The active line still stays pinned to
                                // viewport-centre via AppleLyrics' own
                                // dynamic centring.)
                                onTapAny    = { fullLyrics = true },
                            )
                        }
                    }
                }
            }

            // ── Metadata row (art mode only) ──────────────────────────────────
            AnimatedVisibility(
                visible = !showLyrics,
                enter   = fadeIn(tween(240)),
                exit    = fadeOut(tween(160)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title,  color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (t.isStreaming) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Streaming · ${t.artist}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            // The "fetching" state is communicated by the
                            // artwork pulse intensification (1.05@1.1s vs
                            // resting 1.032@2.6s) — see fetchPulse above.
                            // No spinner, no "Loading…" text by design.
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(t.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(10.dp))
                    val isFav = liked.contains(t.id)
                    IconButton(onClick = { vm.toggleLike(t.id) }) {
                        Icon(
                            Ic.Star,
                            "Favourite",
                            tint     = if (isFav) LocalAppColors.current.accent else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(onClick = { showActions = true }) {
                        Icon(Ic.More, "More",
                            tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Seek bar — iOS-style thin rail, no thumb at rest ───────────
            // While the stream URL is resolving (isFetching) the bar shows a
            // sweeping accent shimmer in place of progress — no spinner, no
            // text. Once playback starts the shimmer disappears and the
            // normal track + filled-progress takes over.
            // The buffered-ahead indicator was removed per design feedback:
            // users should see only their playback progress, not the
            // fetcher's progress.
            val safeDur = dur.coerceAtLeast(1L)
            com.beatdrop.kt.ui.components.IosSeekBar(
                positionMs = pos,
                durationMs = safeDur,
                onSeek = { vm.seekTo(it) },
                loading = isFetching,
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                Text("-${fmt((dur - pos).coerceAtLeast(0L))}", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            // ── Transport controls — clean monochrome row, big inset play orb ─
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxl, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Previous
                IconButton(
                    onClick = { vm.prev() },
                    modifier = Modifier.size(54.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { vm.seekTo((vm.position.value - 5000).coerceAtLeast(0)) },
                            onTap = { vm.prev() },
                        )
                    },
                ) {
                    Icon(Ic.SkipPrev, "Previous", tint = C.text.copy(alpha = 0.92f), modifier = Modifier.size(32.dp))
                }

                // Play / pause — inset glass orb (matches the HTML reference)
                Box(
                    Modifier
                        .size(76.dp)
                        .premiumGlass(level = com.beatdrop.kt.ui.components.GlassLevel.Z5_ActiveLens, shape = CircleShape)
                        .pressableScale(onClick = { vm.togglePlay() }),
                    Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Ic.TransportPause else Ic.TransportPlay,
                        "Play/Pause",
                        tint     = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Next
                IconButton(
                    onClick = { vm.next() },
                    modifier = Modifier.size(54.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { vm.seekTo((vm.position.value + 5000).coerceAtMost(vm.duration.value)) },
                            onTap = { vm.next() },
                        )
                    },
                ) {
                    Icon(Ic.SkipNext, "Next", tint = C.text.copy(alpha = 0.92f), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Volume slider (Apple Music style) ──────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.VolumeDown, "Min volume",
                    tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                Slider(
                    value         = volume,
                    onValueChange = { vm.setVolume(it) },
                    valueRange    = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor   = C.accent,              // Green active track
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        thumbColor         = Color.White,
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Icon(Ic.VolumeUp, "Max volume",
                    tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom dock: Lyrics · AirPlay · Queue — glass pill ─────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (C.isDark) 0.13f else 0.24f),
                                Color.White.copy(alpha = if (C.isDark) 0.06f else 0.14f),
                            ),
                        ),
                    )
                    .border(0.8.dp, Color.White.copy(alpha = if (C.isDark) 0.18f else 0.32f), RoundedCornerShape(50))
                    .drawWithContent {
                        drawContent()
                        // Top rim light on dock pill
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY   = size.height * 0.4f,
                            ),
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Lyrics toggle — green accent when active
                IconButton(onClick = {
                    showLyrics = !showLyrics
                    if (!showLyrics) fullLyrics = false  // close full mode when leaving lyrics
                }) {
                    Icon(
                        Ic.Lyrics, "Lyrics",
                        tint = if (showLyrics) C.accent else Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // Shuffle toggle — accent when on. Previously this slot
                // was empty (only a separator + comment existed) so users
                // reported the shuffle icon as 'missing'.
                IconButton(onClick = { vm.toggleShuffle() }) {
                    val shuffleOn by vm.shuffle.collectAsState()
                    Icon(
                        Ic.Shuffle, "Shuffle",
                        tint = if (shuffleOn) C.accent else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // Download — diegetic progress ring drawn around the glyph.
                // Streaming tracks: tap to download (ring fills as bytes
                // arrive, morphs to a green check when done).
                // Local tracks (no videoId): always show the green check
                // — they're inherently 'on this device' since they came
                // from the local library scan or a completed download.
                val videoId = track?.sourceVideoId
                val isLocalFile = track != null && videoId == null
                val isDownloaded = isLocalFile ||
                    (videoId != null && vm.isOnlineDownloaded(videoId))
                val dlJob = videoId?.let { vm.downloadJobFor(it) }
                val isDownloading = dlJob?.status == com.beatdrop.kt.youtube.DownloadStatus.DOWNLOADING
                if (track != null) {
                    com.beatdrop.kt.ui.components.DiegeticDownloadIcon(
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        progressPercent = dlJob?.progress ?: 0,
                        onClick = {
                            // No-op for local files (they're already here).
                            if (videoId != null && !isDownloaded && !isDownloading) {
                                vm.downloadOnlineWithMetadata(videoId)
                            }
                        },
                    )
                    // Smart Shuffle tooltip
                    val shuffleMsg by vm.smartShuffleMessage.collectAsState()
                    if (shuffleMsg != null) {
                        LaunchedEffect(shuffleMsg) {
                            kotlinx.coroutines.delay(3500)
                            vm.clearSmartShuffleMessage()
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = shuffleMsg != null,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 },
                        ) {
                            Text(
                                shuffleMsg ?: "",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                maxLines = 2,
                            )
                        }
                    }
                }
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // AirPlay
                IconButton(onClick = { com.beatdrop.kt.playback.AudioOutput.openSwitcher(ctx) }) {
                    Icon(Ic.Airplay, "Audio output",
                        tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // Queue
                IconButton(onClick = onOpenQueue) {
                    Icon(Ic.Queue, "Queue",
                        tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        // ── Full-screen lyrics overlay ─────────────────────────────────────
        // Triggered by tapping any line in the preview lyrics pane. Fills
        // the entire NowPlaying viewport (above transport, above header)
        // so the active line — still centred dynamically by AppleLyrics —
        // sits in the true middle of the screen. Tap any line here to
        // seek; tap the ✕ to collapse back to the preview pane.
        AnimatedVisibility(
            visible = fullLyrics && showLyrics && lyrics.isNotEmpty(),
            enter   = fadeIn(tween(150)),
            exit    = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                artColor.copy(alpha = if (C.isDark) 0.32f else 0.18f),
                                if (C.isDark) Color(0xFF050507) else Color(0xFFEDEDF2),
                                if (C.isDark) Color(0xFF000000) else Color(0xFFF7F7F9),
                            )
                        )
                    ),
            ) {
                if (lyricsLoading && lyrics.isEmpty()) {
                    // Ghost lines while LrcLib / sidecar fetch is in flight.
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        com.beatdrop.kt.ui.components.LyricsSilhouettes()
                    }
                } else {
                    AppleLyrics(
                        lines       = lyrics,
                        activeIndex = activeLyric,
                        modifier    = Modifier.fillMaxSize(),
                        onSeek      = { vm.seekTo(it) },
                        // No onTapAny here → taps fall through to onSeek
                        // (proper Apple Music behaviour in full mode).
                    )
                }
                // Apple Music-style song header pinned above full lyrics.
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .border(0.7.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                        .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.10f)),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).size(96).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { fullLyrics = false }, modifier = Modifier.size(40.dp)) {
                        Icon(Ic.Close, "Close lyrics", tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}