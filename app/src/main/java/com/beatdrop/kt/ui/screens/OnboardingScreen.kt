package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.*

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // Icon
            Box(modifier = Modifier.size(72.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Your music, your way", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("BeatDrop scans your device for music and streams from the BeatDrop catalogue. No account needed.",
                color = TextMedium, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.weight(1f))
            // CTA
            Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("By continuing you agree to our Terms of Service", color = TextHint, fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }
}
