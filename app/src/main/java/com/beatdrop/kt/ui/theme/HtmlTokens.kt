/*
 * BeatDrop Premium — Design Tokens
 * Pixel-perfect port of the HTML prototype's CSS :root + glass system.
 * Every color, dimension, and shape is derived from the HTML source.
 */

package com.beatdrop.kt.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// CSS :root tokens → Compose Colors
// ═══════════════════════════════════════════════════════════════════

object HtmlTokens {
    // --accent: #FF375F
    val Accent = Color(0xFFFF375F)
    // --accent-dim: rgba(255,55,95,0.35)
    val AccentDim = Color(0x59FF375F)
    // --accent-glow: rgba(255,55,95,0.55)
    val AccentGlow = Color(0x8CFF375F)
    // --glass-bg: rgba(18,16,22,0.72)
    val GlassBg = Color(0xB8121016)
    // --glass-border: rgba(255,255,255,0.09)
    val GlassBorder = Color(0x17FFFFFF)
    // --glass-hi: rgba(255,255,255,0.12)
    val GlassHi = Color(0x1FFFFFFF)
    // --surface: rgba(255,255,255,0.07)
    val Surface = Color(0x12FFFFFF)
    // --surface-hover: rgba(255,255,255,0.11)
    val SurfaceHover = Color(0x1CFFFFFF)

    // body background: #050608
    val PageBg = Color(0xFF050608)

    // Card cover gradients (c-1 through c-8)
    val Cover1 = Color(0xFFFF7788)
    val Cover1End = Color(0xFF4A1530)
    val Cover2 = Color(0xFF2A80B0)
    val Cover2End = Color(0xFF01253E)
    val Cover3 = Color(0xFFFFC55A)
    val Cover3End = Color(0xFF4A2C0A)
    val Cover4 = Color(0xFFC09AFF)
    val Cover4End = Color(0xFF2C1A5E)
    val Cover5 = Color(0xFF25D278)
    val Cover5End = Color(0xFF064E2A)
    val Cover6 = Color(0xFFFF3D8E)
    val Cover6End = Color(0xFF2C0458)
    val Cover7 = Color(0xFF10DEA8)
    val Cover7End = Color(0xFF073E50)
    val Cover8 = Color(0xFFF25A7A)
    val Cover8End = Color(0xFFF88C6E)

    // Browse tile gradients (bt-1 through bt-8)
    val Tile1Start = Color(0xFFFF7A8A)
    val Tile1End = Color(0xFFC73560)
    val Tile2Start = Color(0xFF2A80B0)
    val Tile2End = Color(0xFF0152A0)
    val Tile3Start = Color(0xFF22D076)
    val Tile3End = Color(0xFF0F9855)
    val Tile4Start = Color(0xFFC09AFF)
    val Tile4End = Color(0xFF7550E2)
    val Tile5Start = Color(0xFFFFC55A)
    val Tile5End = Color(0xFFD08520)
    val Tile6Start = Color(0xFFFF3D8E)
    val Tile6End = Color(0xFF7A0FC4)
    val Tile7Start = Color(0xFF10DEA8)
    val Tile7End = Color(0xFF1598C0)
    val Tile8Start = Color(0xFFF25A7A)
    val Tile8End = Color(0xFFFFCF70)

    // Screen backgrounds (dynamic per screen)
    // Library: radial-gradient at 50% -10% #5a2238, at 100% 25% #30183e
    val BgLibrary1 = Color(0xFF5A2238)
    val BgLibrary2 = Color(0xFF30183E)
    // Home/Discover: #1a3d50, #182d3e
    val BgHome1 = Color(0xFF1A3D50)
    val BgHome2 = Color(0xFF182D3E)
    // Search: #281d50, #182650
    val BgSearch1 = Color(0xFF281D50)
    val BgSearch2 = Color(0xFF182650)
    // Now Playing: #781828, #3a1020
    val BgNow1 = Color(0xFF781828)
    val BgNow2 = Color(0xFF3A1020)

    // Text opacities
    val TextFull = Color.White                                     // 100%
    val TextSecondary = Color.White.copy(alpha = 0.65f)            // 65%
    val TextTertiary = Color.White.copy(alpha = 0.48f)             // ~48%
    val TextLow = Color.White.copy(alpha = 0.38f)                  // 38%
    val TextDim = Color.White.copy(alpha = 0.28f)                  // 28%

    // Glass overlays
    val OverlayDark = Color(0xB308060A)  // rgba(8,6,10,0.70)
    val OverlayMedium = Color(0x59FFFFFF) // rgba(255,255,255,0.35)
    val Black45 = Color(0x73000000)
    val Black60 = Color(0x99000000)

    // Special
    val AccentGradientEnd = Color(0xFFFF7A94)
    val AccentGradientDarkEnd = Color(0xFFD01E43)
    val AccentGradientDarkEnd2 = Color(0xFFB71F46)
    val AvatarShadow = Color(0x66FF375F)  // accent 40%
}

// Cover gradient pairs for quick lookup
val CoverGradients = listOf(
    HtmlTokens.Cover1 to HtmlTokens.Cover1End,
    HtmlTokens.Cover2 to HtmlTokens.Cover2End,
    HtmlTokens.Cover3 to HtmlTokens.Cover3End,
    HtmlTokens.Cover4 to HtmlTokens.Cover4End,
    HtmlTokens.Cover5 to HtmlTokens.Cover5End,
    HtmlTokens.Cover6 to HtmlTokens.Cover6End,
    HtmlTokens.Cover7 to HtmlTokens.Cover7End,
    HtmlTokens.Cover8 to HtmlTokens.Cover8End,
)

val TileGradients = listOf(
    HtmlTokens.Tile1Start to HtmlTokens.Tile1End,
    HtmlTokens.Tile2Start to HtmlTokens.Tile2End,
    HtmlTokens.Tile3Start to HtmlTokens.Tile3End,
    HtmlTokens.Tile4Start to HtmlTokens.Tile4End,
    HtmlTokens.Tile5Start to HtmlTokens.Tile5End,
    HtmlTokens.Tile6Start to HtmlTokens.Tile6End,
    HtmlTokens.Tile7Start to HtmlTokens.Tile7End,
    HtmlTokens.Tile8Start to HtmlTokens.Tile8End,
)

fun safeGradientIndex(index: Int, size: Int): Int {
    val mod = index % size
    return if (mod < 0) mod + size else mod
}
