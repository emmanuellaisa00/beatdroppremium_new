/*
 * AppColors — backward-compatible accessor that maps old PremiumGlass tokens
 * to the new HTML-derived tokens. This lets existing screen code work without
 * rewriting every file, while still using the exact HTML colors underneath.
 */

package com.beatdrop.kt.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg0: Color = HtmlTokens.PageBg,
    val bg1: Color = Color(0xFF050507),
    val bg2: Color = Color(0xFF0A0A0E),
    val bg3: Color = Color(0xFF101014),
    val bg4: Color = Color(0xFF16181E),
    val bg5: Color = Color(0xFF1C1E26),
    val accent: Color = HtmlTokens.Accent,
    val accentDark: Color = HtmlTokens.AccentGradientDarkEnd,
    val purple: Color = Color(0xFF7C5CFF),
    val accentSoft: Color = HtmlTokens.AccentDim,
    val accentBorder: Color = HtmlTokens.AccentGlow,
    val text: Color = Color.White,
    val textSecondary: Color = HtmlTokens.TextSecondary,
    val textTertiary: Color = HtmlTokens.TextTertiary,
    val border: Color = HtmlTokens.GlassBorder,
    val separator: Color = Color(0x0DFFFFFF),
    val glassSurface: Color = HtmlTokens.GlassBg,
    val glassBorder: Color = HtmlTokens.GlassBorder,
    val glassHighlight: Color = HtmlTokens.GlassHi,
    val glassInnerShadow: Color = Color(0x4D000000),
    val glassRimLight: Color = Color(0x14FFFFFF),
    val glassTint: Color = Color(0xCC0A0A10),
    val glassShadow: Color = Color(0x73000000),
    val glassShadowStrong: Color = Color(0x99000000),
    val glassGlow: Color = HtmlTokens.AccentGlow,
    val glassAmbient: Color = HtmlTokens.AccentDim,
    val glassFloating: Color = HtmlTokens.Surface,
    val glassFloatingBorder: Color = HtmlTokens.GlassBorder,
    val glassCardElevated: Color = Color(0x701C1E26),
    val glassCardElevatedBorder: Color = Color(0x14FFFFFF),
    val glassNav: Color = Color(0x9E16181E),
    val glassNavBorder: Color = Color(0x0EFFFFFF),
    val glassPlayer: Color = Color(0x9E16181E),
    val glassPlayerBorder: Color = Color(0x0EFFFFFF),
    val glassModal: Color = Color(0xE6101014),
    val glassModalBorder: Color = Color(0x1FFFFFFF),
    val glassSheetBackground: Color = Color(0xDE08060A),
    val green: Color = Color(0xFF30D158),
    val blue: Color = Color(0xFF0A84FF),
    val orange: Color = Color(0xFFFF9F0A),
    val red: Color = Color(0xFFFF453A),
    val isDark: Boolean = true,
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }
