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
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun ChannelScreen(
    channelId: String = "",
    channelName: String = "Channel",
    channelThumb: String? = null,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
) {
    val channelTracks = SampleData.libraryTracks.take(6)

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 110.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(CoverGradients.get(2)), contentAlignment = Alignment.Center) {
                        Text(channelName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(channelName, style = MaterialTheme.typography.headlineMedium)
                    Text("${channelTracks.size} videos · BeatDrop Catalogue", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium), modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = {}, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.height(40.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Play All", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(onClick = {}, shape = CircleShape, color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Shuffle, null, tint = TextHigh, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
            itemsIndexed(channelTracks) { i, t ->
                TrackRow(
                    track = t.copy(isPlaying = false),
                    index = i,
                    onClick = onExpandPlayer,
                    onArtistClick = { onOpenArtist(t.artist) },
                    onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) },
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(channelName, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}
