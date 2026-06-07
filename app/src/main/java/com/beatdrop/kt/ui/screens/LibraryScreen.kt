package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Library — rewritten from scratch to pixel-match the BeatDrop HTML concept.
 *
 * Layout (top to bottom):
 *   HtmlHeader           "BeatDrop" + "Your music everywhere" + 3 circular glass icons
 *   Search bar           Full-width 52 dp pill, hands off to a real input on focus
 *   Stats chips          3 floating glass tiles (Songs / Albums / Artists)
 *   Segmented control    Songs / Albums / Artists with sliding inset orb
 *   Action cards         2x2 grid (Play All / Shuffle / Favorites / Downloads)
 *   Recently Played      Horizontal carousel of 168 dp jump cards
 *   Songs list           Vertical list of SongRows
 */
@Composable
fun LibraryScreen(
    vm: PlayerViewModel,
    onOpenAlbum: (String, String) -> Unit = { _, _ -> },
    onOpenArtist: (String) -> Unit = {},
    onOpenLocalDiscover: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val C       = LocalAppColors.current
    val query   by vm.query.collectAsState()
    val loaded  by vm.loaded.collectAsState()
    val tracks  by vm.tracks.collectAsState()
    val current by vm.current.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }

    // Pre-compute album & artist counts from the loaded tracks (no extra fetches).
    val albumCount = remember(tracks) { tracks.map { it.album }.distinct().size }
    val artistCount = remember(tracks) { tracks.map { it.artist }.distinct().size }
    val recentlyPlayed = remember(tracks) {
        tracks.sortedByDescending { it.dateAdded }.take(10)
    }
    val albumsGrouped = remember(tracks) {
        tracks.groupBy { it.album }
            .filterKeys { it.isNotBlank() }
            .map { (album, ts) -> Triple(album, ts.first().artist, ts.first().artworkUri) }
            .sortedBy { it.first.lowercase() }
    }
    val artistsGrouped = remember(tracks) {
        tracks.groupBy { it.artist }
            .filterKeys { it.isNotBlank() }
            .map { (artist, ts) -> artist to ts.size }
            .sortedBy { it.first.lowercase() }
    }

    ScreenScaffold {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────
            item {
                HtmlHeader(
                    title = "BeatDrop",
                    subtitle = "Your music everywhere",
                    actions = {
                        CircularGlassIcon(Ic.Playlist,  "Playlists",  onClick = onOpenPlaylists)
                        CircularGlassIcon(Ic.Download,  "Downloads",  onClick = onOpenDownloads)
                        CircularGlassIcon(Ic.Settings,  "Settings",   onClick = onOpenSettings)
                    },
                )
            }

            // ── Search ────────────────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 6.dp)) {
                    BeatDropSearchField(
                        value = query,
                        onChange = vm::setQuery,
                        placeholder = "Search songs, albums, artists",
                        onSubmit = null,
                    )
                }
            }

            // ── Stats chips ───────────────────────────────────────────────
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatChip("${tracks.size}",  "Songs",   Modifier.weight(1f))
                    StatChip("$albumCount",     "Albums",  Modifier.weight(1f), onClick = { tabIndex = 1 })
                    StatChip("$artistCount",    "Artists", Modifier.weight(1f), onClick = { tabIndex = 2 })
                }
            }

            // ── Segmented control ─────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = PageHorizontalPadding)) {
                    SegmentedGlass(
                        options = listOf("Songs", "Albums", "Artists"),
                        selectedIndex = tabIndex,
                        onSelect = { tabIndex = it },
                    )
                }
            }

            // ── Action cards (only on the Songs tab — matches HTML) ──────
            if (tabIndex == 0) {
                item {
                    Column(
                        Modifier.padding(horizontal = PageHorizontalPadding, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionCardSmall(
                                icon = Ic.TransportPlay, filledIcon = true,
                                title = "Play All",
                                subtitle = "${tracks.size} songs",
                                onClick = { if (tracks.isNotEmpty()) vm.playList(tracks, tracks.first().id) },
                                modifier = Modifier.weight(1f),
                            )
                            ActionCardSmall(
                                icon = Ic.Shuffle,
                                title = "Shuffle",
                                subtitle = "Mix it up",
                                onClick = {
                                    if (tracks.isNotEmpty()) {
                                        val shuf = tracks.shuffled()
                                        vm.playList(shuf, shuf.first().id)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionCardSmall(
                                icon = Ic.Heart,
                                title = "Favorites",
                                subtitle = "Your liked songs",
                                onClick = onOpenPlaylists,
                                modifier = Modifier.weight(1f),
                            )
                            ActionCardSmall(
                                icon = Ic.Download,
                                title = "Downloads",
                                subtitle = "Offline tracks",
                                onClick = onOpenDownloads,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ── Recently Played carousel (only on the Songs tab) ─────────
            if (tabIndex == 0 && recentlyPlayed.isNotEmpty()) {
                item {
                    SectionTitle("Recently Played", trailing = "See all", onTrailingClick = onOpenLocalDiscover)
                }
                item {
                    JumpCarousel(
                        cards = recentlyPlayed.map {
                            JumpCardData(
                                id = it.id,
                                title = it.title,
                                artist = it.artist,
                                artworkUri = it.artworkUri,
                                glyph = if (it.id.hashCode() % 2 == 0) CoverGlyph.Note else CoverGlyph.Disc,
                            )
                        },
                        onClick = { c ->
                            val t = recentlyPlayed.find { it.id == c.id } ?: return@JumpCarousel
                            vm.playList(recentlyPlayed, t.id)
                        },
                    )
                }
            }

            // ── List content for the current tab ──────────────────────────
            when {
                !loaded -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = C.accent) }
                }
                tracks.isEmpty() -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No songs in your library yet.",
                            color = C.text.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                tabIndex == 0 -> {
                    item { SectionTitle("Songs", trailing = "${tracks.size} tracks") }
                    items(tracks, key = { it.id }) { t ->
                        val active = current?.id == t.id
                        Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 5.dp)) {
                            SongRow(
                                title = t.title,
                                artist = if (t.album.isNotBlank()) "${t.artist} · ${t.album}" else t.artist,
                                duration = formatMs(t.durationMs),
                                artworkUri = t.artworkUri,
                                active = active,
                                onClick = { vm.playList(tracks, t.id) },
                                onMenu = { /* future: action sheet */ },
                            )
                        }
                    }
                }
                tabIndex == 1 -> {
                    val albums = albumsGrouped
                    item { SectionTitle("Albums", trailing = "${albums.size} albums") }
                    itemsIndexed(albums.chunked(2)) { _, pair ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = PageHorizontalPadding, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pair.forEach { (album, artist, art) ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .pressableScale(onClick = { onOpenAlbum(album, artist) }, scaleTo = 0.96f),
                                ) {
                                    MonochromeCover(
                                        artworkUri = art,
                                        cornerRadius = 16.dp,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        album,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = C.text,
                                        maxLines = 1,
                                    )
                                    Text(
                                        artist,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = C.text.copy(alpha = 0.50f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                else -> {
                    val artists = artistsGrouped
                    item { SectionTitle("Artists", trailing = "${artists.size} artists") }
                    items(artists) { (artist, count) ->
                        Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 5.dp)) {
                            SongRow(
                                title = artist,
                                artist = if (count == 1) "1 song" else "$count songs",
                                duration = "",
                                artworkUri = null,
                                onClick = { onOpenArtist(artist) },
                                onMenu = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
