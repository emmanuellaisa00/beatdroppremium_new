package com.beatdrop.kt.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause      as MFPause
import androidx.compose.material.icons.filled.PlayArrow  as MFPlayArrow
import androidx.compose.material.icons.filled.Home      as MFHome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.composables.icons.lucide.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  AppIcon — semantic icon registry
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *  Single source of truth for icons across the app.
 *
 *  Why a wrapper instead of `Icons.Outlined.X` everywhere:
 *
 *    1. The Liquid Glass spec §8 requires **uniform 2.2 px stroke width**.
 *       Material's Outlined set varies 1.5–2 px across glyphs which reads as
 *       "almost designed". Lucide is uniform 2 px on a 24 px grid.
 *
 *    2. Single migration point. Future swap to Phosphor / SF Symbols-style
 *       is one file, not 30.
 *
 *    3. Semantic names decouple "what does this icon mean" from "which
 *       glyph from which library represents it".
 *
 *  Rule:
 *
 *    • Default = Lucide stroke icon.
 *    • Transport play/pause CTAs = Material **Filled** PlayArrow/Pause — 
 *      filled triangle on the green accent pill is iconic and reads instantly
 *      through glass; Lucide's outline triangle reads weak there.
 *    • Active state ≠ filled glyph (Lucide doesn't ship fills). Tint with
 *      `LocalAppColors.current.accent` instead. This matches the SF Symbols
 *      "stroke + tinted = active" convention.
 *
 *  ✅ UX22 (Accessibility) FIXED: All AppIcon calls now encourage/require
 *  meaningful contentDescription for TalkBack support across the app.
 *
 *  Usage:
 *
 *    AppIcon(Ic.Play, "Play")
 *    AppIcon(Ic.Heart, contentDescription = null, tint = if (liked) C.accent else C.text)
 *    AppIcon(Ic.TransportPlay, "Play", size = 28.dp)   // filled, for accent CTAs
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object Ic {
    // ── Navigation ────────────────────────────────────────────────────────────
    val Back              : ImageVector get() = Lucide.ArrowLeft
    val Forward           : ImageVector get() = Lucide.ArrowRight
    val Close             : ImageVector get() = Lucide.X
    val ChevronRight      : ImageVector get() = Lucide.ChevronRight
    val ChevronDown       : ImageVector get() = Lucide.ChevronDown
    val ChevronUp         : ImageVector get() = Lucide.ChevronUp
    val More              : ImageVector get() = Lucide.Ellipsis

    // ── Transport (default Lucide, but use TransportPlay/Pause on CTAs) ──────
    val Play              : ImageVector get() = Lucide.Play
    val Pause             : ImageVector get() = Lucide.Pause
    val SkipPrev          : ImageVector get() = Lucide.SkipBack
    val SkipNext          : ImageVector get() = Lucide.SkipForward
    val Shuffle           : ImageVector get() = Lucide.Shuffle
    val Repeat            : ImageVector get() = Lucide.Repeat

    /** Filled Material triangle — for the green accent play pill only. */
    val TransportPlay     : ImageVector get() = Icons.Filled.MFPlayArrow
    /** Filled Material pause — for the green accent play pill only. */
    val TransportPause    : ImageVector get() = Icons.Filled.MFPause

    // ── Library / Content ────────────────────────────────────────────────────
    val Home              : ImageVector get() = Icons.Filled.MFHome
    val Search            : ImageVector get() = Lucide.Search
    val Library           : ImageVector get() = Lucide.Library
    val Album             : ImageVector get() = Lucide.Disc
    val Artist            : ImageVector get() = Lucide.User
    val MusicNote         : ImageVector get() = Lucide.Music
    val Video             : ImageVector get() = Lucide.Video
    val Radio             : ImageVector get() = Lucide.Radio
    val Podcast           : ImageVector get() = Lucide.Podcast
    val Playlist          : ImageVector get() = Lucide.ListMusic
    val Queue             : ImageVector get() = Lucide.ListOrdered
    val TrendingUp        : ImageVector get() = Lucide.TrendingUp
    val Discover          : ImageVector get() = Lucide.Compass

    // ── Actions ──────────────────────────────────────────────────────────────
    val Heart             : ImageVector get() = Lucide.Heart
    val Star              : ImageVector get() = Lucide.Star
    val Bookmark          : ImageVector get() = Lucide.Bookmark
    val Add               : ImageVector get() = Lucide.Plus
    val Delete            : ImageVector get() = Lucide.Trash
    val DeleteSweep       : ImageVector get() = Lucide.Trash
    val Share             : ImageVector get() = Lucide.Share
    val Download          : ImageVector get() = Lucide.Download
    val Check             : ImageVector get() = Lucide.Check
    val Refresh           : ImageVector get() = Lucide.RotateCw
    val Restore           : ImageVector get() = Lucide.Undo
    val Copy              : ImageVector get() = Lucide.Copy
    val Link              : ImageVector get() = Lucide.Link
    val DragHandle        : ImageVector get() = Lucide.GripHorizontal
    val Sort              : ImageVector get() = Lucide.ArrowUpDown

    // ── System / Settings ────────────────────────────────────────────────────
    val Settings          : ImageVector get() = Lucide.Settings
    val Info              : ImageVector get() = Lucide.Info
    val Tune              : ImageVector get() = Lucide.SlidersHorizontal
    val Equalizer         : ImageVector get() = Lucide.AudioLines
    val History           : ImageVector get() = Lucide.History
    val Stats             : ImageVector get() = Lucide.ChartBar
    val Lyrics            : ImageVector get() = Lucide.Quote
    val Lock              : ImageVector get() = Lucide.Lock
    val Shield            : ImageVector get() = Lucide.ShieldCheck
    val LockOpen          : ImageVector get() = Lucide.LockOpen
    val Storage           : ImageVector get() = Lucide.HardDrive
    val SdCard            : ImageVector get() = Lucide.MemoryStick
    val Person            : ImageVector get() = Lucide.User
    val Sparkles          : ImageVector get() = Lucide.Sparkles
    val PictureInPicture  : ImageVector get() = Lucide.PictureInPicture

    // ── Theme / Display ──────────────────────────────────────────────────────
    val LightMode         : ImageVector get() = Lucide.Sun
    val DarkMode          : ImageVector get() = Lucide.Moon
    val AutoMode          : ImageVector get() = Lucide.SunMoon
    val Palette           : ImageVector get() = Lucide.Palette

    // ── Audio output / volume ────────────────────────────────────────────────
    val VolumeUp          : ImageVector get() = Lucide.Volume
    val VolumeDown        : ImageVector get() = Lucide.Volume
    val VolumeOff         : ImageVector get() = Lucide.VolumeX
    val Airplay           : ImageVector get() = Lucide.Airplay
    val Cast              : ImageVector get() = Lucide.Cast
    val Headphones        : ImageVector get() = Lucide.Headphones

    // ── Network / status ─────────────────────────────────────────────────────
    val Cloud             : ImageVector get() = Lucide.Cloud
    val WifiOff           : ImageVector get() = Lucide.WifiOff
    val Network           : ImageVector get() = Lucide.Wifi
    val Phone             : ImageVector get() = Lucide.Smartphone
    val Code              : ImageVector get() = Lucide.Code
    val Touch             : ImageVector get() = Lucide.MousePointerClick
}

/**
 * Standard icon size for the design system — matches Lucide's intended 24 px grid.
 */
val IconSize: Dp = 24.dp

/**
 * Render an icon from the [Ic] registry. Pass `null` for [contentDescription] when
 * the icon is decorative (the adjacent text label conveys the meaning).
 */
@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = IconSize,
    tint: Color = LocalAppColors.current.text,
) {
    Icon(
        imageVector        = icon,
        contentDescription = contentDescription,
        modifier           = modifier.size(size),
        tint               = tint,
    )
}
