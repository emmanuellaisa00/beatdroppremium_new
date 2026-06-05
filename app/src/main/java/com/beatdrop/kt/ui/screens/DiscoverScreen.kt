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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.ui.components.BeatDropSearchButton
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.OnlineResult

// ═══════════════════════════════════════════════════════════════════════════════
// 100% Online Discover Screen — Spotify Glassmorphism Skin
// Accent: #21FF6B (Spotify Green), glass cards blur 24-32px
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: PlayerViewModel, onOpenSearch: () -> Unit = {}, onExpandPlayer: () -> Unit = {}) {
    val C = LocalAppColors.current
    var trending  by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var popHits   by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var hiphopHits by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    val cachedTrending  by vm.cachedTrending.collectAsState()
    val cachedPopHits   by vm.cachedPopHits.collectAsState()
    val cachedHiphop    by vm.cachedHiphop.collectAsState()
    val discoverLoading by vm.discoverLoading.collectAsState()
    val madeForYou      by vm.madeForYou.collectAsState()

    // Skeletons appear ONLY on the very first launch (no on-device cache).
    // Subsequent app opens hydrate cachedTrending from DataStore in
    // PlayerViewModel.init → loadDiscoverCache(), so by the time
    // DiscoverScreen composes we already have data.
    var loading   by remember { mutableStateOf(cachedTrending.isEmpty()) }

    LaunchedEffect(Unit) {
        vm.getDiscoverData()
        vm.loadMadeForYou()
    }

    // Show cached data instantly, update when fresh data arrives
    LaunchedEffect(cachedTrending, cachedPopHits, cachedHiphop) {
        if (cachedTrending.isNotEmpty()) {
            trending    = cachedTrending
            popHits     = cachedPopHits
            hiphopHits  = cachedHiphop
            loading     = false
        } else if (!discoverLoading && cachedTrending.isEmpty()) {
            loading = false
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Pull-to-refresh — drag down from the top to force a fresh fetch.
    // discoverLoading flips the spinner; vm.getDiscoverData(forceRefresh
    // = true) bypasses the 5-min cache window.
    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    ScreenScaffold {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = discoverLoading,
        onRefresh = {
            vm.getDiscoverData(forceRefresh = true)
            vm.loadMadeForYou(force = true)
        },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 10.dp, bottom = 170.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Discover", style = Type.largeTitle, color = C.text, modifier = Modifier.weight(1f))

                // Unified opaque search button — guarantees icon contrast in
                // every theme (the legacy glassRow + tint = 0xDDFFFFFF in
                // light mode put a white icon on a near-white surface).
                BeatDropSearchButton(onClick = onOpenSearch, contentDescription = "Search online")
            }
        }

        if (loading) {
            // Check if we're offline with no cache
            val isOnline = com.beatdrop.kt.util.NetworkMonitor.isOnline.value
            if (!isOnline && trending.isEmpty() && popHits.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Ic.WifiOff, null, tint = C.textTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("You're offline", style = Type.title3, color = C.text)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Connect to the internet to discover trending music.",
                                style = Type.footnote, color = C.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
                return@LazyColumn
            }
            item { DiscoverShimmerContent() }
            return@LazyColumn
        }

        val featured   = trending.firstOrNull()
        val quickGrid  = trending.drop(1).take(6)

        // ── Featured Hero card (glass style, blur 28px) ─────────────────────
        // Featured + quickGrid all come from the same `trending` list, so we
        // use that as the skip-next/prev context. The featured one is index 0.
        featured?.let { feat ->
            item {
                OnlineFeaturedHero(feat) {
                    vm.prepareAndPlayOnline(feat, trending, 0)
                    onExpandPlayer()
                }
            }
        }

        // ── Made For You — curated YT Music playlist tiles ─────────────────
        // Spotify "Made for You" / "Daily Mix" style. Each card opens its
        // playlist; tapping it starts playback at track 0 and the full
        // playlist becomes the onlineContext for next/prev.
        if (madeForYou.isNotEmpty()) {
            item { OnlineEyebrow("MADE FOR YOU") }
            item {
                MadeForYouCarousel(madeForYou) { preview ->
                    vm.playFeaturedPlaylist(preview.meta.playlistId)
                    onExpandPlayer()
                }
            }
        }

        // ── Quick-pick grid (opaque elevated cards) ────────────────────────
        if (quickGrid.isNotEmpty()) {
            item { OnlineEyebrow("HOT TRENDING") }
            item {
                OnlineQuickGrid(quickGrid) { track ->
                    // quickGrid is `trending.drop(1).take(6)` — find original index in `trending`
                    val ctxIdx = trending.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                    vm.prepareAndPlayOnline(track, trending, ctxIdx)
                    onExpandPlayer()
                }
            }
        }

        // ── Carousels (glass cards, accent green play button) ─────────────
        if (popHits.isNotEmpty()) {
            item {
                OnlineCarousel("Trending Pop Hits", popHits) { track ->
                    val ctxIdx = popHits.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                    vm.prepareAndPlayOnline(track, popHits, ctxIdx)
                    onExpandPlayer()
                }
            }
        }

        if (hiphopHits.isNotEmpty()) {
            item {
                OnlineCarousel("Global Hot Charts", hiphopHits) { track ->
                    val ctxIdx = hiphopHits.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                    vm.prepareAndPlayOnline(track, hiphopHits, ctxIdx)
                    onExpandPlayer()
                }
            }
        }
    }
    }   // PullToRefreshBox
    }   // ScreenScaffold
}

