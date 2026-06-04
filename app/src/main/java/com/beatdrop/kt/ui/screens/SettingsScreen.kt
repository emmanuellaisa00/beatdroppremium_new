package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.text.input.KeyboardType
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
fun SettingsScreen(vm: PlayerViewModel, onBack: () -> Unit, onOpenEq: () -> Unit = {}, onOpenDebug: () -> Unit = {}) {
    val C = LocalAppColors.current
    val theme by vm.theme.collectAsState()
    val haptics by vm.haptics.collectAsState()
    val defaultShuffle by vm.defaultShuffle.collectAsState()
    val autoDj by vm.autoDjEnabled.collectAsState()
    val crossfadeMs by vm.crossfadeMs.collectAsState()
    val resolverBackend by vm.resolverBackend.collectAsState()
    val streamQuality by vm.streamQuality.collectAsState()
    val musicSearch by vm.musicSearchEnabled.collectAsState()
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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = C.text)
                }
                Spacer(Modifier.width(4.dp))
                Text("Settings", color = C.text, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }
        }

        // ── APPEARANCE ───────────────────────────────────────────────────────
        item { SectionHeader("APPEARANCE", Icons.Outlined.ColorLens) }
        item {
            GlassCard {
                Text("Theme", color = C.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to Icons.Outlined.SettingsBrightness, "dark" to Icons.Outlined.DarkMode, "light" to Icons.Outlined.LightMode).forEach { (t, icon) ->
                        GlassChip(t.replaceFirstChar { it.uppercase() }, theme == t, icon) { vm.setTheme(t) }
                    }
                }
            }
        }

        // ── PLAYBACK ─────────────────────────────────────────────────────────
        item { SectionHeader("PLAYBACK", Icons.Outlined.PlayCircle) }
        item {
            GlassCard {
                ToggleRow("Haptic feedback", Icons.Outlined.TouchApp, haptics) { vm.setHaptics(it) }
                GlassDivider()
                ToggleRow("Shuffle by default", Icons.Outlined.Shuffle, defaultShuffle) { vm.setDefaultShuffle(it) }
                GlassDivider()
                ToggleRow("Auto-Mix (smart crossfade)", Icons.Outlined.AutoAwesome, autoDj) { vm.setAutoDjEnabled(it) }
                if (autoDj) {
                    GlassDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Crossfade length", color = C.text, modifier = Modifier.weight(1f))
                        Text("${crossfadeMs / 1000}s", color = C.accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = (crossfadeMs / 1000).toFloat(),
                        onValueChange = { vm.setCrossfadeMs((it.toInt()) * 1000) },
                        valueRange = 4f..12f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = C.accent,
                            activeTrackColor = C.accent,
                            inactiveTrackColor = C.accent.copy(alpha = 0.25f),
                        ),
                    )
                    Text(
                        "Auto-Mix picks the next track using artist, album, your play history, BPM, and key — and blends them seamlessly.",
                        color = C.textTertiary, fontSize = 12.sp,
                    )
                }
            }
        }

        // ── STREAMING ────────────────────────────────────────────────────────
        item { SectionHeader("STREAMING", Icons.Outlined.NetworkCheck) }
        item {
            GlassCard {
                ToggleRow("Music-only search", Icons.Outlined.MusicNote, musicSearch) {
                    vm.setMusicSearchEnabled(it)
                }
                Text(
                    if (musicSearch)
                        "Searches YouTube Music — clean song results, no reaction videos or lyric channels."
                    else "Searches all of YouTube — useful for podcasts, interviews, live sets.",
                    color = C.textTertiary, fontSize = 12.sp,
                )
                GlassDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Stream quality", color = C.text, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "auto" to "Auto",
                        "high" to "High",
                        "medium" to "Medium",
                        "low" to "Low",
                    ).forEach { (key, label) ->
                        GlassChip(label, streamQuality == key) { vm.setStreamQuality(key) }
                    }
                }
                Text(
                    when (streamQuality) {
                        "high"   -> "≈ 256 kbps. Best fidelity. Uses ~120 MB / hour."
                        "medium" -> "≈ 128 kbps. Balanced. Uses ~60 MB / hour."
                        "low"    -> "≈ 64 kbps. Data saver. Uses ~30 MB / hour."
                        else     -> "Picks the highest bitrate available (default)."
                    },
                    color = C.textTertiary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                GlassDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cloud, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Resolver backend (optional)", color = C.text, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                var backendText by remember(resolverBackend) { mutableStateOf(resolverBackend) }
                OutlinedTextField(
                    value = backendText,
                    onValueChange = { backendText = it },
                    placeholder = { Text("https://my-resolver.workers.dev") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = C.accent,
                        unfocusedBorderColor = C.textTertiary.copy(alpha = 0.4f),
                        focusedTextColor = C.text,
                        unfocusedTextColor = C.text,
                    ),
                )
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isValidUrl = backendText.isBlank() || backendText.matches(Regex("https?://.*"))
                    if (!isValidUrl && backendText.isNotBlank()) {
                        Text("URL must start with http:// or https://", color = Color(0xFFE53935), fontSize = 11.sp)
                    }
                    GlassChip("Save", isValidUrl && backendText != resolverBackend) {
                        if (isValidUrl) vm.setResolverBackend(backendText)
                    }
                    if (resolverBackend.isNotBlank()) {
                        GlassChip("Clear", false) {
                            backendText = ""; vm.setResolverBackend("")
                        }
                    }
                }
                Text(
                    "Optional. Deploy the included Cloudflare Worker (see docs) " +
                        "and paste its URL here. When set, BeatDrop tries this " +
                        "resolver FIRST — closes most remaining 'won't stream' gaps.",
                    color = C.textTertiary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            GlassCard {
                NavRow("Equalizer", Icons.Outlined.GraphicEq, onOpenEq)
            }
        }
        item {
            GlassCard {
                NavRow("Debug Log (diagnose playback)", Icons.Outlined.Code, onOpenDebug)
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Nightlight, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
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
        item { SectionHeader("LIBRARY", Icons.Outlined.LibraryMusic) }
        item {
            GlassCard {
                StatRow(Icons.Outlined.MusicNote, "Songs", "${tracks.size}")
                GlassDivider()
                StatRow(Icons.Outlined.Favorite, "Liked", "${liked.size}")
                GlassDivider()
                StatRow(Icons.Outlined.Album, "Albums", "${vm.albums().size}")
                GlassDivider()
                StatRow(Icons.Outlined.Person, "Artists", "${vm.artists().size}")
            }
        }

        // ── ABOUT ────────────────────────────────────────────────────────────
        item { SectionHeader("ABOUT", Icons.Outlined.Info) }
        item {
            val uriHandler = LocalUriHandler.current
            GlassCard {
                StatRow(Icons.Outlined.PhoneAndroid, "App", "BeatDrop Premium")
                GlassDivider()
                StatRow(Icons.Outlined.Tune, "Engine", "Media3 / ExoPlayer")
                GlassDivider()
                StatRow(Icons.Outlined.AutoAwesome, "Design", "Liquid Glass")
                GlassDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Code, null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
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
    HorizontalDivider(
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
        Icon(Icons.Outlined.ChevronRight, null, tint = C.textTertiary, modifier = Modifier.size(20.dp))
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
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.verticalGradient(
                    listOf(
                        if (active) C.accent.copy(alpha = 0.12f) else Color.White.copy(alpha = if (C.isDark) 0.06f else 0.10f),
                        Color.Transparent
                    ),
                    startY = 0f, endY = size.height * 0.4f,
                ))
            }
            .then(
                if (active) Modifier.border(0.5.dp, C.accent.copy(alpha = 0.35f), shape)
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
