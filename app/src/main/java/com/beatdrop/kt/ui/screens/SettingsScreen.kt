package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.components.GlassCard
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

/**
 * Polished Settings screen with Liquid Glass cards, icons per section,
 * accent-tinted chips, glass dividers, and proper spacing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: PlayerViewModel, onBack: () -> Unit, onOpenEq: () -> Unit = {}, onOpenDJ: () -> Unit = {}) {
    val C = LocalAppColors.current
    val theme by vm.theme.collectAsState()
    val haptics by vm.haptics.collectAsState()
    val defaultShuffle by vm.defaultShuffle.collectAsState()
    val autoDj by vm.autoDjEnabled.collectAsState()
    val sleepLeft by vm.sleepMinutesLeft.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val liked by vm.liked.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 160.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = C.text)
                }
                Spacer(Modifier.width(4.dp))
                Text("Settings", color = C.text, fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
        }

        // ── APPEARANCE ───────────────────────────────────────────────────────
        item { SectionHeader("APPEARANCE", Icons.Filled.ColorLens) }
        item {
            GlassCard {
                Text("Theme", color = C.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to Icons.Filled.SettingsBrightness, "dark" to Icons.Filled.DarkMode, "light" to Icons.Filled.LightMode).forEach { (t, icon) ->
                        GlassChip(t.replaceFirstChar { it.uppercase() }, theme == t, icon) { vm.setTheme(t) }
                    }
                }
            }
        }

        // ── PLAYBACK ─────────────────────────────────────────────────────────
        item { SectionHeader("PLAYBACK", Icons.Filled.PlayCircle) }
        item {
            GlassCard {
                ToggleRow("Haptic feedback", Icons.Filled.TouchApp, haptics) { vm.setHaptics(it) }
                GlassDivider()
                ToggleRow("Shuffle by default", Icons.Filled.Shuffle, defaultShuffle) { vm.setDefaultShuffle(it) }
                GlassDivider()
                ToggleRow("Auto DJ crossfade", Icons.Filled.AutoAwesome, autoDj) { vm.setAutoDjEnabled(it) }
            }
        }

        item {
            GlassCard {
                NavRow("Equalizer", Icons.Filled.GraphicEq, onOpenEq)
            }
        }
        item {
            GlassCard {
                NavRow("DJ Mode", Icons.Filled.Album, onOpenDJ)
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Nightlight, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sleep timer", color = C.text, modifier = Modifier.weight(1f))
                    Text(
                        if (sleepLeft > 0) "$sleepLeft min left" else "Off",
                        color = if (sleepLeft > 0) C.accent else C.textSecondary,
                        fontWeight = if (sleepLeft > 0) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { m ->
                        GlassChip("$m min", false) { vm.startSleepTimer(m) }
                    }
                    GlassChip("Off", sleepLeft == 0) { vm.cancelSleepTimer() }
                }
            }
        }

        // ── LIBRARY STATS ────────────────────────────────────────────────────
        item { SectionHeader("LIBRARY", Icons.Filled.LibraryMusic) }
        item {
            GlassCard {
                StatRow(Icons.Filled.MusicNote, "Songs", "${tracks.size}")
                GlassDivider()
                StatRow(Icons.Filled.Favorite, "Liked", "${liked.size}")
                GlassDivider()
                StatRow(Icons.Filled.Album, "Albums", "${vm.albums().size}")
                GlassDivider()
                StatRow(Icons.Filled.Person, "Artists", "${vm.artists().size}")
            }
        }

        // ── ABOUT ────────────────────────────────────────────────────────────
        item { SectionHeader("ABOUT", Icons.Filled.Info) }
        item {
            val uriHandler = LocalUriHandler.current
            GlassCard {
                StatRow(Icons.Filled.PhoneAndroid, "App", "BeatDrop Premium")
                GlassDivider()
                StatRow(Icons.Filled.Tune, "Engine", "Media3 / ExoPlayer")
                GlassDivider()
                StatRow(Icons.Filled.AutoAwesome, "Design", "Liquid Glass")
                GlassDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Code, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Created by", color = C.text, modifier = Modifier.weight(1f))
                    Text(
                        "laisadevstudio",
                        color = C.accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.pressableScale(
                            onClick = { uriHandler.openUri("https://laisadevstudio.vercel.app") },
                            scaleTo = 0.94f,
                        ),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}


@Composable
private fun GlassDivider() {
    val C = LocalAppColors.current
    Divider(
        color = if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SectionHeader(t: String, icon: ImageVector? = null) {
    val C = LocalAppColors.current
    Row(
        Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = C.textTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(t, color = C.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun ToggleRow(label: String, icon: ImageVector, value: Boolean, onChange: (Boolean) -> Unit) {
    val C = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = C.text, modifier = Modifier.weight(1f))
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.accent),
        )
    }
}

@Composable
private fun NavRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().pressableScale(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = C.text, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = C.textTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    val C = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = C.text, modifier = Modifier.weight(1f))
        Text(value, color = C.textSecondary)
    }
}

@Composable
private fun GlassChip(label: String, active: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .clip(shape)
            .background(
                if (active) C.accent.copy(alpha = 0.55f)
                else if (C.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )
            .then(
                if (active) Modifier.border(0.5.dp, C.accent.copy(alpha = 0.3f), shape)
                else Modifier.border(0.5.dp, C.liquidGlassBorder, shape)
            )
            .pressableScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) Color.White else C.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = if (active) Color.White else C.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
