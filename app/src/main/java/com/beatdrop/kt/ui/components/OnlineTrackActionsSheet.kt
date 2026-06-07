package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp

/**
 * Long-press action sheet for ONLINE / catalogue tracks.
 * Includes streaming-specific actions (download, add to library, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineTrackActionsSheet(
    visible: Boolean,
    track: Track,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onAddToLibrary: () -> Unit = {},
    onDownload: () -> Unit = {},
    onGoToAlbum: () -> Unit = {},
    onGoToArtist: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenInBeatDrop: () -> Unit = {},
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = GlassBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        .width(36.dp).height(4.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                )
            },
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                // ── Track header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CoverGradients.get(track.coverIndex)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMedium),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    // Quick download button
                    Surface(
                        onClick = { onDownload() },
                        shape = RoundedCornerShape(12.dp),
                        color = Accent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Accent.copy(alpha = 0.30f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Filled.Download, null, tint = Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save", style = MaterialTheme.typography.bodySmall.copy(color = Accent, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Divider(color = GlassBorder, modifier = Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(8.dp))

                // ── Actions ──
                val actions: List<Triple<ImageVector, String, () -> Unit>> = listOf(
                    Triple(Icons.Filled.SkipNext, "Play next", onPlayNext),
                    Triple(Icons.Filled.PlaylistAdd, "Add to queue", onAddToQueue),
                    Triple(Icons.Filled.PlaylistAdd, "Add to playlist", onAddToPlaylist),
                    Triple(Icons.Filled.LibraryAdd, "Add to library", onAddToLibrary),
                    Triple(Icons.Filled.Download, "Download", onDownload),
                    Triple(Icons.Filled.Album, "Go to album", onGoToAlbum),
                    Triple(Icons.Filled.Person, "Go to artist", onGoToArtist),
                    Triple(Icons.Filled.Share, "Share", onShare),
                    Triple(Icons.Filled.OpenInNew, "Open in BeatDrop", onOpenInBeatDrop),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.heightIn(max = 400.dp).padding(horizontal = 12.dp),
                ) {
                    items(actions) { (icon, label, action) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { action(); onDismiss() }
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                        ) {
                            Icon(icon, null, tint = TextHigh, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
