package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

@Composable
fun DJScreen(vm: PlayerViewModel, onBack: () -> Unit = {}) {
    val C = LocalAppColors.current
    val tracks by vm.tracks.collectAsState()
    val deckA by vm.deckATrack.collectAsState()
    val deckB by vm.deckBTrack.collectAsState()
    val aPlaying by vm.deckAPlaying.collectAsState()
    val bPlaying by vm.deckBPlaying.collectAsState()
    val xfade by vm.crossfade.collectAsState()

    var picking by remember { mutableStateOf<Char?>(null) } // 'A' or 'B'

    Column(
        Modifier.fillMaxSize()
            .statusBarsPadding().padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = C.text)
            }
            Spacer(Modifier.width(8.dp))
            Text("DJ Mode", color = C.text, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Deck("DECK A", deckA, aPlaying, C.accent, Modifier.weight(1f),
                onLoad = { picking = 'A' }, onToggle = { vm.toggleDeckA() })
            Deck("DECK B", deckB, bPlaying, C.blue, Modifier.weight(1f),
                onLoad = { picking = 'B' }, onToggle = { vm.toggleDeckB() })
        }

        Spacer(Modifier.height(20.dp))
        Text("CROSSFADER", color = C.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = C.accent, fontWeight = FontWeight.Black)
            Slider(value = xfade, onValueChange = { vm.setCrossfade(it) }, valueRange = 0f..1f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("B", color = C.blue, fontWeight = FontWeight.Black)
        }
        Text("${((1 - xfade) * 100).toInt()}% / ${(xfade * 100).toInt()}%", color = C.textSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }

    picking?.let { deck ->
        TrackPickerSheet(tracks, onPick = {
            if (deck == 'A') vm.loadDeckA(it) else vm.loadDeckB(it)
            picking = null
        }, onDismiss = { picking = null })
    }
}

@Composable
private fun Deck(label: String, track: Track?, playing: Boolean, accent: Color, modifier: Modifier, onLoad: () -> Unit, onToggle: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    Column(
        modifier.clip(RoundedCornerShape(Radius.lg))
            .background(if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.5f))
            .androidx.compose.foundation.border(0.8.dp, C.liquidGlassBorder, RoundedCornerShape(Radius.lg))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(C.bg4), Alignment.Center) {
            if (track != null) {
                AsyncImage(model = ImageRequest.Builder(ctx).data(track.artworkUri).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
            Box(Modifier.size(18.dp).clip(CircleShape).background(C.bg0))
        }
        Spacer(Modifier.height(10.dp))
        Text(track?.title ?: "Empty", color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        Text(track?.artist ?: "Tap Load", color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(C.bg4).pressableScale(onClick = onLoad).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("Load", color = C.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(accent).pressableScale(onClick = onToggle), Alignment.Center) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackPickerSheet(tracks: List<Track>, onPick: (Track) -> Unit, onDismiss: () -> Unit) {
    val C = LocalAppColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = C.bg1) {
        Text("Load a track", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(16.dp, 4.dp))
        LazyColumn(Modifier.fillMaxHeight(0.7f)) {
            items(tracks) { t ->
                Row(Modifier.fillMaxWidth().pressableScale(onClick = { onPick(t) }).padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(t.artist, color = C.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
