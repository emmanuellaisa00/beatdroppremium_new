package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.R
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.ui.components.pressableScale
import kotlinx.coroutines.delay

/**
 * Branded splash: BeatDrop logo + name + "From Laisacorp".
 * Shows for ~1.8s with a gentle scale/fade-in, then calls [onDone].
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val C = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "logoAlpha",
    )

    LaunchedEffect(Unit) {
        visible = true
        // 700 ms (was 1400 ms). The Android-12+ system splash already
        // showed the logo for ~600 ms before this composable rendered,
        // so a long in-app splash on top of it felt sluggish. 700 ms is
        // enough for the scale-in spring + the wordmark + 'From Laisacorp'
        // to land cleanly, then we hand off to the app.
        delay(700)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pressableScale(onClick = { onDone() })
            // Flat #0E0A1F — exact match to themes.xml
            // windowSplashScreenBackground. Updated alongside the new
            // 'Cascade Drop' app icon: deep midnight indigo gives
            // maximum chromatic separation from the icon's warm
            // violet/coral/gold gradient so the logo glows on the
            // backdrop. System splash → branded Compose splash → app
            // is one continuous colour, zero visible swap.
            .background(Color(0xFF0E0A1F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.beatdrop_logo),
                contentDescription = "BeatDrop",
                modifier = Modifier
                    .size(128.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(22.dp))
            Row(Modifier.scale(scale)) {
                Text("Beat", color = C.accent, style = Type.largeTitle, fontWeight = FontWeight.Black, fontSize = 34.sp)
                Text("Drop", color = Color.White, style = Type.largeTitle, fontWeight = FontWeight.Black, fontSize = 34.sp)
            }
        }

        // "From Laisacorp" + "Tap to skip"
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Tap to skip",
                color = C.textTertiary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "FROM",
                color = C.textTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Laisacorp",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
    }
}
