package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.OnlineResult
import com.beatdrop.kt.youtube.YouTubeTrending
import kotlinx.coroutines.async

/**
 * Online trending / discovery screen.
 */
@Composable
fun TrendingScreen(
    vm: PlayerViewModel,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    var trending by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var newReleases by remember { mutableStateOf<List<OnlineResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isLoading = true
        val t = async { YouTubeTrending.fetchTrending() }
        val n = async { YouTubeTrending.fetchNewReleases() }
        trending = t.await()
        newReleases = n.await()
        isLoading = false
    }

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.14f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = "Discover",
                onBack = onBack,
                leadingIcon = Ic.TrendingUp,
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GlassChipT(label = "🔥 Trending",    selected = selectedTab == 0) { selectedTab = 0 }
                GlassChipT(label = "✨ New Releases", selected = selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = C.accent)
                }
                else -> {
                    val items = if (selectedTab == 0) trending else newReleases
                    if (items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Nothing available right now", color = C.textSecondary, style = Type.body)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
                            contentPadding = PaddingValues(bottom = 190.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(items, key = { _, r -> r.videoId }) { idx, result ->
                                TrendingRow(
                                    result = result,
                                    // Pass the surrounding list as context so
                                    // skip-next/prev in NowPlayingScreen and
                                    // MiniPlayer can walk through it.
                                    onPlay = {
                                        vm.prepareAndPlayOnline(result, items, idx)
                                        onExpandPlayer()
                                    },
                                    onDownload = { vm.downloadOnline(result) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassChipT(label: String, selected: Boolean, onClick: () -> Unit) {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(Radius.pill)
    if (selected) {
        Box(
            Modifier.clip(shape).background(C.accent)
                .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = Type.callout, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Box(
            Modifier.glassRow(radius = Radius.pill)
                .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = Type.callout, color = C.text, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TrendingRow(result: OnlineResult, onPlay: () -> Unit, onDownload: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .glassRow()
            .pressableScale(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(Radius.sm)).background(C.bg3)) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(result.thumbnailUrl).crossfade(true).size(160).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Icon(Ic.Play, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, style = Type.callout, color = C.text, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(result.author, style = Type.footnote, color = C.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(result.durationText, style = Type.caption, color = C.textTertiary,
            modifier = Modifier.padding(horizontal = 6.dp))
        IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
            Icon(Ic.Download, "Download", tint = C.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}
