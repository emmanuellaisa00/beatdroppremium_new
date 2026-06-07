package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.TrackRow
import com.beatdrop.kt.ui.theme.*

@Composable
fun ArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    onPlay: () -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
) {
    val artistTracks = SampleData.libraryTracks.filter { it.artist.contains(artistName, ignoreCase = true) }
    val albums = SampleData.recentlyPlayed.filter { it.artist.equals(artistName, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(CoverGradients.get(1)), contentAlignment = Alignment.Center) {
                        Text(artistName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 42.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(artistName, style = MaterialTheme.typography.headlineMedium)
                    Text("${artistTracks.size} songs · ${albums.size} albums", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium), modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = onPlay, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.height(44.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.FavoriteBorder, null, tint = TextHigh, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Share, null, tint = TextHigh, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { Spacer(Modifier.height(10.dp)); SectionHeader("Albums"); Spacer(Modifier.height(14.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                        albums.forEach { album ->
                            Column(modifier = Modifier.width(154.dp).clickable { onOpenAlbum(album.id) }) {
                                Box(modifier = Modifier.size(154.dp).clip(RoundedCornerShape(12.dp)).background(CoverGradients.get(album.coverIndex)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Album, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(56.dp))
                                }
                                Text(album.title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                                Text(album.artist, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextLow, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
            if (artistTracks.isNotEmpty()) {
                item { Spacer(Modifier.height(34.dp)); SectionHeader("Popular", actionLabel = null); Spacer(Modifier.height(4.dp)) }
                itemsIndexed(artistTracks) { i, t ->
                    TrackRow(t, i, onPlay, onArtistClick = { onOpenArtist(t.artist) }, onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) })
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text(artistName, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}
