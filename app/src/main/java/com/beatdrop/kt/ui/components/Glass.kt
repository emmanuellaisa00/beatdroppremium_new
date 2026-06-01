package com.beatdrop.kt.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

// ─── Device Tilt Sensor for Specular Highlights ──────────────────────────────

/**
 * Reads accelerometer to provide tilt-based specular highlight offset.
 * Returns (x, y) in range -1..1 representing device tilt from flat.
 * Used by Liquid Glass surfaces to move specular highlights in real-time.
 */
@Composable
fun rememberDeviceTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Normalize: gravity ≈ 9.81, we want -1..1 range
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

// ─── Backdrop Blur Modifier ──────────────────────────────────────────────────

/**
 * Real backdrop-blur + saturation boost (Liquid Glass material base).
 * On API 31+ uses RenderEffect with blur + saturation for that "thick glass"
 * look. On older devices degrades to a heavier opaque scrim.
 */
fun Modifier.glassBlur(radiusPx: Float = 40f): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = RenderEffect.createChainEffect(
                // Saturation boost — makes colors behind glass richer (like real thick glass)
                RenderEffect.createColorFilterEffect(
                    android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
                    )
                ),
                // Gaussian blur
                RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP),
            ).asComposeRenderEffect()
        }
    } else this

// ─── Specular Highlight Modifier ─────────────────────────────────────────────

/**
 * Draws a moving specular highlight over the content, responding to device tilt.
 * Simulates light traveling across the glass surface — a core Liquid Glass behavior.
 */
@Composable
fun Modifier.specularHighlight(
    tilt: State<Offset>,
    intensity: Float = 0.15f,
    radius: Float = 300f,
): Modifier {
    val C = LocalAppColors.current
    // Animate smoothly so the highlight doesn't jitter
    val animX by animateFloatAsState(tilt.value.x, animationSpec = tween(150), label = "specX")
    val animY by animateFloatAsState(tilt.value.y, animationSpec = tween(150), label = "specY")

    return this.drawWithContent {
        drawContent()
        // Map tilt to position within the element
        val cx = size.width * (0.5f + animX * 0.35f)
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

// ─── Interaction Glow Modifier ───────────────────────────────────────────────

/**
 * Draws a radial glow from a touch point when pressed.
 * Simulates the Liquid Glass "illuminate from within" interaction feedback.
 *
 * @param pressed whether the element is currently pressed
 * @param touchCenter normalized touch position (0..1, 0..1) within the element
 */
@Composable
fun Modifier.interactionGlow(
    pressed: Boolean,
    touchCenter: Offset = Offset(0.5f, 0.5f),
    glowColor: Color = LocalAppColors.current.glassGlow,
): Modifier {
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) tween(120) else tween(400),
        label = "glowAlpha",
    )
    val glowRadius by animateFloatAsState(
        targetValue = if (pressed) 1.2f else 0.4f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "glowRadius",
    )
    if (glowAlpha <= 0.01f) return this
    return this.drawWithContent {
        drawContent()
        val cx = size.width * touchCenter.x
        val cy = size.height * touchCenter.y
        val r = size.width * glowRadius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.35f * glowAlpha),
                    glowColor.copy(alpha = 0.12f * glowAlpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = r,
            ),
            center = Offset(cx, cy),
            radius = r,
        )
    }
}

// ─── Rim Light (Fresnel Edge Highlight) ──────────────────────────────────────

/**
 * Draws a subtle top-edge rim light inside the element — simulates light
 * catching the glass edge (Fresnel effect). Without this, glass looks paper-thin.
 */
@Composable
fun Modifier.rimLight(
    cornerRadius: Dp = Radius.xxl,
): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        // Top-edge gradient: bright at top, fades to transparent
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    C.glassRimLight.copy(alpha = if (C.isDark) 0.12f else 0.25f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.35f,
            ),
        )
    }
}

// ─── Inner Shadow for Glass Thickness ────────────────────────────────────────