@Composable private fun OnlineEyebrow(text: String) {
    val C = LocalAppColors.current
    Text(text, style = Type.overline, color = C.textTertiary, modifier = Modifier.padding(start = Spacing.lg, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun OnlineFeaturedHero(track: OnlineResult, onPlay: () -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 4.dp)
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(Radius.lg))
            .background(C.bg3)
            .pressableScale(onClick = onPlay, scaleTo = 0.98f),
    ) {
        if (track.thumbnailUrl != null) {
            AsyncImage(
                model  = ImageRequest.Builder(ctx).data(track.thumbnailUrl).crossfade(true).size(512).build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }
        // Gradient overlay
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)))
        ))
        // Metadata
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("TRENDING #1", style = Type.overline, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(track.title,  style = Type.title2, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.author, style = Type.callout, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Glass play button with Spotify Green accent
        Box(
            Modifier
                .align(Alignment.BottomEnd).padding(18.dp).size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.accent.copy(alpha = 0.85f))        // Spotify Green
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        startY = 0f, endY = size.height * 0.4f,
                    ))
                }
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            Alignment.Center,
        ) {
            Icon(Ic.TransportPlay, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun OnlineQuickGrid(list: List<OnlineResult>, onPlay: (OnlineResult) -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        list.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { t ->
                    // HOT TRENDING grid card — opaque elevated surface.
                    // Was using glassRow which in light mode resolves to
                    // glassCardElevated * alpha 0.95 = near-solid white, so
                    // the title text + thumbnail vanished into the background
                    // (the "white screen covering Hot Trending" bug). Now
                    // backs with a guaranteed-opaque bg2 layer at theme-
                    // appropriate alpha + a faint 1.dp hairline border.
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(C.bg2.copy(alpha = if (C.isDark) 0.55f else 0.92f))
                            .border(
                                0.5.dp,
                                C.glassCardElevatedBorder,
                                RoundedCornerShape(Radius.md),
                            )
                            .pressableScale(onClick = { onPlay(t) }, scaleTo = 0.97f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(topStart = Radius.md, bottomStart = Radius.md))
                                .background(C.bg3),
                        ) {
                            if (t.thumbnailUrl != null) {
                                AsyncImage(
                                    model  = ImageRequest.Builder(ctx).data(t.thumbnailUrl).crossfade(true).size(96).build(),
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Text(t.title, style = Type.caption, color = C.text, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OnlineCarousel(title: String, list: List<OnlineResult>, onPlay: (OnlineResult) -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(top = 18.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(list) { t ->
                Column(
                    Modifier
                        .width(150.dp)
                        .pressableScale(onClick = { onPlay(t) }, scaleTo = 0.96f),
                ) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(C.bg3),
                    ) {
                        if (t.thumbnailUrl != null) {
                            AsyncImage(
                                model  = ImageRequest.Builder(ctx).data(t.thumbnailUrl).crossfade(true).size(256).build(),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                        // Glass play button with Spotify Green
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd).padding(8.dp).size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(C.accent.copy(alpha = 0.90f))
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                        startY = 0f, endY = size.height * 0.4f,
                                    ))
                                }
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                            Alignment.Center,
                        ) {
                            Icon(Ic.TransportPlay, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title,  style = Type.callout, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.author, style = Type.footnote, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─── Local Discover Screen ─────────────────────────────────────────────────────

@Composable
fun LocalDiscoverScreen(vm: PlayerViewModel, onBack: () -> Unit = {}, onOpenSearch: () -> Unit = {}) {
    val C = LocalAppColors.current
    val tracks by vm.tracks.collectAsState()
    val counts by vm.playCounts.collectAsState()

    val featured   = remember(tracks, counts) {
        val byId = tracks.associateBy { it.id }
        counts.entries.maxByOrNull { it.value }?.let { byId[it.key] } ?: tracks.firstOrNull()
    }
    val recent     = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(12) }
    val mostPlayed = remember(tracks, counts) {
        val byId = tracks.associateBy { it.id }
        counts.entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }.take(12)
    }
    val jumpBackIn = remember(tracks) { tracks.shuffled().take(12) }
    val quickGrid  = remember(tracks) { tracks.shuffled().take(6) }

    val topPaddingLocal = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ScreenScaffold {
    LazyColumn(contentPadding = PaddingValues(top = topPaddingLocal + 10.dp, bottom = 170.dp)) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Ic.Back, "Back", tint = C.text) }
                Text("Local Discover", style = Type.largeTitle, color = C.text, modifier = Modifier.weight(1f))

                // Unified opaque search button.
                BeatDropSearchButton(onClick = onOpenSearch, contentDescription = "Search online")
            }
        }

        if (tracks.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { Text("Your library is empty.", style = Type.body, color = C.textSecondary) } }
            return@LazyColumn
        }

        featured?.let { f -> item { LocalFeaturedHero(f) { vm.play(f) } } }

        if (quickGrid.isNotEmpty()) {
            item { LocalEyebrow("QUICK PICKS") }
            item { LocalQuickGrid(quickGrid) { vm.play(it) } }
        }

        if (mostPlayed.isNotEmpty()) item { LocalCarousel("Most Played", mostPlayed, vm) }
        item { LocalCarousel("Recently Added", recent, vm) }
        item { LocalCarousel("Jump Back In", jumpBackIn, vm) }
    }
    }
}

@Composable private fun LocalEyebrow(text: String) {
    val C = LocalAppColors.current
    Text(text, style = Type.overline, color = C.textTertiary, modifier = Modifier.padding(start = Spacing.lg, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun LocalFeaturedHero(track: Track, onPlay: () -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 4.dp)
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(Radius.lg))
            .background(C.bg3)
            .pressableScale(onClick = onPlay, scaleTo = 0.98f),
    ) {
        AsyncImage(model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).size(512).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("FEATURED", style = Type.overline, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(track.title,  style = Type.title2, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = Type.callout, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd).padding(18.dp).size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.accent.copy(alpha = 0.85f))
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f, endY = size.height * 0.4f,
                    ))
                }
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            Alignment.Center,
        ) {
            Icon(Ic.TransportPlay, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LocalQuickGrid(list: List<Track>, onPlay: (Track) -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        list.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(C.glassCardElevated)
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = if (C.isDark) 0.06f else 0.12f), Color.Transparent),
                                    startY = 0f, endY = size.height * 0.3f,
                                ))
                            }
                            .border(0.5.dp, C.glassCardElevatedBorder, RoundedCornerShape(Radius.md))
                            .pressableScale(onClick = { onPlay(t) }, scaleTo = 0.97f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(topStart = Radius.md, bottomStart = Radius.md)).background(C.bg3)) {
                            AsyncImage(model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).size(96).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Text(t.title, style = Type.caption, color = C.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LocalCarousel(title: String, list: List<Track>, vm: PlayerViewModel) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(top = 18.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.id }) { t ->
                Column(Modifier.width(150.dp).pressableScale(onClick = { vm.playList(list, t.id) }, scaleTo = 0.96f)) {
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(Radius.md)).background(C.bg3)) {
                        AsyncImage(model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        // Spotify Green play button
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd).padding(8.dp).size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(C.accent.copy(alpha = 0.90f))
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                        startY = 0f, endY = size.height * 0.4f,
                                    ))
                                }
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                            Alignment.Center,
                        ) {
                            Icon(Ic.TransportPlay, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title,  style = Type.callout, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, style = Type.footnote, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Made For You carousel — Spotify-style square tiles with a coloured
 * gradient overlay corner, playlist title, and subtitle. Tap → opens
 * the playlist (caller decides start track + onlineContext wiring).
 */
@Composable
private fun MadeForYouCarousel(
    items: List<com.beatdrop.kt.youtube.MadeForYou.PlaylistPreview>,
    onOpen: (com.beatdrop.kt.youtube.MadeForYou.PlaylistPreview) -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items) { preview ->
            val accent = Color(preview.meta.accentHex)
            Column(
                Modifier
                    .width(170.dp)
                    .pressableScale(onClick = { onOpen(preview) }, scaleTo = 0.96f),
            ) {
                Box(
                    Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(accent.copy(alpha = 0.30f)),
                ) {
                    if (preview.coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(preview.coverUrl).crossfade(true).size(512).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Accent-coloured gradient that fades from the bottom-
                    // left corner so the title-area underneath the tile has
                    // visual continuity with the cover (Spotify pattern).
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.0f),
                                        accent.copy(alpha = 0.55f),
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                                ),
                            ),
                    )
                    // Play affordance — small accent pill bottom-right.
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd).padding(10.dp).size(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(C.accent),
                        Alignment.Center,
                    ) {
                        Icon(Ic.TransportPlay, "Play", tint = Color.Black,
                            modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    preview.meta.title,
                    style = Type.headline, color = C.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    preview.meta.subtitle,
                    style = Type.footnote, color = C.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DiscoverShimmerContent() {
    val C = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue  = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    // Tinted accent shimmer — content materializing, not loading.
    // The faint accent wash signals "your stuff is coming" rather than
    // the neutral gray which read as "the app is broken / empty".
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            C.accent.copy(alpha = 0.04f),
            C.accent.copy(alpha = 0.12f),
            C.accent.copy(alpha = 0.04f),
        ),
        start = Offset(xOffset, 0f),
        end   = Offset(xOffset + 200f, 200f),
    )

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        // Hero Card Shimmer
        Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(Radius.lg)).background(shimmerBrush))
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(100.dp, 16.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(Radius.md)).background(shimmerBrush))
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(Radius.md)).background(shimmerBrush))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(Radius.md)).background(shimmerBrush))
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(Radius.md)).background(shimmerBrush))
        }
    }
}