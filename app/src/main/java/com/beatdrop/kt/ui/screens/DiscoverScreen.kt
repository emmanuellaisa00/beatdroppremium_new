package com.beatdrop.kt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.OnlineResult
import com.beatdrop.kt.youtube.searchYoutube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── 100% Online Discover Screen (Fetched Directly from YouTube) ─────────────
@Composable
fun DiscoverScreen(vm: PlayerViewModel, onOpenSearch: () -> Unit = {}, onExpandPlayer: () -> Unit = {}) {
    val C = LocalAppColors.current
    var trending by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var popHits by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var hiphopHits by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Use cached discover data from ViewModel (auto-refreshes every 5 min)
        vm.getDiscoverData()
        // Observe the cached state
    }

    // Observe cached data from ViewModel
    val cachedTrending by vm.cachedTrending.collectAsState()
    val cachedPopHits by vm.cachedPopHits.collectAsState()
    val cachedHiphop by vm.cachedHiphop.collectAsState()
    val discoverLoading by vm.discoverLoading.collectAsState()

    LaunchedEffect(cachedTrending, cachedPopHits, cachedHiphop) {
        if (cachedTrending.isNotEmpty()) {
            trending = cachedTrending
            popHits = cachedPopHits
            hiphopHits = cachedHiphop
            loading = false
        } else if (!discoverLoading) {
            loading = false
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 10.dp, bottom = 170.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Discover", style = Type.largeTitle, color = C.text, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(C.bg2)
                        .pressableScale(onClick = onOpenSearch, scaleTo = 0.85f),
                    Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, "Search online", tint = C.text, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (loading) {
            item { DiscoverShimmerContent() }
            return@LazyColumn
        }

        val featured = trending.firstOrNull()
        val quickGrid = trending.drop(1).take(6)

        // ── Featured Hero card (Direct Stream Playback) ─────────────────────
        featured?.let { feat ->
            item {
                OnlineFeaturedHero(feat) {
                    vm.prepareAndPlayOnline(feat)
                    onExpandPlayer()
                }
            }
        }

        // ── Quick-pick grid (Stream Playback) ────────────────────────────────
        if (quickGrid.isNotEmpty()) {
            item { OnlineEyebrow("HOT TRENDING") }
            item {
                OnlineQuickGrid(quickGrid) { track ->
                    vm.prepareAndPlayOnline(track)
                    onExpandPlayer()
                }
            }
        }

        // ── Carousels (Direct Stream Playback) ───────────────────────────────
        if (popHits.isNotEmpty()) {
            item {
                OnlineCarousel("Trending Pop Hits", popHits) { track ->
                    vm.prepareAndPlayOnline(track)
                    onExpandPlayer()
                }
            }
        }

        if (hiphopHits.isNotEmpty()) {
            item {
                OnlineCarousel("Global Hot Charts", hiphopHits) { track ->
                    vm.prepareAndPlayOnline(track)
                    onExpandPlayer()
                }
            }
        }
    }
}

