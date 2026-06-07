package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.DownloadManagerV2
import com.beatdrop.kt.youtube.YouTubePlaylist

@Composable
fun PlaylistDownloadScreen(
    vm: PlayerViewModel,
    playlistId: String,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    var playlistInfo by remember { mutableStateOf<com.beatdrop.kt.youtube.PlaylistInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        isLoading = true
        playlistInfo = YouTubePlaylist.fetchPlaylist(playlistId)
        isLoading = false
    }

    val info = playlistInfo

    ScreenScaffold(ambientColor = C.glassAmbient) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = info?.title?.ifBlank { "Playlist" } ?: "Playlist",
                subtitle = info?.let { "${it.videos.size} videos" } ?: playlistId,
                onBack = onBack,
                leadingIcon = Ic.Playlist,
            )

            // Batch controls
            info?.let { playlist ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${playlist.videos.size} videos",
                        style = Type.footnote, color = C.textSecondary,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == playlist.videos.size) emptySet()
                        else playlist.videos.map { it.videoId }.toSet()
                    }) {
                        Text(
                            if (selectedIds.size == playlist.videos.size) "Deselect All" else "Select All",
                            color = C.accent, style = Type.caption,
                        )
                    }
                    if (selectedIds.isNotEmpty()) {
                        TintedGlassButton(modifier = Modifier.height(40.dp)) {
                            Row(
                                Modifier.fillMaxSize().pressableScale(onClick = {
                                    val app = vm.getApplication<android.app.Application>()
                                    val selected = playlist.videos.filter { it.videoId in selectedIds }
                                    DownloadManagerV2.enqueueBatch(selected, app)
                                    isDownloading = true
                                }),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(12.dp))
                                Icon(Ic.Download, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Download ${selectedIds.size}",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = Type.callout,
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = C.accent)
                }
                info == null || info.videos.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Could not load playlist", style = Type.body, color = C.textSecondary)
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
                    contentPadding = PaddingValues(bottom = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(info.videos, key = { it.videoId }) { result ->
                        val isSelected = result.videoId in selectedIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .glassRow()
                                .pressableScale(onClick = {
                                    selectedIds = if (isSelected) selectedIds - result.videoId
                                    else selectedIds + result.videoId
                                })
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedIds = if (isSelected) selectedIds - result.videoId
                                    else selectedIds + result.videoId
                                },
                                colors = CheckboxDefaults.colors(checkedColor = C.accent),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(result.title, style = Type.callout, color = C.text,
                                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${result.author} · ${result.durationText}",
                                    style = Type.caption, color = C.textSecondary,
                                )
                            }
                            IconButton(onClick = { vm.playOnline(result) }, modifier = Modifier.size(36.dp)) {
                                Icon(Ic.Play, "Play", tint = C.textTertiary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
