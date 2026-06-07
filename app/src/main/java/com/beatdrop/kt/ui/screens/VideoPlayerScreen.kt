package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*

@Composable
fun VideoPlayerScreen(
    videoPath: String = "",
    title: String = "Video",
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Placeholder for actual video surface
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.PlayCircleFilled, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text("Video Player", color = Color.White.copy(alpha = 0.50f), fontWeight = FontWeight.Bold)
            Text(title, color = Color.White.copy(alpha = 0.30f), style = MaterialTheme.typography.bodyMedium)
        }
        // Controls overlay
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