@Composable
private fun OnlineEyebrow(text: String) {
    val C = LocalAppColors.current
    Text(text, style = Type.overline, color = C.textTertiary, modifier = Modifier.padding(start = Spacing.lg, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun OnlineFeaturedHero(track: OnlineResult, onPlay: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 4.dp)
            .aspectRatio(1.6f).clip(RoundedCornerShape(Radius.lg)).background(C.bg3)
            .pressableScale(onClick = onPlay, scaleTo = 0.98f),
    ) {
        if (track.thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(track.thumbnailUrl).crossfade(true).size(512).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("TRENDING #1", style = Type.overline, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(track.title, style = Type.title2, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.author, style = Type.callout, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp).clip(RoundedCornerShape(24.dp)).background(C.accent), Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun OnlineQuickGrid(list: List<OnlineResult>, onPlay: (OnlineResult) -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        list.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(C.bg2)
                            .pressableScale(onClick = { onPlay(t) }, scaleTo = 0.97f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)).background(C.bg3)) {
                            if (t.thumbnailUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx).data(t.thumbnailUrl).crossfade(true).size(96).build(),
                                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                )
                            }
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
private fun OnlineCarousel(title: String, list: List<OnlineResult>, onPlay: (OnlineResult) -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(top = 18.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list) { t ->
                Column(Modifier.width(150.dp).pressableScale(onClick = { onPlay(t) }, scaleTo = 0.96f)) {
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(Radius.md)).background(C.bg3)) {
                        if (t.thumbnailUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(t.thumbnailUrl).crossfade(true).size(256).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).background(C.accent.copy(alpha = 0.95f)), Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title, style = Type.callout, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.author, style = Type.footnote, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─── Local Discover Screen (Preserved, built from local library) ──────────────────────────
/** Apple-Music / Spotify-style home built entirely from the local library. */
@Composable
fun LocalDiscoverScreen(vm: PlayerViewModel, onBack: () -> Unit = {}, onOpenSearch: () -> Unit = {}) {
    val C = LocalAppColors.current
    val tracks by vm.tracks.collectAsState()
    val counts by vm.playCounts.collectAsState()

    val featured = remember(tracks, counts) {
        val byId = tracks.associateBy { it.id }
        counts.entries.maxByOrNull { it.value }?.let { byId[it.key] } ?: tracks.firstOrNull()
    }
    val recent = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(12) }
    val mostPlayed = remember(tracks, counts) {
        val byId = tracks.associateBy { it.id }
        counts.entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }.take(12)
    }
    val jumpBackIn = remember(tracks) { tracks.shuffled().take(12) }
    val quickGrid = remember(tracks) { tracks.shuffled().take(6) }

    val topPaddingLocal = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(contentPadding = PaddingValues(top = topPaddingLocal + 10.dp, bottom = 170.dp)) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = C.text)
                }
                Text("Local Discover", style = Type.largeTitle, color = C.text, modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(C.bg2)
                    .pressableScale(onClick = onOpenSearch, scaleTo = 0.85f), Alignment.Center) {
                    Icon(Icons.Filled.Search, "Search online", tint = C.text, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (tracks.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { Text("Your library is empty.", style = Type.body, color = C.textSecondary) } }
            return@LazyColumn
        }

        // ── Featured hero ───────────────────────────────────────────────────
        featured?.let { f ->
            item { LocalFeaturedHero(f) { vm.play(f) } }
        }

        // ── Quick-pick grid (2 cols of compact rows) ────────────────────────
        if (quickGrid.isNotEmpty()) {
            item { LocalEyebrow("QUICK PICKS") }
            item { LocalQuickGrid(quickGrid) { vm.play(it) } }
        }

        // ── Carousels ───────────────────────────────────────────────────────
        if (mostPlayed.isNotEmpty()) item { LocalCarousel("Most Played", mostPlayed, vm) }
        item { LocalCarousel("Recently Added", recent, vm) }
        item { LocalCarousel("Jump Back In", jumpBackIn, vm) }
    }
}

@Composable
private fun LocalEyebrow(text: String) {
    val C = LocalAppColors.current
    Text(text, style = Type.overline, color = C.textTertiary, modifier = Modifier.padding(start = Spacing.lg, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun LocalFeaturedHero(track: Track, onPlay: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 4.dp)
            .aspectRatio(1.6f).clip(RoundedCornerShape(Radius.lg)).background(C.bg3)
            .pressableScale(onClick = onPlay, scaleTo = 0.98f),
    ) {
        AsyncImage(model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).size(512).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("FEATURED", style = Type.overline, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(track.title, style = Type.title2, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = Type.callout, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp).clip(RoundedCornerShape(24.dp)).background(C.accent), Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LocalQuickGrid(list: List<Track>, onPlay: (Track) -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        list.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(C.bg2)
                            .pressableScale(onClick = { onPlay(t) }, scaleTo = 0.97f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)).background(C.bg3)) {
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
    val C = LocalAppColors.current
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
                        Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).background(C.accent.copy(alpha = 0.95f)), Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title, style = Type.callout, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, style = Type.footnote, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f),
        ),
        start = Offset(xOffset, 0f),
        end = Offset(xOffset + 200f, 200f)
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Hero Card Shimmer
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(Radius.lg))
                .background(shimmerBrush)
        )
        Spacer(Modifier.height(32.dp))

        // Grid Title Shimmer
        Box(
            Modifier
                .size(100.dp, 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
        Spacer(Modifier.height(16.dp))

        // Grid Shimmer (2 rows of 2 compact items)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
        }
    }
}
