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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * LyricsSilhouettes — ghost lines for the lyrics pane while LrcLib /
 * sidecar lookup is in flight.
 *
 * Replaces the legacy CircularProgressIndicator with content-shaped
 * silhouettes that match what real lyric lines look like (one large
 * "active" silhouette in the centre, two faint neighbour silhouettes
 * above and below). A subtle white-tinted shimmer sweeps across so the
 * pane feels alive instead of empty. Zero text labels, zero spinners.
 *
 * Designed to occupy the same vertical space as a typical mid-song
 * lyric view so when real lyrics arrive there is minimal layout shift.
 */
@Composable
fun LyricsSilhouettes(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "lyrics-silh")
    val sweepX by infinite.animateFloat(
        initialValue = -360f,
        targetValue  = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.03f),
        ),
        start = Offset(sweepX, 0f),
        end   = Offset(sweepX + 240f, 240f),
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Two faint neighbour lines above the "active" placeholder.
        SilhouetteBar(brush = brush, widthFraction = 0.55f, heightDp = 18, alpha = 0.55f)
        SilhouetteBar(brush = brush, widthFraction = 0.70f, heightDp = 18, alpha = 0.70f)
        // Active line — larger, fuller, mimics the 34sp ExtraBold active line.
        SilhouetteBar(brush = brush, widthFraction = 0.88f, heightDp = 28, alpha = 1.0f)
        SilhouetteBar(brush = brush, widthFraction = 0.62f, heightDp = 28, alpha = 1.0f)
        // Two faint neighbours below.
        SilhouetteBar(brush = brush, widthFraction = 0.75f, heightDp = 18, alpha = 0.70f)
        SilhouetteBar(brush = brush, widthFraction = 0.48f, heightDp = 18, alpha = 0.45f)
        SilhouetteBar(brush = brush, widthFraction = 0.65f, heightDp = 18, alpha = 0.30f)
    }
}

@Composable
private fun SilhouetteBar(
    brush: Brush,
    widthFraction: Float,
    heightDp: Int,
    alpha: Float,
) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(heightDp.dp)
            .clip(RoundedCornerShape((heightDp / 2).dp))
            .background(brush)
            .background(Color.White.copy(alpha = 0.02f * alpha)),
    )
}
