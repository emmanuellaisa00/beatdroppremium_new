package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import kotlin.math.roundToInt

private val ROW_HEIGHT = 64.dp

@Composable
fun QueueScreen(vm: PlayerViewModel, onClose: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val queue by vm.queue.collectAsState()
    val current by vm.current.collectAsState()
    val density = LocalDensity.current
    val rowPx = with(density) { ROW_HEIGHT.toPx() }

    // Drag state: index being dragged + live vertical offset (px).
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(Modifier.fillMaxSize().background(Color.Transparent).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, "Close", tint = C.text) }
            Text("Up Next", color = C.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Text("Hold ⠿ to reorder", color = C.textTertiary, fontSize = 11.sp, modifier = Modifier.padding(end = 12.dp))
        }
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Queue is empty", color = C.textSecondary) }
            return@Column
        }

        val listState = rememberLazyListState()
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 40.dp)) {
            itemsIndexed(queue, key = { i, t -> t.id + i }) { index, t ->
                val isCurrent = current?.id == t.id
                val isDragging = index == draggingIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 16f else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                        }
                        .background(if (isDragging) C.bg2 else C.bg0)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Drag handle — long-press then drag to reorder
                    Icon(
                        Icons.Filled.DragHandle, "Reorder", tint = C.textTertiary,
                        modifier = Modifier
                            .size(28.dp)
                            .pointerInput(index, queue.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingIndex = index; dragOffset = 0f },
                                    onDragEnd = {
                                        if (draggingIndex >= 0) {
                                            val moved = (dragOffset / rowPx).roundToInt()
                                            val target = (draggingIndex + moved).coerceIn(0, queue.size - 1)
                                            if (target != draggingIndex) vm.moveQueueItem(draggingIndex, target)
                                        }
                                        draggingIndex = -1; dragOffset = 0f
                                    },
                                    onDragCancel = { draggingIndex = -1; dragOffset = 0f },
                                    onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                                )
                            },
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(Radius.sm)).background(C.bg3)) {
                        AsyncImage(model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).size(96).build(),
                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        Modifier.weight(1f).pressableScale(onClick = { vm.playQueueIndex(index) }, scaleTo = 0.98f),
                    ) {
                        Text(t.title, color = if (isCurrent) C.accent else C.text,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(t.artist, color = C.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (isCurrent) {
                        Icon(Icons.Filled.GraphicEq, "Now playing", tint = C.accent, modifier = Modifier.size(18.dp))
                    } else {
                        IconButton(onClick = { vm.removeFromQueue(index) }) {
                            Icon(Icons.Filled.Close, "Remove", tint = C.textTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
