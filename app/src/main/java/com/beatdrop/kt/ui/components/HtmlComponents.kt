/*
 * BeatDrop Premium — Shared UI Components
 * Pixel-perfect match of the HTML prototype's visual elements.
 */

package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.*

// ═══════════════════════════════════════════════════════════════════
// IDENTIY BAR (avatar + greeting + header icons)
// HTML: .identity, .avatar, .greet, .header-icons
// ═══════════════════════════════════════════════════════════════════

@Composable
fun IdentityBar(
    greeting: String,
    userName: String,
    onAvatarClick: () -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 22.dp),
    ) {
        // Avatar — 36dp circle with accent gradient + glow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HtmlTokens.Accent, HtmlTokens.AccentGradientDarkEnd2)))
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "B",
                color = Color.White,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = greeting,
                style = BdTypography.greeting,
                color = HtmlTokens.TextSecondary,
            )
            Text(
                text = userName,
                style = BdTypography.greetingBold,
                color = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        // Header icons
        if (onSearchClick != null) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(21.dp).clickable(onClick = onSearchClick),
            )
            Spacer(Modifier.width(20.dp))
        }
        if (onAddClick != null) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add",
                tint = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(21.dp).clickable(onClick = onAddClick),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LARGE TITLE with optional accent gradient word
// HTML: .large-title, .accent
// ═══════════════════════════════════════════════════════════════════

@Composable
fun LargeTitle(text: String, accentWord: String? = null) {
    if (accentWord != null && text.contains(accentWord)) {
        val parts = text.split(accentWord, limit = 2)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append(parts.first()) }
                withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(HtmlTokens.Accent, HtmlTokens.AccentGradientEnd)))) { append(accentWord) }
                if (parts.size > 1) withStyle(SpanStyle(color = Color.White)) { append(parts[1]) }
            },
            style = BdTypography.largeTitle,
        )
    } else {
        Text(text, style = BdTypography.largeTitle, color = Color.White)
    }
}

// ═══════════════════════════════════════════════════════════════════
// FILTER PILLS
// HTML: .filters, .pill, .pill.active
// ═══════════════════════════════════════════════════════════════════

