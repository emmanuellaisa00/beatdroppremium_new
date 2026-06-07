package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.IconPuck
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.TintedGlassButton
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.pressableScale
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.util.ClipboardWatcher

/**
 * Screen shown when a URL is detected (from clipboard, share menu, or deep link).
 */
@Composable
fun ClipUrlScreen(
    vm: PlayerViewModel,
    url: String,
    onExpandPlayer: () -> Unit,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    var isLoading by remember { mutableStateOf(false) }
    val detected = remember { ClipboardWatcher.parseUrl(url) }

    LaunchedEffect(url) {
        if (detected != null && !detected.isPlaylist) {
            isLoading = true
            vm.playOnlineByUrl(url)
            isLoading = false
        }
    }

    ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.18f) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(title = "Link Detected", onBack = onBack, leadingIcon = Ic.Link)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.lg)
                    .padding(top = 24.dp, bottom = 190.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconPuck(icon = Ic.Link, contentDescription = null, size = 84.dp, tint = C.accent)
                Spacer(Modifier.height(20.dp))

                // URL card
                Box(
                    Modifier.fillMaxWidth().glassCard(radius = Radius.lg).padding(20.dp),
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(url, style = Type.body, color = C.text)
                        if (detected != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Platform: ${detected.platform}", style = Type.footnote, color = C.textTertiary)
                            if (detected.isPlaylist) {
                                Text(
                                    "Playlist detected",
                                    style = Type.footnote, color = C.accent,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (isLoading) {
                    CircularProgressIndicator(color = C.accent)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintedGlassButton(modifier = Modifier.height(48.dp).width(140.dp)) {
                            Row(
                                Modifier.fillMaxSize().pressableScale(onClick = {
                                    vm.playOnlineByUrl(url); onExpandPlayer()
                                }),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = Color.White, style = Type.headline)
                            }
                        }
                        Box(
                            Modifier
                                .height(48.dp)
                                .width(160.dp)
                                .glassCard(radius = Radius.xl)
                                .pressableScale(onClick = { vm.downloadOnlineByUrl(url) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Ic.Download, null, tint = C.text, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download", color = C.text, style = Type.headline)
                            }
                        }
                    }
                }
            }
        }
    }
}
