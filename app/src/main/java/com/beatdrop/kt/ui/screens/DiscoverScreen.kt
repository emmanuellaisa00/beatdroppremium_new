package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.youtube.OnlineResult

/**
 * Home / Discover — rewritten from scratch to pixel-match the BeatDrop HTML
 * concept (Home screen).
 *
 * Layout:
 *   Top row:    profile avatar + green "All / Music / Podcasts" pills
 *   Quick grid: 2-column glass panel of 6 circular thumbs + title/sub rows
 *   "Jump back in" carousel — 168 dp square art + name/artist
 *   "More for you" carousel — same shape, different data
 */
@Composable
fun DiscoverScreen(
    vm: PlayerViewModel,
    onOpenSearch: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onExpandPlayer: () -> Unit = {},
    onOpenCollection: (com.beatdrop.kt.youtube.PlayableCollection) -> Unit = {},
) {
    val C = LocalAppColors.current
    val trending  by vm.cachedTrending.collectAsState()
    val popHits   by vm.cachedPopHits.collectAsState()
    val hiphopHits by vm.cachedHiphop.collectAsState()
    val madeForYou by vm.madeForYou.collectAsState()
    val current by vm.current.collectAsState()
    var filterIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        vm.getDiscoverData()
        vm.loadMadeForYou()
    }

    // Pre-compute the merged quick-access list at the composable level so we
    // don't call remember() from inside the LazyListScope content lambda
    // (which isn't @Composable).
    val quickItems = remember(trending, popHits, current) {
        val merged = (current?.let {
            listOf(QuickAccessItem(
                id = "current",
                title = it.title,
                subtitle = it.artist,
                artworkUri = it.artworkUri,
                ringAccent = true,
            ))
        } ?: emptyList()) +
        trending.take(3).map {
            QuickAccessItem(
                id = it.videoId,
                title = it.title,
                subtitle = it.author,
                artworkUri = it.thumbnailUrl,
            )
        } +
        popHits.take(2).map {
            QuickAccessItem(
                id = it.videoId,
                title = it.title,
                subtitle = it.author,
                artworkUri = it.thumbnailUrl,
                glyph = CoverGlyph.Disc,
            )
        }
        merged.take(6)
    }

    ScreenScaffold {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 220.dp),
        ) {
            // ── Top row: avatar + filter pills ────────────────────────────
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = PageHorizontalPadding, end = PageHorizontalPadding, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularGlassIcon(Ic.Person, "Profile", onClick = {})
                    FilterPillRow(
                        options = listOf("All", "Music", "Podcasts"),
                        selectedIndex = filterIndex,
                        onSelect = { filterIndex = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Quick access (2-col panel of recent / pinned items) ──────
            if (quickItems.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 18.dp)) {
                        QuickAccessPanel(
                            rows = quickItems,
                            onItemClick = { item ->
                                val r = (trending + popHits + hiphopHits).find { it.videoId == item.id }
                                if (r != null) {
                                    vm.playOnline(r)
                                    onExpandPlayer()
                                } else if (item.id == "current") {
                                    onExpandPlayer()
                                }
                            },
                        )
                    }
                }
            }

            // ── Jump back in ──────────────────────────────────────────────
            if (trending.isNotEmpty()) {
                item { SectionTitle("Jump back in", trailing = "See all", onTrailingClick = onOpenSearch) }
                item {
                    JumpCarousel(
                        cards = trending.take(10).map {
                            JumpCardData(
                                id = it.videoId,
                                title = it.title,
                                artist = it.author,
                                artworkUri = it.thumbnailUrl,
                            )
                        },
                        onClick = { c ->
                            val r = trending.find { it.videoId == c.id } ?: return@JumpCarousel
                            vm.playOnline(r)
                            onExpandPlayer()
                        },
                    )
                }
            }

            // ── Pop hits ──────────────────────────────────────────────────
            if (popHits.isNotEmpty()) {
                item { SectionTitle("Pop right now", trailing = "See all") }
                item {
                    JumpCarousel(
                        cards = popHits.take(10).map {
                            JumpCardData(
                                id = it.videoId,
                                title = it.title,
                                artist = it.author,
                                artworkUri = it.thumbnailUrl,
                                glyph = CoverGlyph.Disc,
                            )
                        },
                        onClick = { c ->
                            val r = popHits.find { it.videoId == c.id } ?: return@JumpCarousel
                            vm.playOnline(r)
                            onExpandPlayer()
                        },
                    )
                }
            }

            // ── Hip-hop ───────────────────────────────────────────────────
            if (hiphopHits.isNotEmpty()) {
                item { SectionTitle("Hip-hop heat", trailing = "See all") }
                item {
                    JumpCarousel(
                        cards = hiphopHits.take(10).map {
                            JumpCardData(
                                id = it.videoId,
                                title = it.title,
                                artist = it.author,
                                artworkUri = it.thumbnailUrl,
                            )
                        },
                        onClick = { c ->
                            val r = hiphopHits.find { it.videoId == c.id } ?: return@JumpCarousel
                            vm.playOnline(r)
                            onExpandPlayer()
                        },
                    )
                }
            }

            // ── Made for you collections (album/playlist tiles) ───────────
            if (madeForYou.isNotEmpty()) {
                item { SectionTitle("Made for you") }
                item {
                    JumpCarousel(
                        cards = madeForYou.take(10).map {
                            JumpCardData(
                                id = it.meta.playlistId,
                                title = it.meta.title,
                                artist = it.meta.subtitle,
                                artworkUri = it.coverUrl,
                            )
                        },
                        onClick = { c ->
                            val p = madeForYou.find { it.meta.playlistId == c.id } ?: return@JumpCarousel
                            onOpenCollection(
                                com.beatdrop.kt.youtube.PlayableCollection.Featured(p.meta)
                            )
                        },
                    )
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (trending.isEmpty() && popHits.isEmpty() && hiphopHits.isEmpty() && madeForYou.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, start = PageHorizontalPadding, end = PageHorizontalPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Loading discover…",
                            color = C.text.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kept for compatibility with MainActivity route Dest.LocalDiscover.
 * Local-library only, same layout vocabulary.
 */
@Composable
fun LocalDiscoverScreen(
    vm: PlayerViewModel,
    onBack: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    val C = LocalAppColors.current
    val tracks by vm.tracks.collectAsState()

    val recent = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(10) }
    val random = remember(tracks) { tracks.shuffled().take(10) }

    ScreenScaffold {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 220.dp),
        ) {
            item {
                HtmlHeader(
                    title = "Local Discover",
                    subtitle = "From your library",
                    actions = {
                        CircularGlassIcon(Ic.Search, "Search", onClick = onOpenSearch)
                        CircularGlassIcon(Ic.Close,  "Close",  onClick = onBack)
                    },
                )
            }
            if (recent.isNotEmpty()) {
                item { SectionTitle("Recently added") }
                item {
                    JumpCarousel(
                        cards = recent.map {
                            JumpCardData(it.id, it.title, it.artist, it.artworkUri)
                        },
                        onClick = { c ->
                            val t = recent.find { it.id == c.id } ?: return@JumpCarousel
                            vm.playList(recent, t.id)
                        },
                    )
                }
            }
            if (random.isNotEmpty()) {
                item { SectionTitle("Random mix") }
                item {
                    JumpCarousel(
                        cards = random.map {
                            JumpCardData(it.id, it.title, it.artist, it.artworkUri, glyph = CoverGlyph.Disc)
                        },
                        onClick = { c ->
                            val t = random.find { it.id == c.id } ?: return@JumpCarousel
                            vm.playList(random, t.id)
                        },
                    )
                }
            }
            if (recent.isEmpty() && random.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add music to your library to see local picks.",
                            color = C.text.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
