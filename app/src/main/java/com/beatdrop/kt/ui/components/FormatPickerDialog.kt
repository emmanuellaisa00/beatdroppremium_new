package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.*

/**
 * Download quality / format picker dialog.
 * Used when downloading tracks or playlists.
 */

data class FormatOption(
    val label: String,
    val description: String,
    val bitrate: String,
    val size: String,
)

@Composable
fun FormatPickerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    val formats = listOf(
        FormatOption("Lossless", "FLAC · Best quality", "1411 kbps", "~45 MB"),
        FormatOption("High", "AAC 320", "320 kbps", "~12 MB"),
        FormatOption("Medium", "AAC 192", "192 kbps", "~7 MB"),
        FormatOption("Low", "AAC 128", "128 kbps", "~5 MB"),
    )

    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = GlassBg,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Text(
                        "Download quality",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        "Choose format and bitrate",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMedium),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    formats.forEachIndexed { index, format ->
                        val isSelected = index == selectedIndex
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Accent.copy(alpha = 0.12f) else SurfaceTile,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Accent.copy(alpha = 0.40f) else GlassBorder,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { selectedIndex = index }
                                    .padding(16.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            format.label,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) Accent else Color.White,
                                            ),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            format.bitrate,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextLow,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        )
                                    }
                                    Text(
                                        format.description,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextLow),
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                                Text(
                                    format.size,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextHint,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Spacer(Modifier.width(10.dp))
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Accent,
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Check, null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .border(1.5.dp, GlassBorder, CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Surface(
                    onClick = { onSelect(selectedIndex); onDismiss() },
                    shape = RoundedCornerShape(14.dp),
                    color = Accent,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            "Download",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMedium,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            },
        )
    }
}
