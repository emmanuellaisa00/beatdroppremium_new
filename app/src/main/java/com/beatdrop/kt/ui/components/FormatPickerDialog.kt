package com.beatdrop.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.youtube.FormatKind
import com.beatdrop.kt.youtube.FormatOption
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Format picker dialog — shown before download to let users choose quality/format.
 * SnapTube-style: lists all available formats with size estimates.
 */
@Composable
fun FormatPickerDialog(
    title: String,
    audioFormats: List<FormatOption>,
    videoFormats: List<FormatOption>,
    muxedFormats: List<FormatOption>,
    onSelected: (FormatOption, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val C = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=audio, 1=video

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = C.bg1,
        title = {
            Column {
                Text("Download", color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(title, color = C.textSecondary, fontSize = 13.sp, maxLines = 1)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Tab selector: Audio | Video
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.bg2)
                        .padding(3.dp)
                ) {
                    TabButton("Audio", selectedTab == 0) { selectedTab = 0 }
                    Spacer(Modifier.width(4.dp))
                    TabButton("Video", selectedTab == 1) { selectedTab = 1 }
                }

                Spacer(Modifier.height(12.dp))

                val formats = if (selectedTab == 0) audioFormats else videoFormats + muxedFormats

                if (formats.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        Text("Loading formats…", color = C.textTertiary)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(formats) { format ->
                            FormatRow(
                                format = format,
                                isAudio = selectedTab == 0,
                                onClick = { onSelected(format, selectedTab == 1) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.textSecondary)
            }
        }
    )
}

@Composable
private fun RowScope.TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) C.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else C.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FormatRow(format: FormatOption, isAudio: Boolean, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(C.bg2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isAudio) Ic.MusicNote else Ic.Video,
            contentDescription = null,
            tint = C.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(format.label, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (format.height > 0) {
                Text(
                    "${format.width}×${format.height}" + if (format.fps > 0) " @${format.fps}fps" else "",
                    color = C.textTertiary, fontSize = 11.sp,
                )
            }
        }
        Text(
            "${format.estimatedSizeMB} MB",
            color = C.textSecondary, fontSize = 12.sp,
        )
    }
}
