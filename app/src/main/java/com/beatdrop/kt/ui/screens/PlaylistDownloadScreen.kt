package com.beatdrop.kt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.BeatDropError
import com.beatdrop.kt.data.BeatDropResult
import com.beatdrop.kt.data.safeCall
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.ErrorBanner
import com.beatdrop.kt.ui.theme.*

@Composable
fun PlaylistDownloadScreen(
    playlistId: String = "",
    onBack: () -> Unit,
) {
    val tracks = listOf("4×4", "Inside", "Glock In My Purse", "Bus Stop", "Hardstone Psycho", "Ice Age", "Brother Stone")
    val downloaded = remember { mutableStateListOf(*Array(tracks.size) { false }) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isComplete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            try {
                while (progress < 1f) {
                    kotlinx.coroutines.delay(300)
                    progress += 0.08f

                    val nextIndex = downloaded.indexOf(false)
                    if (nextIndex >= 0 && progress >= (nextIndex + 1).toFloat() / tracks.size) {
                        // Simulate occasional download failure for a track
                        if (nextIndex == 4 && downloaded.none { !it && tracks.indexOf(tracks[nextIndex]) < nextIndex }) {
                            // Track 5 might fail — 10% chance
                            if ((0..9).random() == 0) {
                                errorMessage = "\"${tracks[nextIndex]}\" failed to download"
                                progress = (nextIndex + 1).toFloat() / tracks.size
                                continue
                            }
                        }
                        downloaded[nextIndex] = true
                    }

                    progress = progress.coerceAtMost(1f)
                }
                isComplete = downloaded.all { it }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Download interrupted"
            }
            isDownloading = false
            progress = 1f
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Error banner
            androidx.compose.animation.AnimatedVisibility(
                visible = errorMessage != null,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
            ) {
                if (errorMessage != null) {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null },
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 96.dp, bottom = 120.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(tracks.indices.toList()) { i ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(CoverGradients.get(1)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tracks[i], fontWeight = FontWeight.SemiBold, color = Color.White)
                            when {
                                downloaded[i] -> Text("Downloaded", color = Accent, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                                isDownloading && i == downloaded.indexOf(false) -> Text("Downloading…", color = TextMedium, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (downloaded[i]) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Accent, modifier = Modifier.size(22.dp))
                        } else if (isDownloading && i == downloaded.indexOf(false)) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Accent, strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        // Download button
        if (!isDownloading && !isComplete) {
            Button(
                onClick = { isDownloading = true; progress = 0f },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
                    .align(Alignment.BottomCenter).padding(bottom = 40.dp),
            ) { Text("Download All", fontWeight = FontWeight.Bold) }
        }

        // Progress
        if (isDownloading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .align(Alignment.BottomCenter).padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Accent, trackColor = SurfaceTile,
                )
                Spacer(Modifier.height(8.dp))
                Text("${(progress * 100).toInt().coerceIn(0, 100)}%", color = TextMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Complete state
        if (isComplete) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0D2818),
                border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
                    .align(Alignment.BottomCenter).padding(bottom = 40.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("All downloads complete", fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Download Playlist", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
