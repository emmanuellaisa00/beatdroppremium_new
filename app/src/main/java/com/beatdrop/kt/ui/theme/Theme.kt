package com.beatdrop.kt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * BeatDrop — Premium Frosted-Glass Design System
 *
 * Inspired by Apple Music + iOS 26 Liquid Glass + the BeatDrop HTML concept.
 *
 * Visual language:
 *   • Pure / near-black background, gently warmed by ambient color glows.
 *   • Dark obsidian glass surfaces — translucent, never solid.
 *   • Single accent: Apple Music pink (#FA2D48).
 *   • Monochrome iconography, color only in real artwork.
 *   • Strong blur, low opacity, soft inner highlight + dark inner shadow.
 *   • Generous spacing (24 dp gutters), springs everywhere.
 */
data class AppColors(
    val bg0: Color, val bg1: Color, val bg2: Color, val bg3: Color, val bg4: Color, val bg5: Color,
    val accent: Color, val accentDark: Color, val purple: Color,
    val accentSoft: Color, val accentBorder: Color,
    val text: Color, val textSecondary: Color, val textTertiary: Color,
    val border: Color, val separator: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val glassInnerShadow: Color,
    val glassRimLight: Color,
    val glassTint: Color,
    val glassShadow: Color,
    val glassShadowStrong: Color,
    val glassGlow: Color,
    val glassAmbient: Color,
    val glassFloating: Color,
    val glassFloatingBorder: Color,
    val glassCardElevated: Color,
    val glassCardElevatedBorder: Color,
    val glassNav: Color,
    val glassNavBorder: Color,
    val glassPlayer: Color,
    val glassPlayerBorder: Color,
    val glassModal: Color,
    val glassModalBorder: Color,
    val glassSheetBackground: Color,
    val green: Color, val blue: Color, val orange: Color, val red: Color,
    val isDark: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════════
// DARK THEME — Pure black with deep obsidian glass
// ═══════════════════════════════════════════════════════════════════════════════
val DarkColors = AppColors(
    // Black ladder — content sits on near-black so glass surfaces read as glass,
    // not as flat tinted rectangles.
    bg0 = Color(0xFF000000),  // page (deepest, true black)
    bg1 = Color(0xFF050507),  // list backdrop
    bg2 = Color(0xFF0A0A0E),  // card backdrop
    bg3 = Color(0xFF101014),  // elevated card
    bg4 = Color(0xFF16181E),  // modal / sheet
    bg5 = Color(0xFF1C1E26),  // pickers / dropdowns

    // Accent — Apple Music pink. Single action colour across the app.
    accent     = Color(0xFFFA2D48),
    accentDark = Color(0xFFD41F38),
    purple     = Color(0xFF7C5CFF),

    accentSoft   = Color(0x1FFA2D48),     // 12% — backgrounds, soft pills
    accentBorder = Color(0x80FA2D48),     // 50% — outlines, focus rings

    // Text — single white at four opacities
    text          = Color(0xFFFFFFFF),           // 100% — primary
    textSecondary = Color.White.copy(alpha = 0.65f), // 65% — supporting
    textTertiary  = Color.White.copy(alpha = 0.45f), // 45% — metadata

    border    = Color(0x12FFFFFF),
    separator = Color(0x0DFFFFFF),

    // ── Glass material (dark obsidian recipe) ──────────────────────────────
    // Base: rgba(28,30,38, 0.42) → 0x6B1C1E26
    glassSurface     = Color(0x6B1C1E26),
    glassBorder      = Color(0x0FFFFFFF),     // 6% — barely-there hairline
    glassHighlight   = Color(0x12FFFFFF),     // 7% — top sheen
    glassInnerShadow = Color(0x4D000000),     // 30% — inset bottom edge
    glassRimLight    = Color(0x14FFFFFF),     // 8% — inset top edge
    glassTint        = Color(0xCC0A0A10),
    glassShadow      = Color(0x73000000),     // 45% — ambient drop
    glassShadowStrong = Color(0x99000000),    // 60% — strong elevation
    glassGlow        = Color(0x3DFA2D48),     // pink accent glow
    glassAmbient     = Color(0x29FA2D48),     // ambient pink background hint

    // Elevated cards (slightly more present than base glass)
    glassCardElevated     = Color(0x701C1E26),
    glassCardElevatedBorder = Color(0x14FFFFFF),

    // Floating dock — dark, strong blur
    // rgba(22,24,30, 0.62) → 0x9E16181E
    glassNav      = Color(0x9E16181E),
    glassNavBorder = Color(0x0EFFFFFF),

    // Mini player — same recipe as the dock, sits as matched pair
    glassPlayer      = Color(0x9E16181E),
    glassPlayerBorder = Color(0x0EFFFFFF),

    // Modal sheets — deepest, most opaque
    glassModal      = Color(0xE6101014),
    glassModalBorder = Color(0x1FFFFFFF),

    // Small floating chips & search field
    glassFloating       = Color(0x701C1E26),
    glassFloatingBorder  = Color(0x14FFFFFF),
    glassSheetBackground = Color(0xE6101014),

    green  = Color(0xFF1ED760),    // Spotify green — used only where explicitly needed
    blue   = Color(0xFF0A84FF),
    orange = Color(0xFFFF9F0A),
    red    = Color(0xFFFA2D48),
    isDark = true,
)

// ═══════════════════════════════════════════════════════════════════════════════
// LIGHT THEME — Soft off-white with frosted milk-glass
// ═══════════════════════════════════════════════════════════════════════════════
val LightColors = AppColors(
    bg0 = Color(0xFFF7F7F9),
    bg1 = Color(0xFFFFFFFF),
    bg2 = Color(0xFFF1F1F5),
    bg3 = Color(0xFFE8E8EE),
    bg4 = Color(0xFFD8D8DE),
    bg5 = Color(0xFFC8C8CE),

    accent     = Color(0xFFFA2D48),
    accentDark = Color(0xFFB81B30),
    purple     = Color(0xFF5E48E8),

    accentSoft   = Color(0x1FFA2D48),
    accentBorder = Color(0x60FA2D48),

    text          = Color(0xFF000000),
    textSecondary = Color(0xA6000000),
    textTertiary  = Color(0x66000000),

    border    = Color(0x14000000),
    separator = Color(0x0D000000),

    glassSurface     = Color(0xCCFFFFFF),
    glassBorder      = Color(0x14000000),
    glassHighlight   = Color(0xE6FFFFFF),
    glassInnerShadow = Color(0x14000000),
    glassRimLight    = Color(0xCCFFFFFF),
    glassTint        = Color(0xCCFFFFFF),
    glassShadow      = Color(0x1A000000),
    glassShadowStrong = Color(0x33000000),
    glassGlow        = Color(0x29FA2D48),
    glassAmbient     = Color(0x14FA2D48),

    glassCardElevated      = Color(0xE0FFFFFF),
    glassCardElevatedBorder = Color(0x18000000),

    glassNav      = Color(0xE8FFFFFF),
    glassNavBorder = Color(0x14000000),

    glassPlayer      = Color(0xEEFFFFFF),
    glassPlayerBorder = Color(0x1F000000),

    glassModal      = Color(0xF0FFFFFF),
    glassModalBorder = Color(0x29000000),

    glassFloating       = Color(0xD9FFFFFF),
    glassFloatingBorder  = Color(0x14000000),
    glassSheetBackground = Color(0xEEFFFFFF),

    green  = Color(0xFF1DB954),
    blue   = Color(0xFF007AFF),
    orange = Color(0xFFFF9500),
    red    = Color(0xFFFA2D48),
    isDark = false,
)

// ═══════════════════════════════════════════════════════════════════════════════
// Concentric Radius System (iOS 26 / Liquid Glass rule: inner = outer - padding)
// ═══════════════════════════════════════════════════════════════════════════════
object Radius {
    val xs   = 8.dp
    val sm   = 16.dp
    val md   = 22.dp
    val lg   = 28.dp
    val xl   = 36.dp
    val xxl  = 44.dp
    val pill = 999.dp

    fun inner(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Spacing System — 24 dp primary gutter
// ═══════════════════════════════════════════════════════════════════════════════
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 20.dp
    val xxl = 24.dp     // primary screen gutter
    val xxxl = 32.dp    // section separation
}

// ═══════════════════════════════════════════════════════════════════════════════
// Blur Budget — Depth-driven
// ═══════════════════════════════════════════════════════════════════════════════
object Blur {
    val z0 = 0f
    val z1 = 8f
    val z2 = 24f
    val z3 = 40f
    val z4 = 50f
    val z5 = 55f
    val z6 = 60f

    // Legacy aliases
    val subtle = z1
    val light  = z2
    val medium = z2
    val heavy  = z4
    val player = z4
    val ultra  = z6
}

// ═══════════════════════════════════════════════════════════════════════════════
// Springs — One physics system, three presets
// ═══════════════════════════════════════════════════════════════════════════════
object Springs {
    val Snap = androidx.compose.animation.core.spring<Float>(
        stiffness = 400f,
        dampingRatio = 0.75f,
    )
    val Settle = androidx.compose.animation.core.spring<Float>(
        stiffness = 220f,
        dampingRatio = 0.82f,
    )
    val Drift = androidx.compose.animation.core.spring<Float>(
        stiffness = 110f,
        dampingRatio = 0.95f,
    )

    val SnapDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
        stiffness = 400f, dampingRatio = 0.75f,
    )
    val SettleDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
        stiffness = 220f, dampingRatio = 0.82f,
    )

    val SnapOffset = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        stiffness = 400f, dampingRatio = 0.75f,
    )
}

