package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassShadow
import com.beatdrop.kt.ui.components.noiseOverlay
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type

private data class RadioMix(val title: String, val desc: String, val gradient: List<Color>)

private val MIXES = listOf(
    RadioMix("Chill Vibes",   "Mellow tracks to relax",     listOf(Color(0xFF24304A), Color(0xFF34253F))),
    RadioMix("Energy Boost",  "High-tempo hits",            listOf(Color(0xFF3B2632), Color(0xFF542835))),
    RadioMix("Deep Focus",    "Ambient & instrumental",     listOf(Color(0xFF143447), Color(0xFF10283D))),
    RadioMix("Throwbacks",    "Classics from your library", listOf(Color(0xFF3D3518), Color(0xFF4A3D17))),
    RadioMix("Night Drive",   "Smooth evening tracks",      listOf(Color(0xFF322A38), Color(0xFF3C2C36))),
    RadioMix("Fresh Mix",     "Random picks for you",       listOf(Color(0xFF153C32), Color(0xFF174B35))),
)

@Composable
fun RadioScreen(vm: PlayerViewModel) {
    val C = LocalAppColors.current
    val tracks by vm.tracks.collectAsState()

    ScreenScaffold(ambientColor = C.glassAmbient) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "Radio", style = Type.largeTitle, color = C.text,
                modifier = Modifier.padding(start = Spacing.lg, top = 14.dp, bottom = 12.dp),
            )

            if (tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Add music to unlock radio mixes", color = C.textSecondary, style = Type.body)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.lg, 4.dp, Spacing.lg, 190.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(MIXES) { mix ->
                        val shape = RoundedCornerShape(Radius.xl)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                                // Content-safe concept tile: no haze/glassCard here.
                                // The previous stacked glass over colored gradient created
                                // visible dark square artifacts inside every tile.
                                .glassShadow(elevation = 10.dp, shape = shape, isDark = C.isDark)
                                .clip(shape)
                                .background(Brush.linearGradient(mix.gradient))
                                .background(Color.Black.copy(alpha = if (C.isDark) 0.18f else 0.06f))
                                .border(0.7.dp, Color.White.copy(alpha = 0.12f), shape)
                                .noiseOverlay(opacity = 0.018f)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.10f),
                                                Color.Transparent,
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.42f,
                                        ),
                                    )
                                }
                                .pressableScale(onClick = {
                                    val shuffled = tracks.shuffled().take(20)
                                    if (shuffled.isNotEmpty()) vm.playList(shuffled, shuffled.first().id)
                                })
                                .padding(14.dp),
                        ) {
                            Column(Modifier.align(Alignment.BottomStart)) {
                                Icon(
                                    Ic.Radio, null,
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(mix.title, style = Type.headline, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    mix.desc,
                                    style = Type.caption,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
