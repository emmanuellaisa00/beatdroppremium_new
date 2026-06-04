package com.beatdrop.kt.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Mini Player
// Spec: blur(50px) — higher than nav for elevation, outer radius=44dp
// accent=#21FF6B (Spotify Green)
// ═══════════════════════════════════════════════════════════════════════════════

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
    val tilt = rememberDeviceTilt()

    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val animX by animateFloatAsState(dragX, label = "miniX")
    val animY by animateFloatAsState(dragY.coerceAtMost(0f), label = "miniY")

    // ── Concentric radius system: outer=44dp, inner=36dp (44-8) ──────────────
    val outerRadius = 44.dp
    val innerRadius = Radius.inner(outerRadius, 8.dp)  // 44 - 8 = 36dp
    val outerShape  = RoundedCornerShape(outerRadius)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .graphicsLayer {
                translationX = animX
                translationY = animY
                shadowElevation = if (C.isDark) 18f else 10f
                shape = outerShape
                clip = false
            }
            .clip(outerShape)
            // ── Glass fill (rgba(18,18,22,.45) — card tint from spec) ────────
            .background(
                if (C.isDark) Color(0xCC0A0A10)
                else Color(0xD8F2F2F7)
            )
            // ── Backdrop blur (50px — player level, higher than nav) ─────────
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect.createChainEffect(
                            RenderEffect.createColorFilterEffect(
                                android.graphics.ColorMatrixColorFilter(
                                    android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
                                )
                            ),
                            RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP),
                        ).asComposeRenderEffect()
                    }
                } else Modifier
            )
            // ── Top reflection gradient ──────────────────────────────────────
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (C.isDark) 0.08f else 0.14f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.35f,
                    ),
                )
                // Bottom inner glow for depth
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (C.isDark) Color.White.copy(alpha = 0.04f)
                                     else Color.White.copy(alpha = 0.05f),
                        ),
                        startY = size.height * 0.70f,
                        endY = size.height,
                    ),
                )
            }
            // ── Specular highlight (device tilt) ──────────────────────────────
            .specularHighlight(tilt, intensity = if (C.isDark) 0.10f else 0.08f, radius = 220f)
            // ── Rim light (Fresnel top-edge) ──────────────────────────────────
            .rimLight(outerRadius)
            // ── Hairline border ──────────────────────────────────────────────
            .border(
                width  = if (C.isDark) 1.dp else 0.7.dp,
                color  = if (C.isDark) Color(0x33FFFFFF) else Color(0x1A000000),
                shape  = outerShape,
            )
            // ── Gestures: tap → expand, swipe left/right → next/prev ─────────
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() })
            },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // ── Artwork — concentric inner radius (36dp) ─────────────────────
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(C.bg3)
            ) {
                AsyncImage(
                    model  = ImageRequest.Builder(ctx)
                        .data(track.artworkUri)
                        .crossfade(true)
                        .size(coil.size.Size(96, 96))
                        .build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.width(12.dp))

            // ── Metadata: track title + artist ───────────────────────────────
            Column(Modifier.weight(1f)) {
                Text(
                    text      = track.title,
                    color     = C.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    fontSize  = 14.sp,
                )
                Text(
                    text      = track.artist,
                    color     = C.textSecondary,
                    fontSize  = 12.sp,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }

            // ── Tinted glass play button (Spotify Green accent) ─────────────
            IconButton(onClick = onToggle) {
                TintedGlassButton(
                    modifier     = Modifier.size(40.dp),
                    tintColor    = C.accent,
                    cornerRadius = 20.dp,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint         = Color.White,
                        modifier     = Modifier.size(20.dp),
                    )
                }
            }

            // ── Skip next ───────────────────────────────────────────────────
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = null,
                    tint         = C.text,
                    modifier     = Modifier.size(22.dp),
                )
            }
        }

        // ── Progress bar — accent green, refined 2.5dp ───────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .align(Alignment.BottomStart)
                .background(
                    if (C.isDark) Color(0x1AFFFFFF)
                    else Color(0x14000000)
                )
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(C.accent)
            )
        }
    }
}