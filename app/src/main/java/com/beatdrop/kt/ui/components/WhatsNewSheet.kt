package com.beatdrop.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke

/**
 * Post-update changelog sheet.
 * Shows what's new after the app is updated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    version: String = "2.0.0",
) {
    val changes = listOf(
        "🎵 Redesigned with premium glass UI",
        "📻 New Radio tab with auto-mix stations",
        "📥 Playlist bulk download support",
        "🎤 Inline Apple-style lyrics",
        "🐛 Fixed playback queue ordering",
        "⚡ Faster search and library loading",
    )

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
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            ) {
                // Version badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Accent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Accent.copy(alpha = 0.30f)),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Text(
                        "v$version",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Accent,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                Text(
                    "What's New",
                    style = MaterialTheme.typography.headlineLarge,
                )

                Text(
                    "Here's what we've been working on",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 320.dp),
                ) {
                    items(changes) { change ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceTile,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Check, null,
                                        tint = Accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                change,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextHigh,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Continue button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    color = Accent,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Continue",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}
