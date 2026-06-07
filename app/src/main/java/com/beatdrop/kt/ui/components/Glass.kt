package com.beatdrop.kt.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.Blur
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

/**
 * App-wide device tilt state.
 */
val LocalDeviceTilt = staticCompositionLocalOf<State<Offset>?> { null }

@Composable
fun rememberDeviceTilt(): State<Offset> {
    LocalDeviceTilt.current?.let { return it }
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

@SuppressLint("NewApi")
fun Modifier.glassBlur(@Suppress("UNUSED_PARAMETER") radiusPx: Float = 36f): Modifier = this

@SuppressLint("NewApi")
@Composable
fun Modifier.masterGlass(
    radius: Dp = Radius.lg,
    @Suppress("UNUSED_PARAMETER") blur: Float = 36f,
    @Suppress("UNUSED_PARAMETER") tintAlpha: Float = 0.08f,
    @Suppress("UNUSED_PARAMETER") borderAlpha: Float = 0.12f,
): Modifier = this.premiumGlass(
    level = GlassLevel.Z2_Card,
    shape = RoundedCornerShape(radius),
)

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

@Composable
fun Modifier.rimLight(cornerRadius: Dp = Radius.lg): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (C.isDark) 0.10f else 0.30f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.35f,
            ),
        )
    }
}

@Composable
fun Modifier.innerGlow(): Modifier {
    val C = LocalAppColors.current
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = if (C.isDark) 0.25f else 0.08f),
                ),
                startY = size.height * 0.7f,
                endY = size.height,
            ),
        )
    }
}

/**
 * Tinted glass button — accent pink rendered as soft glass.
 * Used selectively for primary CTAs.
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
            .background(tintColor.copy(alpha = 0.92f))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent,
                    )
                )
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.20f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Glass card — shim to premiumGlass(Z2_Card).
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.glassCard(
    radius: Dp = Radius.lg,
    @Suppress("UNUSED_PARAMETER") blur: Float = Blur.light,
): Modifier = this.premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(radius))

/**
 * Glass row — shim to premiumGlass(Z1_List).
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.glassRow(
    radius: Dp = Radius.md,
    @Suppress("UNUSED_PARAMETER") blur: Float = Blur.subtle,
): Modifier = this.premiumGlass(level = GlassLevel.Z1_List, shape = RoundedCornerShape(radius))

/**
 * Glass sheet — modal background.
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.glassSheet(radius: Dp = Radius.xl): Modifier = this.premiumGlass(
    level = GlassLevel.Z6_Floating,
    shape = RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = 0.dp, bottomEnd = 0.dp),
)
