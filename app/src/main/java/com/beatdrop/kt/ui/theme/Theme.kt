package com.beatdrop.kt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BeatDropColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTile,
    onSurfaceVariant = TextMedium,
    outline = GlassBorder,
    inverseSurface = DockBg,
)

@Composable
fun BeatDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeatDropColorScheme,
        typography = BeatDropTypography,
        shapes = BeatDropShapes,
        content = content
    )
}
