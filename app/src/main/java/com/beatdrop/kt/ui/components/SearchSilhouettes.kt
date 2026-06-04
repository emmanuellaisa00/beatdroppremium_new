package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * SearchResultSilhouettes — content-shaped placeholders for the online search
 * results list while a query is in flight.
 *
 * Replaces the bare CircularProgressIndicator that previously sat in the
 * center of the screen with N rows that exactly match the layout of a real
 * CatalogRow: a circular artwork dot, a fat title bar, a thin author bar,
 * and a trailing action dot. A faint accent shimmer sweeps across all
 * silhouettes simultaneously so the screen feels alive.
 *
 * When real results arrive the parent simply switches branches — there is
 * no layout shift because the silhouette row height matches CatalogRow.
 */
@Composable
fun SearchResultSilhouettes(rowCount: Int = 6, modifier: Modifier = Modifier) {
    val C = LocalAppColors.current
    val infinite = rememberInfiniteTransition(label = "search-silh")
    val xOffset by infinite.animateFloat(
        initialValue = -400f,
        targetValue  = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            C.accent.copy(alpha = 0.04f),
            C.accent.copy(alpha = 0.14f),
            C.accent.copy(alpha = 0.04f),
        ),
        start = Offset(xOffset, 0f),
        end   = Offset(xOffset + 240f, 240f),
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        repeat(rowCount) { idx ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork silhouette — matches the 44.dp Coil thumbnail.
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Title bar — varies width so the column doesn't look
                    // like a Lego set.
                    val titleFraction = listOf(0.85f, 0.7f, 0.92f, 0.6f, 0.78f, 0.83f)[idx % 6]
                    Box(
                        Modifier
                            .fillMaxWidth(titleFraction)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush),
                    )
                    // Author bar — shorter, slimmer.
                    val authorFraction = listOf(0.4f, 0.55f, 0.35f, 0.5f, 0.42f, 0.6f)[idx % 6]
                    Box(
                        Modifier
                            .fillMaxWidth(authorFraction)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(brush),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Trailing action dot (matches the download button position).
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush),
                )
            }
        }
    }
}
