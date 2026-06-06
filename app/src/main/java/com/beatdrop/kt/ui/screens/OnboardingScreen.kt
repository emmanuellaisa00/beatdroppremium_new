package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Onboarding — rewritten from scratch in the same visual language as the
 * HTML concept. Pure black + soft pink ambient, pink glass disc logo, three
 * monochrome glass feature cards, full-width pink CTA pill.
 */
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val C = LocalAppColors.current

    ScreenScaffold(ambientIntensity = 0.20f) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = PageHorizontalPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo disc — pink radial with a centred note glyph
            Box(
                Modifier
                    .size(112.dp)
                    .shadow(
                        elevation = 30.dp,
                        shape = CircleShape,
                        ambientColor = C.accent.copy(alpha = 0.55f),
                        spotColor = C.accent.copy(alpha = 0.45f),
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                C.accent,
                                C.accentDark,
                                Color(0xFF3A0610),
                            ),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Ic.MusicNote, null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "BeatDrop",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = C.text,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your music, beautifully played.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            // Feature cards — monochrome glass
            OnbCard(Ic.MusicNote, "Your Local Library", "Instantly plays every song already on your phone.")
            Spacer(Modifier.height(10.dp))
            OnbCard(Ic.Lyrics,    "Synced Lyrics",       "Drop a .lrc file next to a track and sing along in real time.")
            Spacer(Modifier.height(10.dp))
            OnbCard(Ic.Playlist,  "Playlists & Queue",   "Create playlists, reorder your queue, manage your music.")
            Spacer(Modifier.height(10.dp))
            OnbCard(Ic.Sparkles,  "Auto-Mix",            "Smart crossfade between tracks — matches BPM, key, taste.")

            Spacer(Modifier.weight(1f))

            // CTA pill — solid pink
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = C.accent.copy(alpha = 0.50f),
                        spotColor = C.accent.copy(alpha = 0.40f),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(listOf(C.accent, C.accentDark)),
                    )
                    .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                    .pressableScale(onClick = onGetStarted, scaleTo = 0.96f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Get Started",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "We'll ask for permission to your music in the next step.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnbCard(icon: ImageVector, title: String, body: String) {
    val C = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = C.text.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = C.text.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Normal)
        }
    }
}
