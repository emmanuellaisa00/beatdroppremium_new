package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.theme.LocalAppColors
import kotlin.math.abs

/**
 * BeatDrop Mini Player — matches the HTML concept exactly:
 *   • 70 dp tall, radius 36, dark obsidian glass (same recipe as the dock)
 *   • Floats 14 dp above the dock, 20 dp horizontal insets
 *   • Circular album thumb on the left
 *   • Title (white) + artist (55% white) center
 *   • Devices/cast icon + plain white play triangle (no background fill)
 *   • Hairline pink progress line at the bottom edge
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onExpand: () -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current

    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val animX by animateFloatAsState(dragX, label = "miniX")
    val animY by animateFloatAsState(dragY.coerceAtMost(0f), label = "miniY")

    val outerShape = RoundedCornerShape(36.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                translationX = animX
                translationY = animY
            }
            .premiumGlass(level = GlassLevel.Z3_MiniPlayer, shape = outerShape)
            .pointerInput(track.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            dragX < -120f -> onNext()
                            dragX >  120f -> onPrev()
                            dragY < -100f -> onExpand()
                        }
                        dragX = 0f; dragY = 0f
                    },
                    onDragCancel = { dragX = 0f; dragY = 0f },
                ) { change, amount ->
                    change.consume()
                    if (abs(amount.x) > abs(amount.y)) dragX += amount.x
                    else dragY += amount.y
                }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onExpand() }) },
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art — circular
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(C.bg3)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (track.artworkUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(track.artworkUri)
                            .crossfade(true)
                            .size(coil.size.Size(140, 140))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Ic.MusicNote,
                        contentDescription = null,
                        tint = C.text.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = C.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = C.text.copy(alpha = 0.55f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Devices/cast icon — subtle, monochrome
            IconButton(onClick = onExpand, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Ic.Airplay,
                    contentDescription = null,
                    tint = C.text.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Play button — pure white triangle, NO background fill
            IconButton(onClick = onToggle, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = if (isPlaying) Ic.TransportPause else Ic.TransportPlay,
                    contentDescription = null,
                    tint = C.text,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Hairline progress at the bottom — subtle pink accent
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 36.dp)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(1.5.dp)
                .background(C.accent.copy(alpha = 0.75f))
        )
    }
}
