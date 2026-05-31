package com.beatdrop.kt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.youtube.DownloadStatus
import com.beatdrop.kt.youtube.OnlineResult

@Composable
fun SearchScreen(vm: PlayerViewModel) {
    val C = LocalAppColors.current
    val q by vm.onlineQuery.collectAsState()
    val results by vm.onlineResults.collectAsState()
    val searching by vm.isSearching.collectAsState()
    val fetchingId by vm.fetchingVideoId.collectAsState()
    val message by vm.onlineMessage.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val history by vm.searchHistory.collectAsState()
    val jobs by vm.downloadJobs.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearOnlineMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)
        ) {
            Text(
                "Browse", color = C.text, fontSize = 26.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 10.dp),
            )

            // ── Search field ──────────────────────────────────────────────────
            OutlinedTextField(
                value = q,
                onValueChange = { vm.setOnlineQuery(it); if (it.length >= 2) vm.loadSuggestions() },
                placeholder = { Text("Search songs, artists, albums…") },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.textTertiary) },
                trailingIcon = {
                    if (q.isNotEmpty()) IconButton(onClick = { vm.setOnlineQuery("") }) {
                        Icon(Icons.Filled.Close, "Clear", tint = C.textTertiary)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.accent,
                    unfocusedBorderColor = C.border,
                    focusedContainerColor = C.bg2,
                    unfocusedContainerColor = C.bg2,
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.runOnlineSearch() }),
            )

            // ── Search history or autocomplete suggestions ─────────────────────────────────────────────
            val showHistory = q.isEmpty() && history.isNotEmpty() && results.isEmpty()
            val showSuggestions = q.isNotEmpty() && suggestions.isNotEmpty() && results.isEmpty()

            AnimatedVisibility(visible = showHistory || showSuggestions) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier = Modifier.heightIn(max = 260.dp),
                ) {
                    if (showHistory) {
                        item {
                            Text(
                                "Recent Searches",
                                color = C.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp)
                            )
                        }
                        items(history) { query ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(query)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.History, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(query, color = C.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { vm.deleteHistoryQuery(query) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Delete", tint = C.textTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Divider(color = C.bg3.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    } else if (showSuggestions) {
                        items(suggestions) { suggestion ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(suggestion)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(suggestion, color = C.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Divider(color = C.bg3.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                searching -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = C.accent, strokeWidth = 3.dp)
                }
                results.isNotEmpty() -> {
                    Text(
                        "${results.size} songs found",
                        color = C.textTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                        items(results, key = { it.videoId }) { r ->
                            val job = jobs[r.videoId]
                            CatalogRow(
                                result = r,
                                isFetching = fetchingId == r.videoId,
                                isSaved = job?.status == DownloadStatus.COMPLETED,
                                onPlay = { vm.playOnline(r) },
                                onSave = {
                                    when (job?.status) {
                                        DownloadStatus.FAILED -> vm.retryDownload(r)
                                        DownloadStatus.QUEUED,
                                        DownloadStatus.DOWNLOADING -> vm.cancelDownload(r.videoId)
                                        else -> vm.downloadOnline(r)
                                    }
                                },
                            )
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.MusicNote, null,
                                tint = C.textTertiary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Search millions of songs",
                                color = C.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Find any song to stream or save to your library",
                                color = C.textTertiary, fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp),
        )
    }
}

// ─── Catalog-style result row (Apple Music / Spotify look) ────────────────────
@Composable
private fun CatalogRow(
    result: OnlineResult,
    isFetching: Boolean,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current

    Row(
        Modifier.fillMaxWidth()
            .pressableScale(onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork with play overlay
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(C.bg3),
            Alignment.Center,
        ) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(result.thumbnailUrl).crossfade(true).size(128).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Subtle play overlay
            if (!isFetching) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow, "Play",
                        tint = Color.White, modifier = Modifier.size(22.dp)
                    )
                }
            }
            // Fetching spinner overlay
            if (isFetching) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        // Title + artist (clean, catalog-style)
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                color = C.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                result.author,
                color = C.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Duration
        Text(
            result.durationText,
            color = C.textTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // Save action (rebranded from "Download")
        IconButton(
            onClick = onSave,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                if (isSaved) "Saved to library" else "Save to library",
                tint = if (isSaved) C.accent else C.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
