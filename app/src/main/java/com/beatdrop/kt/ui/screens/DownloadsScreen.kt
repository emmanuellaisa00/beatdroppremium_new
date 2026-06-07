package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.TrackRow
import com.beatdrop.kt.ui.theme.*

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
) {
    val tracks = SampleData.albumTracks // In production: from ViewModel

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (tracks.isEmpty()) {
            // Empty state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Download, null, tint = TextHint, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No downloads yet", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text("Songs you download will appear here", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium))
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceTile,
                        border = BorderStroke(1.dp, GlassBorder),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Text("Browse music", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 40.dp)) {
                item { SectionHeader("Downloaded (${tracks.size} songs)", actionLabel = null) }
                item { Spacer(Modifier.height(10.dp)) }
                itemsIndexed(tracks) { i, t ->
                    TrackRow(
                        track = t,
                        index = i,
                        onClick = onTrackClick,
                        onArtistClick = { onOpenArtist(t.artist) },
                        onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Downloads", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
