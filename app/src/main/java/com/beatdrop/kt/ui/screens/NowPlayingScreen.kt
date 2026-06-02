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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.AppleLyrics
import com.beatdrop.kt.ui.components.glassBlur
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.components.rememberArtworkColor
import com.beatdrop.kt.ui.theme.LocalAppColors

@androidx.compose.material3.ExperimentalMaterial3Api
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
    val pos           by vm.position.collectAsState()
    val dur           by vm.duration.collectAsState()
    val lyrics        by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val activeLyric   by vm.activeLyric.collectAsState()
    val liked         by vm.liked.collectAsState()
    val volume        by vm.volume.collectAsState()
    val mixingNext    by vm.mixingNext.collectAsState()
    var showLyrics    by remember { mutableStateOf(false) }
    var showActions   by remember { mutableStateOf(false) }

    val t = track ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Nothing playing", color = C.textSecondary)
        }
        return
    }

    if (showActions) {
        com.beatdrop.kt.ui.components.TrackActionsSheet(vm, t, onDismiss = { showActions = false })
    }

    // ── Art pulse animation ───────────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        1f, 1.032f,
        infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse",
    )
    val artScale by animateFloatAsState(
        if (isPlaying) pulse else 0.88f,
        spring(stiffness = Spring.StiffnessLow),
        label = "art",
    )

    val artColor = rememberArtworkColor(t.artworkUri)
    var dragAccum by remember { mutableStateOf(0f) }

    // ── Full-screen backdrop ──────────────────────────────────────────────────
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(artColor.copy(alpha = 0.72f), Color(0xFF080810))
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragAccum > 180f) onCollapse(); dragAccum = 0f },
                ) { _, dy -> if (dy > 0) dragAccum += dy }
            }
    ) {
        // Blurred art backdrop
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(80f, 80f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    alpha = 0.38f
                },
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))

        // ── Content column ────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Drag pill
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .pressableScale(onClick = onCollapse)
                )
            }

            // ── Auto-Mix "next up" pill ────────────────────────────────────────
            // Shown only during a crossfade. The icon + title give immediate
            // feedback that the seamless blend is happening.
            mixingNext?.let { upNext ->
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 2.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Mixing in: ${upNext.title}",
                        color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp),
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
                label = "modeSwitch",
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
                                .fillMaxWidth(0.90f)
                                .aspectRatio(1f)
                                .scale(artScale)
                                .shadow(32.dp, RoundedCornerShape(22.dp), clip = false)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Black.copy(alpha = 0.15f)),
                            Alignment.Center,
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                } else {
                    // ── LYRICS MODE ───────────────────────────────────────────
                    Column(Modifier.fillMaxSize()) {
                        // Compact header: thumbnail + title + artist
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    t.artist,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Star icon in lyrics header
                            val isFav = liked.contains(t.id)
                            IconButton(onClick = { vm.toggleLike(t.id) }) {
                                Icon(
                                    if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    "Favourite",
                                    tint = if (isFav) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Filled.MoreHoriz, "More",
                                    tint = Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(22.dp),
                                )
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
                                    Text(
                                        "No synced lyrics available.",
                                        color = Color.White.copy(alpha = 0.55f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            else -> {
                                AppleLyrics(
                                    lines       = lyrics,
                                    activeIndex = activeLyric,
                                    modifier    = Modifier.weight(1f),
                                    onSeek      = { vm.seekTo(it) },
                                )
                            }
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
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title,
                            color      = Color.White,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            t.artist,
                            color    = Color.White.copy(alpha = 0.62f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    val isFav = liked.contains(t.id)
                    // Star — filled gold when liked, bright white outline when not
                    IconButton(onClick = { vm.toggleLike(t.id) }) {
                        Icon(
                            if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                            "Favourite",
                            tint     = if (isFav) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    // More — opens track actions sheet
                    IconButton(onClick = { showActions = true }) {
                        Icon(
                            Icons.Filled.MoreHoriz, "More",
                            tint     = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // ── Seek bar ──────────────────────────────────────────────────────
            val safeDur = dur.coerceAtLeast(1L)
            Slider(
                value         = pos.coerceIn(0, safeDur).toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange    = 0f..safeDur.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor   = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                    thumbColor         = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                thumb = {
                    // Larger thumb for one-handed use
                    Box(
                        Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.White)
                    )
                },
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                Text("-${fmt((dur - pos).coerceAtLeast(0L))}", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            // ── Transport controls ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Previous / seek-back
                IconButton(onClick = { vm.prev() }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(46.dp))
                }
                // Play / pause — large circle button
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .pressableScale(onClick = { vm.togglePlay() }),
                    Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/Pause",
                        tint     = Color.White,
                        modifier = Modifier.size(42.dp),
                    )
                }
                // Next / seek-forward
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(46.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Volume slider (SnapTube / Apple Music style) ──────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.VolumeDown, "Min volume",
                    tint     = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
                Slider(
                    value         = volume,
                    onValueChange = { vm.setVolume(it) },
                    valueRange    = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor   = Color.White.copy(alpha = 0.90f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        thumbColor         = Color.White,
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Icon(
                    Icons.Filled.VolumeUp, "Max volume",
                    tint     = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom dock: Lyrics · AirPlay · Queue ─────────────────────────
            // Frosted glass pill so icons are always legible on any background
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Lyrics toggle
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Filled.FormatQuote, "Lyrics",
                        tint     = if (showLyrics) C.accent else Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                // Thin divider
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // AirPlay / audio output
                IconButton(onClick = {
                    com.beatdrop.kt.playback.AudioOutput.openSwitcher(ctx)
                }) {
                    Icon(
                        Icons.Filled.Airplay, "Audio output",
                        tint     = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                // Thin divider
                Box(Modifier.height(22.dp).width(0.8.dp).background(Color.White.copy(alpha = 0.22f)))
                // Queue
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Filled.List, "Queue",
                        tint     = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
