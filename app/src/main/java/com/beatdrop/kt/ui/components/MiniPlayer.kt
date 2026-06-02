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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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

/**
 * Liquid Glass mini-player with gestures (iOS 26 style):
 *  - Backdrop blur + saturation (real glass material)
 *  - Specular rim light (top-edge glow)
 *  - Context-aware drop shadow
 *  - Tinted glass play button (not flat solid)
 *  - Concentric corner radii (outer = 24dp, artwork = 16dp = 24-8 padding)
 *  - tap → expand to Now Playing
 *  - swipe left → next, swipe right → previous
 *  - swipe up → expand
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
    val tilt = rememberDeviceTilt()

    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val animX by animateFloatAsState(dragX, label = "miniX")
    val animY by animateFloatAsState(dragY.coerceAtMost(0f), label = "miniY")

    val outerRadius = 24.dp
    val innerRadius = Radius.inner(outerRadius, 8.dp)  // Concentric: 24 - 8 = 16dp
    val outerShape = RoundedCornerShape(outerRadius)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .graphicsLayer {
                translationX = animX
                translationY = animY
                // Context-aware drop shadow
                shadowElevation = if (C.isDark) 12f else 6f
                shape = outerShape
                clip = false
            }
            .clip(outerShape)
            // Backdrop blur + saturation boost (API 31+)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect.createChainEffect(
                            RenderEffect.createColorFilterEffect(
                                android.graphics.ColorMatrixColorFilter(
                                    android.graphics.ColorMatrix().apply { setSaturation(1.7f) }
                                )
                            ),
                            RenderEffect.createBlurEffect(45f, 45f, Shader.TileMode.CLAMP),
                        ).asComposeRenderEffect()
                        clip = true
                    }
                else Modifier
                    // Pre-API-31 fallback: heavier opaque fill
                    .background(if (C.isDark) Color(0xF0101018) else Color(0xF0F2F2F7))
            )
            // Glass fill
            .background(if (C.isDark) Color(0xC00E0C18) else Color(0xC8F4F4FA))
            // Rim light (Fresnel top-edge for thickness)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            C.glassRimLight.copy(alpha = if (C.isDark) 0.10f else 0.18f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.4f,
                    ),
                )
            }
            // Specular highlight (device tilt)
            .specularHighlight(tilt, intensity = if (C.isDark) 0.08f else 0.05f, radius = 180f)
            // Hairline border
            .border(
                if (C.isDark) 0.8.dp else 0.5.dp,
                C.liquidGlassBorder,
                outerShape,
            )
            .pointerInput(track.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            dragX < -120f -> onNext()
                            dragX > 120f -> onPrev()
                            dragY < -100f -> onExpand()
                        }
                        dragX = 0f; dragY = 0f
                    },
                    onDragCancel = { dragX = 0f; dragY = 0f },
                ) { change, amount ->
                    change.consume()
                    if (abs(amount.x) > abs(amount.y)) dragX += amount.x else dragY += amount.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() })
            },
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Artwork — concentric inner radius
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(C.bg3)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).size(coil.size.Size(96, 96)).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = C.text, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(track.artist, color = C.textSecondary, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            // Tinted glass play button (not flat solid)
            IconButton(onClick = onToggle) {
                TintedGlassButton(
                    modifier = Modifier.size(38.dp),
                    cornerRadius = 19.dp,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(20.dp),
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, null, tint = C.text)
            }
        }
        // Progress bar
        Box(
            Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomStart)
                .background(if (C.isDark) Color(0x1AFFFFFF) else Color(0x14000000))
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                    .background(C.accent)
            )
        }
    }
}
