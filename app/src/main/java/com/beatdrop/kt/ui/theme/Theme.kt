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
    // ── Black ladder (per Premium Glass spec) ─────────────────────────
    // Never #000000 — flat black kills the optical illusion of
    // smoked-glass hardware. Use a graduated ladder so each successive
    // surface has just enough delta to read as 'physical, deeper'.
    //
    //   bg0  #040404 — page background (deepest)
    //   bg1  #060606 — list backdrop
    //   bg2  #080808 — card backdrop
    //   bg3  #0B0B0B — elevated card
    //   bg4  #111114 — modal / sheet
    //   bg5  #18181B — extreme elevation (pickers, dropdowns)
    bg0 = Color(0xFF040404),
    bg1 = Color(0xFF060606),
    bg2 = Color(0xFF080808),
    bg3 = Color(0xFF0B0B0B),
    bg4 = Color(0xFF111114),
    bg5 = Color(0xFF18181B),

    // Accent — concept green (#21FF6B), matching the uploaded iOS glass
    // reference. This becomes the single BeatDrop action colour across dark
    // and light mode.
    accent     = Color(0xFF21FF6B),
    accentDark = Color(0xFF13C94F),
    purple     = Color(0xFF234A93),

    accentSoft  = Color(0x1A21FF6B),
    accentBorder = Color(0x8021FF6B),

    // ── Text (one colour, four opacities) ────────────────────────────
    // Per Premium Glass spec: typography hierarchy comes from opacity
    // on a SINGLE white, never from different greys. 100/65/45/30 %
    // tiers map to: track title / artist / metadata / disabled.
    text          = Color(0xFFFFFFFF),         // 100% — primary content
    textSecondary = Color.White.copy(alpha = 0.65f), // 65%  — supporting
    textTertiary  = Color.White.copy(alpha = 0.45f), // 45%  — metadata

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

    // Same concept green in light mode, darkened slightly for legibility
    // against milky glass surfaces.
    accent     = Color(0xFF16C957),
    accentDark = Color(0xFF0E9F42),
    purple     = Color(0xFF1E6AB8),

    accentSoft   = Color(0x1F16C957),
    accentBorder = Color(0x6016C957),

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
    val lg  = 16.dp      // primary screen gutter — matches compact iOS music UIs
    val xl  = 18.dp
    val xxl = 24.dp
}

// ═══════════════════════════════════════════════════════════════════════════════
// Blur Budget — Depth-driven (Premium Glass spec)
// ═══════════════════════════════════════════════════════════════════════════════
// Each successive z-level uses a stronger blur. Higher object = stronger
// blur = more apparent depth. Spec exact values:
//   z0 background       =  0
//   z1 lists            =  ~6
//   z2 album cards      = 20
//   z3 mini player      = 40
//   z4 tab bar          = 45
//   z5 active lens      = 55
//   z6 floating buttons = 60
// Legacy named tokens (subtle/light/…) kept as aliases so existing call
// sites compile during the rollout — they all just delegate to a z-level.
object Blur {
    // ── Depth-named (canonical) ────────────────────────────────────────
    val z0 = 0f
    val z1 = 6f
    val z2 = 20f
    val z3 = 40f
    val z4 = 45f
    val z5 = 55f
    val z6 = 60f

    // ── Legacy aliases ─────────────────────────────────────────────────
    val subtle = z1
    val light  = z2
    val medium = z2
    val heavy  = z4
    val player = z3
    val ultra  = z6
}

// ═══════════════════════════════════════════════════════════════════════════════
// Spring Family — Premium Glass spec
// ═══════════════════════════════════════════════════════════════════════════════
// One spring system across the entire app, three named presets. NEVER use
// raw androidx.compose.animation.core.tween() for press/release/transition
// motion — that's how Material-stock apps end up feeling cheap. Tween is
// fine for one-shot fades that aren't a press response (e.g. content
// crossfades on a load).
//
//   Snap   — fast, lightly bouncy. Tab lens motion, chip press, button release.
//   Settle — medium, well-damped. Most card-press / sheet-open / item-enter.
//   Drift  — slow, very damped. Long content reposition (auto-scroll into view).
//
// Mass is always 1 (Compose's default). Differences come from stiffness +
// damping ratio. Numbers match the spec's tab-lens recipe (Stiffness 400,
// Damping 30, normalised to Compose's dampingRatio 0..1 scale).
object Springs {
    /** Press release — 100→96 / 96→100 motion. */
    val Snap = androidx.compose.animation.core.spring<Float>(
        stiffness = 400f,
        dampingRatio = 0.75f,
    )
    /** Most card / sheet / element-enter motion. */
    val Settle = androidx.compose.animation.core.spring<Float>(
        stiffness = 220f,
        dampingRatio = 0.82f,
    )
    /** Long reposition (lyric auto-scroll, queue settle). */
    val Drift = androidx.compose.animation.core.spring<Float>(
        stiffness = 110f,
        dampingRatio = 0.95f,
    )

    // Dp-typed variants for size/position animations.
    val SnapDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
        stiffness = 400f, dampingRatio = 0.75f,
    )
    val SettleDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
        stiffness = 220f, dampingRatio = 0.82f,
    )

    // IntOffset variants for layout-position animations (the active tab lens).
    val SnapOffset = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        stiffness = 400f, dampingRatio = 0.75f,
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Radius Family — Premium Glass spec
// ═══════════════════════════════════════════════════════════════════════════════
// All radii derived mathematically from a base unit. No random values.
// Spec: 8, 16, 24, 40. Add 6 and 56 for completeness (smaller chips,
// massive sheets). The existing `Radius` object stays for backward
// compatibility; RadiusFamily is the spec-aligned ladder we prefer for
// new surfaces.
object RadiusFamily {
    val xs = 6.dp     // dense chip
    val sm = 8.dp     // small button
    val md = 16.dp    // card
    val lg = 24.dp    // large card / sheet
    val xl = 40.dp    // mini player / tab bar
    val xxl = 56.dp   // modal
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
        MaterialTheme(colorScheme = scheme, typography = BeatDropTypography, content = content)
    }
}