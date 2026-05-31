package com.beatdrop.kt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(
    vm: PlayerViewModel,
    onOpenEq: () -> Unit = {},
    onOpenManualDJ: () -> Unit = {}
) {
    val C = LocalAppColors.current
    val theme by vm.theme.collectAsState()
    val haptics by vm.haptics.collectAsState()
    val defaultShuffle by vm.defaultShuffle.collectAsState()
    val autoDj by vm.autoDjEnabled.collectAsState()
    val sleepLeft by vm.sleepMinutesLeft.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val liked by vm.liked.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().background(C.bg0),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 170.dp)
    ) {
        item {
            Text(
                "Activity & Settings",
                color = C.text,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
            )
        }

        // ── AUTO-DJ SMART MIX ────────────────────────────────────────────────
        item { SectionHeader("AUTO-DJ AUTOMIX") }
        item {
            Card(C) {
                ToggleRow("Auto-DJ Smart Mix", autoDj) { vm.setAutoDjEnabled(it) }
                Text(
                    "When enabled, the app automatically selects matching songs from your library and blends them together with real-time crossfading when a song ends.",
                    color = C.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }

        // ── MANUAL DJ DECK ───────────────────────────────────────────
        item { SectionHeader("MANUAL DJ CONTROLLER") }
        item {
            Card(C) {
                Row(
                    Modifier.fillMaxWidth()
                        .pressableScale(onClick = onOpenManualDJ)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.GraphicEq, null, tint = C.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Launch DJ Decks", color = C.text, fontWeight = FontWeight.Bold)
                        Text("Manually mix and crossfade tracks", color = C.textSecondary, fontSize = 11.sp)
                    }
                    Text("›", color = C.textSecondary, fontSize = 22.sp)
                }
            }
        }

        // ── APPEARANCE ───────────────────────────────────────────────────────
        item { SectionHeader("APPEARANCE") }
        item {
            Card(C) {
                Text("Theme", color = C.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "dark", "light").forEach { t ->
                        Chip(t.replaceFirstChar { it.uppercase() }, theme == t) { vm.setTheme(t) }
                    }
                }
            }
        }

        // ── PLAYBACK ─────────────────────────────────────────────────────────
        item { SectionHeader("PLAYBACK") }
        item {
            Card(C) {
                ToggleRow("Haptic feedback", haptics) { vm.setHaptics(it) }
                Divider(color = C.separator)
                ToggleRow("Shuffle by default", defaultShuffle) { vm.setDefaultShuffle(it) }
            }
        }

        item {
            Card(C) {
                Row(
                    Modifier.fillMaxWidth().pressableScale(onClick = onOpenEq).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Equalizer", color = C.text, modifier = Modifier.weight(1f))
                    Text("›", color = C.textSecondary, fontSize = 20.sp)
                }
            }
        }

        item {
            Card(C) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep timer", color = C.text, modifier = Modifier.weight(1f))
                    Text(if (sleepLeft > 0) "$sleepLeft min left" else "Off", color = C.textSecondary)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { m ->
                        Chip("$m min", false) { vm.startSleepTimer(m) }
                    }
                    Chip("Off", sleepLeft == 0) { vm.cancelSleepTimer() }
                }
            }
        }

        // ── LIBRARY STATS ────────────────────────────────────────────────────
        item { SectionHeader("LIBRARY") }
        item {
            Card(C) {
                InfoRow("Songs", "${tracks.size}")
                Divider(color = C.separator)
                InfoRow("Liked", "${liked.size}")
                Divider(color = C.separator)
                InfoRow("Albums", "${vm.albums().size}")
                Divider(color = C.separator)
                InfoRow("Artists", "${vm.artists().size}")
            }
        }

        // ── ABOUT ────────────────────────────────────────────────────────────
        item { SectionHeader("ABOUT") }
        item {
            val uriHandler = LocalUriHandler.current
            Card(C) {
                InfoRow("App", "BeatDrop (Kotlin)")
                Divider(color = C.separator)
                InfoRow("Engine", "Media3 / ExoPlayer")
                Divider(color = C.separator)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Created by", color = C.text, modifier = Modifier.weight(1f))
                    Text(
                        "laisadevstudio",
                        color = C.accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pressableScale(
                            onClick = { uriHandler.openUri("https://laisadevstudio.vercel.app") },
                            scaleTo = 0.94f,
                            haptic = false,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(t: String) {
    val C = LocalAppColors.current
    Text(t, color = C.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun Card(C: com.beatdrop.kt.ui.theme.AppColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg))
            .background(C.bg2).padding(16.dp),
        content = content,
    )
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val C = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.text, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val C = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.text, modifier = Modifier.weight(1f))
        Text(value, color = C.textSecondary)
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    val C = LocalAppColors.current
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(if (active) C.accent else C.bg4)
            .pressableScale(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (active) Color.White else C.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
