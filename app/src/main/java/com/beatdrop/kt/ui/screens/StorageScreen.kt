package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*
import androidx.compose.ui.draw.clip

@Composable
fun StorageScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 96.dp).padding(horizontal = 20.dp)) {
            Text("Storage", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(24.dp))
            // Storage bar
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceTile) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Used: 2.4 GB", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Free: 28.6 GB", color = TextMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 0.08f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Accent,
                        trackColor = SurfaceTile,
                    )
                    Spacer(Modifier.height(20.dp))
                    listOf(
                        Triple("Downloads", "1.8 GB", 0.75f),
                        Triple("Cache", "0.4 GB", 0.17f),
                        Triple("Other", "0.2 GB", 0.08f)
                    ).forEach { (label, size, _) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = TextMedium, fontWeight = FontWeight.Medium)
                            Text(size, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Clear Cache", fontWeight = FontWeight.Bold)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Text("Storage", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
