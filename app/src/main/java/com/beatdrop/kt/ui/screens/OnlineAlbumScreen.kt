package com.beatdrop.kt.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.youtube.OnlineResult
import com.beatdrop.kt.youtube.PlayableCollection
import com.beatdrop.kt.youtube.YouTubePlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OnlineAlbumScreen — Spotify/iOS-style detail screen for ANY YT-Music
 * online collection (album OR playlist). The PlayableCollection sealed
 * interface unifies the two so a single screen serves both — they have
 * structurally identical layout requirements (cover + title + author +
 * track list) and divergent kinds were resulting in near-duplicated code.
 *
 *   ┌─────────────────────────────────┐
 *   │   ⟨ back                        │
 *   │                                 │
 *   │        ┌──────────┐             │
 *   │        │  cover   │   (blurred  │
 *   │        │   art    │    cover as │
 *   │        └──────────┘    backdrop)│
 *   │   TITLE (Bold 28sp, UPPERCASE)  │
 *   │   ⦿ Artist · Year / N tracks    │
 *   │                                 │
 *   │   [ ▶ Play ]   ↻ Shuffle   ⤓    │
 *   │                                 │
 *   │   1  ⌧ TRACK ONE       3:42  ⤓  │
 *   │      Artist · ft. X                │
 *   │   2  ⌧ TRACK TWO       4:21  ⤓  │
 *   │   …                              │
 *   └─────────────────────────────────┘
 *
 * Tracks are fetched on first composition via YouTubePlaylist.fetchPlaylist
 * (both albums and playlists use a playlistId). The cover is shown
 * immediately from collection.coverUrl + drawn behind everything with a
 * 60-px blur as a cinematic backdrop. Track titles render UPPERCASE,
 * subtitle row carries the artist + any feat./ft./x collaborators.
 */
