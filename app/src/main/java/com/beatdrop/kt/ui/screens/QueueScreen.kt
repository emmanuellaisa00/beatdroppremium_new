package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.TrackRow
import com.beatdrop.kt.ui.theme.*

@Composable
fun QueueScreen(
    onClose: () -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
) {
    val tracks = SampleData.libraryTracks // In production: from playback queue

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QueueMusic, null, tint = TextHint, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Queue is empty", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text("Play a song to start building your queue", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(52.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Up next", style = MaterialTheme.typography.headlineLarge)
                    TextButton(onClick = onClose) {
                        Text("Clear", style = MaterialTheme.typography.bodySmall.copy(color = TextLow, fontWeight = FontWeight.ExtraBold))
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(tracks) { i, t ->
                        TrackRow(
                            track = t,
                            index = i,
                            onClick = {},
                            onArtistClick = { onOpenArtist(t.artist) },
                            onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) },
                        )
                    }
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onClose)
            Text("Queue", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
