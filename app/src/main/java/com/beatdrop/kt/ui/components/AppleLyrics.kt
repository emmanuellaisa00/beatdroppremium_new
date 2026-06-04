package com.beatdrop.kt.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.lyrics.LyricLine
import kotlin.math.abs

/**
 * Apple Music-style synced lyrics (matches Image 1):
 *
 *  ACTIVE line  — 34 sp ExtraBold, full white, zero blur, max alpha
 *  ± 1 line     — 24 sp SemiBold, 65% alpha, slight scale-down
 *  ± 2 lines    — 20 sp Medium, 38% alpha, blur 3px
 *  ± 3 lines    — 18 sp, 24% alpha, blur 6px
 *  further      — 17 sp, 14% alpha, blur 10px
 *
 *  - Top & bottom gradient fade so text dissolves into the background.
 *  - Active line auto-scrolls to the **actual vertical centre** of the
 *    viewport (computed from BoxWithConstraints — no magic number).
 *  - Tap-to-expand: pass `onTapAny` (e.g. preview mode) to handle every
 *    tap as "open full lyrics". When omitted (full mode), taps fall back
 *    to `onSeek(line.timeMs)` to seek to that line.
 *  - Pulsing dot placeholder for instrumental gaps.
 */
@Composable
fun AppleLyrics(
    lines: List<LyricLine>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {},
    onTapAny: (() -> Unit)? = null,
) {
    val state = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Center offset = viewport height / 2, in pixels. animateScrollToItem
        // places the *top* of the item at (current top + offset); a NEGATIVE
        // offset moves the item DOWN from the top. To centre the item, we want
        // its top placed at -(viewportHeight/2 - approxLineCentre) from the
        // anchor — i.e. push it down by half the viewport, minus a rough line
        // half-height so the LINE'S centre (not its top) sits at viewport mid.
        val viewportPx = with(density) { maxHeight.toPx() }
        val approxActiveLineHalfPx = with(density) { 32.dp.toPx() } // 34sp ExtraBold ≈ 64dp tall
        val centerOffset = -((viewportPx / 2f) - approxActiveLineHalfPx).toInt()

        LaunchedEffect(activeIndex, viewportPx) {
            if (activeIndex >= 0) {
                state.animateScrollToItem(
                    index = activeIndex.coerceAtLeast(0),
                    scrollOffset = centerOffset,
                )
            }
        }

        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Top + bottom fade so lyrics dissolve into the blurred backdrop
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.10f to Color.Black,
                            0.88f to Color.Black,
                            1.00f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(top = 60.dp, bottom = 240.dp),
            horizontalAlignment = Alignment.Start,
        ) {
        itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
            val distance = abs(i - activeIndex)
            val isActive = distance == 0

            // ── Animated scale ────────────────────────────────────────────────
            val scale by animateFloatAsState(
                targetValue = when (distance) {
                    0    -> 1.00f
                    1    -> 0.90f
                    else -> 0.85f
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessLow,
                ),
                label = "lyricScale$i",
            )

            // ── Animated colour (alpha encodes distance) ──────────────────────
            val color by animateColorAsState(
                targetValue = when (distance) {
                    0    -> Color.White
                    1    -> Color.White.copy(alpha = 0.65f)
                    2    -> Color.White.copy(alpha = 0.38f)
                    3    -> Color.White.copy(alpha = 0.24f)
                    else -> Color.White.copy(alpha = 0.14f)
                },
                animationSpec = tween(300),
                label = "lyricColor$i",
            )

            // ── Font size based on distance ───────────────────────────────────
            val fontSize = when (distance) {
                0    -> 34.sp   // Active: large and bold — matches Apple Music / Image 1
                1    -> 24.sp
                2    -> 20.sp
                3    -> 18.sp
                else -> 17.sp
            }
            val fontWeight = when (distance) {
                0    -> FontWeight.ExtraBold
                1    -> FontWeight.SemiBold
                else -> FontWeight.Medium
            }
            val lineH = when (distance) {
                0 -> 42.sp
                1 -> 30.sp
                else -> 24.sp
            }

            // ── Blur: real RenderEffect on API 31+, pure alpha fallback below ──
            val blurPx = when (distance) {
                0, 1 -> 0f
                2    -> 3f
                3    -> 6f
                else -> 10f
            }
            val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0f) {
                Modifier.graphicsLayer {
                    renderEffect = RenderEffect
                        .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            } else Modifier

            if (line.text.isBlank()) {
                GapDots(isActive)
            } else {
                Text(
                    text       = line.text,
                    color      = color,
                    fontSize   = fontSize,
                    fontWeight = fontWeight,
                    textAlign  = TextAlign.Start,
                    lineHeight = lineH,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = if (isActive) 10.dp else 6.dp)
                        .graphicsLayer {
                            scaleX          = scale
                            scaleY          = scale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .then(blurMod)
                        .pressableScale(
                            // In preview mode (onTapAny != null), every line
                            // tap expands to full lyrics — Apple Music behaviour.
                            // In full mode (onTapAny == null), per-line taps
                            // seek to that line's timestamp.
                            onClick  = { onTapAny?.invoke() ?: onSeek(line.timeMs) },
                            scaleTo  = 0.97f,
                            haptic   = false,
                        ),
                )
            }
        }
    }   // LazyColumn
    }   // BoxWithConstraints
}

// ── Instrumental gap dots ─────────────────────────────────────────────────────
@Composable
private fun GapDots(active: Boolean) {
    val infinite = rememberInfiniteTransition(label = "dots")
    val bounce by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = EaseInOutSine),
            RepeatMode.Reverse,
        ),
        label = "dotBounce",
    )
    val baseAlpha = if (active) 0.90f else 0.28f
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(0, 1, 2).forEachIndexed { idx, _ ->
            val offset = idx * 0.33f
            val a = (baseAlpha * (0.6f + 0.4f * ((bounce + offset) % 1f))).coerceIn(0f, 1f)
            Text(
                "●",
                color    = Color.White.copy(alpha = a),
                fontSize = if (active) 16.sp else 11.sp,
            )
        }
    }
}
