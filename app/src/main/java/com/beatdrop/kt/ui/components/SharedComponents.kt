package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.Album
import com.beatdrop.kt.data.Playlist
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke

// ═══════════════════════════════════════════════════
// BOTTOM DOCK — 4 tabs: Discover, Search, Library, Radio
// ═══════════════════════════════════════════════════
@Composable
fun BottomDock(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        "Discover" to Icons.Filled.Home,
        "Search" to Icons.Filled.Search,
        "Library" to Icons.Filled.LibraryMusic,
        "Radio" to Icons.Filled.Radio,
    )

    Surface(
        modifier = modifier.fillMaxWidth().height(66.dp),
        shape = RoundedCornerShape(33.dp),
        color = DockBg,
        shadowElevation = 28.dp,
        border = BorderStroke(0.5.dp, GlassBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isActive = index == activeTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) },
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).then(if (isActive) Modifier.background(Color.White.copy(alpha = 0.10f), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, label, modifier = Modifier.size(22.dp), tint = if (isActive) Color.White else Color.White.copy(alpha = 0.45f))
                    }
                    if (isActive) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Accent, modifier = Modifier.offset(y = (-4).dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// MINI PLAYER — premium glass
// ═══════════════════════════════════════════════════
@Composable
fun MiniPlayer(
    trackName: String,
    artistName: String,
    coverIndex: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxWidth().height(66.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MiniBg,
        shadowElevation = 24.dp,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Box {
            Row(modifier = Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(CoverGradients.get(coverIndex)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trackName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artistName, style = MaterialTheme.typography.bodySmall.copy(color = TextMedium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Cast, null, tint = TextHigh, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(38.dp)) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).align(Alignment.BottomStart).background(Color.White.copy(alpha = 0.08f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(1.dp)).background(AccentGradient))
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// FILTER PILLS
// ═══════════════════════════════════════════════════
@Composable
fun FilterPills(pills: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.padding(horizontal = 20.dp)) {
        items(pills.indices.toList()) { i ->
            val active = i == selectedIndex
            Surface(
                modifier = Modifier.height(34.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelected(i) },
                shape = CircleShape, color = if (active) Accent else SurfaceTile,
                border = BorderStroke(1.dp, if (active) Color.Transparent else GlassBorder),
                shadowElevation = if (active) 6.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(pills[i], style = MaterialTheme.typography.labelLarge.copy(color = if (active) Color.White else TextHigh))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// QUICK ACCESS GRID
// ═══════════════════════════════════════════════════
@Composable
fun QuickAccessGrid(items: List<Playlist>, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Surface(
                        modifier = Modifier.weight(1f).height(58.dp).clickable(onClick = { onClick(item.id) }),
                        shape = RoundedCornerShape(12.dp), color = SurfaceTile, border = BorderStroke(1.dp, GlassBorder), shadowElevation = 4.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(58.dp).background(CoverGradients.get(item.coverIndex)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(20.dp))
                            }
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// ALBUM CARD + ROW
// ═══════════════════════════════════════════════════
@Composable
fun AlbumCard(title: String, artist: String, coverIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(154.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(154.dp).clip(RoundedCornerShape(12.dp)).background(CoverGradients.get(coverIndex)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(56.dp))
        }
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
        Text(artist, style = MaterialTheme.typography.bodyMedium.copy(color = TextLow), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun AlbumCardRow(albums: List<Album>, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 20.dp), modifier = modifier) {
        items(albums) { AlbumCard(it.title, it.artist, it.coverIndex, { onClick(it.id) }) }
    }
}

// ═══════════════════════════════════════════════════
// TRACK ROW — with artist + album click-through
// ═══════════════════════════════════════════════════
@Composable
fun TrackRow(
    track: Track,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .then(if (track.isPlaying) Modifier.background(Accent.copy(alpha = 0.07f), RoundedCornerShape(10.dp)) else Modifier),
    ) {
        Box(modifier = Modifier.width(22.dp), contentAlignment = Alignment.Center) {
            if (track.isPlaying) EqualizerAnimation() else Text("${index + 1}", style = MaterialTheme.typography.bodyMedium.copy(color = TextHint, fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(CoverGradients.get(track.coverIndex)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (track.isPlaying) Accent else Color.White,
                    fontWeight = if (track.isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row {
                if (track.artist.isNotBlank()) {
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMedium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onArtistClick() },
                    )
                }
                if (track.album.isNotEmpty()) {
                    Text(" · ", style = MaterialTheme.typography.bodySmall.copy(color = TextHint), modifier = Modifier.padding(top = 2.dp))
                    Text(
                        track.album,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextLow),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onAlbumClick() },
                    )
                }
            }
        }
        Text(track.duration, style = MaterialTheme.typography.bodySmall.copy(color = TextHint, fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.MoreVert, null, tint = TextHigh, modifier = Modifier.size(18.dp)) }
    }
}

// ═══════════════════════════════════════════════════
// EQ ANIMATION
// ═══════════════════════════════════════════════════
@Composable
fun EqualizerAnimation(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp), verticalAlignment = Alignment.Bottom, modifier = modifier.height(14.dp)) {
        repeat(3) { i ->
            val h by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(750, 250 * i, EaseInOut), RepeatMode.Reverse), "eq$i")
            Box(modifier = Modifier.width(3.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(Accent))
        }
    }
}

// ═══════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════
@Composable
fun SectionHeader(title: String, actionLabel: String? = "See all", onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (actionLabel != null) Text(
            actionLabel.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(color = TextHint, fontWeight = FontWeight.ExtraBold, letterSpacing = (0.10 * 11).sp),
            modifier = Modifier.let { m -> onAction?.let { m.clickable(onClick = it) } ?: m },
        )
    }
}

// ═══════════════════════════════════════════════════
// IDENTITY BAR
// ═══════════════════════════════════════════════════
@Composable
fun IdentityBar(greeting: String, userName: String, onSearch: (() -> Unit)? = null, onAdd: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(Brush.linearGradient(listOf(Accent, AccentDark)), CircleShape), contentAlignment = Alignment.Center) {
            Text(userName.firstOrNull()?.uppercase() ?: "A", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        if (userName.isNotEmpty()) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium))
            Text(" ", style = MaterialTheme.typography.bodyMedium)
            Text(userName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        } else {
            Text(greeting, style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium))
        }
        Spacer(Modifier.weight(1f))
        onSearch?.let { IconButton(onClick = it, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Search, null, tint = TextHigh, modifier = Modifier.size(21.dp)) } }
        onAdd?.let { IconButton(onClick = it, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Add, null, tint = TextHigh, modifier = Modifier.size(21.dp)) } }
    }
}

// ═══════════════════════════════════════════════════
// BROWSE TILE
// ═══════════════════════════════════════════════════
@Composable
fun BrowseTile(label: String, tileIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(100.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = Color.Transparent, shadowElevation = 10.dp) {
        Box(modifier = Modifier.fillMaxSize().background(TileGradients.get(tileIndex), RoundedCornerShape(14.dp)).padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 20.sp), modifier = Modifier.align(Alignment.TopStart))
            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp).size(72.dp).background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// BACK BUTTON
// ═══════════════════════════════════════════════════
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.size(36.dp), shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}
