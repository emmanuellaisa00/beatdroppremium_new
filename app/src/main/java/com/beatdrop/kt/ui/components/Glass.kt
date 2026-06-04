package com.beatdrop.kt.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.Blur
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

// ═══════════════════════════════════════════════════════════════════════════════
// Device Tilt Sensor — Specular Highlights
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Reads accelerometer to provide tilt-based specular highlight offset.
 * Returns (x, y) in range -1..1 representing device tilt from flat.
 */
@Composable
fun rememberDeviceTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer  = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) return@DisposableEffect onDispose { }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                tilt.value = Offset(x, y)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return tilt
}

// ═══════════════════════════════════════════════════════════════════════════════
// Backdrop Blur + Saturation Boost
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Self-blur (API 31+). **WARNING**: this is NOT backdrop blur — it blurs
 * the element AND its children. Use [com.beatdrop.kt.ui.components.hazeGlass]
 * for real backdrop-blur. Kept for compatibility with surfaces that have
 * no child content (e.g. ambient overlays), but glass cards / rows /
 * headers / sheets MUST NOT use this — text and icons get smeared.
 */
@SuppressLint("NewApi")
fun Modifier.glassBlur(radiusPx: Float = 36f): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = RenderEffect.createChainEffect(
                RenderEffect.createColorFilterEffect(
                    android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
                    )
                ),
                RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP),
            ).asComposeRenderEffect()
        }
    } else this

// ═══════════════════════════════════════════════════════════════════════════════
// Master Glass Surface Modifier
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Master glass surface — applies the full rendering stack:
 *
 *   Shadow Layer
 *   ↓ Ambient Glow Layer
 *   ↓ Glass Surface
 *   ↓ Backdrop Blur (API 31+)
 *   ↓ Noise Texture
 *   ↓ Reflection Layer
 *   ↓ Content Layer
 *
 * Usage:
 *   .masterGlass(radius = Radius.lg, blur = Blur.medium, tintAlpha = 0.12f)
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.masterGlass(
    radius: Dp = Radius.lg,
    blur: Float = 36f,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.12f,
): Modifier {
    val C = LocalAppColors.current
    return this
        // ── Glass Surface (gradient fill) ──────────────────────────────────
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = tintAlpha + 0.04f),
                    Color.White.copy(alpha = tintAlpha * 0.25f),
                ),
                startY = 0f,
                endY = Float.POSITIVE_INFINITY,
            )
        )
        // NOTE: Backdrop blur is intentionally NOT applied here. Compose's
        // RenderEffect blurs the element AND its children, which smears
        // text/icons. Real backdrop blur lives in `Modifier.hazeGlass(...)`;
        // call it on the parent surface BEFORE content is composed in.
        // ── Noise (organic / premium) ──────────────────────────────────────
        .noiseOverlay(opacity = 0.03f)
        // ── Top Reflection (Fresnel rim light) ─────────────────────────────
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        C.glassHighlight.copy(alpha = if (C.isDark) 0.12f else 0.22f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.35f,
                ),
            )
        }
        // ── Bottom Inner Shadow (depth/thickness) ──────────────────────────
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        C.glassInnerShadow.copy(alpha = 0.06f),
                    ),
                    startY = size.height * 0.65f,
                    endY = size.height,
                ),
            )
        }
        // ── Specular Highlight (device tilt) ───────────────────────────────
        .specularHighlight(
            rememberDeviceTilt(),
            intensity = 0.12f,
            radius = 280f,
        )
        // ── Hairline Border ────────────────────────────────────────────────
        .border(
            1.dp,
            Color.White.copy(alpha = borderAlpha),
            RoundedCornerShape(radius),
        )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Specular Highlight — Device Tilt Responsive
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draws a moving specular highlight responding to device tilt.
 * Simulates light traveling across the glass surface.
 */
