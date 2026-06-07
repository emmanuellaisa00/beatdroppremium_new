/*
 * BeatDrop Premium — Mini Player + Bottom Dock
 * Matches HTML .mini and .dock components exactly.
 */

package com.beatdrop.kt.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.*

// ═══════════════════════════════════════════════════════════════════
// MINI PLAYER
// HTML: .mini (glass bar with cover, info, play button, progress)
// ═══════════════════════════════════════════════════════════════════

@Composable
fun MiniPlayer(
    trackName: String,
    artistName: String,
    coverIndex: Int,
    isPlaying: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idx = safeGradientIndex(coverIndex, CoverGradients.size)
    val (start, end) = CoverGradients[idx]
    val shape = RoundedCornerShape(16.dp)
    val glassBg = Color(0xB308060A) // rgba(8,6,10,0.70)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(glassBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onExpand),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
            ) {
                // Cover art (40dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(start, end), 145f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackName,
                        color = Color.White,
                        fontWeight = FontWeight.W700,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = artistName,
                        color = HtmlTokens.TextTertiary,
                        fontWeight = FontWeight.W500,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
                // Cast icon
                Icon(
                    Icons.Filled.Cast,
                    null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).padding(end = 4.dp),
                )
                // Play/pause button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 10.dp, vertical = 0.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.12.dp.value / 1.dp.value)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(HtmlTokens.Accent),
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BOTTOM DOCK
// HTML: .dock with 4 tabs (Home, Search, Library, Add)
// ═══════════════════════════════════════════════════════════════════

data class TabSpec(
    val name: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun BottomDock(
    tabs: List<TabSpec>,
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(Color(0xB308060A))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(vertical = 8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == activeIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onTabSelected(index) },
            ) {
                // Active indicator circle
                Box(
                    modifier = Modifier
                        .size(if (active) 40.dp else 32.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) HtmlTokens.Accent.copy(alpha = 0.15f) else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            if (active) HtmlTokens.Accent else Color.White.copy(alpha = 0.60f)
                    ) {
                        tab.icon()
                    }
                }
                Text(
                    text = tab.name,
                    color = if (active) HtmlTokens.Accent else Color.White.copy(alpha = 0.45f),
                    fontWeight = if (active) FontWeight.W700 else FontWeight.W600,
                    fontSize = 10.sp,
                    letterSpacing = 0.02.sp,
                )
            }
        }
    }
}
