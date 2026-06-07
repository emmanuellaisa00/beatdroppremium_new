package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.Accent
import com.beatdrop.kt.ui.theme.Background
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true; delay(1800); onDone() }

    val alpha by animateFloatAsState(if (visible) 1f else 0f, animationSpec = tween(600), label = "splash_alpha")
    val scale by animateFloatAsState(if (visible) 1f else 0.8f, animationSpec = tween(800, easing = EaseOutBack), label = "splash_scale")

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        ) {
            Text(
                "Beat",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = androidx.compose.ui.graphics.Color.White,
                ),
            )
            Text(
                "Drop",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Accent,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Premium",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.50f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (0.18 * 13).sp,
                ),
            )
        }
    }
}