@Composable
fun Modifier.specularHighlight(
    tilt: State<Offset>,
    intensity: Float = 0.15f,
    radius: Float = 300f,
): Modifier {
    val animX by animateFloatAsState(tilt.value.x, animationSpec = tween(150), label = "specX")
    val animY by animateFloatAsState(tilt.value.y, animationSpec = tween(150), label = "specY")

    return this.drawWithContent {
        drawContent()
        val cx = size.width  * (0.5f + animX * 0.35f)
        val cy = size.height * (0.5f - animY * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity),
                    Color.White.copy(alpha = intensity * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius,
            ),
            center = Offset(cx, cy),
            radius = radius,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Rim Light — Fresnel Top-Edge Highlight
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draws a subtle top-edge rim light — simulates light catching the glass edge.
 * Without this, glass looks paper-thin.
 */
@Composable
fun Modifier.rimLight(cornerRadius: Dp = Radius.lg): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    C.glassRimLight.copy(alpha = if (C.isDark) 0.16f else 0.30f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.35f,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Inner Glow — Bottom-Edge Soft Glow
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draws a subtle bottom-edge inner glow for depth perception.
 * Gives glass a sense of thickness and dimensionality.
 */
@Composable
fun Modifier.innerGlow(): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    C.glassInnerShadow.copy(alpha = if (C.isDark) 0.06f else 0.04f),
                ),
                startY = size.height * 0.7f,
                endY = size.height,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tinted Glass Button — Accent Color Glass Material
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * A tinted glass button — accent color rendered as translucent glass material.
 * Use selectively for primary actions/CTAs only.
 * Accent: #21FF6B (Spotify Green)
 */
@Composable
fun TintedGlassButton(
    modifier: Modifier = Modifier,
    tintColor: Color = LocalAppColors.current.accent,
    cornerRadius: Dp = Radius.xl,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier
            .clip(shape)
            .background(tintColor.copy(alpha = 0.55f))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent,
                    )
                )
            )
            .border(0.6.dp, tintColor.copy(alpha = 0.40f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Glass Card — For album cards, grid items, carousel items
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Glass card for album grids, recommendation rows, etc.
 * Blur: 24-32px (blur budget for 60fps)
 *
 * Renders the full stack per spec §2:
 *   Shadow → Surface → Blur → Noise → Reflection → Inner shadow → Border.
 */
/**
 * Glass card for hero blocks, album/artist art frames, mix tiles, dialog
 * surfaces. Default blur 16 dp — strong enough to read as "frosted" but
 * gentle enough that 16–22 sp titles laid on top stay crisp. The tint
 * carries the body of the surface.
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.glassCard(
    radius: Dp = Radius.md,
    blur: Float = Blur.light,
): Modifier {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(radius)
    return this
        .glassShadow(elevation = 15.dp, shape = shape, isDark = C.isDark)
        .clip(shape)
        // Real backdrop blur (no-op outside ScreenScaffold).
        .hazeGlass(shape = shape, tintColor = C.glassCardElevated, blurRadius = blur.dp)
        // Background fallback for screens without a HazeState (hazeGlass
        // is a no-op there) — the tinted glass surface still shows.
        .background(C.glassCardElevated)
        .noiseOverlay(opacity = 0.025f)
        .drawWithContent {
            drawContent()
            // Top reflection — stronger in light mode per spec
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (C.isDark) 0.06f else 0.12f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.30f,
                ),
            )
            // Bottom inner shadow — thickness/depth
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        C.glassInnerShadow.copy(alpha = if (C.isDark) 0.06f else 0.04f),
                    ),
                    startY = size.height * 0.70f,
                    endY = size.height,
                ),
            )
        }
        .border(0.5.dp, C.glassCardElevatedBorder, shape)
}

/**
 * Glass row — list-row variant of glassCard with smaller radius/blur,
 * tuned for 60fps in long LazyColumns.
 */
/**
 * Glass row — list-row variant of glassCard tuned for dense small-text
 * surfaces (Search results, Trending, Discover rows, Library tracks).
 *
 * Default blur is intentionally low (Blur.subtle = 8 dp). Anything larger
 * smears 13–14 sp body text into a fog. The substantial tint (alpha 0.95)
 * does most of the visual work; the blur is a hint, not a wash.
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.glassRow(
    radius: Dp = Radius.sm,
    blur: Float = Blur.subtle,
): Modifier {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(radius)
    val tint = C.glassCardElevated.copy(alpha = 0.95f)
    return this
        .clip(shape)
        .hazeGlass(shape = shape, tintColor = tint, blurRadius = blur.dp)
        .background(tint)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (C.isDark) 0.05f else 0.10f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.40f,
                ),
            )
        }
        .border(0.5.dp, C.glassCardElevatedBorder, shape)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Glass Sheet — For bottom sheets, modals
// Blur: 60px (spec rule)
// ═══════════════════════════════════════════════════════════════════════════════

@SuppressLint("NewApi")
@Composable
fun Modifier.glassSheet(radius: Dp = Radius.lg): Modifier {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .hazeGlass(shape = shape, tintColor = C.glassModal, blurRadius = 60.dp)
        .background(C.glassModal)
        .noiseOverlay(opacity = 0.03f)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        C.glassHighlight.copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.30f,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        C.glassInnerShadow.copy(alpha = 0.04f),
                    ),
                    startY = size.height * 0.70f,
                    endY = size.height,
                ),
            )
        }
        .specularHighlight(
            rememberDeviceTilt(),
            intensity = 0.08f,
            radius = 400f,
        )
        .border(1.dp, C.glassModalBorder, RoundedCornerShape(radius))
}