@Composable
fun FilterPills(
    pills: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        itemsIndexed(pills) { index, label ->
            val active = index == selectedIndex
            val shape = RoundedCornerShape(17.dp)
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(shape)
                    .background(
                        if (active) Brush.linearGradient(listOf(HtmlTokens.Accent, HtmlTokens.AccentGradientDarkEnd))
                        else Brush.verticalGradient(listOf(HtmlTokens.Surface, HtmlTokens.Surface))
                    )
                    .then(
                        if (!active) Modifier.border(1.dp, HtmlTokens.GlassBorder, shape) else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelected(index) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = BdTypography.pill,
                    color = if (active) Color.White else Color.White.copy(alpha = 0.80f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SECTION HEADER
// HTML: .section h2, .section .see
// ═══════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = BdTypography.sectionTitle, color = Color.White)
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel.uppercase(),
                style = BdTypography.caption.copy(fontSize = 11.sp, letterSpacing = 1.1.sp),
                color = HtmlTokens.TextLow,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ALBUM CARD (carousel item)
// HTML: .card, .card .art, .card .nm, .card .ar
// ═══════════════════════════════════════════════════════════════════

@Composable
fun AlbumCard(
    title: String,
    artist: String = "",
    coverIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idx = safeGradientIndex(coverIndex, CoverGradients.size)
    val (start, end) = CoverGradients[idx]
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.width(154.dp).clickable(onClick = onClick)) {
        // Cover art with gradient + glass overlay
        Box(
            modifier = Modifier
                .size(154.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(start, end), 145f))
                .drawWithContent {
                    drawContent()
                    // Top highlight: radial-gradient(75% 55% at 28% 22%)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.28f, size.height * 0.22f),
                            radius = size.width * 0.55f,
                        ),
                    )
                    // Bottom shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f)),
                            startY = size.height * 0.58f,
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = BdTypography.cardTitle,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                style = BdTypography.cardArtist,
                color = HtmlTokens.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ALBUM CARD ROW (horizontal carousel)
// ═══════════════════════════════════════════════════════════════════

@Composable
fun AlbumCardRow(
    items: List<Triple<String, String, Int>>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier,
    ) {
        items(items) { (title, artist, coverIdx) ->
            AlbumCard(title, artist, coverIdx) { onClick(title) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// QUICK ACCESS GRID (2-column, icon + name)
// HTML: .quick-grid, .quick
// ═══════════════════════════════════════════════════════════════════

@Composable
fun QuickAccessGrid(
    items: List<Pair<String, Int>>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (name, coverIdx) ->
                    QuickAccessItem(name, coverIdx) { onClick(name) }
                }
                // Fill empty cell
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickAccessItem(
    name: String,
    coverIndex: Int,
    onClick: () -> Unit,
) {
    val idx = safeGradientIndex(coverIndex, CoverGradients.size)
    val (start, end) = CoverGradients[idx]
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .clip(shape)
            .background(HtmlTokens.Surface)
            .border(1.dp, HtmlTokens.GlassBorder, shape)
            .clickable(onClick = onClick),
    ) {
        // Mini cover
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Brush.linearGradient(listOf(start, end), 145f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = BdTypography.label.copy(fontSize = 13.5.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp).weight(1f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// TRACK ROW
// HTML: .track, .track .art, .track .info
// ═══════════════════════════════════════════════════════════════════

@Composable
fun TrackRow(
    title: String,
    artist: String,
    duration: String = "",
    coverIndex: Int = 0,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    index: Int? = null,
    modifier: Modifier = Modifier,
) {
    val idx = safeGradientIndex(coverIndex, CoverGradients.size)
    val (start, end) = CoverGradients[idx]
    val bgColor = if (isPlaying) HtmlTokens.Accent.copy(alpha = 0.07f) else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        // Number or EQ
        if (isPlaying) {
            EqualizerBars(modifier = Modifier.width(22.dp))
        } else if (index != null) {
            Text(
                text = "${index + 1}",
                style = BdTypography.timecode.copy(fontSize = 14.sp, fontWeight = FontWeight.W600),
                color = HtmlTokens.TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        // Cover
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(start, end), 145f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BdTypography.trackTitle,
                color = if (isPlaying) HtmlTokens.Accent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = BdTypography.trackArtist,
                color = HtmlTokens.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Duration
        if (duration.isNotBlank()) {
            Text(
                text = duration,
                style = BdTypography.timecode,
                color = HtmlTokens.TextLow,
            )
            Spacer(Modifier.width(8.dp))
        }
        // More button
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.MoreVert, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// EQUALIZER ANIMATION (3-bar)
// HTML: .track.playing .eq
// ═══════════════════════════════════════════════════════════════════

@Composable
fun EqualizerBars(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(14.dp),
    ) {
        repeat(3) { i ->
            val infiniteTransition = rememberInfiniteTransition()
            val height by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HtmlTokens.Accent),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BROWSE TILE
// HTML: .browse-tile
// ═══════════════════════════════════════════════════════════════════

@Composable
fun BrowseTile(
    label: String,
    tileIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idx = safeGradientIndex(tileIndex, TileGradients.size)
    val (start, end) = TileGradients[idx]
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(start, end), 145f))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.28f, size.height * 0.22f),
                        radius = size.maxDimension * 0.5f,
                    ),
                )
            },
    ) {
        Text(
            text = label,
            style = BdTypography.sectionTitle.copy(fontSize = 16.sp),
            color = Color.White,
        )
        // Decorative rotated square in bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                null,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BACK BUTTON (glass circle)
// HTML: .back-btn
// ═══════════════════════════════════════════════════════════════════

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, HtmlTokens.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.ArrowBackIosNew,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// ICON BUTTON (glass circle, 36dp)
// HTML: .icon-btn-32
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, HtmlTokens.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════
// SCREEN BACKGROUND (per-screen gradient)
// HTML: .phone.theme-library .bg::before
// ═══════════════════════════════════════════════════════════════════

enum class ScreenTheme {
    LIBRARY, HOME, SEARCH, NOW_PLAYING, NEUTRAL
}

@Composable
fun ScreenBackground(
    theme: ScreenTheme,
    modifier: Modifier = Modifier,
) {
    val colors = when (theme) {
        ScreenTheme.LIBRARY -> listOf(HtmlTokens.BgLibrary1, HtmlTokens.BgLibrary2)
        ScreenTheme.HOME -> listOf(HtmlTokens.BgHome1, HtmlTokens.BgHome2)
        ScreenTheme.SEARCH -> listOf(HtmlTokens.BgSearch1, HtmlTokens.BgSearch2)
        ScreenTheme.NOW_PLAYING -> listOf(HtmlTokens.BgNow1, HtmlTokens.BgNow2)
        ScreenTheme.NEUTRAL -> listOf(HtmlTokens.BgLibrary2, HtmlTokens.BgHome2)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(colors[0], Color.Transparent),
                    center = Offset(0.5f, -0.1f),
                    radius = 900f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(colors[1], Color.Transparent),
                    center = Offset(1f, 0.25f),
                    radius = 700f,
                )
            )
    )
}

// ═══════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Icon(icon, null, tint = HtmlTokens.TextTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = BdTypography.sectionTitle, color = HtmlTokens.TextSecondary)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = BdTypography.bodyMedium, color = HtmlTokens.TextTertiary)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = HtmlTokens.Accent, fontWeight = FontWeight.W700)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SPACERS
// ═══════════════════════════════════════════════════════════════════

@Composable
fun HeroSpacer() = Spacer(Modifier.height(18.dp))
@Composable
fun SectionSpacer() = Spacer(Modifier.height(34.dp))
