package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun GenreDetailScreen(
    genreName: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onTrackClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 40.dp)) {
            item { SectionHeader(genreName, actionLabel = null); Spacer(Modifier.height(14.dp)) }
            item { AlbumCardRow(SampleData.recentlyPlayed, onOpenAlbum) }
            item { Spacer(Modifier.height(34.dp)) }
            item { SectionHeader("Top in $genreName", actionLabel = null); Spacer(Modifier.height(10.dp)) }
            items(SampleData.libraryTracks.size) { i ->
                val t = SampleData.libraryTracks[i]
                TrackRow(
                    track = t,
                    index = i,
                    onClick = onTrackClick,
                    onArtistClick = { onOpenArtist(t.artist) },
                    onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(genreName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
