package com.beatdrop.kt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.R

// Manrope font family matching the premium HTML
// val Manrope = FontFamily(
//     Font(R.font.manrope_regular, FontWeight.Normal),
//     Font(R.font.manrope_medium, FontWeight.Medium),
//     Font(R.font.manrope_semibold, FontWeight.SemiBold),
//     Font(R.font.manrope_bold, FontWeight.Bold),
//     Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
//     Font(R.font.manrope_black, FontWeight.Black),
// )
// Fallback to system until fonts are added
val Manrope = FontFamily.Default

val BeatDropTypography = Typography(
    // Large title — 36sp / 900 (HTML: .large-title)
    displayLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Black,
        fontSize = 36.sp, letterSpacing = (-0.042 * 36).sp, lineHeight = 36.sp,
    ),
    // Now Playing track name — 28sp / 900
    displayMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Black,
        fontSize = 28.sp, letterSpacing = (-0.030 * 28).sp, lineHeight = 28.6.sp,
    ),
    // Lyrics active line — 26sp / 900
    displaySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Black,
        fontSize = 26.sp, letterSpacing = (-0.024 * 26).sp, lineHeight = 30.7.sp,
    ),
    // Section header — 22sp / 900
    headlineLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Black,
        fontSize = 22.sp, letterSpacing = (-0.032 * 22).sp, lineHeight = 22.sp,
    ),
    // Album title — 25sp / 900
    headlineMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Black,
        fontSize = 25.sp, letterSpacing = (-0.030 * 25).sp, lineHeight = 25.sp,
    ),
    // Compact header title — 17sp / 800
    headlineSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, letterSpacing = (-0.016 * 17).sp, lineHeight = 22.sp,
    ),
    // Track title — 15sp / 600-700
    titleMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = (-0.014 * 15).sp, lineHeight = 20.sp,
    ),
    // Card/mini name — 14sp / 700-800
    titleSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, letterSpacing = (-0.014 * 14).sp, lineHeight = 17.5.sp,
    ),
    // Body — 13-15sp / 500-600
    bodyLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = (-0.014 * 15).sp, lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = (-0.005 * 13).sp, lineHeight = 17.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 0.sp, lineHeight = 16.sp,
    ),
    // Pill — 13sp / 700
    labelLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, letterSpacing = (-0.005 * 13).sp, lineHeight = 34.sp,
    ),
    // Tab label — 9sp / 800 uppercase
    labelSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = 9.sp, letterSpacing = (0.08 * 9).sp, lineHeight = 12.sp,
    ),
)
