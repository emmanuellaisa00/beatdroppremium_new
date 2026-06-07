/*
 * BeatDrop Premium — Theme
 * Manrope font family, dark-only glass theme matching the HTML prototype.
 */

package com.beatdrop.kt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Manrope loaded from res/font/ — falls back to system sans-serif
val Manrope = FontFamily.Default  // Will use Manrope when font files are added

object BdTypography {
    val largeTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W900,
        fontSize = 36.sp,
        letterSpacing = (-1.5).sp,
        lineHeight = 36.sp,
    )
    val sectionTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W900,
        fontSize = 22.sp,
        letterSpacing = (-0.7).sp,
    )
    val screenTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 25.sp,
        letterSpacing = (-0.75).sp,
    )
    val bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
    )
    val bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        letterSpacing = (-0.07).sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
    )
    val label = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 13.sp,
        letterSpacing = (-0.07).sp,
    )
    val caption = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        letterSpacing = (1.1).sp,  // 0.10em uppercase
    )
    val pill = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        letterSpacing = (-0.07).sp,
    )
    val trackTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
    )
    val trackArtist = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W500,
        fontSize = 12.5.sp,
    )
    val cardTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 14.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 17.5.sp,
    )
    val cardArtist = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
    )
    val nowTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W900,
        fontSize = 28.sp,
        letterSpacing = (-0.75).sp,
        lineHeight = 28.6.sp,
    )
    val nowArtist = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
    )
    val lyricsLine = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W900,
        fontSize = 26.sp,
        letterSpacing = (-0.62).sp,
        lineHeight = 30.7.sp,
    )
    val compactTitle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 17.sp,
        letterSpacing = (-0.27).sp,
    )
    val timecode = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
    )
    val greeting = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        letterSpacing = (-0.07).sp,
    )
    val greetingBold = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        letterSpacing = (-0.07).sp,
    )
}

private val BdDarkScheme = darkColorScheme(
    primary = HtmlTokens.Accent,
    onPrimary = Color.White,
    background = HtmlTokens.PageBg,
    surface = HtmlTokens.Surface,
    onSurface = Color.White,
    onSurfaceVariant = HtmlTokens.TextSecondary,
)

@Composable
fun BeatDropTheme(
    themePref: String = "dark",
    content: @Composable () -> Unit,
) {
    val appColors = AppColors()
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = BdDarkScheme,
            typography = androidx.compose.material3.Typography(),
            content = content,
        )
    }
}
