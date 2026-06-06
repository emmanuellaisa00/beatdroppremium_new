package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

/* ═══════════════════════════════════════════════════════════════════════════
 * HTML Building Blocks
 *
 * These are direct Compose translations of the components in beatdrop.html.
 * Every visual surface in the new screens is built from these primitives so
 * Library/Search/NowPlaying/Discover/Onboarding/Splash all share the same
 * vocabulary and pixel rhythm.
 *
 *   HtmlScaffold        — page background (pure black + pink/navy ambient)
 *   PageGutter          — 24 dp horizontal page padding
 *   HtmlHeader          — wordmark + tagline + circular icon row
 *   PageTitle           — large bold title (e.g. "Search", "Now Playing")
 *   CircularGlassIcon   — 40 dp circular dark-glass icon button
 *   InsetGlassOrb       — 44 dp recessed glass disc (active dock / play)
 *   MonochromeCover     — flat dark cover with a centred music symbol
 *   StatChip            — 76 dp glass tile with a number above a label
 *   SegmentedGlass      — Songs/Albums/Artists pill with sliding active orb
 *   FilterPill          — green "All / Music / Podcasts" capsule (HOME)
 *   QuickAccessTile     — circular thumb + title/sub row (HOME 2-col grid)
 *   JumpCard            — 175 dp square art + name/artist (HOME carousel)
 *   BrowseTile          — colourful 2-col tile (SEARCH "Browse all")
 *   DiscoverPortrait    — 9/16 portrait card with #tag (SEARCH discover)
 *   SongRow             — 72 dp song row with art / title / artist / meta
 *   ActionCardSmall     — 84 dp glass tile (Play All / Shuffle / etc.)
 *   SectionTitle        — 17 sp section header with optional trailing link
 * ═══════════════════════════════════════════════════════════════════════════ */


/* ─────────────────────────────────────────────────────────────────────────────
 * Page scaffolding
 * ───────────────────────────────────────────────────────────────────────────── */

/** Standard 24 dp horizontal page padding. */
val PageHorizontalPadding: Dp = 24.dp


/* ─────────────────────────────────────────────────────────────────────────────
 * Header — wordmark + tagline + right-side circular glass icons
 * Matches HTML .header (96 dp tall, padded top 56 for safe area)
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun HtmlHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val C = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = PageHorizontalPadding, end = PageHorizontalPadding, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = C.text,
                letterSpacing = (-0.7).sp,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.text.copy(alpha = 0.50f),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

/** Big section title shown at the top of full screens like "Search". */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    val C = LocalAppColors.current
    Text(
        text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp,
        color = C.text,
        modifier = modifier,
    )
}


/* ─────────────────────────────────────────────────────────────────────────────
 * Buttons & active surfaces
 * ───────────────────────────────────────────────────────────────────────────── */

