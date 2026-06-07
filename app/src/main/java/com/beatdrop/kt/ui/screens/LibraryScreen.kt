package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun LibraryScreen(
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onTrackClick: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Playlists", "Albums", "Artists", "Downloaded", "Recently played")

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background).padding(bottom = 200.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                IdentityBar("Good evening,", "Alex", onSearch = onOpenSearch)
                Spacer(Modifier.height(22.dp))
                Text(
                    buildAnnotatedString {
                        append("Your ")
                        withStyle(SpanStyle(color = Accent)) { append("Library") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            FilterPills(
                pills = filters,
                selectedIndex = filterIndex,
                onSelected = { filterIndex = it },
            )
        }
        item { Spacer(Modifier.height(22.dp)); QuickAccessGrid(SampleData.libraryQuickAccess, onOpenAlbum) }
        item {
            Spacer(Modifier.height(34.dp)); SectionHeader("Recently played")
            Spacer(Modifier.height(14.dp)); AlbumCardRow(SampleData.recentlyPlayed, onOpenAlbum)
        }
        item {
            Spacer(Modifier.height(34.dp)); SectionHeader("From your library", "Shuffle", onAction = onTrackClick)
            Spacer(Modifier.height(4.dp))
        }
        itemsIndexed(SampleData.libraryTracks) { i, t ->
            TrackRow(
                track = t,
                index = i,
                onClick = onTrackClick,
                onArtistClick = { onOpenArtist(t.artist) },
                onAlbumClick = { if (t.album.isNotEmpty()) onOpenAlbum(t.album) },
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
