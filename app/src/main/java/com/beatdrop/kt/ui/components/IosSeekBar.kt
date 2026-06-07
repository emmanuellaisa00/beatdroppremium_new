package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * IosSeekBar — Apple-Music-style playback seek bar.
 *
 * Design rules:
 *   • **No thumb** at rest. The bar is a thin (3.dp) horizontal rail.
 *   • Filled portion = accent green. Unfilled portion = white-12%.
 *   • Touch / drag: the entire rail thickens (3 → 6.dp) and a small
 *     accent dot (10.dp) materialises at the touch point. Both elements
 *     spring back when the finger lifts.
 *   • Tap anywhere on the rail → seek to that position.
 *   • Drag → live-scrub. onSeek fires only on touch-up so we don't
 *     spam the controller with mid-drag seeks.
 *   • **Loading state** ([loading] = true): the entire bar fills with
 *     a subtle accent wash, and a brighter highlight sweeps left→right
 *     on a 1.4s loop — no spinner, no progress bar, no text. Disappears
 *     instantly the moment loading flips false.
 *   • No buffered-ahead indicator. Users see only their own progress,
 *     not the fetcher's progress — per design feedback.
 */
@Composable
fun IosSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val C = LocalAppColors.current
    val density = LocalDensity.current

    // Drag state — mid-scrub position (-1L = not scrubbing).
    var dragPosMs by remember { mutableStateOf(-1L) }
    var widthPx by remember { mutableStateOf(0f) }

    // The displayed playhead = either the live position OR the
    // user's drag override (so the bar tracks the finger smoothly).
    val displayMs = if (dragPosMs >= 0L) dragPosMs else positionMs
    val safeDur = durationMs.coerceAtLeast(1L)
    val progressFraction = (displayMs.toFloat() / safeDur).coerceIn(0f, 1f)

    // Spring-animated thickness: thick on touch, thin at rest.
    val isInteracting = dragPosMs >= 0L
    val barThickness by animateDpAsState(
        targetValue = if (isInteracting) 6.dp else 3.dp,
        animationSpec = spring(),
        label = "bar-thickness",
    )
    val thumbAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(160),
        label = "thumb-alpha",
    )

    // Loading shimmer — accent highlight band sweeping left→right.
    val infinite = rememberInfiniteTransition(label = "seek-loader")
    val shimmerX by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue  = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)              // larger touch target than the visual rail
            .pointerInput(safeDur) {
                detectTapGestures(
                    onPress = { offset ->
                        // Tap-to-seek: compute fraction, fire onSeek immediately.
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        val ms = (frac * safeDur).toLong()
                        dragPosMs = ms
                        try {
                            tryAwaitRelease()
                            onSeek(ms)
                        } finally {
                            dragPosMs = -1L
                        }
                    },
                )
            }
            .pointerInput(safeDur) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        dragPosMs = (frac * safeDur).toLong()
                    },
                    onDragEnd = {
                        val final = dragPosMs
                        if (final >= 0L) onSeek(final)
                        dragPosMs = -1L
                    },
                    onDragCancel = { dragPosMs = -1L },
                    onDrag = { change, _ ->
                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragPosMs = (frac * safeDur).toLong()
                    },
                )
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barThickness)
                .align(androidx.compose.ui.Alignment.Center)
                .clip(RoundedCornerShape(barThickness / 2)),
        ) {
            widthPx = size.width
            val h = size.height
            val r = h / 2f

            if (loading) {
                // ── Loading state — accent wash + sweeping highlight band ──
                // Wash: full-width accent at 0.22 alpha.
                drawRoundRect(
                    color = C.accent.copy(alpha = 0.22f),
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(r, r),
                )
                // Sweep: a 35% width gradient band gliding across.
                val bandWidth = size.width * 0.35f
                val bandStartX = shimmerX * size.width - bandWidth / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            C.accent.copy(alpha = 0.0f),
                            C.accent.copy(alpha = 0.65f),
                            C.accent.copy(alpha = 0.0f),
                        ),
                        startX = bandStartX,
                        endX   = bandStartX + bandWidth,
                    ),
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(r, r),
                )
                return@Canvas
            }

            // ── Normal state — track + filled progress ─────────────────
            // Unfilled track (white-12%).
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                size = Size(size.width, h),
                cornerRadius = CornerRadius(r, r),
            )
            // Filled portion (accent).
            val filledW = size.width * progressFraction
            if (filledW > 0f) {
                drawRoundRect(
                    color = C.accent,
                    size = Size(filledW, h),
                    cornerRadius = CornerRadius(r, r),
                )
            }
            // Interaction thumb — small accent dot at the playhead, only
            // visible while the user is touching the bar. Drawn ABOVE the
            // bar so it stays crisp on top of the rounded fill.
            if (thumbAlpha > 0f) {
                val thumbRadius = 5.dp.toPx()
                drawCircle(
                    color = C.accent.copy(alpha = thumbAlpha),
                    radius = thumbRadius,
                    center = Offset(filledW.coerceIn(0f, size.width), h / 2f),
                )
            }
        }
    }
}
