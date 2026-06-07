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

@Composable
fun EqScreen(onBack: () -> Unit) {
    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val gains = remember { mutableStateListOf(0.5f, 0.7f, 0.6f, 0.4f, 0.8f) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 96.dp).padding(horizontal = 20.dp)) {
            Text("Equalizer", style = MaterialTheme.typography.headlineLarge.copy(color = Color.White))
            Spacer(Modifier.height(8.dp))
            Text("Adjust audio frequencies", color = TextMedium, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                bands.forEachIndexed { i, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Slider(value = gains[i], onValueChange = { gains[i] = it }, modifier = Modifier.height(160.dp).width(40.dp), colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
                        Spacer(Modifier.height(8.dp))
                        Text(label, color = TextLow, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Text("Equalizer", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
