package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Search modes — kept for compatibility with MainActivity routes.
 *   HYBRID      = bottom-tab Search → local library + online
 *   ONLINE_ONLY = Discover → search button → online only
 */
enum class SearchMode { HYBRID, ONLINE_ONLY }

/**
 * Search — rewritten from scratch to pixel-match the BeatDrop HTML concept.
 *
 * Layout:
 *   PageTitle             "Search" (with avatar left + camera right)
 *   SearchBar             56 dp obsidian pill with "What do you want to listen to?"
 *   Discover row          3 portrait cards (#tag) — one is the bright #iammusic
 *   Browse all            2-col coloured tile grid
 *   (when typing)         Live result list / suggestions / history
 */
@Composable
fun SearchScreen(
    vm: PlayerViewModel,
    onExpandPlayer: () -> Unit = {},
    onOpenOnlineCollection: (com.beatdrop.kt.youtube.PlayableCollection) -> Unit = {},
    mode: SearchMode = SearchMode.HYBRID,
) {
    val C = LocalAppColors.current
    val query     by vm.onlineQuery.collectAsState()
    val results   by vm.onlineResults.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val history   by vm.searchHistory.collectAsState()
    val message   by vm.onlineMessage.collectAsState()

    val showEmptyState = query.isEmpty() && results.isEmpty()

    ScreenScaffold {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 220.dp),
        ) {
            // ── Top row: avatar + "Search" title + camera ─────────────────
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = PageHorizontalPadding, end = PageHorizontalPadding, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularGlassIcon(Ic.Person, "Profile", onClick = {})
                    Spacer(Modifier.width(14.dp))
                    PageTitle("Search", Modifier.weight(1f))
                    CircularGlassIcon(Ic.Sparkles, "Snap to search", onClick = {})
                }
            }

            // ── Search bar ────────────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 6.dp)) {
                    BeatDropSearchField(
                        value = query,
                        onChange = {
                            vm.setOnlineQuery(it)
                            if (mode == SearchMode.HYBRID) vm.runLocalSearch(it)
                            if (it.length >= 2) vm.loadSuggestions()
                        },
                        placeholder = "What do you want to listen to?",
                        onSubmit = { vm.runOnlineSearch() },
                    )
                }
            }

            if (showEmptyState) {
                // ── Discover something new (3 portrait cards) ────────────
                item { SectionTitle("Discover something new") }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DiscoverPortrait(
                            tag = "#trending",
                            onClick = { vm.setOnlineQuery("trending"); vm.runOnlineSearch() },
                            modifier = Modifier.weight(1f),
                        )
                        DiscoverPortrait(
                            tag = "#today",
                            onClick = { vm.setOnlineQuery("new music today"); vm.runOnlineSearch() },
                            modifier = Modifier.weight(1f),
                        )
                        DiscoverPortrait(
                            tag = "#newrelease",
                            bright = true,
                            onClick = { vm.setOnlineQuery("new releases"); vm.runOnlineSearch() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Browse all (2-col coloured tile grid) ────────────────
                item { SectionTitle("Browse all") }
                item {
                    Column(
                        Modifier.padding(horizontal = PageHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrowseTile("Pop",       Color(0xFFFF2DB5), onClick = { vm.setOnlineQuery("pop hits");      vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                            BrowseTile("Chill",     Color(0xFF117A59), onClick = { vm.setOnlineQuery("chill music");   vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrowseTile("Hip-Hop",   Color(0xFF6A1EE0), onClick = { vm.setOnlineQuery("hip hop");       vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                            BrowseTile("R&B",       Color(0xFFA78BDC), onClick = { vm.setOnlineQuery("r&b");           vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrowseTile("Electronic",Color(0xFF00B4D8), onClick = { vm.setOnlineQuery("electronic");    vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                            BrowseTile("Rock",      Color(0xFFD62828), onClick = { vm.setOnlineQuery("rock classics"); vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrowseTile("Jazz",      Color(0xFFE76F51), onClick = { vm.setOnlineQuery("jazz");          vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                            BrowseTile("Workout",   Color(0xFFFCBF49), onClick = { vm.setOnlineQuery("workout music"); vm.runOnlineSearch() }, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ── Recent searches ──────────────────────────────────────
                if (history.isNotEmpty()) {
                    item { SectionTitle("Recent searches") }
                    items(history.take(8)) { term ->
                        Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 4.dp)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .premiumGlass(level = GlassLevel.Z1_List, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                                    .pressableScale(
                                        onClick = {
                                            vm.setOnlineQuery(term)
                                            vm.runOnlineSearch()
                                        },
                                        scaleTo = 0.98f,
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.History, null, tint = C.text.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(term, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Unit
            } else {
                // ── Typed query — suggestions until results arrive ───────
                if (results.isEmpty() && suggestions.isNotEmpty()) {
                    items(suggestions.take(8)) { s ->
                        Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 4.dp)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .premiumGlass(level = GlassLevel.Z1_List, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                                    .pressableScale(
                                        onClick = {
                                            vm.setOnlineQuery(s)
                                            vm.runOnlineSearch()
                                        },
                                        scaleTo = 0.98f,
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.Search, null, tint = C.text.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(s, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── Results ──────────────────────────────────────────────
                if (results.isNotEmpty()) {
                    item { SectionTitle("Results", trailing = "${results.size}") }
                    items(results, key = { it.videoId }) { r ->
                        Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 5.dp)) {
                            SongRow(
                                title = r.title,
                                artist = if (r.durationText.isNotBlank()) "${r.author} · ${r.durationText}" else r.author,
                                duration = "",
                                artworkUri = r.thumbnailUrl,
                                onClick = {
                                    vm.playOnline(r)
                                    onExpandPlayer()
                                },
                                onMenu = { /* future: action sheet */ },
                            )
                        }
                    }
                }

                // Loading indicator if no results yet but query is set
                if (results.isEmpty() && suggestions.isEmpty() && query.length >= 2) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(top = 60.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = C.accent, strokeWidth = 2.5.dp)
                        }
                    }
                }
            }

            // ── Snackbar message ──────────────────────────────────────────
            if (message != null) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding, vertical = 12.dp),
                    ) {
                        Text(
                            message ?: "",
                            color = C.text.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .premiumGlass(level = GlassLevel.Z2_Card, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
