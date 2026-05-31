package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.components.AppleLyrics
import com.beatdrop.kt.ui.components.rememberArtworkColor
import com.beatdrop.kt.ui.components.glassBlur
import com.beatdrop.kt.ui.components.pressableScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow

@Composable
fun NowPlayingScreen(vm: PlayerViewModel, onCollapse: () -> Unit, onOpenQueue: () -> Unit = {}) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val track by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val pos by vm.position.collectAsState()
    val dur by vm.duration.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val activeLyric by vm.activeLyric.collectAsState()
    var showLyrics by remember { mutableStateOf(false) }
    val liked by vm.liked.collectAsState()
    val volume by vm.volume.collectAsState()

    val t = track ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nothing playing", color = C.textSecondary) }
        return
    }

    // Breathing pulse while playing (artwork pulse)
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val artScale by animateFloatAsState(if (isPlaying) 1f else 0.88f, spring(stiffness = Spring.StiffnessLow), label = "art")

    // Dynamic color extracted from the album art (Apple Music style)
    val artColor = rememberArtworkColor(t.artworkUri)

    // Swipe-down-to-dismiss gesture
    var dragAccum by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(artColor.copy(alpha = 0.65f), Color(0xFF0D0D12))))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragAccum > 180f) onCollapse(); dragAccum = 0f },
                ) { _, dy -> if (dy > 0) dragAccum += dy }
            }
    ) {
        // Blurred album-art backdrop for liquid-glass depth
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().glassBlur(75f).alpha(0.4f),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 1. Header Row (Downward pill indicator) ──────────────────────
            Box(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(36.dp, 5.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .pressableScale(onClick = onCollapse)
                )
            }

            // ─── 2. Artwork Box (or Synced Lyrics) ───────────────────────────
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    Box(
                        Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
                            .scale(artScale * (if (isPlaying) pulse else 1f))
                            .shadow(28.dp, RoundedCornerShape(24.dp), clip = false)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.2f)),
                        Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                            contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    if (lyricsLoading && lyrics.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else if (lyrics.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(
                                "No synced lyrics available for this song.",
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        AppleLyrics(lyrics, activeLyric, Modifier.fillMaxSize()) { vm.seekTo(it) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── 3. Metadata & Actions Row (iOS Style) ───────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))

                // Favorite button (iOS Style Star outline/filled)
                val isFav = liked.contains(t.id)
                IconButton(onClick = { vm.toggleLike(t.id) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // More Options button (...)
                IconButton(onClick = { /* More details sheet can go here */ }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── 4. Sleek Progress Timeline & Negative Remaining Label ───────
            val safeDur = dur.coerceAtLeast(1L)
            Slider(
                value = pos.coerceIn(0, safeDur).toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..safeDur.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                // Negative countdown for remaining time (Classic iOS detail)
                val remaining = (dur - pos).coerceAtLeast(0L)
                Text("-${fmt(remaining)}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ─── 5. Playback Raw Controls (Clean, Large, Centered) ───────────
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prev() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious, null,
                        tint = Color.White, modifier = Modifier.size(44.dp)
                    )
                }
                IconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(80.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White, modifier = Modifier.size(54.dp)
                    )
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        Icons.Filled.SkipNext, null,
                        tint = Color.White, modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── 6. Volume Slider with Speaker Icons (iOS Style Bottom Placement) ───
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.VolumeMute, null,
                    tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = { vm.setVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        thumbColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Icon(
                    Icons.Filled.VolumeUp, null,
                    tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ─── 7. Bottom Utilities Bar (Lyrics, AirPlay/Audio output, Queue) ─────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quotes/Lyrics bubble on the Left
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) C.accent else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // AirPlay/Audio output icon in the Center
                IconButton(onClick = { com.beatdrop.kt.playback.AudioOutput.openSwitcher(ctx) }) {
                    Icon(
                        imageVector = Icons.Filled.Airplay,
                        contentDescription = "Audio output",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Queue/Playing Next list icon on the Right
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}


