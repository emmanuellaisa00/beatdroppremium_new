package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.components.glassShadow
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

@Composable
fun ArtistScreen(vm: PlayerViewModel, artistName: String, onBack: () -> Unit) {
    val C = LocalAppColors.current
    var sheetTrack by remember { mutableStateOf<com.beatdrop.kt.data.Track?>(null) }
    val ctx = LocalContext.current
    val group = remember(artistName) { vm.artists().firstOrNull { it.artist == artistName } }
    val tracks = group?.tracks ?: emptyList()
    val current by vm.current.collectAsState()

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.16f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(title = artistName, subtitle = "${tracks.size} songs", onBack = onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = 190.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Avatar puck — glass-framed circle with rim + specular
                        Box(
                            Modifier
                                .size(112.dp)
                                .glassShadow(elevation = 20.dp, shape = CircleShape, isDark = C.isDark)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(C.accent, C.accentDark)))
                                .drawWithContent {
                                    drawContent()
                                    // Top rim — Fresnel highlight
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.30f),
                                                Color.Transparent,
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.45f,
                                        ),
                                    )
                                    // Specular sheen
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.20f),
                                                Color.Transparent,
                                            ),
                                            center = Offset(size.width * 0.35f, size.height * 0.30f),
                                            radius = size.minDimension * 0.45f,
                                        ),
                                        center = Offset(size.width * 0.35f, size.height * 0.30f),
                                        radius = size.minDimension * 0.45f,
                                    )
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                artistName.take(1).uppercase(),
                                color = Color.White,
                                style = Type.largeTitle.copy(fontWeight = FontWeight.Black),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            artistName, style = Type.title1, color = C.text,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Text("${tracks.size} songs", style = Type.subhead, color = C.textSecondary)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TintedGlassButton(modifier = Modifier.height(44.dp).width(124.dp)) {
                                Row(
                                    Modifier.fillMaxSize().pressableScale(
                                        onClick = { if (tracks.isNotEmpty()) vm.playList(tracks, tracks.first().id) },
                                    ),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play", color = Color.White, style = Type.headline)
                                }
                            }
                            Box(
                                Modifier
                                    .height(48.dp)
                                    .width(132.dp)
                                    .glassCard(radius = Radius.xl)
                                    .pressableScale(
                                        onClick = { if (tracks.isNotEmpty()) vm.playList(tracks.shuffled(), tracks.first().id) },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Ic.Shuffle, null, tint = C.text, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle", color = C.text, style = Type.headline)
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("Songs") }
                itemsIndexed(tracks, key = { _, t -> t.id }) { _, t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glassRow()
                            .pressableScale(
                                onClick = { vm.playList(tracks, t.id) },
                                onLongClick = { sheetTrack = t },
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(Radius.sm)).background(C.bg3)) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(t.artworkUri).crossfade(true).size(96).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title,
                                style = Type.headline,
                                color = if (current?.id == t.id) C.accent else C.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                t.album,
                                style = Type.footnote,
                                color = C.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(fmt(t.durationMs), style = Type.footnote, color = C.textTertiary)
                    }
                }
            }
        }
        sheetTrack?.let { tk ->
            com.beatdrop.kt.ui.components.TrackActionsSheet(vm, tk, onDismiss = { sheetTrack = null })
        }
    }
}
