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
fun StatsScreen(onBack: () -> Unit = {}) {
    val stats = listOf(
        "Total listening time" to "142 hours",
        "Songs played" to "1,847",
        "Unique artists" to "213",
        "Most played track" to "4×4 — Don Toliver",
        "Most played artist" to "Travis Scott",
        "Most played genre" to "Hip-Hop",
        "Downloads" to "89 songs",
    )

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 96.dp).padding(horizontal = 20.dp)) {
            Text("Your listening stats", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Last 30 days", color = TextMedium, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            stats.forEach { (label, value) ->
                Surface(
                    shape = RoundedCornerShape(14.dp), color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = TextMedium, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Stats", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
