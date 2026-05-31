package com.beatdrop.kt.ui.screens

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─── Data Model for Online Discover ──────────────────────────────────────────
data class OnlineDiscoverTrack(
    val title: String,
    val artist: String,
    val coverUrl: String
)

// Curated fallbacks in case of network issues
private val FALLBACK_TRENDING = listOf(
    OnlineDiscoverTrack("Hello", "Adele", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/7e/e6/82/7ee682bd-1b17-6adc-be63-b5af1bdff369/26UMGIM51126.rgb.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Blinding Lights", "The Weeknd", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/c3/84/c4/c384c423-fbca-1d2a-f8f4-27e4e116e01a/20UMGIM04391.rgb.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("As It Was", "Harry Styles", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/c3/8c/fa/c38cfaa3-ec7d-df9d-da3e-3cb8dbbd93cf/886449914757.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Shape of You", "Ed Sheeran", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/d5/d3/18/d5d318bd-3be5-1f9e-213c-70e9a3bbbc91/190295847372.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Flowers", "Miley Cyrus", "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/2b/ef/ee/2befee3a-4ef3-48ee-bd51-be456eeef211/886449983999.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Cruel Summer", "Taylor Swift", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/b4/cb/7a/b4cb7ad7-1b03-7cb3-8a3c-b5af3c8340d0/19UMGIM53916.rgb.jpg/600x600bb.jpg")
)

