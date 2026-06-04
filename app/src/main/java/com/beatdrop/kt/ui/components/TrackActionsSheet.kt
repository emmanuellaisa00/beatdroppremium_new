package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Spotify/Apple-Music-style long-press action sheet for a track:
 * Play next · Add to queue · Add to playlist · Like.
 * Glass-styled bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(vm: PlayerViewModel, track: Track, onDismiss: () -> Unit) {
    val C = LocalAppColors.current
    val liked by vm.liked.collectAsState()
    val playlists by vm.playlists.collectAsState()
    var showPlaylists by remember { mutableStateOf(false) }
    val isLiked = liked.contains(track.id)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = C.glassSheetBackground,
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(20.dp, 4.dp, 20.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = C.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = C.separator)

        if (!showPlaylists) {
            ActionItem(if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (isLiked) "Remove from Liked" else "Add to Liked",
                tint = if (isLiked) C.accent else C.text) { vm.toggleLike(track.id) }
            ActionItem(Icons.Outlined.QueueMusic, "Play next") { vm.playNext(track); onDismiss() }
            ActionItem(Icons.Outlined.Add, "Add to queue") { vm.addToQueueEnd(track); onDismiss() }
            ActionItem(Icons.Outlined.PlaylistAdd, "Add to playlist…") { showPlaylists = true }
            Spacer(Modifier.height(24.dp))
        } else {
            Text("Add to playlist", color = C.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 4.dp))
            if (playlists.isEmpty()) {
                Text("No playlists yet. Create one in the Playlists tab.",
                    color = C.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(20.dp, 8.dp))
            }
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(playlists.keys.toList()) { name ->
                    ActionItem(Icons.Outlined.PlaylistAdd, name) {
                        vm.addToPlaylist(name, track.id); onDismiss()
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().pressableScale(onClick = onClick, scaleTo = 0.98f).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: C.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint ?: C.text, fontSize = 15.sp)
    }
}
