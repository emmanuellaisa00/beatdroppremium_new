package com.beatdrop.kt.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.Blur
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Premium Glass — the single unified material used everywhere in the app.
 *
 * BeatDrop Glassmorphism (matches the HTML concept):
 *   • Dark obsidian fill — rgba(28,30,38, 0.42) base, rgba(22,24,30, 0.62) for dock/player
 *   • Soft inner top highlight (white ~7%) — the "rim"
 *   • Inset bottom dark shadow (black ~30%) — gives the surface "thickness"
 *   • Hairline border at 6–14% white
 *   • Layered ambient + contact shadows for true floating
 *   • No bright colored fills, no harsh glows — surfaces read as material
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.premiumGlass(
    level: GlassLevel = GlassLevel.Z2_Card,
    shape: Shape = RoundedCornerShape(22.dp),
    tintBoost: Float = 0f,
): Modifier {
    val C = LocalAppColors.current

    // Per-level fill darkness. Higher = more opaque = closer to the viewer.
    val fillAlpha = when (level) {
        GlassLevel.Z1_List        -> 0.36f
        GlassLevel.Z2_Card        -> 0.42f
        GlassLevel.Z3_MiniPlayer  -> 0.62f
        GlassLevel.Z4_TabBar      -> 0.62f
        GlassLevel.Z5_ActiveLens  -> 0.18f
        GlassLevel.Z6_Floating    -> 0.70f
    } + tintBoost.coerceIn(0f, 0.15f)

    val rimAlpha = if (C.isDark) 0.07f else 0.55f
    val borderAlpha = when (level) {
        GlassLevel.Z3_MiniPlayer,
        GlassLevel.Z4_TabBar,
        GlassLevel.Z6_Floating -> if (C.isDark) 0.055f else 0.12f
        else -> if (C.isDark) 0.06f else 0.10f
    }
    val innerShadowAlpha = if (C.isDark) 0.30f else 0.10f
    val noiseOpacity = 0.012f

    // Substrate color — dark obsidian for dark theme, milky white for light
    val baseFill = if (C.isDark) Color(0xFF1C1E26) else Color.White
    val deepFill = if (C.isDark) Color(0xFF16181E) else Color(0xFFF5F5F8)

    return this
        // ── Shadow stack ──────────────────────────────────────────────
        .then(
            if (level >= GlassLevel.Z3_MiniPlayer) {
                Modifier.shadow(
                    elevation = level.volumeShadowDp,
                    shape = shape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
            } else Modifier
        )
        .shadow(
            elevation = level.ambientShadowDp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (C.isDark) 0.45f else 0.18f),
            spotColor = Color.Black.copy(alpha = if (C.isDark) 0.35f else 0.12f),
        )
        .shadow(
            elevation = level.contactShadowDp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (C.isDark) 0.55f else 0.25f),
            spotColor = Color.Black.copy(alpha = if (C.isDark) 0.45f else 0.20f),
        )
        .clip(shape)
        // ── Substrate (translucent obsidian / milk) ───────────────────
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    if (level == GlassLevel.Z5_ActiveLens)
                        Color.White.copy(alpha = 0.10f)
                    else
                        baseFill.copy(alpha = fillAlpha),
                    if (level == GlassLevel.Z5_ActiveLens)
                        Color.White.copy(alpha = 0.02f)
                    else
                        deepFill.copy(alpha = fillAlpha * 0.95f),
                ),
            ),
            shape,
        )
        // ── Inner top highlight ──────────────────────────────────────
        .drawWithContent {
            drawContent()
            // Top sheen (rim light) — soft white gradient from the top edge
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = rimAlpha),
                        Color.White.copy(alpha = rimAlpha * 0.15f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f,
                ),
            )
            // Bottom dark inset — gives the glass "thickness"
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = innerShadowAlpha),
                    ),
                    startY = size.height * 0.70f,
                    endY = size.height,
                ),
            )
        }
        // ── Active lens accent glow (for Z5 only) ────────────────────
        .then(
            if (level == GlassLevel.Z5_ActiveLens) {
                Modifier.drawWithContent {
                    drawContent()
                    // Soft pink accent ring on the active lens
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                C.accent.copy(alpha = 0.20f),
                                C.accent.copy(alpha = 0.03f),
                                Color.Transparent,
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.35f),
                            radius = size.maxDimension * 0.7f,
                        ),
                    )
                }
            } else Modifier
        )
        // ── Noise ────────────────────────────────────────────────────
        .noiseOverlay(opacity = noiseOpacity)
        // ── Hairline border ──────────────────────────────────────────
        .border(0.5.dp, Color.White.copy(alpha = borderAlpha), shape)
}

/**
 * Depth levels in the Premium Glass hierarchy.
 *   Z1 = list rows (least elevated)
 *   Z2 = album cards / standard floating
 *   Z3 = mini player
 *   Z4 = tab bar / dock
 *   Z5 = active tab "lens" (inset glass orb behind active icon)
 *   Z6 = modal sheets / dialogs
 */
enum class GlassLevel(
    val blurPx: Float,
    val ambientShadowDpValue: Float,
    val volumeShadowDpValue: Float,
    val contactShadowDpValue: Float,
) {
    Z1_List       (Blur.z1,  2f,  0f, 1f),
    Z2_Card       (Blur.z2, 10f,  0f, 2f),
    Z3_MiniPlayer (Blur.z3, 22f, 30f, 4f),
    Z4_TabBar     (Blur.z4, 22f, 30f, 4f),
    Z5_ActiveLens (Blur.z5,  2f,  0f, 1f),
    Z6_Floating   (Blur.z6, 24f, 36f, 5f);

    val ambientShadowDp: Dp get() = ambientShadowDpValue.dp
    val volumeShadowDp:  Dp get() = volumeShadowDpValue.dp
    val contactShadowDp: Dp get() = contactShadowDpValue.dp
}
