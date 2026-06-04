package com.beatdrop.kt.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.youtube.DownloadStatus
import com.beatdrop.kt.youtube.OnlineResult

// ═══════════════════════════════════════════════════════════════════════════════
// Spotify Glassmorphism Search Screen
// Accent: #21FF6B (Spotify Green)
// Glass search bar with blur 28px
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SearchScreen(vm: PlayerViewModel, onExpandPlayer: () -> Unit = {}) {
    val C = LocalAppColors.current
    val q          by vm.onlineQuery.collectAsState()
    val results    by vm.onlineResults.collectAsState()
    val searching  by vm.isSearching.collectAsState()
    val message    by vm.onlineMessage.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val history    by vm.searchHistory.collectAsState()
    val jobs       by vm.downloadJobs.collectAsState()

    var snackbarMessage   by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val listState          = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) keyboardController?.hide()
    }

    val lastFailed by vm.lastFailedOnline.collectAsState()

    LaunchedEffect(message) {
        message?.let { msg ->
            val result = if (lastFailed != null) {
                snackbarHostState.showSnackbar(msg, actionLabel = "Retry")
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            if (result == SnackbarResult.ActionPerformed) vm.retryOnlinePlay()
            vm.clearOnlineMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                "Browse",
                color     = C.text,
                fontSize  = 28.sp,
                fontWeight = FontWeight.Black,
                modifier  = Modifier.padding(vertical = 10.dp),
            )

            // ── Offline banner ──────────────────────────────────────────────
            val isOnline = com.beatdrop.kt.util.NetworkMonitor.isOnline.value
            if (!isOnline) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3CD))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WifiOff, null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("You're offline. Search results won't load.", color = Color(0xFF856404), fontSize = 12.sp)
                }
            }

            // ── Glass Search field — blur 28px, accent green cursor ─────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(C.glassCardElevated)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = RenderEffect.createChainEffect(
                                    RenderEffect.createColorFilterEffect(
                                        android.graphics.ColorMatrixColorFilter(
                                            android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
                                        )
                                    ),
                                    RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP),
                                ).asComposeRenderEffect()
                            }
                        } else Modifier
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (C.isDark) 0.06f else 0.12f), Color.Transparent),
                            startY = 0f, endY = size.height * 0.4f,
                        ))
                    }
                    .border(0.7.dp, C.glassCardElevatedBorder, RoundedCornerShape(50.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value          = q,
                    onValueChange  = { vm.setOnlineQuery(it); if (it.length >= 2) vm.loadSuggestions() },
                    placeholder    = { Text("Search songs, artists, albums…", color = C.textTertiary) },
                    leadingIcon    = { Icon(Icons.Outlined.Search, null, tint = C.textTertiary) },
                    trailingIcon   = {
                        if (q.isNotEmpty()) IconButton(onClick = { vm.setOnlineQuery("") }) {
                            Icon(Icons.Outlined.Close, "Clear", tint = C.textTertiary)
                        }
                    },
                    singleLine     = true,
                    shape          = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor   = C.text,
                        unfocusedTextColor = C.text,
                        cursorColor        = C.accent,       // Spotify Green cursor
                    ),
                    modifier          = Modifier.fillMaxWidth(),
                    keyboardOptions   = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions   = KeyboardActions(onSearch = { vm.runOnlineSearch() }),
                )
            }

            // ── Search history or autocomplete suggestions ──────────────────
            val showHistory    = q.isEmpty() && history.isNotEmpty() && results.isEmpty()
            val showSuggestions = q.isNotEmpty() && suggestions.isNotEmpty() && results.isEmpty()

            AnimatedVisibility(visible = showHistory || showSuggestions) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier       = Modifier.heightIn(max = 260.dp),
                ) {
                    if (showHistory) {
                        item {
                            Text(
                                "Recent Searches",
                                color     = C.textSecondary,
                                fontSize  = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier  = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
                            )
                        }
                        items(history) { query ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(query)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.History, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    query, color = C.text, fontSize = 15.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick  = { vm.deleteHistoryQuery(query) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, "Delete", tint = C.textTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = C.separator, thickness = 0.5.dp)
                        }
                    } else if (showSuggestions) {
                        items(suggestions) { suggestion ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressableScale(onClick = {
                                        vm.setOnlineQuery(suggestion)
                                        vm.runOnlineSearch()
                                    })
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(suggestion, color = C.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = C.separator, thickness = 0.5.dp)
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
                        color     = C.textTertiary,
                        fontSize  = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier  = Modifier.padding(bottom = 8.dp),
                    )
                    LazyColumn(
                        state          = listState,
                        contentPadding = PaddingValues(bottom = 160.dp),
                    ) {
                        items(results, key = { it.videoId }) { r ->
                            val job = jobs[r.videoId]
                            CatalogRow(
                                result  = r,
                                isSaved = job?.status == DownloadStatus.COMPLETED,
                                onPlay  = {
                                    vm.prepareAndPlayOnline(r)
                                    onExpandPlayer()
                                },
                                onSave  = {
                                    when (job?.status) {
                                        DownloadStatus.FAILED        -> vm.retryDownload(r)
                                        DownloadStatus.QUEUED,
                                        DownloadStatus.DOWNLOADING   -> vm.cancelDownload(r.videoId)
                                        else                         -> vm.downloadOnline(r)
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
                                Icons.Outlined.MusicNote, null,
                                tint     = C.textTertiary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Search millions of songs",
                                color     = C.textSecondary,
                                fontSize  = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Find any song to stream or save to your library",
                                color     = C.textTertiary,
                                fontSize  = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Catalog-style result row — glass play overlay, Spotify Green save icon
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CatalogRow(
    result: OnlineResult,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    val C  = LocalAppColors.current
    val ctx = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork with glass play overlay
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(C.bg3),
            Alignment.Center,
        ) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model  = ImageRequest.Builder(ctx).data(result.thumbnailUrl).crossfade(true).size(128).build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            // Glass play overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        // Title + artist
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                color     = C.text,
                fontWeight = FontWeight.SemiBold,
                fontSize  = 15.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                result.author,
                color     = C.textSecondary,
                fontSize  = 13.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
        }

        // Duration
        Text(
            result.durationText,
            color     = C.textTertiary,
            fontSize  = 12.sp,
            modifier  = Modifier.padding(horizontal = 8.dp),
        )

        // Save action — Spotify Green when saved
        IconButton(
            onClick   = onSave,
            modifier  = Modifier.size(36.dp),
        ) {
            Icon(
                if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                if (isSaved) "Saved to library" else "Save to library",
                tint     = if (isSaved) C.accent else C.textTertiary,   // Green when saved
                modifier = Modifier.size(22.dp),
            )
        }
    }
}