object RadiusFamily {
    val xs = 8.dp
    val sm = 16.dp
    val md = 22.dp
    val lg = 28.dp
    val xl = 36.dp
    val xxl = 44.dp
}

val LocalAppColors = staticCompositionLocalOf { DarkColors }

private val BeatDropTypography = Typography(
    displayLarge = Type.largeTitle,
    headlineLarge = Type.title1,
    headlineMedium = Type.title2,
    titleLarge = Type.title3,
    titleMedium = Type.headline,
    bodyLarge = Type.body,
    bodyMedium = Type.callout,
    bodySmall = Type.footnote,
    labelLarge = Type.callout,
    labelMedium = Type.caption,
    labelSmall = Type.overline,
)

@Composable
fun BeatDropTheme(themePref: String = "dark", content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "dark"  -> true
        "light" -> false
        else    -> isSystemInDarkTheme()
    }
    val appColors = if (dark) DarkColors else LightColors
    val scheme = if (dark)
        darkColorScheme(
            primary       = appColors.accent,
            background    = appColors.bg0,
            surface       = appColors.bg1,
            onPrimary     = Color.White,
            onBackground  = appColors.text,
            onSurface     = appColors.text,
        )
    else
        lightColorScheme(
            primary      = appColors.accent,
            background   = appColors.bg0,
            surface      = appColors.bg1,
            onPrimary    = Color.White,
            onBackground = appColors.text,
            onSurface    = appColors.text,
        )
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = scheme, typography = BeatDropTypography, content = content)
    }
}
