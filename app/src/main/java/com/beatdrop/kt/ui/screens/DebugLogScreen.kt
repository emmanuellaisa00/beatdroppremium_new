package com.beatdrop.kt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.DebugLog
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * On-screen diagnostic log. Shows exactly what happens during search → resolve →
 * play (which client resolved, HTTP codes, playability reasons, the final GET,
 * ExoPlayer state/errors). Copy/Share buttons make it easy to send back for help.
 */
@Composable
fun DebugLogScreen(vm: PlayerViewModel, onBack: () -> Unit) {
    val C = LocalAppColors.current
    val ctx = LocalContext.current
    val entries by vm.debugLog.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to the newest line.
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    ScreenScaffold(ambientColor = C.glassAmbient) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Ic.Back, "Back", tint = C.text) }
            Text("Debug Log", color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
            Text("${entries.size}", color = C.textTertiary, fontSize = 13.sp,
                modifier = Modifier.padding(end = 6.dp))
            IconButton(onClick = { copyToClipboard(ctx, vm.dumpDebugLog()) }) {
                Icon(Ic.Copy, "Copy", tint = C.text)
            }
            IconButton(onClick = { shareText(ctx, vm.dumpDebugLog()) }) {
                Icon(Ic.Share, "Share", tint = C.text)
            }
            IconButton(onClick = { vm.clearDebugLog() }) {
                Icon(Ic.Delete, "Clear", tint = C.text)
            }
        }

        Text(
            "Tip: tap Clear, then search a song and tap a result. Then Copy/Share this log.",
            color = C.textTertiary, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No log entries yet.", color = C.textTertiary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
                    .glassCard(radius = 20.dp)
                    .background(Color(0xAA0B0B0F), RoundedCornerShape(20.dp))
                    .padding(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(entries) { e ->
                    val color = when (e.level) {
                        DebugLog.Level.E -> Color(0xFFFF6B6B)
                        DebugLog.Level.W -> Color(0xFFFFD166)
                        DebugLog.Level.I -> Color(0xFF8EE6A0)
                        DebugLog.Level.D -> Color(0xFF9AA0AA)
                    }
                    Text(
                        "${e.clock} ${e.level} [${e.tag}] ${e.msg}",
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("BeatDrop debug log", text))
}

private fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "BeatDrop debug log")
    }
    ctx.startActivity(Intent.createChooser(send, "Share debug log").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
