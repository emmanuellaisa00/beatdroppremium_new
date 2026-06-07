package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

@Composable
fun PermissionPrompt(onRequest: () -> Unit) {
    val C = LocalAppColors.current
    ScreenScaffold(ambientColor = C.glassAmbient) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Column(Modifier.fillMaxWidth().glassCard(radius = Radius.xl).padding(28.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Ic.MusicNote, null, tint = C.accent, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Allow access to your music", color = C.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("BeatDrop reads audio files on your device to build your library.",
            color = C.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("Grant permission") }
    }
    }
    }
}
