package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Splash — pure black radial with a subtle pink ambient glow + animated
 * BeatDrop wordmark. Tap anywhere to skip. Auto-dismisses after ~700 ms.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val C = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splashScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splashAlpha",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(700)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pressableScale(onClick = { onDone() })
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1F0712),
                        Color(0xFF050307),
                        Color(0xFF000000),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha),
        ) {
            Text(
                "BeatDrop",
                color = C.text,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your music, beautifully played.",
                color = C.text.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Tap to skip",
                color = C.text.copy(alpha = 0.40f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "FROM",
                color = C.text.copy(alpha = 0.35f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Laisacorp",
                color = C.text.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
    }
}
