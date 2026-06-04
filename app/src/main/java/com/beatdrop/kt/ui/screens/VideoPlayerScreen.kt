package com.beatdrop.kt.ui.screens

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.beatdrop.kt.PlayerViewModel
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Video player screen with Picture-in-Picture (PiP) support.
 * Plays video downloads in full screen, with a PiP toggle.
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    vm: PlayerViewModel,
    videoPath: String,
    title: String,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.parse(videoPath)))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
                override fun onPlaybackStateChanged(s: Int) {
                    duration = this@apply.duration.coerceAtLeast(0L)
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Auto-update position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            position = exoPlayer.currentPosition
            delay(500)
        }
    }

    // Enter PiP when user navigates away
    DisposableEffect(context) {
        val activity = context as? android.app.Activity
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    activity.enterPictureInPictureMode(params)
                } catch (_: Exception) { }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(8.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 1)
            // PiP button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IconButton(onClick = {
                    val activity = context as? android.app.Activity ?: return@IconButton
                    try {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(16, 9))
                            .build()
                        activity.enterPictureInPictureMode(params)
                    } catch (_: Exception) { }
                }) {
                    Icon(Icons.Outlined.PictureInPicture, "PiP", tint = Color.White)
                }
            }
        }

        // Video player
        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { it.player = exoPlayer },
            )
        }
    }
}

private suspend fun delay(ms: Long) = kotlinx.coroutines.delay(ms)