/** 40 dp circular dark-glass icon button used in headers. */
@Composable
fun CircularGlassIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Box(
        modifier
            .size(40.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = CircleShape)
            .pressableScale(onClick = onClick, scaleTo = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription,
            tint = C.text.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Recessed glass orb — active dock tab background, segmented active state. */
@Composable
fun InsetGlassOrb(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier
            .size(size)
            .premiumGlass(level = GlassLevel.Z5_ActiveLens, shape = CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}


/* ─────────────────────────────────────────────────────────────────────────────
 * MonochromeCover — flat dark tile with centred music note / disc symbol
 * Used when album art isn't loaded. Matches HTML .cover.
 * ───────────────────────────────────────────────────────────────────────────── */
enum class CoverGlyph { Note, Disc, None }

@Composable
fun MonochromeCover(
    modifier: Modifier = Modifier,
    artworkUri: Any? = null,
    glyph: CoverGlyph = CoverGlyph.Note,
    cornerRadius: Dp = 14.dp,
    glyphAlpha: Float = 0.55f,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.025f),
                    ),
                )
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(artworkUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            when (glyph) {
                CoverGlyph.Note -> Icon(
                    Ic.MusicNote, null,
                    tint = C.text.copy(alpha = glyphAlpha),
                    modifier = Modifier.fillMaxSize(0.45f),
                )
                CoverGlyph.Disc -> Icon(
                    Ic.Album, null,
                    tint = C.text.copy(alpha = glyphAlpha),
                    modifier = Modifier.fillMaxSize(0.45f),
                )
                CoverGlyph.None -> {}
            }
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * Stat chip — 76 dp tall glass tile with big number + small uppercase label
 * Matches HTML .chip (Library screen)
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun StatChip(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val C = LocalAppColors.current
    Column(
        modifier
            .height(76.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.pressableScale(onClick = onClick, scaleTo = 0.97f) else Modifier)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            number,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            color = C.text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = C.text.copy(alpha = 0.50f),
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * SegmentedGlass — Songs / Albums / Artists with a sliding inset glass pill
 * Matches HTML .segmented + .seg-pill
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun SegmentedGlass(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(24.dp))
            .padding(5.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemW = this.maxWidth / options.size
            val animOffset by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f),
                label = "segOffset",
            )
            // Sliding inset orb
            Box(
                Modifier
                    .offset(x = itemW * animOffset)
                    .size(width = itemW, height = this.maxHeight)
                    .premiumGlass(level = GlassLevel.Z5_ActiveLens, shape = RoundedCornerShape(20.dp))
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, label ->
                    val active = i == selectedIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(i) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (active) C.text else C.text.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * FilterPill — green Spotify-style capsule used on the HOME header
 * (All / Music / Podcasts). Matches HTML .pill / .pill.active
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun FilterPillRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .height(44.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(22.dp))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (active) Color(0xFF1ED760) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(i) },
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.Black else C.text.copy(alpha = 0.75f),
                )
            }
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * QuickAccessTile — HOME 2-column grid row.
 * 44 dp circular thumb + title/sub. Matches HTML .quick-row.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun QuickAccessTile(
    title: String,
    subtitle: String,
    artworkUri: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ringAccent: Boolean = false,
    glyph: CoverGlyph = CoverGlyph.Note,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .then(
                    if (ringAccent) Modifier.border(2.dp, C.accent, CircleShape)
                    else Modifier
                )
                .padding(if (ringAccent) 2.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            MonochromeCover(
                modifier = Modifier.fillMaxSize(),
                artworkUri = artworkUri,
                glyph = glyph,
                cornerRadius = 22.dp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.50f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 2-column grid container for QuickAccessTile rows, in a single glass panel. */
@Composable
fun QuickAccessPanel(
    rows: List<QuickAccessItem>,
    onItemClick: (QuickAccessItem) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(26.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { item ->
                    QuickAccessTile(
                        title = item.title,
                        subtitle = item.subtitle,
                        artworkUri = item.artworkUri,
                        ringAccent = item.ringAccent,
                        glyph = item.glyph,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

data class QuickAccessItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUri: Any? = null,
    val ringAccent: Boolean = false,
    val glyph: CoverGlyph = CoverGlyph.Note,
)


/* ─────────────────────────────────────────────────────────────────────────────
 * JumpCard — 175 dp square art + name/artist (HOME "Jump back in" carousel)
 * Matches HTML .jump-card.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun JumpCard(
    title: String,
    artist: String,
    artworkUri: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: CoverGlyph = CoverGlyph.Note,
) {
    val C = LocalAppColors.current
    Column(
        modifier
            .width(168.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
    ) {
        MonochromeCover(
            artworkUri = artworkUri,
            glyph = glyph,
            cornerRadius = 12.dp,
            modifier = Modifier.size(168.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = C.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            artist,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = C.text.copy(alpha = 0.50f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun JumpCarousel(
    cards: List<JumpCardData>,
    onClick: (JumpCardData) -> Unit,
) {
    LazyRow(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = PageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(cards, key = { it.id }) { c ->
            JumpCard(
                title = c.title,
                artist = c.artist,
                artworkUri = c.artworkUri,
                glyph = c.glyph,
                onClick = { onClick(c) },
            )
        }
    }
}

data class JumpCardData(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: Any? = null,
    val glyph: CoverGlyph = CoverGlyph.Note,
)


/* ─────────────────────────────────────────────────────────────────────────────
 * BrowseTile — colourful 2-col tile (SEARCH "Browse all")
 * Matches HTML .browse-card.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun BrowseTile(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(100.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.72f)),
                ),
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.10f), shape)
            .pressableScale(onClick = onClick, scaleTo = 0.96f)
            .padding(14.dp),
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * DiscoverPortrait — 9/16 portrait card with #tag (SEARCH "Discover something new")
 * Matches HTML .discover-card.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun DiscoverPortrait(
    tag: String,
    artworkUri: Any? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bright: Boolean = false,
) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(9f / 16f)
            .clip(shape)
            .background(if (bright) Color.White else Color(0xFF0D0D12))
            .border(0.6.dp, Color.White.copy(alpha = 0.08f), shape)
            .pressableScale(onClick = onClick, scaleTo = 0.96f),
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(artworkUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            tag,
            color = if (bright) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * SongRow — 72 dp song row with art / title / artist / duration / menu
 * Matches HTML .song-row (Library / search results / album).
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun SongRow(
    title: String,
    artist: String,
    duration: String,
    artworkUri: Any? = null,
    active: Boolean = false,
    onClick: () -> Unit,
    onMenu: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .premiumGlass(
                level = if (active) GlassLevel.Z5_ActiveLens else GlassLevel.Z1_List,
                shape = RoundedCornerShape(22.dp),
            )
            .pressableScale(onClick = onClick, scaleTo = 0.98f)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonochromeCover(
            artworkUri = artworkUri,
            modifier = Modifier.size(48.dp),
            cornerRadius = 14.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                artist,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.50f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            Equalizer(modifier = Modifier.padding(end = 8.dp))
        }
        Text(
            duration,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = C.text.copy(alpha = 0.45f),
        )
        if (onMenu != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .pressableScale(onClick = onMenu, scaleTo = 0.85f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Ic.More, "More",
                    tint = C.text.copy(alpha = 0.80f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Tiny animated equalizer bars used inside the active song row. */
@Composable
private fun Equalizer(modifier: Modifier = Modifier) {
    val C = LocalAppColors.current
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    Row(
        modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(300, 100, 500, 200).forEach { delay ->
            val h by infinite.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.tween(900, delayMillis = delay),
                    androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "eqBar$delay",
            )
            Box(
                Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(h)
                    .background(C.accent.copy(alpha = 0.95f), RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun androidx.compose.animation.core.InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float>,
    label: String,
): State<Float> = androidx.compose.animation.core.animateFloat(
    initialValue, targetValue, animationSpec, label
)


/* ─────────────────────────────────────────────────────────────────────────────
 * ActionCardSmall — 84 dp glass tile (HOME "Play All / Shuffle / Favorites / Downloads")
 * Matches HTML .action-card.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun ActionCardSmall(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filledIcon: Boolean = false,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .height(84.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(26.dp))
            .pressableScale(onClick = onClick, scaleTo = 0.97f)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.045f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = C.text.copy(alpha = 0.92f),
                modifier = Modifier.size(if (filledIcon) 18.dp else 17.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.text,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.50f),
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * SectionTitle — 17 sp bold title + optional trailing link
 * Matches HTML .section-title.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun SectionTitle(
    title: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = PageHorizontalPadding, end = PageHorizontalPadding, top = 32.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = C.text,
        )
        if (trailing != null) {
            Text(
                trailing,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = C.text.copy(alpha = 0.50f),
                modifier = Modifier.then(
                    if (onTrailingClick != null) Modifier.pressableScale(onClick = onTrailingClick, scaleTo = 0.94f)
                    else Modifier
                ),
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 * SearchBarHtml — the rounded 56 dp dark glass search bar from HTML
 * (with the small magnifier and 14 sp placeholder). Read-only variant used
 * as a navigation shortcut to the full SearchScreen.
 * ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun SearchBarReadOnly(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val C = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = RoundedCornerShape(28.dp))
            .pressableScale(onClick = onClick, scaleTo = 0.98f)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Ic.Search, null,
            tint = C.text.copy(alpha = 0.60f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            placeholder,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = C.text.copy(alpha = 0.45f),
        )
    }
}
