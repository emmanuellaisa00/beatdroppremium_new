package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.theme.*

@Composable
fun LyricsScreen(
    onBack: () -> Unit = {},
) {
    val track = SampleData.defaultTrack
    val lyricsLines = SampleData.lyricsLines
    var isPlaying by remember { mutableStateOf(true) }
    var activeLine by remember { mutableIntStateOf(4) } // 5th line active

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenThemes.lyrics),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top bar: mini art + track info | close ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mini cover art
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CoverGradients.get(track.coverIndex)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(track.artist, style = MaterialTheme.typography.bodySmall.copy(color = TextMedium))
                }
                // Close button
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.40f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Lyrics body ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(lyricsLines) { index, line ->
                    if (line == null) {
                        // Gap indicator (animated dots)
                        GapIndicator()
                    } else {
                        val isActive = index == activeLine
                        val isPassed = index < activeLine

                        Text(
                            line,
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = when {
                                    isActive -> Color.White
                                    isPassed -> Color.White.copy(alpha = 0.18f)
                                    else -> Color.White.copy(alpha = 0.32f)
                                },
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { activeLine = index },
                        )
                    }
                }
            }
        }

        // ── Bottom transport bar ──
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xC716161C),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, bottom = 22.dp)
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                // Play button
                Surface(
                    onClick = { isPlaying = !isPlaying },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.Black, modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.42f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Accent),
                    )
                }
                Spacer(Modifier.width(16.dp))
                // Time
                Text(
                    "1:24",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextLow),
                )
            }
        }
    }
}

@Composable
private fun GapIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "gap_pulse")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 22.dp),
    ) {
        repeat(3) { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, delayMillis = i * 180, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulse_$i",
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, delayMillis = i * 180, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "alpha_$i",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        Color.White.copy(alpha = alpha * 0.45f),
                        CircleShape,
                    ),
            )
        }
    }
}
