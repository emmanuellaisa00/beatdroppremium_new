package com.beatdrop.kt.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.beatdrop.kt.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a vibrant accent color from album artwork (Apple Music / Spotify do
 * this to recolor the Now Playing surface). Uses Coil to fetch the bitmap, then
 * AndroidX Palette to pick a swatch. Falls back to the app accent on failure.
 */
@Composable
fun rememberArtworkColor(artworkUri: Uri?): Color {
    val ctx = LocalContext.current
    val fallback = LocalAppColors.current.accent
    var target by remember(artworkUri) { mutableStateOf(fallback) }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) { target = fallback; return@LaunchedEffect }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(ctx)
                val req = ImageRequest.Builder(ctx)
                    .data(artworkUri)
                    .allowHardware(false)        // Palette needs to read pixels
                    .size(Size(192, 192))         // small = fast
                    .build()
                val result = loader.execute(req)
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }.getOrNull()
        }
        target = bmp?.let { extractAccent(it) } ?: fallback
    }

    // Smoothly animate between track colors.
    val animated by animateColorAsState(target, label = "artColor")
    return animated
}

private suspend fun extractAccent(bitmap: Bitmap): Color? = withContext(Dispatchers.Default) {
    runCatching {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        swatch?.let { Color(it.rgb) }
    }.getOrNull()
}
