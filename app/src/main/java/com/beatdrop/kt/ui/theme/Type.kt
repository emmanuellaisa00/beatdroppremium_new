package com.beatdrop.kt.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * iOS 26-inspired type scale — SF Pro-like tight tracking with a premium feel.
 * Larger sizes with tighter letter-spacing for that Apple Music / Spotify
 * "cinematic" readability on glass surfaces.
 */
object Type {
    private val appFont = FontFamily.SansSerif

    // Dense premium scale: closer to Spotify iOS than oversized Android titles.
    // Tight line heights keep more music content visible while preserving the
    // uploaded concept's bold, high-contrast typography.
    val largeTitle = TextStyle(fontFamily = appFont, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.9).sp, lineHeight = 37.sp)
    val title1     = TextStyle(fontFamily = appFont, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = 31.sp)
    val title2     = TextStyle(fontFamily = appFont, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp, lineHeight = 26.sp)
    val title3     = TextStyle(fontFamily = appFont, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp, lineHeight = 22.sp)
    val headline   = TextStyle(fontFamily = appFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp, lineHeight = 20.sp)
    val body       = TextStyle(fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.08).sp, lineHeight = 19.sp)
    val callout    = TextStyle(fontFamily = appFont, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.05).sp, lineHeight = 18.sp)
    val subhead    = TextStyle(fontFamily = appFont, fontSize = 12.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeight = 17.sp)
    val footnote   = TextStyle(fontFamily = appFont, fontSize = 11.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeight = 15.sp)
    val caption    = TextStyle(fontFamily = appFont, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp, lineHeight = 13.sp)
    val overline   = TextStyle(fontFamily = appFont, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, lineHeight = 13.sp)
}
