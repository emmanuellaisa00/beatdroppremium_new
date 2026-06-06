package com.beatdrop.kt.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Now Playing — rewritten from scratch to pixel-match the BeatDrop HTML concept.
 *
 * Layout (top to bottom):
 *   Top bar          chevron-down + uppercase album title
 *   Cover            large rounded square (matches HTML .np-cover)
 *   Track meta       "4×4" big bold title + "Don Toliver" + circular +
 *   Progress bar     thin rail + white knob + 1:12 / 3:22 timestamps
 *   Controls         prev / inset-glass big-play / next
 *   Bottom row       devices speaker (left) + share (right)
 *   Lyrics drawer    peeking up from the bottom edge with a chevron-up hint
 */
@Composable
fun NowPlayingScreen(
    vm: PlayerViewModel,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val t   by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val pos by vm.position.collectAsState()
    val dur by vm.duration.collectAsState()
    val shuffleOn by vm.shuffle.collectAsState()
    val liked by vm.liked.collectAsState()

    val track = t ?: return

    // Vertical-drag-to-collapse — the screen follows the finger then snaps closed.
    var dragOffset by remember { mutableStateOf(0f) }

    val artColor = com.beatdrop.kt.ui.components.rememberArtworkColor(track.artworkUri)

    Box(
        Modifier
            .fillMaxSize()
            // Radial wash sampled from the album cover — calm, lets the cover read.
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        artColor.copy(alpha = 0.55f),
                        Color(0xFF050507),
                        Color(0xFF000000),
                    ),
                )
            )
            .graphicsLayer {
                translationY = dragOffset
                val p = (dragOffset / 420f).coerceIn(0f, 1f)
                scaleX = 1f - p * 0.04f
                scaleY = 1f - p * 0.04f
                alpha = 1f - p * 0.12f
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
        // Blurred art backdrop for added depth (calm — 30% alpha)
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(120f, 120f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    alpha = 0.30f
                },
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = PageHorizontalPadding),
        ) {
            // ── Top bar ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .pressableScale(onClick = onCollapse, scaleTo = 0.85f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Ic.ChevronDown, "Close", tint = C.text.copy(alpha = 0.92f), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    (track.album.ifBlank { "Now Playing" }).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    color = C.text.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(8f),
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(36.dp))
            }

            // ── Cover ──────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(top = 8.dp, bottom = 10.dp)
                    .shadow(elevation = 36.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A0A0E))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (track.artworkUri.toString().isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Ic.MusicNote, null,
                        tint = C.text.copy(alpha = 0.22f),
                        modifier = Modifier.fillMaxSize(0.38f),
                    )
                }
            }

            // ── Track meta ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = C.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.artist,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = C.text.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                val isFav = liked.contains(track.id)
                Box(
                    Modifier
                        .size(42.dp)
                        .premiumGlass(level = GlassLevel.Z2_Card, shape = CircleShape)
                        .pressableScale(onClick = { vm.toggleLike(track.id) }, scaleTo = 0.88f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isFav) Ic.Check else Ic.Add,
                        "Favourite",
                        tint = if (isFav) C.accent else C.text.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // ── Progress ───────────────────────────────────────────────────
            Spacer(Modifier.height(22.dp))
            IosSeekBar(
                positionMs = pos,
                durationMs = dur,
                onSeek = vm::seekTo,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatMs(pos),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.text.copy(alpha = 0.55f),
                )
                Text(
                    formatMs(dur),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.text.copy(alpha = 0.55f),
                )
            }

            // ── Controls ───────────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .pressableScale(onClick = { vm.prev() }, scaleTo = 0.86f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Ic.SkipPrev, "Previous", tint = C.text, modifier = Modifier.size(30.dp))
                }
                Box(
                    Modifier
                        .size(76.dp)
                        .premiumGlass(level = GlassLevel.Z5_ActiveLens, shape = CircleShape)
                        .pressableScale(onClick = { vm.togglePlay() }, scaleTo = 0.94f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Ic.TransportPause else Ic.TransportPlay,
                        "Play/Pause",
                        tint = C.text,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .pressableScale(onClick = { vm.next() }, scaleTo = 0.86f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Ic.SkipNext, "Next", tint = C.text, modifier = Modifier.size(30.dp))
                }
            }

            // ── Bottom row: shuffle + cast + queue (matches HTML left/right) ─
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).pressableScale(onClick = { vm.toggleShuffle() }, scaleTo = 0.85f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Ic.Shuffle, "Shuffle",
                        tint = if (shuffleOn) C.accent else C.text.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    Modifier.size(36.dp).pressableScale(
                        onClick = { com.beatdrop.kt.playback.AudioOutput.openSwitcher(ctx) },
                        scaleTo = 0.85f,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Ic.Airplay, "Audio output", tint = C.text.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                }
                Box(
                    Modifier.size(36.dp).pressableScale(onClick = onOpenQueue, scaleTo = 0.85f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Ic.Queue, "Queue", tint = C.text.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Lyrics drawer (peeking up from the bottom edge) ────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(bottom = 18.dp)
                    .premiumGlass(level = GlassLevel.Z3_MiniPlayer, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                    .pressableScale(onClick = onOpenQueue, scaleTo = 0.98f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Ic.ChevronUp, null,
                        tint = C.text.copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Lyrics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.text,
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
