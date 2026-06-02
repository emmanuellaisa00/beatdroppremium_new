package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.DownloadHistory
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.util.ShareHelper
import com.beatdrop.kt.util.StorageHelper
import com.beatdrop.kt.youtube.DownloadJobV2
import com.beatdrop.kt.youtube.DownloadStatusV2
import com.beatdrop.kt.youtube.DownloadManagerV2
import java.io.File

/**
 * Download management screen — shows active downloads, history, and storage info.
 * Supports pause/resume/cancel/retry/share/re-download.
 */
@Composable
fun DownloadsScreen(
    vm: com.beatdrop.kt.PlayerViewModel,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    val activeJobs by DownloadManagerV2.jobs.collectAsState()
    val history = remember { DownloadHistory.getAll() }
    var showHistory by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = C.text) }
            Text("Downloads", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(
                StorageHelper.formatSize(DownloadHistory.totalDownloadSize()),
                color = C.textTertiary, fontSize = 12.sp,
            )
        }

        // Tab selector
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            FilterChip(
                selected = !showHistory,
                onClick = { showHistory = false },
                label = { Text("Active") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = C.accent),
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = showHistory,
                onClick = { showHistory = true },
                label = { Text("History") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = C.accent),
            )
            Spacer(Modifier.weight(1f))
            // Retry all failed
            val failedCount = activeJobs.values.count {
                it.status == com.beatdrop.kt.youtube.DownloadStatusV2.FAILED
            }
            if (failedCount > 0) {
                TextButton(onClick = {
                    val app = vm.getApplication<android.app.Application>()
                    DownloadManagerV2.retryAllFailed(app)
                }) {
                    Text("Retry All ($failedCount)", color = C.accent, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (!showHistory) {
            // Active downloads
            val active = activeJobs.values.filter {
                it.status != com.beatdrop.kt.youtube.DownloadStatusV2.IDLE
            }.sortedByDescending { it.status.ordinal }

            if (active.isEmpty()) {
                EmptyState("No active downloads")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(active, key = { it.videoId }) { job ->
                        ActiveDownloadRow(job)
                    }
                }
            }
        } else {
            // Download history
            val records = DownloadHistory.getAll()
            if (records.isEmpty()) {
                EmptyState("No download history yet")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(records, key = { it.videoId }) { record ->
                        HistoryRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadRow(job: com.beatdrop.kt.youtube.DownloadJobV2) {
    val C = LocalAppColors.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = ctx.applicationContext as android.app.Application

    Column(
        Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(job.title, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    when (job.status) {
                        com.beatdrop.kt.youtube.DownloadStatusV2.QUEUED -> "Queued"
                        com.beatdrop.kt.youtube.DownloadStatusV2.DOWNLOADING -> "${job.progress}%"
                        com.beatdrop.kt.youtube.DownloadStatusV2.PAUSED -> "Paused — ${job.progress}%"
                        com.beatdrop.kt.youtube.DownloadStatusV2.COMPLETED -> "Complete"
                        com.beatdrop.kt.youtube.DownloadStatusV2.FAILED -> "Failed: ${job.error?.take(50)}"
                        else -> ""
                    },
                    color = C.textSecondary, fontSize = 12.sp, maxLines = 1,
                )
            }
            // Action buttons
            Row {
                when (job.status) {
                    com.beatdrop.kt.youtube.DownloadStatusV2.DOWNLOADING -> {
                        IconButton(onClick = { DownloadManagerV2.pause(job.videoId) }) {
                            Icon(Icons.Filled.Pause, "Pause", tint = C.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    com.beatdrop.kt.youtube.DownloadStatusV2.PAUSED -> {
                        IconButton(onClick = {
                            DownloadManagerV2.resume(job.videoId, app)
                        }) {
                            Icon(Icons.Filled.PlayArrow, "Resume", tint = C.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    com.beatdrop.kt.youtube.DownloadStatusV2.FAILED -> {
                        IconButton(onClick = {
                            job.result?.let { DownloadManagerV2.retry(it, app) }
                        }) {
                            Icon(Icons.Filled.Refresh, "Retry", tint = C.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    else -> {}
                }
                if (job.status == com.beatdrop.kt.youtube.DownloadStatusV2.DOWNLOADING ||
                    job.status == com.beatdrop.kt.youtube.DownloadStatusV2.QUEUED ||
                    job.status == com.beatdrop.kt.youtube.DownloadStatusV2.PAUSED) {
                    IconButton(onClick = {
                        DownloadManagerV2.cancel(job.videoId, app)
                    }) {
                        Icon(Icons.Filled.Close, "Cancel", tint = C.textTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        // Progress bar
        if (job.status == com.beatdrop.kt.youtube.DownloadStatusV2.DOWNLOADING ||
            job.status == com.beatdrop.kt.youtube.DownloadStatusV2.PAUSED) {
            LinearProgressIndicator(
                progress = { job.progress / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
                color = C.accent,
                trackColor = C.bg3,
            )
        }
        HorizontalDivider(color = C.bg3.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}

@Composable
private fun HistoryRow(record: DownloadHistory.DownloadRecord) {
    val C = LocalAppColors.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (record.isVideo) Icons.Filled.Videocam else Icons.Filled.MusicNote,
            null, tint = C.accent, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(record.title, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                "${record.artist} · ${record.format.uppercase()} · ${StorageHelper.formatSize(record.fileSize)} · ${record.status}",
                color = C.textTertiary, fontSize = 11.sp, maxLines = 1,
            )
        }
        // Share button
        if (record.status == "completed" && record.filePath != null) {
            IconButton(onClick = {
                val file = File(record.filePath)
                if (file.exists()) {
                    ShareHelper.shareFile(ctx, file, record.title)
                }
            }) {
                Icon(Icons.Filled.Share, "Share", tint = C.textTertiary, modifier = Modifier.size(18.dp))
            }
        }
        // Re-download button for deleted items
        if (record.status == "deleted") {
            IconButton(onClick = {
                DownloadManagerV2.redownload(record, app)
            }) {
                Icon(Icons.Filled.Refresh, "Re-download", tint = C.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    val C = LocalAppColors.current
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Download, null, tint = C.textTertiary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(text, color = C.textSecondary, fontSize = 15.sp)
        }
    }
}
