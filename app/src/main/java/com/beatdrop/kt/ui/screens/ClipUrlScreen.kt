package com.beatdrop.kt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.BeatDropError
import com.beatdrop.kt.data.BeatDropResult
import com.beatdrop.kt.data.safeCall
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.ErrorBanner
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke

private sealed class ResolveState {
    data object Loading : ResolveState()
    data class Resolved(val title: String, val artist: String) : ResolveState()
    data class Error(val error: BeatDropError) : ResolveState()
}

@Composable
fun ClipUrlScreen(
    url: String = "",
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit = {},
) {
    var state by remember { mutableStateOf<ResolveState>(ResolveState.Loading) }

    // Simulate resolver with error handling
    LaunchedEffect(url) {
        state = ResolveState.Loading
        val result = safeCall {
            kotlinx.coroutines.delay(1500)
            // Simulate: if URL is empty, throw
            if (url.isBlank()) throw IllegalArgumentException("Empty URL")
            // Simulate success
            "Imported Track" to "BeatDrop Catalogue"
        }
        state = when (result) {
            is BeatDropResult.Success -> ResolveState.Resolved(result.data.first, result.data.second)
            is BeatDropResult.Error -> ResolveState.Error(result.error)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 96.dp).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is ResolveState.Loading -> {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Resolving link…", color = TextMedium, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(url.take(60), color = TextLow, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }

                is ResolveState.Resolved -> {
                    val (title, artist) = state as ResolveState.Resolved
                    Box(
                        modifier = Modifier.size(120.dp)
                            .background(CoverGradients.get(3), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Link, null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    Text(artist, style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium), modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onExpandPlayer,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is ResolveState.Error -> {
                    val error = (state as ResolveState.Error).error
                    Box(
                        modifier = Modifier.size(80.dp)
                            .background(Color(0xFF2A1015), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.LinkOff, null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Couldn't resolve link",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error.message,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                    )
                    Spacer(Modifier.height(28.dp))
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceTile,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text("Go back", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Import", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
