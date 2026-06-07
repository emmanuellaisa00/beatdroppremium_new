package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.SectionHeader
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

/** Listening stats derived from local play counts (persisted via DataStore). */
@Composable
fun StatsScreen(vm: PlayerViewModel, onBack: () -> Unit = {}) {
    val C = LocalAppColors.current
    val counts by vm.playCounts.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val byId = remember(tracks) { tracks.associateBy { it.id } }

    val totalPlays = counts.values.sum()
    val topTracks = remember(counts, tracks) {
        counts.entries.sortedByDescending { it.value }
            .mapNotNull { e -> byId[e.key]?.let { it to e.value } }.take(10)
    }
    val topArtists = remember(counts, tracks) {
        counts.entries.mapNotNull { e -> byId[e.key]?.let { it.artist to e.value } }
            .groupBy { it.first }.mapValues { it.value.sumOf { p -> p.second } }
            .entries.sortedByDescending { it.value }.take(5)
    }
    val maxPlay = (topTracks.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.16f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = "Your Stats",
                subtitle = "$totalPlays total plays",
                onBack = onBack,
                leadingIcon = Ic.Stats,
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = 190.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    // Hero — accent tint behind, glass + reflection on top
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(C.accentSoft)
                            .glassCard(radius = Radius.lg)
                            .padding(24.dp),
                    ) {
                        Column {
                            Text(
                                "$totalPlays",
                                style = Type.largeTitle.copy(fontWeight = FontWeight.Black),
                                color = C.accent,
                            )
                            Text("total plays", style = Type.subhead, color = C.textSecondary)
                        }
                    }
                }
                if (totalPlays == 0) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().glassCard(radius = Radius.lg).padding(36.dp),
                            Alignment.Center,
                        ) { Text("Play some music to see stats.", style = Type.body, color = C.textSecondary) }
                    }
                    return@LazyColumn
                }
                item { SectionHeader("Top Artists") }
                itemsIndexed(topArtists) { i, (artist, plays) ->
                    Row(
                        Modifier.fillMaxWidth().glassRow().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", style = Type.headline, color = C.accent,
                            fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Text(artist, style = Type.body, color = C.text, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$plays plays", style = Type.footnote, color = C.textSecondary)
                    }
                }
                item { SectionHeader("Top Songs") }
                itemsIndexed(topTracks) { i, (t, plays) ->
                    Column(
                        Modifier.fillMaxWidth().glassRow().padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = Type.headline, color = C.accent,
                                fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title, style = Type.title3, color = C.text,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(t.artist, style = Type.footnote, color = C.textSecondary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("$plays", style = Type.footnote, color = C.textSecondary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(C.bg3),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(plays.toFloat() / maxPlay)
                                    .fillMaxHeight()
                                    .background(C.accent),
                            )
                        }
                    }
                }
            }
        }
    }
}
