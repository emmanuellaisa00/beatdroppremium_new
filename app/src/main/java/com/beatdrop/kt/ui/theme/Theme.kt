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
 * iOS 26-inspired Glassmorphism design tokens.
 * Inspired by: Spotify App UI Glassmorphism Concept (Behance)
 *
 * The visual formula:
 *   40% Shadows
 *   25% Reflections
 *   15% Blur
 *   10% Noise
 *   10% Typography
 *
 * Rendering stack (bottom to top):
 *   Shadow Layer
 *   ↓
 *   Ambient Glow Layer
 *   ↓
 *   Glass Surface
 *   ↓
 *   Backdrop Blur
 *   ↓
 *   Noise Texture
 *   ↓
 *   Reflection Layer
 *   ↓
 *   Content Layer
 */
data class AppColors(
    val bg0: Color, val bg1: Color, val bg2: Color, val bg3: Color, val bg4: Color, val bg5: Color,
    val accent: Color, val accentDark: Color, val purple: Color,
    val accentSoft: Color, val accentBorder: Color,
    val text: Color, val textSecondary: Color, val textTertiary: Color,
    val border: Color, val separator: Color,
    // ── Glass Material Tokens ──────────────────────────────────────────────
    val glassSurface: Color,           // Main glass surface fill
    val glassBorder: Color,            // Hairline border on glass
    val glassHighlight: Color,         // Top reflection gradient
    val glassInnerShadow: Color,       // Bottom inner shadow for depth
    val glassRimLight: Color,          // Top-edge rim light (Fresnel)
    val glassTint: Color,              // Tinted glass background
    val glassShadow: Color,            // Context-aware shadow
    val glassShadowStrong: Color,      // Strong shadow for elevated elements
    val glassGlow: Color,              // Accent glow color
    val glassAmbient: Color,           // Ambient background glow
    // ── Floating Glass (search bar, pills) ─────────────────────────────────
    val glassFloating: Color,           // Search bar, floating pills (blur 28px)
    val glassFloatingBorder: Color,
    // ── Elevated / Floating Glass ────────────────────────────────────────────
    val glassCardElevated: Color,       // Floating cards (blur 24-32px)
    val glassCardElevatedBorder: Color,
    val glassNav: Color,                // Bottom navigation (blur 36px)
    val glassNavBorder: Color,
    val glassPlayer: Color,             // Mini player (blur 48-50px)
    val glassPlayerBorder: Color,
    val glassModal: Color,              // Modals / sheets (blur 60px)
    val glassModalBorder: Color,
    // ── Sheet Background ───────────────────────────────────────────────────
    val glassSheetBackground: Color,    // Bottom sheet background
    // ── Standard Colors ──────────────────────────────────────────────────────
    val green: Color, val blue: Color, val orange: Color, val red: Color,
    val isDark: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════════
// DARK THEME — Deep backgrounds for strong glass pop
// ═══════════════════════════════════════════════════════════════════════════════
val DarkColors = AppColors(
    // Backgrounds — near-black with subtle depth
    bg0 = Color(0xFF050505),
    bg1 = Color(0xFF0B0B0B),
    bg2 = Color(0xFF121218),
    bg3 = Color(0xFF1A1A1A),
    bg4 = Color(0xFF242424),
    bg5 = Color(0xFF2E2E2E),

    // Accent — Spotify Green (#21FF6B)
    accent     = Color(0xFF21FF6B),
    accentDark = Color(0xFF1AD95A),
    purple     = Color(0xFF234A93),

    accentSoft  = Color(0x1A21FF6B),
    accentBorder = Color(0x8021FF6B),

    // Text — high contrast white
    text         = Color(0xFFE8E8E8),
    textSecondary = Color(0x8CFFFFFF),
    textTertiary  = Color(0x61FFFFFF),

    border    = Color(0x12FFFFFF),
    separator = Color(0x0DFFFFFF),

    // ── Glass Material ─────────────────────────────────────────────────────
    glassSurface    = Color(0x08FFFFFF),
    glassBorder     = Color(0x1FFFFFFF),
    glassHighlight  = Color(0x33FFFFFF),
    glassInnerShadow = Color(0x0DFFFFFF),
    glassRimLight   = Color(0x4DFFFFFF),
    glassTint       = Color(0xE00A0A10),
    glassShadow     = Color(0x40000000),
    glassShadowStrong = Color(0x80000000),
    glassGlow       = Color(0x3021FF6B),
    glassAmbient    = Color(0x1E1E5080),

    // Elevated glass layers
    glassCardElevated     = Color(0x12FFFFFF),
    glassCardElevatedBorder = Color(0x20FFFFFF),

    glassNav      = Color(0xCC0A0A10),
    glassNavBorder = Color(0x2EFFFFFF),

    glassPlayer      = Color(0xE00A0A10),
    glassPlayerBorder = Color(0x33FFFFFF),

    glassModal      = Color(0xF00A0A10),
    glassModalBorder = Color(0x3DFFFFFF),

    glassFloating       = Color(0x20FFFFFF),
    glassFloatingBorder  = Color(0x28FFFFFF),
    glassSheetBackground = Color(0xE00A0A10),

    green  = Color(0xFF21FF6B),
    blue   = Color(0xFF234A93),
    orange = Color(0xFFFF9F0A),
    red    = Color(0xFFFF453A),
    isDark = true,
)

// ═══════════════════════════════════════════════════════════════════════════════
// LIGHT THEME — Light backgrounds
// ═══════════════════════════════════════════════════════════════════════════════
val LightColors = AppColors(
    bg0 = Color(0xFFF2F2F7),
    bg1 = Color(0xFFFFFFFF),
    bg2 = Color(0xFFF8F8FC),
    bg3 = Color(0xFFE8E8EE),
    bg4 = Color(0xFFD8D8DE),
    bg5 = Color(0xFFC8C8CE),

    accent     = Color(0xFF1DB954),
    accentDark = Color(0xFF1AA848),
    purple     = Color(0xFF1E6AB8),

    accentSoft   = Color(0x1F1DB954),
    accentBorder = Color(0x601DB954),

    text         = Color(0xFF000000),
    textSecondary = Color(0x8C000000),
    textTertiary  = Color(0x59000000),

    border    = Color(0x14000000),
    separator = Color(0x0D000000),

    glassSurface     = Color(0xCCFFFFFF),
    glassBorder      = Color(0x1A000000),
    glassHighlight   = Color(0xF5FFFFFF),
    glassInnerShadow = Color(0x0A000000),
    glassRimLight    = Color(0xEEFFFFFF),
    glassTint        = Color(0xCCFFFFFF),
    glassShadow      = Color(0x15000000),
    glassShadowStrong = Color(0x30000000),
    glassGlow        = Color(0x201DB954),
    glassAmbient     = Color(0x0A1E50C8),

    glassCardElevated      = Color(0xDDFFFFFF),
    glassCardElevatedBorder = Color(0x1A000000),

    glassNav      = Color(0xE8FFFFFF),
    glassNavBorder = Color(0x18000000),

    glassPlayer      = Color(0xEEFFFFFF),
    glassPlayerBorder = Color(0x20000000),

    glassModal      = Color(0xF0FFFFFF),
    glassModalBorder = Color(0x28000000),

    glassFloating       = Color(0xCCFFFFFF),
    glassFloatingBorder  = Color(0x18000000),
    glassSheetBackground = Color(0xEEFFFFFF),

    green  = Color(0xFF1DB954),
    blue   = Color(0xFF007AFF),
    orange = Color(0xFFFF9500),
    red    = Color(0xFFFF3B30),
    isDark = false,
)

// ═══════════════════════════════════════════════════════════════════════════════
// Concentric Radius System (iOS 26 / Liquid Glass rule: inner = outer - padding)
// ═══════════════════════════════════════════════════════════════════════════════
object Radius {
    val xs   = 6.dp
    val sm   = 20.dp   // Small cards, chips
    val md   = 28.dp   // Album cards, compact items
    val lg   = 36.dp   // Large cards, player elements
    val xl   = 44.dp   // Mini player outer
    val xxl  = 56.dp   // Now Playing card
    val pill = 999.dp  // Pills, search bars

    /** Concentric inner radius = outer - padding */
    fun inner(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Spacing System
// ═══════════════════════════════════════════════════════════════════════════════
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 20.dp
    val xxl = 28.dp
}

// ═══════════════════════════════════════════════════════════════════════════════
// Blur Budget (60fps target)
// ═══════════════════════════════════════════════════════════════════════════════
object Blur {
    val subtle = 8f    // List rows, dense small-text surfaces (Search/Trending/etc.)
    val light  = 16f   // Cards, compact elements
    val medium = 28f   // Medium cards, elevated surfaces
    val heavy  = 40f   // Navigation bar
    val player = 48f   // Mini player (higher than nav for elevation)
    val ultra  = 60f   // Modals, sheets, full overlays
}

val LocalAppColors = staticCompositionLocalOf { DarkColors }

// ═══════════════════════════════════════════════════════════════════════════════
// Theme Provider
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun BeatDropTheme(themePref: String = "light", content: @Composable () -> Unit) {
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
            onPrimary     = Color.Black,
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
        MaterialTheme(colorScheme = scheme, content = content)
    }
}