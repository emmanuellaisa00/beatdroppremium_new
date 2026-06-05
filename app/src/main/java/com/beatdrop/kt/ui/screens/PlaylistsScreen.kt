package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

@Composable
fun PlaylistsScreen(vm: PlayerViewModel, onBack: () -> Unit = {}, onOpen: (String) -> Unit) {
    val C = LocalAppColors.current
    val playlists by vm.playlists.collectAsState()
    val liked by vm.liked.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.14f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = "Playlists",
                subtitle = "${playlists.size + 1} libraries",
                onBack = onBack,
                leadingIcon = Ic.Library,
                trailing = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Ic.Add, "New playlist", tint = C.accent)
                    }
                },
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = 180.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Built-in "Liked Songs"
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glassRow()
                            .pressableScale(onClick = { onOpen(LIKED_NAME) })
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(Radius.sm)).background(C.accentSoft),
                            Alignment.Center,
                        ) {
                            Icon(Ic.Heart, null, tint = C.accent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Liked Songs", style = Type.title3, color = C.text, fontWeight = FontWeight.SemiBold)
                            Text("${liked.size} songs", style = Type.footnote, color = C.textSecondary)
                        }
                    }
                }
                items(playlists.keys.toList()) { name ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glassRow()
                            .pressableScale(onClick = { onOpen(name) })
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(Radius.sm)).background(C.bg3),
                            Alignment.Center,
                        ) {
                            Icon(Ic.Playlist, null, tint = C.textSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                name, style = Type.title3, color = C.text,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text("${playlists[name]?.size ?: 0} songs", style = Type.footnote, color = C.textSecondary)
                        }
                        IconButton(onClick = { vm.deletePlaylist(name) }) {
                            Icon(Ic.Delete, "Delete", tint = C.textTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (showCreate) {
            AlertDialog(
                onDismissRequest = { showCreate = false },
                confirmButton = {
                    TextButton(onClick = { vm.createPlaylist(newName); newName = ""; showCreate = false }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
                title = { Text("New playlist") },
                text = {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, placeholder = { Text("Name") })
                },
            )
        }
    }
}

const val LIKED_NAME = "__liked__"

@Composable
fun PlaylistDetailScreen(vm: PlayerViewModel, name: String, onBack: () -> Unit) {
    val C = LocalAppColors.current
    val playlists by vm.playlists.collectAsState()
    val liked by vm.liked.collectAsState()
    val tracksAll by vm.tracks.collectAsState()
    val current by vm.current.collectAsState()
    var sheetTrack by remember { mutableStateOf<com.beatdrop.kt.data.Track?>(null) }

    val isLiked = name == LIKED_NAME
    val title = if (isLiked) "Liked Songs" else name
    val tracks = remember(name, playlists, liked, tracksAll) {
        if (isLiked) tracksAll.filter { liked.contains(it.id) } else vm.playlistTracks(name)
    }

    ScreenScaffold(ambientColor = C.glassAmbient) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = title,
                subtitle = "${tracks.size} songs",
                onBack = onBack,
                leadingIcon = if (isLiked) Ic.Heart else Ic.Playlist,
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = 180.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Row(
                        Modifier.padding(top = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TintedGlassButton(modifier = Modifier.height(48.dp).width(140.dp)) {
                            Row(
                                Modifier.fillMaxSize().pressableScale(
                                    onClick = { if (tracks.isNotEmpty()) vm.playList(tracks, tracks.first().id) },
                                ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.Play, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = androidx.compose.ui.graphics.Color.White, style = Type.headline)
                            }
                        }
                        // Downloaded-status badge — green check pill when
                        // every track in this playlist exists on the
                        // device (any track with a non-null `data` path is
                        // on-device). User asked: 'when you save song it
                        // is supposed to save it to libraries and never
                        // loose it next time entering — playing all should
                        // have a tick on download.'
                        val allOnDevice = remember(tracks) {
                            tracks.isNotEmpty() && tracks.all { !it.data.isNullOrBlank() }
                        }
                        if (allOnDevice) {
                            Row(
                                Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(C.accent.copy(alpha = 0.18f))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.Check, "All downloaded",
                                    tint = C.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("All saved", color = C.accent,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (tracks.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().glassCard(radius = Radius.lg).padding(36.dp),
                            Alignment.Center,
                        ) { Text("No songs yet.", style = Type.body, color = C.textSecondary) }
                    }
                }
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glassRow()
                            .pressableScale(
                                onClick = { vm.playList(tracks, t.id) },
                                onLongClick = { sheetTrack = t },
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${index + 1}", style = Type.footnote, color = C.textTertiary, modifier = Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title,
                                style = Type.title3,
                                color = if (current?.id == t.id) C.accent else C.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                t.artist, style = Type.footnote, color = C.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!isLiked) {
                            IconButton(onClick = { vm.removeFromPlaylist(name, t.id) }) {
                                Icon(Ic.Delete, "Remove", tint = C.textTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        sheetTrack?.let { tk ->
            com.beatdrop.kt.ui.components.TrackActionsSheet(vm, tk, onDismiss = { sheetTrack = null })
        }
    }
}
