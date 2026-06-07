package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.data.DownloadHistory
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.util.StorageHelper

/** Storage management screen. */
@Composable
fun StorageScreen(onBack: () -> Unit) {
    val C = LocalAppColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showClearDialog by remember { mutableStateOf(false) }

    val storageLocations = remember { StorageHelper.getStorageLocations(context) }
    val totalDownloadSize = remember { DownloadHistory.totalDownloadSize() }
    val completedCount = remember { DownloadHistory.countByStatus("completed") }
    val deletedCount = remember { DownloadHistory.countByStatus("deleted") }

    ScreenScaffold(ambientColor = C.glassAmbient) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = "Storage",
                subtitle = StorageHelper.formatSize(totalDownloadSize),
                onBack = onBack,
                leadingIcon = Ic.Storage,
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState)
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = 190.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // Download size hero card
                Box(
                    Modifier.fillMaxWidth().glassCard(radius = Radius.lg).padding(20.dp),
                ) {
                    Column {
                        Text("Download Storage", style = Type.title3, color = C.text, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            StorageHelper.formatSize(totalDownloadSize),
                            style = Type.largeTitle.copy(fontWeight = FontWeight.Black),
                            color = C.accent,
                        )
                        Text("$completedCount files", style = Type.subhead, color = C.textSecondary)
                    }
                }

                SectionHeader("Storage Locations")

                storageLocations.forEach { storage ->
                    StorageLocationCard(storage)
                    Spacer(Modifier.height(8.dp))
                }

                SectionHeader("Actions")

                OutlinedButton(
                    onClick = { showClearDialog = true },
                    shape = RoundedCornerShape(Radius.md),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF453A)),
                ) {
                    Icon(Ic.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Downloads", style = Type.callout)
                }

                Spacer(Modifier.height(8.dp))

                if (deletedCount > 0) {
                    OutlinedButton(
                        onClick = { /* Recover deleted — re-download */ },
                        shape = RoundedCornerShape(Radius.md),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Ic.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recover $deletedCount Deleted Downloads", style = Type.callout)
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Downloads?") },
                text = { Text("This will delete all downloaded music files. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                    }) { Text("Delete All", color = Color(0xFFFF453A)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun StorageLocationCard(storage: StorageHelper.StorageInfo) {
    val C = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .glassCard(radius = Radius.md)
            .padding(14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (storage.isRemovable) Ic.SdCard else Ic.Storage,
                    null, tint = C.accent, modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(storage.label, style = Type.callout, color = C.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${storage.freeGB.toInt()} / ${storage.totalGB.toInt()} GB",
                    style = Type.footnote, color = C.textSecondary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { storage.usedPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (storage.usedPercent > 90) Color(0xFFFF453A) else C.accent,
                trackColor = C.bg3,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${storage.usedPercent}% used · ${String.format("%.1f", storage.freeGB)} GB free",
                style = Type.caption, color = C.textTertiary,
            )
        }
    }
}
