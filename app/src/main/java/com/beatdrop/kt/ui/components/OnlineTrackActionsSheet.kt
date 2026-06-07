package com.beatdrop.kt.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.youtube.DownloadStatus
import com.beatdrop.kt.youtube.OnlineResult

/** Long-press action sheet for online/search/discover tracks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineTrackActionsSheet(
    vm: PlayerViewModel,
    result: OnlineResult,
    onDismiss: () -> Unit,
) {
    val C = LocalAppColors.current
    val jobs by vm.downloadJobs.collectAsState()
    val job = jobs[result.videoId]
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = C.glassSheetBackground,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 4.dp, 20.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(result.title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.author, color = C.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = C.separator)

        ActionItem(Ic.Playlist, "Play next") { vm.playOnlineNext(result); onDismiss() }
        ActionItem(Ic.Add, "Add to queue") { vm.addOnlineToQueue(result); onDismiss() }
        when (job?.status) {
            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED ->
                ActionItem(Ic.Close, "Cancel download", tint = C.textSecondary) { vm.cancelDownload(result.videoId); onDismiss() }
            DownloadStatus.FAILED ->
                ActionItem(Ic.Refresh, "Retry download", tint = C.accent) { vm.retryDownload(result); onDismiss() }
            DownloadStatus.COMPLETED ->
                ActionItem(Ic.Check, "Downloaded", tint = C.accent) { onDismiss() }
            else ->
                ActionItem(Ic.Download, "Download") { vm.downloadOnline(result); onDismiss() }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick, scaleTo = 0.98f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: C.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint ?: C.text, fontSize = 15.sp)
    }
}
