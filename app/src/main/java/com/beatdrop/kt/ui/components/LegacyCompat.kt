/*
 * Legacy compatibility shim — provides all old component names that screens reference.
 * Maps them to the new HTML-based components/tokens.
 */

package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.*

// ═══════════════════════════════════════════════════════════════════
// Ic — icon shortcut (old screens use Ic.Home, Ic.Search, etc.)
// ═══════════════════════════════════════════════════════════════════

object Ic {
    val Home: ImageVector get() = Icons.Filled.Home
    val Search: ImageVector get() = Icons.Filled.Search
    val Library: ImageVector get() = Icons.Filled.LibraryMusic
    val Add: ImageVector get() = Icons.Filled.Add
    val Settings: ImageVector get() = Icons.Filled.Settings
    val Back: ImageVector get() = Icons.Filled.ArrowBackIosNew
    val More: ImageVector get() = Icons.Filled.MoreVert
    val Play: ImageVector get() = Icons.Filled.PlayArrow
    val Pause: ImageVector get() = Icons.Filled.Pause
    val SkipNext: ImageVector get() = Icons.Filled.SkipNext
    val SkipPrev: ImageVector get() = Icons.Filled.SkipPrevious
    val Shuffle: ImageVector get() = Icons.Filled.Shuffle
    val Repeat: ImageVector get() = Icons.Filled.Repeat
    val Heart: ImageVector get() = Icons.Filled.Favorite
    val HeartBorder: ImageVector get() = Icons.Filled.FavoriteBorder
    val Share: ImageVector get() = Icons.Filled.Share
    val Download: ImageVector get() = Icons.Filled.Download
    val Music: ImageVector get() = Icons.Filled.MusicNote
    val Queue: ImageVector get() = Icons.Filled.QueueMusic
    val Timer: ImageVector get() = Icons.Filled.Timer
    val Equalizer: ImageVector get() = Icons.Filled.Equalizer
    val Disc: ImageVector get() = Icons.Filled.Album
    val Person: ImageVector get() = Icons.Filled.Person
    val ChevronDown: ImageVector get() = Icons.Filled.KeyboardArrowDown
    val ChevronUp: ImageVector get() = Icons.Filled.KeyboardArrowUp
    val Close: ImageVector get() = Icons.Filled.Close
    val Delete: ImageVector get() = Icons.Filled.Delete
    val Edit: ImageVector get() = Icons.Filled.Edit
    val Check: ImageVector get() = Icons.Filled.Check
    val Radio: ImageVector get() = Icons.Filled.Radio
    val Trending: ImageVector get() = Icons.Filled.TrendingUp
    val Playlist: ImageVector get() = Icons.Filled.List
    val Mic: ImageVector get() = Icons.Filled.Mic
    val Lyrics: ImageVector get() = Icons.Filled.Lyrics
    val Cast: ImageVector get() = Icons.Filled.Cast
    val Notifications: ImageVector get() = Icons.Filled.Notifications
    val Storage: ImageVector get() = Icons.Filled.Storage
    val Folder: ImageVector get() = Icons.Filled.Folder
    val Link: ImageVector get() = Icons.Filled.Link
    val Info: ImageVector get() = Icons.Filled.Info
    val Debug: ImageVector get() = Icons.Filled.BugReport
    val Moon: ImageVector get() = Icons.Filled.DarkMode
    val Sun: ImageVector get() = Icons.Filled.LightMode
    val Wifi: ImageVector get() = Icons.Filled.Wifi
    val Lock: ImageVector get() = Icons.Filled.Lock
}

// ═══════════════════════════════════════════════════════════════════
// ScreenScaffold — provides dark background + glass overlay
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ScreenScaffold(
    ambientIntensity: Float = 0.08f,
    ambientColor: Color = HtmlTokens.AccentDim,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HtmlTokens.PageBg),
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════
// GlassHeader — scroll-aware frosted header
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (onBack != null) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = BdTypography.compactTitle,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (actions != null) {
            actions()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Modifier extensions — glassCard, glassRow, glassShadow, pressableScale
// ═══════════════════════════════════════════════════════════════════

@Composable
fun Modifier.glassCard(radius: androidx.compose.ui.unit.Dp = 22.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(Color(0xB8121016))
    .border(0.5.dp, HtmlTokens.GlassBorder, RoundedCornerShape(radius))

@Composable
fun Modifier.glassRow(radius: androidx.compose.ui.unit.Dp = 14.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(Color(0x6B121016))
    .border(0.5.dp, HtmlTokens.GlassBorder, RoundedCornerShape(radius))

fun Modifier.glassShadow(): Modifier = this
fun Modifier.pressableScale(): Modifier = this

// ═══════════════════════════════════════════════════════════════════
// TintedGlassButton — accent-colored glass button
// ═══════════════════════════════════════════════════════════════════

@Composable
fun TintedGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(HtmlTokens.Accent.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════
// GlassTabBar2 + TabSpec2 (old dock — maps to BottomDock)
// ═══════════════════════════════════════════════════════════════════

data class TabSpec2(
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun GlassTabBar2(
    tabs: List<TabSpec2>,
    activeTab: String,
    onTab: (String) -> Unit,
) {
    val idx = tabs.indexOfFirst { it.label == activeTab }.coerceAtLeast(0)
    BottomDock(
        tabs = tabs.map { TabSpec(it.label, it.icon) },
        activeIndex = idx,
        onTabSelected = { onTab(tabs[it].label) },
    )
}

// ═══════════════════════════════════════════════════════════════════
// specularHighlight — tilt-based light effect
// ═══════════════════════════════════════════════════════════════════

@Composable
fun Modifier.specularHighlight(
    tilt: androidx.compose.runtime.State<Offset>,
    intensity: Float = 0.15f,
    radius: Float = 300f,
): Modifier = this.drawWithContent {
    drawContent()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = intensity), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.35f),
            radius = radius,
        ),
        center = Offset(size.width * 0.5f, size.height * 0.35f),
        radius = radius,
    )
}

// ═══════════════════════════════════════════════════════════════════
// AnnotatedString helper
// ═══════════════════════════════════════════════════════════════════

@Composable
fun Text(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, maxLines: Int, overflow: TextOverflow, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = text, style = style, color = color, maxLines = maxLines, overflow = overflow, modifier = modifier)
}
