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
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun OnlineAlbumScreen(
    collectionId: String = "",
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenChannel: (String) -> Unit = {},
) {
    val album = SampleData.defaultAlbum
    var isLiked by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(210.dp).clip(RoundedCornerShape(16.dp)).background(CoverGradients.get(1)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(72.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(album.title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${album.artist} · Album · ${album.year}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text("${album.songCount} songs · ${album.duration}", style = MaterialTheme.typography.bodySmall.copy(color = TextLow), modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(onClick = { isLiked = !isLiked }, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (isLiked) Accent else TextHigh, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Download, null, tint = TextHigh, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Share, null, tint = TextHigh, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(onClick = onExpandPlayer, shape = CircleShape, color = Accent, shadowElevation = 12.dp, modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }
            }
            itemsIndexed(album.tracks) { i, t ->
                TrackRow(t, i, onExpandPlayer, onArtistClick = { onOpenArtist(t.artist) }, onAlbumClick = {})
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(album.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}
