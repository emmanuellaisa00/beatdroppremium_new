package com.beatdrop.kt.ui.screens

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
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.AppleLyrics
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.components.rememberArtworkColor
import com.beatdrop.kt.ui.components.specularHighlight
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Now Playing Screen
// Background: #050505 + ambient glow rgba(50,120,255,.18)
// Accent: #21FF6B (Spotify Green)
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
    var dragAccum by remember { mutableStateOf(0f) }
    val tilt = com.beatdrop.kt.ui.components.rememberDeviceTilt()

    // ── Full-screen backdrop — theme-aware deep dark gradient ───────────────
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        artColor.copy(alpha = 0.78f),
                        if (C.isDark) Color(0xFF050505) else Color(0xFF1A1A2E),
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragAccum > 180f) onCollapse(); dragAccum = 0f },
                ) { _, dy -> if (dy > 0) dragAccum += dy }
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

        // Contrast overlay — adapts to theme
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (C.isDark) Color.Black.copy(alpha = 0.35f)
                    else Color.Black.copy(alpha = 0.50f),
                )
        )

        // ── Ambient player glow — adapts to theme (spec rule) ────────────────
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (C.isDark) Color(0x2D3278FF) else Color(0x1A9D4EDD),  // dark: blue, light: purple
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.3f),
                            radius = size.width * 0.7f,
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
                    (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 12 }) togetherWith
                    (fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 12 })
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
                                    tint     = if (isFav) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.80f),
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
                                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
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
                            tint     = if (isFav) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(onClick = { showActions = true }) {
                        Icon(Ic.More, "More",
                            tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Seek bar — accent green track + diegetic buffer indicator ────
            // The slider sits inside a Box so we can paint a thin buffer
            // overlay on top of it. When the buffer is ahead of the playhead
            // by ≥2 s a faint accent rail is drawn from pos→bufferedPos with
            // a small pulsing dot at the buffer head. Communicates "ready
            // ahead" without any text/spinner. Resting opacity is low so
            // it never competes with the main track.
            val safeDur = dur.coerceAtLeast(1L)
            val buffered by vm.bufferedPosition.collectAsState()
            val bufferLeadMs = (buffered - pos).coerceAtLeast(0L)
            val showBufferRail = t.isStreaming && bufferLeadMs >= 2_000L && safeDur > 0L
            val bufferPulse by rememberInfiniteTransition(label = "buffer-pulse").animateFloat(
                initialValue = 0.35f, targetValue = 0.85f,
                animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "buffer-pulse-alpha",
            )
            Box(Modifier.fillMaxWidth().height(40.dp)) {
                Slider(
                    value         = pos.coerceIn(0, safeDur).toFloat(),
                    onValueChange = { vm.seekTo(it.toLong()) },
                    valueRange    = 0f..safeDur.toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor   = C.accent,       // Spotify Green
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        thumbColor         = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    thumb = {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(Color.White)
                                .shadow(4.dp, CircleShape),
                        )
                    },
                )
                if (showBufferRail) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.matchParentSize().padding(horizontal = 12.dp),
                    ) {
                        val w = size.width
                        val centerY = size.height / 2f
                        val posX = (pos.toFloat() / safeDur) * w
                        val bufX = (buffered.coerceAtMost(safeDur).toFloat() / safeDur) * w
                        // Faint accent rail from playhead → buffer head.
                        drawLine(
                            color = C.accent.copy(alpha = 0.35f),
                            start = androidx.compose.ui.geometry.Offset(posX, centerY),
                            end   = androidx.compose.ui.geometry.Offset(bufX, centerY),
                            strokeWidth = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        // Pulsing dot at the buffer head.
                        drawCircle(
                            color = C.accent.copy(alpha = bufferPulse),
                            radius = 3.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(bufX, centerY),
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                Text("-${fmt((dur - pos).coerceAtLeast(0L))}", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            // ── Transport controls — glass capsule with Spotify Green accent ─
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Previous
                IconButton(
                    onClick = { vm.prev() },
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { vm.seekTo((vm.position.value - 5000).coerceAtLeast(0)) },
                            onTap = { vm.prev() },
                        )
                    },
                ) {
                    Icon(Ic.SkipPrev, "Previous", tint = Color.White, modifier = Modifier.size(46.dp))
                }

                // Play / pause — glass circle with green accent
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(C.accent.copy(alpha = 0.25f))       // Green tint glass
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .border(1.dp, C.accent.copy(alpha = 0.40f), CircleShape)
                        .pressableScale(onClick = { vm.togglePlay() }),
                    Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Ic.TransportPause else Ic.TransportPlay,
                        "Play/Pause",
                        tint     = C.accent,         // Spotify Green icon
                        modifier = Modifier.size(42.dp),
                    )
                }

                // Next
                IconButton(
                    onClick = { vm.next() },
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { vm.seekTo((vm.position.value + 5000).coerceAtMost(vm.duration.value)) },
                            onTap = { vm.next() },
                        )
                    },
                ) {
                    Icon(Ic.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(46.dp))
                }
                // Note: During online fetch, next press is handled safely in ViewModel (onlineTransitionInProgress guard)
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
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
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
                // Smart Shuffle — sparkle accent when active
                // ✅ UX11 Fixed: Shows toast/message when toggled (smartShuffleMessage)
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // Download — diegetic progress ring drawn around the glyph.
                // No spinner, no progress bar — the ring is the indicator.
                val videoId = track?.sourceVideoId
                val isDownloaded = videoId != null && vm.isOnlineDownloaded(videoId)
                val dlJob = videoId?.let { vm.downloadJobFor(it) }
                val isDownloading = dlJob?.status == com.beatdrop.kt.youtube.DownloadStatus.DOWNLOADING
                if (videoId != null) {
                    com.beatdrop.kt.ui.components.DiegeticDownloadIcon(
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        progressPercent = dlJob?.progress ?: 0,
                        onClick = { vm.downloadOnlineWithMetadata(videoId) },
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
            enter   = fadeIn(tween(220)),
            exit    = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                artColor.copy(alpha = 0.92f),
                                if (C.isDark) Color(0xFF050505) else Color(0xFF111122),
                            )
                        )
                    ),
            ) {
                AppleLyrics(
                    lines       = lyrics,
                    activeIndex = activeLyric,
                    modifier    = Modifier.fillMaxSize(),
                    onSeek      = { vm.seekTo(it) },
                    // No onTapAny here → taps fall through to onSeek
                    // (proper Apple Music behaviour in full mode).
                )
                // Close button — small floating glass puck top-right
                IconButton(
                    onClick = { fullLyrics = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(40.dp),
                ) {
                    Icon(
                        Ic.Close, "Close lyrics",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}