package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.clickable

/**
 * Inline lyrics view for NowPlaying screen (Apple Music style).
 * Shows synced lyrics with a blurred, vertically scrollable overlay.
 * Active line is highlighted, previous lines dimmed.
 */
@Composable
fun AppleLyrics(
    lines: List<String?>,
    activeLineIndex: Int = 3,
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    // Auto-scroll to active line
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex, scrollOffset = -200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xDD14283C)),
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            )
        }

        if (expanded) {
            // Full lyrics overlay
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 450.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                userScrollEnabled = true,
            ) {
                itemsIndexed(lines.filterNotNull()) { index, line ->
                    val isActive = index == activeLineIndex
                    val isPast = index < activeLineIndex

                    val alpha by animateFloatAsState(
                        when {
                            isActive -> 1f
                            isPast -> 0.35f
                            else -> 0.55f
                        },
                        animationSpec = tween(400),
                        label = "lyric_alpha_$index",
                    )

                    val scale by animateFloatAsState(
                        if (isActive) 1f else 0.92f,
                        animationSpec = tween(400),
                        label = "lyric_scale_$index",
                    )

                    Text(
                        line,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                            color = if (isActive) Accent else Color.White.copy(alpha = alpha),
                            fontSize = if (isActive) 26.sp else 22.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Start,
                        ),
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    )
                }
            }

            // Close button
            TextButton(
                onClick = onToggle,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        } else {
            // Collapsed preview — show active line + next
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                val filteredLines = lines.filterNotNull()
                if (activeLineIndex in filteredLines.indices) {
                    Text(
                        filteredLines[activeLineIndex],
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                        ),
                        maxLines = 1,
                    )
                }
                if ((activeLineIndex + 1) in filteredLines.indices) {
                    Text(
                        filteredLines[activeLineIndex + 1],
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
