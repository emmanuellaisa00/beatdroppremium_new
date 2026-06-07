package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun SearchScreen(
    initialQuery: String = "",
    onSearch: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onOpenGenre: (String) -> Unit = {},
    onOpenOnlineCollection: (String) -> Unit = {},
    onTrackClick: () -> Unit = {},
) {
    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    // Search results (filtered from sample data)
    val matchedSongs = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.libraryTracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }
    val matchedAlbums = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.recentlyPlayed.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }
    val matchedArtists = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.recentlyPlayed.map { it.artist }.distinct().filter {
            it.contains(query, true)
        }
    }
    val matchedPlaylists = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.homeQuickAccess.filter {
            it.title.contains(query, true)
        }
    }
    val hasResults = matchedSongs.isNotEmpty() || matchedAlbums.isNotEmpty() || matchedArtists.isNotEmpty() || matchedPlaylists.isNotEmpty()
    val isSearching = query.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(bottom = 200.dp)) {
        Spacer(Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Search", style = MaterialTheme.typography.displayLarge)
        }

        Spacer(Modifier.height(20.dp))

        // Search bar
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            color = SurfaceTile,
            border = BorderStroke(1.dp, if (isSearching) Accent.copy(alpha = 0.4f) else GlassBorder),
            shadowElevation = if (isSearching) 8.dp else 4.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp)) {
                Icon(Icons.Filled.Search, null, tint = if (isSearching) Accent else TextLow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Artists, songs, albums, playlists…",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextHint, fontWeight = FontWeight.Medium),
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearch(it)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
                if (isSearching) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextMedium, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (isSearching) {
            // ── Search Results ──
            Spacer(Modifier.height(20.dp))

            if (!hasResults) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, null, tint = TextHint, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No results for \"$query\"", style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium))
                        Text("Try different keywords", style = MaterialTheme.typography.bodySmall.copy(color = TextLow), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // ── Top Results: Songs ──
                    if (matchedSongs.isNotEmpty()) {
                        item {
                            SectionHeader("Songs", actionLabel = if (matchedSongs.size > 3) "See all" else null)
                            Spacer(Modifier.height(6.dp))
                        }
                        items(matchedSongs.take(5)) { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onTrackClick)
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(CoverGradients.get(track.coverIndex)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (track.isPlaying) Accent else Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${track.artist}${if (track.album.isNotEmpty()) " · ${track.album}" else ""}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextLow),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Text(track.duration, style = MaterialTheme.typography.bodySmall.copy(color = TextHint))
                            }
                        }
                    }

                    // ── Artists ──
                    if (matchedArtists.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            SectionHeader("Artists")
                            Spacer(Modifier.height(10.dp))
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                            ) {
                                items(matchedArtists) { name ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(100.dp).clickable { onOpenArtist(name) },
                                    ) {
                                        Box(
                                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                                .background(CoverGradients.get(kotlin.math.abs(name.hashCode()) % 8 + 1)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                name.firstOrNull()?.uppercase() ?: "?",
                                                color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp,
                                            )
                                        }
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold, color = TextHigh,
                                            ),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Albums ──
                    if (matchedAlbums.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            SectionHeader("Albums")
                            Spacer(Modifier.height(10.dp))
                        }
                        item {
                            AlbumCardRow(matchedAlbums, onOpenAlbum)
                        }
                    }

                    // ── Playlists ──
                    if (matchedPlaylists.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            SectionHeader("Playlists")
                            Spacer(Modifier.height(10.dp))
                        }
                        items(matchedPlaylists) { playlist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlaylist(playlist.id) }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                                        .background(CoverGradients.get(playlist.coverIndex)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.QueueMusic, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (playlist.subtitle.isNotBlank()) {
                                        Text(playlist.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextLow), modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Text("›", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(alpha = 0.35f))
                            }
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        } else {
            // ── Browse (no query) ──
            Spacer(Modifier.height(24.dp))
            SectionHeader("Top genres")
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 450.dp),
            ) {
                items(SampleData.genres) { genre ->
                    BrowseTile(genre.title, genre.tileIndex) { onOpenGenre(genre.title) }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Browse all")
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 250.dp),
            ) {
                items(SampleData.browseAll) { genre ->
                    BrowseTile(genre.title, genre.tileIndex) { onOpenOnlineCollection("browse_${genre.title}") }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
