package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke

@Composable
fun PlaylistsScreen(
    onBack: () -> Unit = {},
    onOpen: (String) -> Unit = {},
) {
    val playlists = listOf(
        "Liked Songs" to 42,
        "Workout Bangers" to 18,
        "Late Night Vibes" to 31,
        "My Playlist #4" to 14,
        "Road Trip" to 26,
        "Throwbacks" to 53,
    )

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 40.dp)) {
            // Create new button
            item {
                Surface(
                    onClick = { /* create playlist dialog */ },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp), color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Box(modifier = Modifier.size(46.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Create a Playlist", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
            itemsIndexed(playlists) { _, (name, count) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(name) }.padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Box(modifier = Modifier.size(52.dp).background(CoverGradients.get(playlists.indexOf(name) + 1), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QueueMusic, null, tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("$count songs", color = TextLow, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text("›", color = Color.White.copy(alpha = 0.35f), fontWeight = FontWeight.Light, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Playlists", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistName: String = "Playlist",
    onBack: () -> Unit = {},
    onTrackClick: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
) {
    val tracks = com.beatdrop.kt.data.SampleData.albumTracks
    var isShuffled by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(180.dp).background(CoverGradients.get(3), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QueueMusic, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(playlistName, style = MaterialTheme.typography.headlineMedium)
                    Text("${tracks.size} songs", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium), modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = onTrackClick, shape = CircleShape, color = Accent, shadowElevation = 12.dp, modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Surface(onClick = { isShuffled = !isShuffled }, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Shuffle, null, tint = if (isShuffled) Accent else TextHigh, modifier = Modifier.size(18.dp)) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Download, null, tint = TextHigh, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
            items(tracks.size) { i ->
                com.beatdrop.kt.ui.components.TrackRow(
                    track = tracks[i],
                    index = i,
                    onClick = onTrackClick,
                    onArtistClick = { onOpenArtist(tracks[i].artist) },
                    onAlbumClick = { if (tracks[i].album.isNotEmpty()) onOpenAlbum(tracks[i].album) },
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(playlistName, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}
