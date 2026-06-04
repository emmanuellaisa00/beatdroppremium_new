package com.beatdrop.kt.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.data.SortMode
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

private enum class LibTab(val label: String) { SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists") }

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Library Screen
// Logo: "BeatDrop" with accent green (#21FF6B)
// Glass search bar, glass segmented control
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LibraryScreen(
    vm: PlayerViewModel,
    onOpenAlbum: (String, String) -> Unit = { _, _ -> },
    onOpenArtist: (String) -> Unit = {},
    onOpenLocalDiscover: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenStats: () -> Unit = {},
) {
    val C       = LocalAppColors.current
    val query   by vm.query.collectAsState()
    val loaded  by vm.loaded.collectAsState()
    val tracks  by vm.tracks.collectAsState()
    var tab     by remember { mutableStateOf(LibTab.SONGS) }

    Column(Modifier.fillMaxSize()) {
        // ── Top bar: "BeatDrop" logo with accent green ──────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.lg, end = 4.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Beat" in accent green, "Drop" in text color
            Row(Modifier.weight(1f)) {
                Text("Beat", style = Type.largeTitle, color = C.accent)   // Spotify Green
                Text("Drop", style = Type.largeTitle, color = C.text)
            }
            HeaderIcon(Icons.Outlined.QueueMusic, "Playlists", onOpenPlaylists)
            HeaderIcon(Icons.Outlined.BarChart,   "Stats",     onOpenStats)
            HeaderIcon(Icons.Outlined.Explore,    "Discover",  onOpenLocalDiscover)
        }

        // ── Search field — Glass stadium pill (blur 28px) ────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = 8.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(C.glassCardElevated)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect.createChainEffect(
                                RenderEffect.createColorFilterEffect(
                                    android.graphics.ColorMatrixColorFilter(
                                        android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
                                    )
                                ),
                                RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP),
                            ).asComposeRenderEffect()
                        }
                    } else Modifier
                )
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (C.isDark) 0.06f else 0.12f), Color.Transparent),
                        startY = 0f, endY = size.height * 0.4f,
                    ))
                }
                .border(0.7.dp, C.glassCardElevatedBorder, RoundedCornerShape(50.dp))
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search your library", style = Type.body, color = C.textTertiary)
                BasicSearchField(query, vm::setQuery, C.text)
            }
        }

        // ── Segmented control — glass pill style with green active ──────────
        Row(
            Modifier
                .padding(horizontal = Spacing.lg, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.glassCardElevated)
                .border(0.5.dp, C.glassCardElevatedBorder, RoundedCornerShape(14.dp))
                .padding(3.dp)
                .fillMaxWidth(),
        ) {
            LibTab.values().forEach { t ->
                val active = t == tab
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) C.accent.copy(alpha = 0.30f)      // Spotify Green active bg
                            else Color.Transparent
                        )
                        .then(
                            if (active) Modifier.border(0.5.dp, C.accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .pressableScale(onClick = { tab = t }, scaleTo = 0.97f)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        t.label,
                        style  = Type.callout,
                        color  = if (active) C.accent else C.textSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        when {
            !loaded   -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = C.accent) }
            tracks.isEmpty() -> EmptyLibrary()
            else -> when (tab) {
                LibTab.SONGS  -> SongsList(vm)
                LibTab.ALBUMS -> AlbumsGrid(vm, onOpenAlbum)
                LibTab.ARTISTS -> ArtistsList(vm, onOpenArtist)
            }
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .pressableScale(onClick = onClick, scaleTo = 0.85f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = C.textSecondary, modifier = Modifier.size(22.dp))
    }
}

// ─── Songs List ────────────────────────────────────────────────────────────────

@Composable
private fun SongsList(vm: PlayerViewModel) {
    val C      = LocalAppColors.current
    val list   by vm.filteredTracks.collectAsState()
    val current by vm.current.collectAsState()
    val sort   by vm.sort.collectAsState()
    var sheetTrack by remember { mutableStateOf<Track?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
        // Play All / Shuffle action bar
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionPill("Play", Icons.Filled.PlayArrow, C.accent, Color.White, Modifier.weight(1f)) {
                    if (list.isNotEmpty()) vm.playList(list, list.first().id)
                }
                ActionPill("Shuffle", Icons.Outlined.Shuffle, C.bg3, C.text, Modifier.weight(1f)) {
                    if (list.isNotEmpty()) vm.shuffleAll()
                }
            }
        }
        // Sort control row
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${list.size} songs", style = Type.footnote, color = C.textSecondary, modifier = Modifier.weight(1f))
                SortMenu(sort) { vm.setSort(it) }
            }
        }
        itemsIndexed(list, key = { _, t -> t.id }) { _, song ->
            SongRow(song, current?.id == song.id, onClick = { vm.play(song) }, onLongClick = { sheetTrack = song })
        }
    }
    sheetTrack?.let { tk ->
        com.beatdrop.kt.ui.components.TrackActionsSheet(vm, tk, onDismiss = { sheetTrack = null })
    }
}

