package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
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
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.components.TrackRow
import com.beatdrop.kt.ui.theme.*

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit = {},
    onPlay: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
) {
    val album = SampleData.defaultAlbum

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(10.dp)).background(CoverGradients.get(album.coverIndex)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(72.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(album.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${album.artist} · Album",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text("${album.year} · ${album.songCount} songs · ${album.duration}", style = MaterialTheme.typography.bodySmall.copy(color = TextLow), modifier = Modifier.padding(top = 4.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Favorite, null, tint = Accent, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Download, null, tint = TextMedium, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Share, null, tint = TextMedium, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Shuffle, null, tint = TextMedium, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.weight(1f))
                    Surface(onClick = onPlay, shape = CircleShape, color = Accent, shadowElevation = 10.dp, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                    }
                }
            }

            itemsIndexed(album.tracks) { index, track ->
                TrackRow(
                    track = track,
                    index = index,
                    onClick = onPlay,
                    onArtistClick = { onOpenArtist(track.artist) },
                    onAlbumClick = {},
                )
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(album.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}