@Composable
fun OnlineAlbumScreen(
    vm: PlayerViewModel,
    collection: PlayableCollection,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    val C   = LocalAppColors.current
    val ctx = LocalContext.current
    // ── Instant render from PlaylistCache, refresh in background ───────
    // Re-opening the same playlist / album used to re-fetch from the
    // network every single time. Now we synchronously seed from the
    // 24-h disk cache (PlaylistCache) so the screen comes up populated
    // on re-open; the LaunchedEffect below only does a network round
    // trip when the cache is empty for this playlistId.
    val initiallyCached = remember(collection.playlistId) {
        com.beatdrop.kt.youtube.PlaylistCache.get(collection.playlistId)?.videos
            ?: emptyList()
    }
    var tracks by remember(collection.playlistId) { mutableStateOf(initiallyCached) }
    var loading by remember(collection.playlistId) {
        mutableStateOf(initiallyCached.isEmpty())
    }

    LaunchedEffect(collection.playlistId) {
        if (tracks.isNotEmpty()) return@LaunchedEffect    // already had cache
        loading = true
        val fetched = runCatching {
            withContext(Dispatchers.IO) {
                // fetchPlaylist internally consults PlaylistCache too,
                // so this still no-ops on a cache hit that arrived
                // between composition and effect launch.
                YouTubePlaylist.fetchPlaylist(collection.playlistId, maxItems = 100).videos
            }
        }.getOrDefault(emptyList())
        tracks = fetched
        loading = false
    }

    // Total runtime — shown in the secondary line as 'N songs · 1 hr 12 min'.
    // Spec: 'album should show Duration time same for playlists'.
    val totalDurationText = remember(tracks) {
        val secs = tracks.sumOf { it.durationSecs.coerceAtLeast(0) }
        if (secs <= 0) ""
        else {
            val h = secs / 3600
            val m = (secs % 3600) / 60
            when {
                h > 0 && m > 0 -> "$h hr $m min"
                h > 0          -> "$h hr"
                m > 0          -> "$m min"
                else           -> "${secs}s"
            }
        }
    }

    // For playlists / Featured tiles the coverUrl may be null initially
    // (we don't have one before the first track lands). Derive a fallback
    // from the first track's thumbnail once we have it.
    val effectiveCover = collection.coverUrl ?: tracks.firstOrNull()?.thumbnailUrl

    // Secondary line: 'Album · Artist · Year' OR 'Playlist · Author · 56 tracks'.
    // Once tracks are loaded, prefer the live count for playlists since the
    // search-time trackCount can be stale.
    val secondaryLine = remember(collection, tracks, totalDurationText) {
        when (collection) {
            is PlayableCollection.Playlist,
            is PlayableCollection.Featured -> buildString {
                append(collection.kindLabel)
                if (collection.author.isNotBlank()) append(" · ").append(collection.author)
                if (tracks.isNotEmpty()) append(" · ").append("${tracks.size} songs")
                if (totalDurationText.isNotEmpty()) append(" · ").append(totalDurationText)
            }
            is PlayableCollection.Album -> buildString {
                append(collection.secondaryLine)
                if (totalDurationText.isNotEmpty()) append(" · ").append(totalDurationText)
            }
        }
    }

    ScreenScaffold {
        Box(Modifier.fillMaxSize()) {
            // ── Blurred-cover backdrop ────────────────────────────────────
            if (effectiveCover != null) {
                val blurMod =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(60f, 60f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    else Modifier
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(effectiveCover)
                        .crossfade(true).size(720).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.55f)
                        .then(blurMod),
                )
                // Vertical dim gradient — blends to a CONSTANT deep
                // deep black regardless of theme so white foreground
                // text stays legible in light mode (Spotify/Apple Music
                // pattern — album/playlist detail is always cinematic-dark).
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f  to Color.Transparent,
                            0.45f to Color(0xCC000000),
                            1.0f  to Color(0xFF000000),
                        ),
                    ),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF000000)))
            }

            // ── Foreground content ───────────────────────────────────────
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 190.dp),
            ) {
                // Top bar
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Ic.Back, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
                // Cover
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(C.bg3),
                        ) {
                            if (effectiveCover != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx).data(effectiveCover)
                                        .crossfade(true).size(720).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                // Title + author + Play / Shuffle / Download row.
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // True UPPERCASE title — Spotify/Apple-Music-album
                        // pattern. Reads as 'a release', not 'a song'.
                        Text(
                            collection.title.uppercase(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                        // Author row — circular avatar + secondary line.
                        // Avatar uses the cover (square) cropped into a
                        // 28dp circle since real per-artist art needs login.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(C.bg3),
                            ) {
                                if (effectiveCover != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(ctx).data(effectiveCover)
                                            .crossfade(true).size(96).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                secondaryLine,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        // Play / Shuffle / Download buttons.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(C.accent)
                                    .pressableScale(
                                        onClick = {
                                            // Use the tracks we already have in memory —
                                            // no re-fetch, no race with Next button.
                                            if (tracks.isNotEmpty()) {
                                                vm.playOnlineList(tracks, startIndex = 0)
                                            } else {
                                                vm.playFeaturedPlaylist(collection.playlistId)
                                            }
                                            onExpandPlayer()
                                        },
                                        scaleTo = 0.96f,
                                    )
                                    .padding(horizontal = 22.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Ic.TransportPlay, null,
                                        tint = Color.White, modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play", color = Color.White,
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (tracks.isEmpty()) return@IconButton
                                    // Shuffle a copy then play — keeps the
                                    // current screen's track list as the
                                    // navigation context but in a new order.
                                    val shuffled = tracks.shuffled()
                                    vm.playOnlineList(shuffled, startIndex = 0)
                                    onExpandPlayer()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                            ) {
                                Icon(Ic.Shuffle, "Shuffle", tint = Color.White,
                                    modifier = Modifier.size(22.dp))
                            }
                            // Download whole collection — enqueues every track.
                            // Disabled while still resolving (would download
                            // nothing). After tap each row shows its diegetic
                            // ring as bytes arrive.
                            IconButton(
                                onClick = { tracks.forEach { vm.downloadOnline(it) } },
                                enabled = tracks.isNotEmpty(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                            ) {
                                Icon(
                                    Ic.Download,
                                    "Download ${collection.kindLabel.lowercase()}",
                                    tint = if (tracks.isNotEmpty()) Color.White
                                           else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                // Loading silhouettes / track list
                if (loading && tracks.isEmpty()) {
                    item {
                        com.beatdrop.kt.ui.components.SearchResultSilhouettes(
                            rowCount = 8,
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        )
                    }
                } else {
                    itemsIndexed(tracks, key = { _, t -> t.videoId }) { idx, track ->
                        TrackRow(
                            vm = vm,
                            index = idx + 1,
                            track = track,
                            // For albums we hide the artist line when it
                            // matches the album artist (it'd be redundant
                            // on every row). For playlists we always show
                            // it because different songs have different
                            // primary artists.
                            forceShowArtist = collection !is PlayableCollection.Album ||
                                track.author != (collection as? PlayableCollection.Album)?.album?.artist,
                            onPlay = {
                                // Play from the in-memory list at idx — no
                                // network round-trip and onlineContext is set
                                // in the same call frame, so Next works
                                // immediately even if the user mashes it
                                // right after the play tap.
                                vm.playOnlineList(tracks, startIndex = idx)
                                onExpandPlayer()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Track row used inside the OnlineAlbumScreen.
 *
 * Layout (left → right):
 *   • Track index (28.dp wide, dim)
 *   • Cover thumbnail (44.dp square, rounded 6.dp)
 *   • Title in UPPERCASE bold (15sp)
 *     subtitle: primary artist + ft./x collaborators (12sp dim)
 *   • Duration (right-aligned)
 *   • DiegeticDownloadIcon (always present — green check when downloaded)
 *
 * Per the user's spec: 'cover then song name in capital letters,
 * below name we should have artists and ft if necessary or x'.
 */
@Composable
private fun TrackRow(
    vm: PlayerViewModel,
    index: Int,
    track: OnlineResult,
    forceShowArtist: Boolean,
    onPlay: () -> Unit,
) {
    val C   = LocalAppColors.current
    val ctx = LocalContext.current
    val jobs by vm.downloadJobs.collectAsState()
    val job = jobs[track.videoId]
    val isDownloaded = vm.isOnlineDownloaded(track.videoId)
    val isDownloading = job?.status == com.beatdrop.kt.youtube.DownloadStatus.DOWNLOADING

    val duration = track.durationText.ifBlank {
        if (track.durationSecs > 0)
            "%d:%02d".format(track.durationSecs / 60, track.durationSecs % 60)
        else ""
    }

    // Parse the title for collab markers and build a 'Author · ft. X x Y'
    // subtitle. Keeps the primary artist + any feat./ft./x/with/&/x.
    val subtitle = remember(track) { formatArtistLine(track.author, track.title) }

    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onPlay, scaleTo = 0.98f)
            .padding(start = Spacing.lg, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track index — fixed-width column.
        Text(
            index.toString(),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp),
        )
        // Per-track cover thumbnail (per user spec: 'song cover then song
        // name'). 44.dp keeps it well within row height.
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(C.bg3),
        ) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(track.thumbnailUrl)
                        .crossfade(true).size(128).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Title (UPPERCASE) + subtitle with feat./x collaborators.
        Column(Modifier.weight(1f)) {
            Text(
                track.title.uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (forceShowArtist && subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (duration.isNotBlank()) {
            Text(
                duration,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        // Per-track download — diegetic progress ring (no spinner).
        com.beatdrop.kt.ui.components.DiegeticDownloadIcon(
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            progressPercent = job?.progress ?: 0,
            onClick = { vm.downloadOnline(track) },
        )
    }
}

/**
 * Build the subtitle line for a track row.
 *
 *   "Drake — God's Plan"                 →  "Drake"
 *   "Drake ft. 21 Savage — Knife Talk"   →  "Drake · ft. 21 Savage"
 *   "Burna Boy x Wizkid — Ginger"        →  "Burna Boy · x Wizkid"
 *   "Travis Scott & Kanye West - …"      →  "Travis Scott · & Kanye West"
 *
 * Logic: look for the first collab marker (ft./feat./featuring/x/×/&/with)
 * in the title, and if the text after it looks like an artist name (≤40
 * chars, no song-title separator before it), append it to the primary
 * author with the same marker word so the user reads 'who's on this'.
 */
private fun formatArtistLine(primary: String, title: String): String {
    val artistPart = title.substringBefore(" - ", missingDelimiterValue = title)
    val markers = listOf(" ft. ", " ft ", " feat. ", " feat ", " featuring ",
                         " with ", " w/ ", " x ", " × ", " & ", " and ")
    val lower = " $artistPart ".lowercase()
    for (m in markers) {
        val idx = lower.indexOf(m)
        if (idx >= 0) {
            // The substring after the marker (in the original casing).
            val tail = artistPart.substring((idx + m.length - 1).coerceAtMost(artistPart.length))
                .takeWhile { it !in setOf('(', '[', '|') }
                .split(Regex("[,;]"))
                .firstOrNull()
                ?.trim()
                .orEmpty()
            if (tail.length in 2..40) {
                // Use the marker as it appeared (ft / x / &) so the
                // subtitle reads naturally.
                val markerLabel = m.trim()
                return if (primary.isBlank()) "$markerLabel $tail"
                       else "$primary · $markerLabel $tail"
            }
        }
    }
    return primary
}
