package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * DiegeticDownloadIcon — the download button used in the Now Playing controls.
 *
 * Replaces the previous "icon swap + spinner" pattern with a single diegetic
 * progress ring drawn *around* the download glyph itself. The icon stays
 * visible the whole time; the ring fills clockwise from 0 → 100 %, then
 * morphs into a green check on completion.
 *
 *   idle        — Download glyph, no ring
 *   downloading — Download glyph + accent ring filling clockwise
 *                 (progress arc + faint full-circle track for context)
 *   done        — Check glyph in accent, no ring
 *
 * No CircularProgressIndicator, no LinearProgressIndicator, no "Loading…"
 * label. The motion of the ring is the entire affordance.
 */
@Composable
fun DiegeticDownloadIcon(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progressPercent: Int,           // 0..100 — only consulted when isDownloading
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current

    // Smoothly tween between reported progress steps so the ring glides
    // rather than jumping in 5-10 % chunks as bytes arrive.
    val target = progressPercent.coerceIn(0, 100) / 100f
    val animated by animateFloatAsState(
        targetValue = if (isDownloading) target else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "dl-progress",
    )

    // When download completes, the check pops in with a tiny spring.
    val checkScale by animateFloatAsState(
        targetValue = if (isDownloaded) 1f else 0.6f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "check-scale",
    )

    val iconTint = when {
        isDownloaded  -> C.accent
        isDownloading -> C.accent      // ring + icon both accent — unified colour story
        else          -> Color.White
    }
    val glyph = if (isDownloaded) Ic.Check else Ic.Download
    val desc  = when {
        isDownloaded  -> "Downloaded"
        isDownloading -> "Downloading"
        else          -> "Download"
    }

    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            enabled = !isDownloading && !isDownloaded,
        ) {
            Icon(
                glyph,
                contentDescription = desc,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .then(if (isDownloaded) Modifier.scale(checkScale) else Modifier)
                    .drawBehind {
                        if (!isDownloading) return@drawBehind
                        val stroke = 2.dp.toPx()
                        val inset  = stroke / 2f + 2.dp.toPx()
                        val arcSize = Size(size.width + 12.dp.toPx() - inset * 2,
                                           size.height + 12.dp.toPx() - inset * 2)
                        val topLeft = Offset(
                            -6.dp.toPx() + inset,
                            -6.dp.toPx() + inset,
                        )
                        // Faint full-circle track so the user sees the
                        // total length the ring will travel.
                        drawArc(
                            color = Color.White.copy(alpha = 0.18f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        // Progress arc — accent green, filling clockwise.
                        drawArc(
                            color = C.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * animated.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    },
            )
        }
    }
}


