package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.*

@Composable
fun RadioScreen(
    vm: Any? = null,
    onExpandPlayer: () -> Unit = {},
) {
    // Auto-mix stations based on listening history
    val stations = listOf(
        "Chill Mix" to "Based on your listening",
        "Hip-Hop Radio" to "Don Toliver, Travis Scott & more",
        "R&B Vibes" to "Smooth and soulful",
        "Workout Energy" to "High BPM bangers",
        "Late Night Drive" to "Ambient & atmospheric",
        "Throwback Hits" to "2010s throwbacks",
    )

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background).padding(bottom = 200.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                IdentityBar("Radio", "")
                Spacer(Modifier.height(22.dp))
                Text(
                    buildAnnotatedString {
                        append("Auto ")
                        withStyle(
                            SpanStyle(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Accent, Color(0xFFff7a94))
                                )
                            )
                        ) {
                            append("Mix")
                        }
                    },
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Stations tailored to your taste",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        items(count = stations.size) { i ->
            val (name, desc) = stations[i]
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).clickable { onExpandPlayer() },
                shape = RoundedCornerShape(16.dp), color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.size(46.dp).background(CoverGradients.get(i + 1), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Radio, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                        Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = TextLow), modifier = Modifier.padding(top = 2.dp))
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(alpha = 0.35f))
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
