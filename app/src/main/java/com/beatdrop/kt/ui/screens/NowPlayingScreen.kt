package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.theme.*

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
) {
    val track = SampleData.defaultTrack
    var isPlaying by remember { mutableStateOf(true) }
    var isAdded by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0=off, 1=all, 2=one
    var progress by remember { mutableFloatStateOf(0.42f) }

    // Animated ambient haze
    val infiniteTransition = rememberInfiniteTransition(label = "haze")
    val hazeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = EaseInOut), RepeatMode.Reverse),
        label = "haze_alpha",
    )

    Box(modifier = Modifier.fillMaxSize().background(ScreenThemes.nowPlaying)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.Black.copy(alpha = 0.30f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Text(
                    track.album.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (0.22 * 13).sp),
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                    ) { if (track.album.isNotEmpty()) onOpenAlbum(track.album) },
                )
                Surface(onClick = {}, shape = CircleShape, color = Color.Black.copy(alpha = 0.30f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Cover art ──
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp)).background(CoverGradients.get(track.coverIndex)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(100.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── Track meta — artist clickable ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.displayMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp).clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                        ) { onOpenArtist(track.artist) },
                    )
                }
                Surface(
                    onClick = { isAdded = !isAdded }, shape = CircleShape,
                    color = if (isAdded) Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, if (isAdded) Accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isAdded) Icons.Filled.Check else Icons.Outlined.Add, null, tint = if (isAdded) Accent else TextMedium, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Progress bar ──
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.90f)))
                    Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = (progress * 100).dp).size(14.dp).background(Color.White, CircleShape))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1:12", style = MaterialTheme.typography.bodySmall.copy(color = TextLow))
                    Text(track.duration, style = MaterialTheme.typography.bodySmall.copy(color = TextLow))
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Transport controls ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                IconButton(onClick = { isShuffle = !isShuffle }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Shuffle, null, tint = if (isShuffle) Accent else Color.White.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                // Previous
                IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(18.dp))
                // Play/Pause
                Surface(onClick = { isPlaying = !isPlaying }, shape = CircleShape, color = Color.White.copy(alpha = 0.04f), border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)), shadowElevation = 10.dp, modifier = Modifier.size(70.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(18.dp))
                // Next
                IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(12.dp))
                // Repeat
                IconButton(onClick = { repeatMode = (repeatMode + 1) % 3 }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        null, tint = if (repeatMode > 0) Accent else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Bottom actions ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                // Queue
                IconButton(onClick = onOpenQueue, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.QueueMusic, null, tint = TextMedium, modifier = Modifier.size(22.dp))
                }
                // Share
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Share, null, tint = TextMedium, modifier = Modifier.size(22.dp))
                }
                // Devices
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Cast, null, tint = TextMedium, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Lyrics drawer at bottom ──
        Surface(
            onClick = onOpenLyrics,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xDD14283C),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            shadowElevation = 10.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(78.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(12.dp))
                Text("Lyrics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            }
        }
    }
}