// ─── Action Pill — glass button with accent color ─────────────────────────────

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current
    val isAccent = bg == C.accent
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isAccent) C.accent.copy(alpha = 0.25f)
                else if (C.isDark) Color.White.copy(alpha = 0.08f)
                else Color.Black.copy(alpha = 0.05f),
            )
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isAccent) 0.12f else if (C.isDark) 0.06f else 0.08f),
                        Color.Transparent,
                    ),
                    startY = 0f, endY = size.height * 0.4f,
                ))
            }
            .border(0.5.dp, if (isAccent) C.accent.copy(alpha = 0.25f) else C.glassBorder, RoundedCornerShape(14.dp))
            .pressableScale(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = Type.headline, color = fg)
    }
}

// ─── Albums Grid ───────────────────────────────────────────────────────────────

@Composable
private fun AlbumsGrid(vm: PlayerViewModel, onOpen: (String, String) -> Unit) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current
    val albums by vm.albumGroups.collectAsState()

    LazyVerticalGrid(
        columns        = GridCells.Fixed(2),
        contentPadding = PaddingValues(Spacing.lg, 4.dp, Spacing.lg, 170.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
    ) {
        items(albums, key = { it.album + it.artist }) { a ->
            Column(Modifier.pressableScale(onClick = { onOpen(a.album, a.artist) })) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(C.bg3)
                        .border(0.5.dp, C.glassBorder, RoundedCornerShape(Radius.md)),
                ) {
                    AsyncImage(
                        model  = ImageRequest.Builder(ctx).data(a.artworkUri).crossfade(true).size(256).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(a.album,  style = Type.callout,  color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.artist, style = Type.footnote, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─── Artists List ──────────────────────────────────────────────────────────────

@Composable
private fun ArtistsList(vm: PlayerViewModel, onOpen: (String) -> Unit) {
    val C      = LocalAppColors.current
    val artists by vm.artistGroups.collectAsState()

    LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
        itemsIndexed(artists, key = { _, ar -> ar.artist }) { _, ar ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressableScale(onClick = { onOpen(ar.artist) })
                    .padding(horizontal = Spacing.lg, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Accent green gradient avatar
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(C.accent, C.accentDark)))
                        .border(0.5.dp, C.accent.copy(alpha = 0.30f), CircleShape),
                    Alignment.Center,
                ) {
                    Text(ar.artist.take(1).uppercase(), style = Type.title3, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(ar.artist, style = Type.headline, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ar.trackCount} songs", style = Type.footnote, color = C.textSecondary)
                }
            }
        }
    }
}

// ─── Song Row ──────────────────────────────────────────────────────────────────

@Composable
private fun SongRow(song: Track, isCurrent: Boolean, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.lg, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(C.bg3),
            Alignment.Center,
        ) {
            AsyncImage(
                model  = ImageRequest.Builder(ctx).data(song.artworkUri).crossfade(true).size(96).build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
            // Music note placeholder
            Icon(
                Icons.Outlined.MusicNote, null,
                tint = C.textTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style  = Type.headline,
                color  = if (isCurrent) C.accent else C.text,  // Green when current
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                style  = Type.footnote,
                color  = C.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(fmt(song.durationMs), style = Type.footnote, color = C.textTertiary)
    }
}

// ─── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyLibrary() {
    val C = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.MusicNote, null, tint = C.textTertiary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("No music found", style = Type.title3, color = C.text)
        Text("Add audio files to your device.", style = Type.footnote, color = C.textSecondary)
    }
}

// ─── Basic Search Field ────────────────────────────────────────────────────────

@Composable
private fun BasicSearchField(
    value: String,
    onChange: (String) -> Unit,
    color: Color,
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    androidx.compose.foundation.text.BasicTextField(
        value          = value,
        onValueChange  = onChange,
        singleLine     = true,
        textStyle      = Type.body.copy(color = color),
        cursorBrush    = androidx.compose.ui.graphics.SolidColor(LocalAppColors.current.accent),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { keyboardController?.hide() },
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

// ─── Sort Menu ─────────────────────────────────────────────────────────────────

@Composable
private fun SortMenu(current: SortMode, onPick: (SortMode) -> Unit) {
    val C = LocalAppColors.current
    var open by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(C.glassCardElevated)
                .border(0.5.dp, C.glassCardElevatedBorder, RoundedCornerShape(10.dp))
                .pressableScale(onClick = { open = true }, scaleTo = 0.95f)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.SwapVert, null, tint = C.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(current.label, style = Type.caption, color = C.text)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.values().forEach { mode ->
                DropdownMenuItem(
                    text    = { Text(mode.label, color = if (mode == current) C.accent else C.text) },
                    onClick = { onPick(mode); open = false },
                )
            }
        }
    }
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}