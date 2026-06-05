package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val C = LocalAppColors.current
    // Same indigo backdrop as the SplashScreen + system splash so the
    // user lands here in a colour space they've already been seeing for
    // ~700 ms. Replaces the previous purple/pink palette which clashed
    // with the new Ocean-Teal accent + Cascade-Drop logo identity.
    val bgStops = if (C.isDark) listOf(
        Color(0xFF1A1230),   // top — matches logo gradient violet
        Color(0xFF0E0A1F),   // mid — matches splash
        Color(0xFF06040E),   // bottom — deepest
    ) else listOf(
        Color(0xFFE8F4FB),   // top — pale teal wash
        Color(0xFFF5F8FA),   // mid
        Color(0xFFFAFAFD),   // bottom — near white
    )
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(bgStops))
    ) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            // Logo with glow effect
            Box(
                Modifier
                    .size(112.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF7B2CBF).copy(alpha = 0.5f),
                        spotColor = Color(0xFFC77DFF).copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFC77DFF),
                                Color(0xFF7B2CBF),
                                Color(0xFF3D1259),
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Ic.MusicNote,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                "BeatDrop",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = if (C.isDark) Color.White else Color(0xFF101018),
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp,
            )

            Text(
                "Your music, beautifully played.",
                color = if (C.isDark) Color(0xFFD4B0FF) else C.accentDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Feature list with cards
            FeatureCard(Ic.MusicNote, "Your Local Library", "Instantly plays every song already on your phone.", Color(0xFFC77DFF), C.isDark)
            Spacer(Modifier.height(10.dp))
            FeatureCard(Ic.Lyrics, "Synced Lyrics", "Drop a .lrc file next to a track and sing along in real-time.", Color(0xFF0A84FF), C.isDark)
            Spacer(Modifier.height(10.dp))
            FeatureCard(Ic.Playlist, "Playlists & Queue", "Create playlists, reorder your queue, and manage your music.", Color(0xFF30D158), C.isDark)
            Spacer(Modifier.height(10.dp))
            FeatureCard(Ic.Sparkles, "Auto-Mix", "Smart crossfade between tracks — matches BPM, key, and your taste.", Color(0xFFFF9F0A), C.isDark)

            Spacer(Modifier.weight(1f))

            // CTA Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = C.accent.copy(alpha = 0.4f),
                        spotColor = C.accent.copy(alpha = 0.3f),
                    )
                    // CTA matches the in-app accent (Ocean Teal) so the
                    // first thing the user taps uses the same colour as
                    // every other 'primary action' in the app.
                    .background(C.accent)
                    .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .pressableScale(onClick = onGetStarted, haptic = false)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Get Started",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "BeatDrop reads audio on your device.\nNo account, no uploads, no tracking.",
                color = if (C.isDark) Color(0xFF8A8A9A) else Color(0x7F101018),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, body: String, accent: Color, isDark: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.55f)
            )
            .border(
                0.8.dp,
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (isDark) Color.White else Color(0xFF101018),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                body,
                color = if (isDark) Color(0xFFA9A9BC) else Color(0xBF101018),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
