package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// Haze backdrop blur — real CSS backdrop-filter, scoped to ScreenScaffold.
// ═══════════════════════════════════════════════════════════════════════════════

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun Modifier.hazeGlass(
    shape: Shape,
    tintColor: Color,
    blurRadius: Dp = 32.dp,
    noiseFactor: Float = HazeDefaults.noiseFactor,
): Modifier {
    val state = LocalHazeState.current ?: return this
    val style = HazeStyle(
        tint        = tintColor,
        blurRadius  = blurRadius,
        noiseFactor = noiseFactor,
    )
    return this.hazeChild(state = state, shape = shape, style = style)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Z-Layer ordering
// ═══════════════════════════════════════════════════════════════════════════════
object Z {
    const val background     = 0f
    const val artwork        = 10f
    const val card           = 20f
    const val tabs           = 30f
    const val navigation     = 40f
    const val miniPlayer     = 50f
    const val floatingAction = 60f
    const val modal          = 70f
    const val sheet          = 80f
    const val overlay        = 90f
}

// ═══════════════════════════════════════════════════════════════════════════════
// Noise Overlay — sparse dither, premium grain
// ═══════════════════════════════════════════════════════════════════════════════
fun Modifier.noiseOverlay(
    opacity: Float = 0.025f,
    seed: Long = 0xFA2D48L,
    densityDivisor: Int = 140,
): Modifier = this.drawWithCache {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    val n = ((w * h) / densityDivisor).toInt().coerceIn(64, 6000)
    val rng = Random(seed xor (w.toLong() * 31L + h.toLong()))
    val points = List(n) { Offset(rng.nextFloat() * w, rng.nextFloat() * h) }
    val color = Color.White.copy(alpha = (opacity * 4f).coerceAtMost(1f))
    onDrawWithContent {
        drawContent()
        drawPoints(
            points     = points,
            pointMode  = PointMode.Points,
            color      = color,
            strokeWidth = 1f,
            blendMode  = BlendMode.Overlay,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Ambient Glow — very subtle radial color wash behind content
// ═══════════════════════════════════════════════════════════════════════════════
fun Modifier.ambientGlow(
    color: Color,
    intensity: Float = 0.10f,
    centerX: Float = 0.5f,
    centerY: Float = 0.30f,
    radiusFactor: Float = 0.95f,
): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = intensity), Color.Transparent),
            center = Offset(size.width * centerX, size.height * centerY),
            radius = size.maxDimension * radiusFactor,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Glass Shadow
// ═══════════════════════════════════════════════════════════════════════════════
fun Modifier.glassShadow(
    elevation: Dp = 18.dp,
    shape: Shape,
    isDark: Boolean = true,
): Modifier = this.shadow(
    elevation     = elevation,
    shape         = shape,
    clip          = false,
    ambientColor  = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.18f),
    spotColor     = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.22f),
)

// ═══════════════════════════════════════════════════════════════════════════════
// Icon Puck — 40-44 dp circular glass button (matches the HTML .icon-btn)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun IconPuck(
    icon: ImageVector,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    tint: Color = LocalAppColors.current.text,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Box(
        modifier
            .size(size)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = tint.copy(alpha = 0.85f),
            modifier           = Modifier.size(size * 0.42f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Glass Header — floating top bar (light, monochrome, generous padding)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun GlassHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color? = null,
) {
    val C = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.sm),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .premiumGlass(level = GlassLevel.Z2_Card, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Ic.Back, "Back", tint = C.text.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(Spacing.md))
            }
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = leadingTint ?: C.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = Type.title1,
                    color = C.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = Type.caption,
                        color = C.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Screen Scaffold — Pure black with pink ambient glow (matches HTML)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    ambientColor: Color? = null,
    ambientIntensity: Float = 0.12f,
    showNoise: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val C = LocalAppColors.current
    val glow = ambientColor ?: C.accent
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier
                .fillMaxSize()
                .background(if (C.isDark) Color(0xFF000000) else C.bg0)
                .drawBehind {
                    if (C.isDark) {
                        // Soft pink ambient (top-left)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glow.copy(alpha = ambientIntensity * 0.85f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.15f, size.height * 0.10f),
                                radius = size.maxDimension * 0.55f,
                            ),
                        )
                        // Cool navy ambient (bottom-right) — adds depth
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2850A0).copy(alpha = ambientIntensity * 0.55f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.85f),
                                radius = size.maxDimension * 0.55f,
                            ),
                        )
                        // Bottom vignette
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f),
                            ),
                        )
                    } else {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(glow.copy(alpha = ambientIntensity * 0.6f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.0f),
                                radius = size.maxDimension * 0.8f,
                            ),
                        )
                    }
                }
                .then(if (showNoise) Modifier.noiseOverlay(opacity = 0.022f) else Modifier)
                .haze(state = hazeState),
            content = content,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Section Header
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    val C = LocalAppColors.current
    Text(
        label,
        style = Type.title2,
        color = C.text,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = Spacing.xxl, top = Spacing.xxxl, bottom = Spacing.md, end = Spacing.xxl),
    )
}
