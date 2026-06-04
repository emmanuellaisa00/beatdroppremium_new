package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.playback.EqEngine
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.components.GlassCard
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

/** Real EQ UI backed by android.media.audiofx.Equalizer (native DSP). Liquid Glass cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqScreen(onBack: () -> Unit) {
    val C = LocalAppColors.current
    val enabled by EqEngine.enabled.collectAsState()
    val bands by EqEngine.bands.collectAsState()
    val presets by EqEngine.presets.collectAsState()
    val bass by EqEngine.bassStrength.collectAsState()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = C.text) }
                Icon(Icons.Outlined.GraphicEq, null, tint = C.accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Equalizer", color = C.text, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { EqEngine.setEnabled(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = C.accent))
            }
        }

        if (bands.isEmpty()) {
            item {
                GlassCard {
                    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        Text(
                            "EQ initialises once playback starts.\nPlay a track, then return here.",
                            color = C.textSecondary, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // Band sliders in a glass card
        item {
            GlassCard {
                bands.forEach { band ->
                    val db = band.levelMb / 100f
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(freqLabel(band.centerFreqHz), color = C.textSecondary, fontSize = 12.sp, modifier = Modifier.width(42.dp))
                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { EqEngine.setBandLevel(band.index, it.toInt().toShort()) },
                            valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = C.accent, activeTrackColor = C.accent),
                        )
                        Text("%+.0f dB".format(db), color = C.text, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        // Bass boost in a glass card
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bass Boost", color = C.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${bass / 10}%", color = C.accent, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bass.toFloat(), onValueChange = { EqEngine.setBassStrength(it.toInt()) },
                    valueRange = 0f..1000f, enabled = enabled,
                    colors = SliderDefaults.colors(thumbColor = C.accent, activeTrackColor = C.accent),
                )
            }
        }

        // Presets in a glass card
        item {
            Text("PRESETS", color = C.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEachIndexed { i, name ->
                    val shape = RoundedCornerShape(20.dp)
                    Box(
                        Modifier.clip(shape)
                            .background(if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.5f))
                            .border(0.5.dp, C.liquidGlassBorder, shape)
                            .pressableScale(onClick = { EqEngine.setEnabled(true); EqEngine.applyPreset(i.toShort()) })
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(name, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable

private fun freqLabel(hz: Int): String = if (hz >= 1000) "${hz / 1000}k" else "$hz"
