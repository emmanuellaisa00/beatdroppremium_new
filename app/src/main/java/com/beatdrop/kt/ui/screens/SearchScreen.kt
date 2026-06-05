package com.beatdrop.kt.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.ui.components.BeatDropSearchField
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.DownloadStatus
import com.beatdrop.kt.youtube.OnlineResult

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Search Screen
// Accent: #21FF6B (Spotify Green)
// Glass search bar with blur 28px
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SearchScreen(
    vm: PlayerViewModel,
    onExpandPlayer: () -> Unit = {},
    onOpenOnlineAlbum: (com.beatdrop.kt.youtube.OnlineAlbum) -> Unit = {},
) {
    val C = LocalAppColors.current
    val q          by vm.onlineQuery.collectAsState()
    val results    by vm.onlineResults.collectAsState()
    val albums     by vm.albumResults.collectAsState()
    val playlists  by vm.playlistResults.collectAsState()
    val searching  by vm.isSearching.collectAsState()
    val message    by vm.onlineMessage.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val history    by vm.searchHistory.collectAsState()
    val jobs       by vm.downloadJobs.collectAsState()

    // Filter chips state — defaults to ALL so the first paint shows the
    // full Albums + Playlists + Songs stack. Reset to ALL whenever a new
    // search query is submitted so the chip doesn't 'stick' across
    // unrelated queries.
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    LaunchedEffect(q) { filter = SearchFilter.ALL }

    var snackbarMessage   by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val listState          = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) keyboardController?.hide()
    }

    val lastFailed by vm.lastFailedOnline.collectAsState()

    LaunchedEffect(message) {
        message?.let { msg ->
            val result = if (lastFailed != null) {
                snackbarHostState.showSnackbar(msg, actionLabel = "Retry")
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            if (result == SnackbarResult.ActionPerformed) vm.retryOnlinePlay()
            vm.clearOnlineMessage()
        }
    }

    ScreenScaffold {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                "Browse",
                color     = C.text,
                fontSize  = 28.sp,
                fontWeight = FontWeight.Black,
                modifier  = Modifier.padding(vertical = 10.dp),
            )

            // ── Offline banner ──────────────────────────────────────────────
            val isOnline = com.beatdrop.kt.util.NetworkMonitor.isOnline.value
            if (!isOnline) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3CD))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Ic.WifiOff, null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("You're offline. Search results won't load.", color = Color(0xFF856404), fontSize = 12.sp)
                }
            }

            // ── Unified search field ─────────────────────────────────────────
            // Replaces the legacy OutlinedTextField inside glassRow which
            // (a) used C.textTertiary (38% white) for the icon and placeholder
            // — invisible on the glass surface — and (b) had no focus state.
            // The shared BeatDropSearchField is opaque, has an accent focus
            // ring, an explicit clear button, and a green "Search" submit pill.
            BeatDropSearchField(
                value = q,
                onChange = {
                    vm.setOnlineQuery(it)
                    // Two suggestion streams: cheap autocomplete strings
                    // (fires immediately) + debounced rich typed rows
                    // (250ms debounce inside the VM).
                    if (it.length >= 2) {
                        vm.loadSuggestions()
                        vm.loadLiveSuggestions()
                    }
                },
                placeholder = "Search songs, artists, albums…",
                onSubmit = { vm.runOnlineSearch() },
                submitting = searching,
            )

            val liveSuggestions by vm.liveSuggestions.collectAsState()

            // ── Search history or rich typed suggestions ────────────────────
            val showHistory      = q.isEmpty() && history.isNotEmpty() && results.isEmpty()
            val showLiveTyped    = q.isNotEmpty() && liveSuggestions.isNotEmpty() && results.isEmpty()
            val showSuggestions  = q.isNotEmpty() && suggestions.isNotEmpty() && liveSuggestions.isEmpty() && results.isEmpty()

            // ── Rich typed suggestions (Apple Music / Spotify iOS style) ────
            // When the user is typing AND we have live typed results AND
            // they haven't submitted yet, show inline rich rows: 40dp
            // thumbnail + title + type label. Tap = instant (no submit).
            AnimatedVisibility(visible = showLiveTyped) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(liveSuggestions, key = { s ->
                        when (s) {
                            is PlayerViewModel.LiveSuggestion.Song     -> "s:${s.result.videoId}"
                            is PlayerViewModel.LiveSuggestion.Album    -> "a:${s.album.browseId}"
                            is PlayerViewModel.LiveSuggestion.Playlist -> "p:${s.playlist.playlistId}"
                        }
                    }) { suggestion ->
                        LiveSuggestionRow(
                            suggestion = suggestion,
                            onClick = {
                                when (suggestion) {
                                    is PlayerViewModel.LiveSuggestion.Song -> {
                                        // Instant play.
                                        vm.prepareAndPlayOnline(suggestion.result,
                                            liveSuggestions.filterIsInstance<PlayerViewModel.LiveSuggestion.Song>()
                                                .map { it.result },
                                            0,
                                        )
                                        onExpandPlayer()
                                    }
                                    is PlayerViewModel.LiveSuggestion.Album -> {
                                        onOpenOnlineAlbum(suggestion.album)
                                    }
                                    is PlayerViewModel.LiveSuggestion.Playlist -> {
                                        vm.playFeaturedPlaylist(suggestion.playlist.playlistId)
                                        onExpandPlayer()
                                    }
                                }
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showHistory || showSuggestions) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier       = Modifier.heightIn(max = 260.dp),
                ) {
                    if (showHistory) {
                        item {
                            Text(
                                "Recent Searches",
                                color     = C.textSecondary,
                                fontSize  = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier  = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
                            )
                        }
                        items(history) { query ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(query)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.History, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    query, color = C.text, fontSize = 15.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick  = { vm.deleteHistoryQuery(query) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Ic.Close, "Delete", tint = C.textTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = C.separator, thickness = 0.5.dp)
                        }
                    } else if (showSuggestions) {
                        items(suggestions) { suggestion ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(suggestion)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(suggestion, color = C.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = C.separator, thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                // No spinner — content-shaped silhouettes with accent shimmer
                // sweep instead. The shape matches CatalogRow so there's no
                // layout shift when real results arrive.
                searching -> com.beatdrop.kt.ui.components.SearchResultSilhouettes(
                    rowCount = 6,
                    modifier = Modifier.padding(top = 4.dp),
                )
                results.isNotEmpty() || albums.isNotEmpty() || playlists.isNotEmpty() -> {
                    // ── Filter chips: All · Songs · Albums · Playlists ──────
                    val showSongs     = filter == SearchFilter.ALL || filter == SearchFilter.SONGS
                    val showAlbums    = filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS
                    val showPlaylists = filter == SearchFilter.ALL || filter == SearchFilter.PLAYLISTS
                    SearchFilterChips(
                        selected = filter,
                        songCount = results.size,
                        albumCount = albums.size,
                        playlistCount = playlists.size,
                        onSelect = { filter = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        state          = listState,
                        contentPadding = PaddingValues(bottom = 160.dp),
                    ) {
                        // ── Top Result hero card (Spotify / Apple Music) ────
                        // Only when no specific section is filtered. Picks the
                        // single best match across all three result types:
                        //   1. exact-title album match (most relevant)
                        //   2. first album in the list (cover-bearing)
                        //   3. first song
                        // Tap behaviour matches the type: album → detail,
                        // song → instant play.
                        if (filter == SearchFilter.ALL) {
                            val topAlbum = albums.firstOrNull { it.title.equals(q, ignoreCase = true) }
                                ?: albums.firstOrNull()
                            val topSong = results.firstOrNull()
                            if (topAlbum != null || topSong != null) {
                                item {
                                    Text(
                                        "Top Result",
                                        color = C.text,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp, start = 4.dp),
                                    )
                                }
                                item {
                                    if (topAlbum != null) {
                                        TopResultHero(
                                            title = topAlbum.title,
                                            subtitle = buildString {
                                                append("Album")
                                                if (topAlbum.artist.isNotBlank()) append(" · ").append(topAlbum.artist)
                                            },
                                            thumbnailUrl = topAlbum.thumbnailUrl,
                                            onClick = { onOpenOnlineAlbum(topAlbum) },
                                        )
                                    } else if (topSong != null) {
                                        TopResultHero(
                                            title = topSong.title,
                                            subtitle = "Song · ${topSong.author}",
                                            thumbnailUrl = topSong.thumbnailUrl,
                                            onClick = {
                                                vm.prepareAndPlayOnline(topSong, results, 0)
                                                onExpandPlayer()
                                            },
                                        )
                                    }
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }
                        // ── Albums section (Spotify-style horizontal carousel) ──
                        if (showAlbums && albums.isNotEmpty()) {
                            item {
                                SectionEyebrow("Albums", count = albums.size)
                            }
                            item {
                                AlbumCarousel(
                                    albums = albums,
                                    onOpen = onOpenOnlineAlbum,
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        // ── Playlists section ──────────────────────────────────
                        if (showPlaylists && playlists.isNotEmpty()) {
                            item {
                                SectionEyebrow("Playlists", count = playlists.size)
                            }
                            item {
                                PlaylistCarousel(
                                    playlists = playlists,
                                    onOpen = { pl ->
                                        // Reuse playFeaturedPlaylist plumbing — it
                                        // starts playback and sets the onlineContext.
                                        vm.playFeaturedPlaylist(pl.playlistId)
                                        onExpandPlayer()
                                    },
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        // ── Songs header ───────────────────────────────────────
                        if (showSongs && results.isNotEmpty()) {
                            item {
                                SectionEyebrow("Songs", count = results.size)
                            }
                        }
                        if (showSongs) itemsIndexed(results, key = { _, r -> r.videoId }) { idx, r ->
                            val job = jobs[r.videoId]
                            // Predictive prefetch — if the row stays on
                            // screen >400 ms (i.e. user isn't fly-scrolling
                            // past it), kick off a background stream
                            // resolution so the next tap is ~instant.
                            LaunchedEffect(r.videoId) {
                                kotlinx.coroutines.delay(400)
                                vm.prefetchOnlineUrl(r.videoId)
                            }
                            CatalogRow(
                                result  = r,
                                isSaved = job?.status == DownloadStatus.COMPLETED,
                                onPlay  = {
                                    // Smart behavior: if this exact song is already playing, just open Now Playing
                                    // instead of restarting it. This matches Spotify behavior.
                                    val current = vm.current.value
                                    if (current?.sourceVideoId == r.videoId) {
                                        onExpandPlayer()
                                    } else {
                                        vm.prepareAndPlayOnline(r, results, idx)
                                        onExpandPlayer()
                                    }
                                },
                                onSave  = {
                                    when (job?.status) {
                                        DownloadStatus.FAILED        -> vm.retryDownload(r)
                                        DownloadStatus.QUEUED,
                                        DownloadStatus.DOWNLOADING   -> vm.cancelDownload(r.videoId)
                                        else                         -> vm.downloadOnline(r)
                                    }
                                },
                            )
                        }
                    }
                }
                !searching && q.isNotEmpty() && results.isEmpty() && albums.isEmpty() && playlists.isEmpty() -> {
                    // Smarter empty: the canonical 'no results' panel + a
                    // 'Did you mean…?' simplification hint when the query
                    // has 4+ words, + recent searches below so the user
                    // can re-discover something they searched before.
                    LazyColumn(
                        contentPadding = PaddingValues(top = 24.dp, bottom = 160.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Icon(
                                Ic.Search, null,
                                tint     = C.textTertiary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                        item {
                            Text(
                                "No results for \"$q\"",
                                color     = C.textSecondary,
                                fontSize  = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        // Did you mean — first 2 words of the query when 4+ words.
                        val words = q.trim().split(Regex("\\s+"))
                        if (words.size >= 4) {
                            val shorter = words.take(2).joinToString(" ")
                            item { Spacer(Modifier.height(14.dp)) }
                            item {
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(C.accent.copy(alpha = 0.15f))
                                        .pressableScale(
                                            onClick = {
                                                vm.setOnlineQuery(shorter)
                                                vm.runOnlineSearch()
                                            },
                                            scaleTo = 0.95f,
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Try \"$shorter\"",
                                        color = C.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Text(
                                "Try different keywords or check your connection",
                                color     = C.textTertiary,
                                fontSize  = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier  = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                        // Recent searches below so the user can retry an old query.
                        if (history.isNotEmpty()) {
                            item { Spacer(Modifier.height(32.dp)) }
                            item {
                                Text(
                                    "Or try again from recent",
                                    color = C.textTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                )
                            }
                            items(history.take(5)) { past ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .pressableScale(onClick = {
                                            vm.setOnlineQuery(past)
                                            vm.runOnlineSearch()
                                        })
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Ic.History, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Text(past, color = C.text, fontSize = 15.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // ── Browse Categories (Spotify-style empty state) ─────
                    // When the field is empty and there's no history, show
                    // a scrollable grid of curated genre/mood tiles. Reuses
                    // the same FeaturedPlaylist set MadeForYou hands to
                    // Discover so the visual language stays consistent.
                    // Each tile = 100dp tall accent-coloured card, tap →
                    // opens the playlist's first track in Now Playing.
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 160.dp),
                    ) {
                        item {
                            Text(
                                "Browse all",
                                color = C.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 4.dp),
                            )
                        }
                        val categories = com.beatdrop.kt.youtube.MadeForYou.featured
                        val rows = categories.chunked(2)
                        items(rows.size) { rowIdx ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rows[rowIdx].forEach { cat ->
                                    BrowseCategoryCard(
                                        title = cat.title,
                                        accentHex = cat.accentHex,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            vm.playFeaturedPlaylist(cat.playlistId)
                                            onExpandPlayer()
                                        },
                                    )
                                }
                                if (rows[rowIdx].size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Catalog-style result row — glass play overlay, Spotify Green save icon
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CatalogRow(
    result: OnlineResult,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork with glass play overlay
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(C.bg3),
            Alignment.Center,
        ) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model  = ImageRequest.Builder(ctx).data(result.thumbnailUrl).crossfade(true).size(128).build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            // Glass play overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Ic.TransportPlay, "Play", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        // Title + artist
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                color     = C.text,
                fontWeight = FontWeight.SemiBold,
                fontSize  = 15.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                result.author,
                color     = C.textSecondary,
                fontSize  = 13.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
        }

        // Duration
        Text(
            result.durationText,
            color     = C.textTertiary,
            fontSize  = 12.sp,
            modifier  = Modifier.padding(horizontal = 8.dp),
        )

        // Save action — Spotify Green when saved
        IconButton(
            onClick   = onSave,
            modifier  = Modifier.size(36.dp),
        ) {
            Icon(
                Ic.Bookmark,
                if (isSaved) "Saved to library" else "Save to library",
                tint     = if (isSaved) C.accent else C.textTertiary,   // Green when saved
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
// ─── Section headers + typed result carousels ─────────────────────────────────

@Composable
private fun SectionEyebrow(label: String, count: Int) {
    val C = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color      = C.text,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.weight(1f),
        )
        Text(
            count.toString(),
            color    = C.textTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AlbumCarousel(
    albums: List<com.beatdrop.kt.youtube.OnlineAlbum>,
    onOpen: (com.beatdrop.kt.youtube.OnlineAlbum) -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(albums, key = { it.browseId }) { album ->
            Column(
                Modifier
                    .width(150.dp)
                    .pressableScale(onClick = { onOpen(album) }, scaleTo = 0.96f),
            ) {
                Box(
                    Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.bg3),
                ) {
                    if (album.thumbnailUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(album.thumbnailUrl).crossfade(true).size(512).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    album.title,
                    color    = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("Album")
                        if (album.artist.isNotBlank()) append(" · ").append(album.artist)
                        if (album.year.isNotBlank())   append(" · ").append(album.year)
                    },
                    color    = C.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlaylistCarousel(
    playlists: List<com.beatdrop.kt.youtube.OnlinePlaylist>,
    onOpen: (com.beatdrop.kt.youtube.OnlinePlaylist) -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(playlists, key = { it.playlistId }) { pl ->
            Column(
                Modifier
                    .width(150.dp)
                    .pressableScale(onClick = { onOpen(pl) }, scaleTo = 0.96f),
            ) {
                Box(
                    Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.bg3),
                ) {
                    if (pl.thumbnailUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(pl.thumbnailUrl).crossfade(true).size(512).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    pl.title,
                    color    = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("Playlist")
                        if (pl.author.isNotBlank())   append(" · ").append(pl.author)
                        if (pl.trackCount > 0)        append(" · ").append("${pl.trackCount} tracks")
                    },
                    color    = C.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Search filter chips ─────────────────────────────────────────────────────

/** Which sections of the search-results list to show. */
private enum class SearchFilter { ALL, SONGS, ALBUMS, PLAYLISTS }

/**
 * Horizontal pill-chip row for filtering the typed search results.
 *
 *   [ All  127 ] [ Songs  98 ] [ Albums  12 ] [ Playlists  17 ]
 *
 * Active chip = accent-green fill + black text. Inactive = white-12%
 * fill + white-80% text. A chip whose count is zero is still tappable
 * (so the user can switch back to a still-loading section), but
 * desaturated to white-40% to signal 'nothing here yet'.
 */
@Composable
private fun SearchFilterChips(
    selected: SearchFilter,
    songCount: Int,
    albumCount: Int,
    playlistCount: Int,
    onSelect: (SearchFilter) -> Unit,
) {
    val C = LocalAppColors.current
    val total = songCount + albumCount + playlistCount
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        item { Chip("All",       total,         selected == SearchFilter.ALL,       C) { onSelect(SearchFilter.ALL) } }
        item { Chip("Songs",     songCount,     selected == SearchFilter.SONGS,     C) { onSelect(SearchFilter.SONGS) } }
        item { Chip("Albums",    albumCount,    selected == SearchFilter.ALBUMS,    C) { onSelect(SearchFilter.ALBUMS) } }
        item { Chip("Playlists", playlistCount, selected == SearchFilter.PLAYLISTS, C) { onSelect(SearchFilter.PLAYLISTS) } }
    }
}

@Composable
private fun Chip(
    label: String,
    count: Int,
    active: Boolean,
    C: com.beatdrop.kt.ui.theme.AppColors,
    onClick: () -> Unit,
) {
    val bg = if (active) C.accent else Color.White.copy(alpha = 0.10f)
    val fg = when {
        active     -> Color.Black
        count == 0 -> C.text.copy(alpha = 0.40f)
        else       -> C.text.copy(alpha = 0.85f)
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .pressableScale(onClick = onClick, scaleTo = 0.94f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color    = fg,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                color    = if (active) Color.Black.copy(alpha = 0.65f) else C.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ─── Live typed suggestion rows ───────────────────────────────────────────────

@Composable
private fun LiveSuggestionRow(
    suggestion: PlayerViewModel.LiveSuggestion,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    // Albums use a slightly rounded square thumbnail, playlists too,
    // songs use a fully circular thumbnail — visually distinguishes the
    // three types at a glance even before the user reads the subtitle.
    val isSong = suggestion is PlayerViewModel.LiveSuggestion.Song
    val cornerRadius = if (isSong) 22.dp else 6.dp
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick, scaleTo = 0.98f)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(C.bg3),
        ) {
            if (suggestion.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(suggestion.thumbnailUrl).crossfade(true).size(128).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.title,
                color = C.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                suggestion.subtitle,
                color = C.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Ic.ChevronRight,
            null,
            tint = C.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
    androidx.compose.material3.HorizontalDivider(
        color = C.separator.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp),
    )
}

// ─── Top Result hero card ─────────────────────────────────────────────────────

@Composable
private fun TopResultHero(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.bg2.copy(alpha = if (C.isDark) 0.55f else 0.92f))
            .pressableScale(onClick = onClick, scaleTo = 0.97f)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(C.bg3),
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(thumbnailUrl).crossfade(true).size(384).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = C.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = C.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Browse Category card ─────────────────────────────────────────────────────

@Composable
private fun BrowseCategoryCard(
    title: String,
    accentHex: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = Color(accentHex)
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent)
            .pressableScale(onClick = onClick, scaleTo = 0.96f)
            .padding(14.dp),
    ) {
        // Diagonal accent darken overlay for visual depth (Spotify pattern).
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.0f),
                            Color.Black.copy(alpha = 0.30f),
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                ),
        )
        Text(
            title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}