private val FALLBACK_POP = listOf(
    OnlineDiscoverTrack("Cruel Summer", "Taylor Swift", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/b4/cb/7a/b4cb7ad7-1b03-7cb3-8a3c-b5af3c8340d0/19UMGIM53916.rgb.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Flowers", "Miley Cyrus", "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/2b/ef/ee/2befee3a-4ef3-48ee-bd51-be456eeef211/886449983999.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Greedy", "Tate McRae", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/91/9f/95/919f9570-5bfa-2983-cf29-c8c366ff8b8e/886449293159.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Water", "Tyla", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ca/84/c0/ca84c0c1-be56-9fc6-cb31-50e5eb2d1f4d/886449272376.jpg/600x600bb.jpg")
)

private val FALLBACK_HIPHOP = listOf(
    OnlineDiscoverTrack("Not Like Us", "Kendrick Lamar", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/65/59/89/655989e2-fa18-f2bf-ec5a-1918341604a4/24UMGIM41846.rgb.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("LOVING YOU", "Eminem", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a4/82/cf/a482cf19-e58f-2f8c-ce40-7bc09c6bc76c/24UMGIM61284.rgb.jpg/600x600bb.jpg"),
    OnlineDiscoverTrack("Snooze", "SZA", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/aa/c8/b8aac8a6-f761-fa5d-0076-2e87c0bcbc60/886449673999.jpg/600x600bb.jpg")
)

private val okHttp = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .build()

// ─── 100% Online Discover Screen ─────────────────────────────────────────────
@Composable
fun DiscoverScreen(vm: PlayerViewModel, onOpenSearch: () -> Unit = {}) {
    val C = LocalAppColors.current
    var trending by remember { mutableStateOf<List<OnlineDiscoverTrack>>(emptyList()) }
    var popHits by remember { mutableStateOf<List<OnlineDiscoverTrack>>(emptyList()) }
    var hiphopHits by remember { mutableStateOf<List<OnlineDiscoverTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch Global Top Hits from iTunes Top Songs RSS Feed
                val req = Request.Builder().url("https://itunes.apple.com/us/rss/topsongs/limit=30/json").build()
                val responseText = okHttp.newCall(req).execute().use { it.body?.string() }
                if (!responseText.isNullOrBlank()) {
                    val root = JSONObject(responseText)
                    val entries = root.getJSONObject("feed").optJSONArray("entry")
                    if (entries != null && entries.length() > 0) {
                        val parsed = ArrayList<OnlineDiscoverTrack>()
                        for (i in 0 until entries.length()) {
                            val entry = entries.getJSONObject(i)
                            val title = entry.getJSONObject("im:name").optString("label")
                            val artist = entry.getJSONObject("im:artist").optString("label")
                            val images = entry.optJSONArray("im:image")
                            val rawCover = if (images != null && images.length() > 0) {
                                images.getJSONObject(images.length() - 1).optString("label")
                            } else ""
                            // Standardize to high-res iTunes covers
                            val cover = rawCover.replace("170x170", "600x600")
                                .replace("100x100", "600x600")
                                .replace("55x55", "600x600")
                            parsed.add(OnlineDiscoverTrack(title, artist, cover))
                        }
                        trending = parsed.take(10)
                        popHits = parsed.drop(10).take(10)
                        hiphopHits = parsed.drop(20).take(10)
                    }
                }
            } catch (e: Exception) {
                // Network failed or offline, load beautiful fallbacks
                trending = FALLBACK_TRENDING
                popHits = FALLBACK_POP
                hiphopHits = FALLBACK_HIPHOP
            } finally {
                loading = false
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(C.bg0), Alignment.Center) {
            CircularProgressIndicator(color = C.accent, strokeWidth = 3.dp)
        }
        return
    }

    val featured = trending.firstOrNull() ?: FALLBACK_TRENDING.first()
    val quickGrid = trending.drop(1).take(6)

    LazyColumn(
        Modifier.fillMaxSize().background(C.bg0),
        contentPadding = PaddingValues(bottom = 170.dp)
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

        // ── Featured Hero card (Direct Stream Playback) ─────────────────────
        item {
            OnlineFeaturedHero(featured) {
                vm.playOnlineByMetadata(featured.title, featured.artist, featured.coverUrl)
            }
        }

        // ── Quick-pick grid (Stream Playback) ────────────────────────────────
        if (quickGrid.isNotEmpty()) {
            item { OnlineEyebrow("HOT TRENDING") }
            item {
                OnlineQuickGrid(quickGrid) { track ->
                    vm.playOnlineByMetadata(track.title, track.artist, track.coverUrl)
                }
            }
        }

        // ── Carousels (Direct Stream Playback) ───────────────────────────────
        if (popHits.isNotEmpty()) {
            item {
                OnlineCarousel("Trending Pop Hits", popHits) { track ->
                    vm.playOnlineByMetadata(track.title, track.artist, track.coverUrl)
                }
            }
        }

        if (hiphopHits.isNotEmpty()) {
            item {
                OnlineCarousel("Global Hot Charts", hiphopHits) { track ->
                    vm.playOnlineByMetadata(track.title, track.artist, track.coverUrl)
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
private fun OnlineFeaturedHero(track: OnlineDiscoverTrack, onPlay: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 4.dp)
            .aspectRatio(1.6f).clip(RoundedCornerShape(Radius.lg)).background(C.bg3)
            .pressableScale(onClick = onPlay, scaleTo = 0.98f),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(track.coverUrl).crossfade(true).size(512).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("TRENDING #1", style = Type.overline, color = Color.White.copy(alpha = 0.8f))
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
private fun OnlineQuickGrid(list: List<OnlineDiscoverTrack>, onPlay: (OnlineDiscoverTrack) -> Unit) {
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
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(t.coverUrl).crossfade(true).size(96).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
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
private fun OnlineCarousel(title: String, list: List<OnlineDiscoverTrack>, onPlay: (OnlineDiscoverTrack) -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Column(Modifier.padding(top = 18.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list) { t ->
                Column(Modifier.width(150.dp).pressableScale(onClick = { onPlay(t) }, scaleTo = 0.96f)) {
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(Radius.md)).background(C.bg3)) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(t.coverUrl).crossfade(true).size(256).build(),
                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
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

    LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
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
                            AsyncImage(model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).build(),
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
