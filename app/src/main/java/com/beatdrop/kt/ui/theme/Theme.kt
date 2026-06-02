package com.beatdrop.kt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-26 Liquid Glass design tokens.
 *
 * Liquid Glass is NOT just blur — it's a dynamic material with translucency,
 * refraction, specular highlights, content-aware adaptation, and fluid morphing.
 *
 * Key rules (from Apple HIG / WWDC 2025 "Meet Liquid Glass"):
 *   - Glass belongs on the navigation layer only (tab bars, nav bars, floating controls)
 *   - Never glass-on-glass — use fills/vibrancy for elements on top of glass
 *   - Two variants: Regular (adaptive) and Clear (permanently transparent, media-rich)
 *   - Content-aware: glass flips light/dark based on what's behind it
 *   - Concentric radii: inner element radius = outer radius - padding
 */
data class AppColors(
    val bg0: Color, val bg1: Color, val bg2: Color, val bg3: Color, val bg4: Color, val bg5: Color,
    val accent: Color, val accentDark: Color, val purple: Color,
    val accentSoft: Color, val accentBorder: Color,
    val text: Color, val textSecondary: Color, val textTertiary: Color,
    val border: Color, val separator: Color,
    // ── Liquid Glass Material Tokens ──────────────────────────────────────────
    val liquidGlass: Color,             // Regular glass fill
    val liquidGlassBorder: Color,       // Hairline border on glass
    val liquidGlassHighlight: Color,    // Specular top-edge highlight
    val liquidGlassClear: Color,        // Clear variant (more transparent)
    val liquidGlassClearBorder: Color,  // Clear variant border
    val glassTint: Color,               // Tinted glass background
    val glassShadow: Color,             // Context-aware shadow
    val glassShadowStrong: Color,       // Shadow over text/busy content
    val glassGlow: Color,               // Interaction glow color
    val glassInnerShadow: Color,        // Inner shadow for depth/thickness
    val glassRimLight: Color,           // Top-edge rim light (Fresnel)
    // ── Standard Colors ──────────────────────────────────────────────────────
    val green: Color, val blue: Color, val orange: Color, val red: Color,
    val isDark: Boolean,
)

val DarkColors = AppColors(
    bg0 = Color(0xFF0A0910), bg1 = Color(0xFF141020), bg2 = Color(0xFF1A1630),
    bg3 = Color(0xFF1A1A1A), bg4 = Color(0xFF242424), bg5 = Color(0xFF2E2E2E),
    accent = Color(0xFFC77DFF), accentDark = Color(0xFF9D4EDD), purple = Color(0xFF7B2CBF),
    accentSoft = Color(0x24C77DFF), accentBorder = Color(0x80C77DFF),
    text = Color(0xFFFFFFFF), textSecondary = Color(0x99FFFFFF), textTertiary = Color(0x66FFFFFF),
    border = Color(0x14FFFFFF), separator = Color(0x12FFFFFF),
    // Liquid Glass — dark mode
    liquidGlass = Color(0x18FFFFFF),
    liquidGlassBorder = Color(0x30FFFFFF),
    liquidGlassHighlight = Color(0x40FFFFFF),
    liquidGlassClear = Color(0x0DFFFFFF),
    liquidGlassClearBorder = Color(0x1AFFFFFF),
    glassTint = Color(0x8C141020),
    glassShadow = Color(0x40000000),
    glassShadowStrong = Color(0x80000000),
    glassGlow = Color(0x33C77DFF),
    glassInnerShadow = Color(0x1EFFFFFF),
    glassRimLight = Color(0x4DFFFFFF),
    green = Color(0xFF30D158), blue = Color(0xFF0A84FF), orange = Color(0xFFFF9F0A), red = Color(0xFFFF453A),
    isDark = true,
)

val LightColors = AppColors(
    bg0 = Color(0xFFF2F2F7), bg1 = Color(0xFFFFFFFF), bg2 = Color(0xFFF2F2F7),
    bg3 = Color(0xFFE5E5EA), bg4 = Color(0xFFD1D1D6), bg5 = Color(0xFFC7C7CC),
    accent = Color(0xFF9D4EDD), accentDark = Color(0xFF7B2CBF), purple = Color(0xFF6E40C9),
    accentSoft = Color(0x1F9D4EDD), accentBorder = Color(0x619D4EDD),
    text = Color(0xFF000000), textSecondary = Color(0x8C000000), textTertiary = Color(0x59000000),
    border = Color(0x14000000), separator = Color(0x12000000),
    // Liquid Glass — light mode
    liquidGlass = Color(0xB8FFFFFF),
    liquidGlassBorder = Color(0x1F000000),
    liquidGlassHighlight = Color(0xF0FFFFFF),
    liquidGlassClear = Color(0x80FFFFFF),
    liquidGlassClearBorder = Color(0x12000000),
    glassTint = Color(0x8CFFFFFF),
    glassShadow = Color(0x1A000000),
    glassShadowStrong = Color(0x40000000),
    glassGlow = Color(0x269D4EDD),
    glassInnerShadow = Color(0x0D000000),
    glassRimLight = Color(0xE6FFFFFF),
    green = Color(0xFF28A745), blue = Color(0xFF007AFF), orange = Color(0xFFFF9500), red = Color(0xFFFF3B30),
    isDark = false,
)

/** Concentric radius system: inner = outer - padding */
object Radius {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 22.dp; val xxl = 28.dp
    /** Calculate concentric inner radius */
    /** Concentric inner radius = outer - padding (per Liquid Glass HIG). */
    fun inner(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)
}
object Spacing { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 20.dp; val xxl = 28.dp }

val LocalAppColors = staticCompositionLocalOf { DarkColors }

@Composable
fun BeatDropTheme(themePref: String = "light", content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val appColors = if (dark) DarkColors else LightColors
    val scheme = if (dark)
        darkColorScheme(
            primary = appColors.accent, background = appColors.bg0, surface = appColors.bg1,
            onPrimary = Color.White, onBackground = appColors.text, onSurface = appColors.text,
        )
    else
        lightColorScheme(
            primary = appColors.accent, background = appColors.bg0, surface = appColors.bg1,
            onPrimary = Color.White, onBackground = appColors.text, onSurface = appColors.text,
        )
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
