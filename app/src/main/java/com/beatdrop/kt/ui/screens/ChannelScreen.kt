package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Subscriptions
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.components.glassShadow
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.OnlineResult
import com.beatdrop.kt.youtube.YouTubeTrending

@Composable
fun ChannelScreen(
    vm: PlayerViewModel,
    channelId: String,
    channelName: String,
    channelThumb: String?,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    val context = LocalContext.current
    var isSubscribed by remember { mutableStateOf(Subscriptions.isSubscribed(channelId)) }
    var videos by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(channelId) {
        isLoading = true
        videos = YouTubeTrending.fetchChannelVideos(channelId)
        isLoading = false
    }

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.14f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(title = channelName, subtitle = "${videos.size} videos", onBack = onBack)

            // Channel info card
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    .glassCard(radius = Radius.lg)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar — glass-framed circle
                    Box(
                        Modifier
                            .size(64.dp)
                            .glassShadow(elevation = 12.dp, shape = CircleShape, isDark = C.isDark)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(C.accent, C.accentDark)))
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                        startY = 0f, endY = size.height * 0.4f,
                                    ),
                                )
                            }
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        channelThumb?.let {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(it).crossfade(true).size(160).build(),
                                contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } ?: Icon(Ic.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(channelName, style = Type.title2, color = C.text, fontWeight = FontWeight.Bold)
                        Text("${videos.size} videos", style = Type.subhead, color = C.textSecondary)
                    }

                    // Subscribe button
                    if (isSubscribed) {
                        Box(
                            Modifier
                                .height(38.dp)
                                .glassCard(radius = Radius.pill)
                                .pressableScale(onClick = {
                                    Subscriptions.unsubscribe(channelId); isSubscribed = false
                                })
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Subscribed", color = C.text, style = Type.callout, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TintedGlassButton(modifier = Modifier.height(38.dp)) {
                            Row(
                                Modifier.fillMaxSize().pressableScale(onClick = {
                                    Subscriptions.subscribe(Subscriptions.Channel(
                                        channelId = channelId,
                                        name = channelName,
                                        thumbnailUrl = channelThumb,
                                    ))
                                    isSubscribed = true
                                }),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(14.dp))
                                Text("Subscribe", color = Color.White, style = Type.callout, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(14.dp))
                            }
                        }
                    }
                }
            }

            // Videos
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = C.accent)
                }
                videos.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No videos found", style = Type.body, color = C.textSecondary)
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
                    contentPadding = PaddingValues(bottom = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(videos, key = { _, r -> r.videoId }) { idx, result ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .glassRow()
                                .pressableScale(onClick = {
                                    // Pass the full channel-videos list as context
                                    // so skip-next/prev walks through it.
                                    vm.prepareAndPlayOnline(result, videos, idx)
                                    onExpandPlayer()
                                })
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Ic.Play, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    result.title, style = Type.callout, color = C.text,
                                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${result.author} · ${result.durationText}",
                                    style = Type.footnote, color = C.textSecondary, maxLines = 1,
                                )
                            }
                            IconButton(onClick = { vm.downloadOnline(result) }, modifier = Modifier.size(36.dp)) {
                                Icon(Ic.Download, "Download", tint = C.textTertiary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