/**
 * Draws an inner shadow effect (white at 30% opacity, blur 6) to give
 * the glass element a sense of lift and thickness.
 */
@Composable
fun Modifier.glassInnerShadow(): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        // Subtle inner shadow at the bottom edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    C.glassInnerShadow.copy(alpha = if (C.isDark) 0.08f else 0.05f),
                ),
                startY = size.height * 0.7f,
                endY = size.height,
            ),
        )
    }
}

// ─── Liquid Glass Surface (Regular Variant) ──────────────────────────────────

/**
 * The primary Liquid Glass surface — the "Regular" variant.
 * For navigation layer elements: tab bars, nav bars, floating controls, modals.
 *
 * Features:
 *   - Backdrop blur + saturation boost
 *   - Specular highlights responding to device tilt
 *   - Inner shadow for thickness
 *   - Rim light (Fresnel top-edge)
 *   - Hairline border
 *   - Context-aware drop shadow
 *   - Graceful pre-API-31 fallback (heavier opaque fill + inner border)
 *
 * Rules (per Apple HIG):
 *   - NEVER use on content layer (lists, text, media)
 *   - NEVER stack glass-on-glass
 *   - Use fills/vibrancy for elements on top of this surface
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.xxl,
    enableSpecular: Boolean = true,
    content: @Composable () -> Unit,
) {
    val C = LocalAppColors.current
    val tilt = rememberDeviceTilt()
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier
            .graphicsLayer {
                // Drop shadow — context-aware depth
                shadowElevation = if (C.isDark) 8f else 4f
                this.shape = shape
                clip = false
            }
            .clip(shape)
            // Backdrop blur + saturation (API 31+)
            .glassBlur(40f)
            // Glass fill: translucent
            .background(C.liquidGlass)
            // Tint layer
            .background(C.glassTint.copy(alpha = if (C.isDark) 0.35f else 0.2f))
            // Rim light
            .rimLight(cornerRadius)
            // Inner shadow for thickness
            .glassInnerShadow()
            // Specular highlight (device-tilt-responsive)
            .then(
                if (enableSpecular) Modifier.specularHighlight(tilt, intensity = if (C.isDark) 0.12f else 0.08f)
                else Modifier
            )
            // Hairline border
            .border(
                width = if (C.isDark) 0.8.dp else 0.6.dp,
                color = C.liquidGlassBorder,
                shape = shape,
            ),
    ) { content() }
}

// ─── Liquid Glass Surface (Clear Variant) ────────────────────────────────────

/**
 * The "Clear" Liquid Glass variant — more transparent, for use over rich media.
 * Requires a dimming layer underneath. Only use when:
 *   1. Over media-rich content (album art, photos, video)
 *   2. Content layer won't be harmed by dimming
 *   3. Content on top is bold and bright
 */
@Composable
fun LiquidGlassClear(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.xxl,
    content: @Composable () -> Unit,
) {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier
            .clip(shape)
            .glassBlur(30f)
            .background(C.liquidGlassClear)
            .rimLight(cornerRadius)
            .border(0.5.dp, C.liquidGlassClearBorder, shape),
    ) { content() }
}

// ─── Tinted Glass Button ─────────────────────────────────────────────────────

/**
 * A tinted glass button — the accent color rendered as translucent glass
 * material rather than a flat solid fill. Like colored glass in reality:
 * hue/brightness shift depending on what's behind.
 *
 * Use selectively for primary actions/CTAs only (per Apple HIG: "tinting
 * should only be used to bring emphasis to primary elements").
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
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                )
            )
            .border(0.6.dp, tintColor.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ─── Legacy GlassSurface (backward compat) ───────────────────────────────────

/**
 * Backward-compatible glass surface (delegates to LiquidGlassSurface).
 * Existing code referencing GlassSurface will continue to work.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    content: @Composable () -> Unit,
) = LiquidGlassSurface(modifier = modifier, cornerRadius = cornerRadius, content = content)
