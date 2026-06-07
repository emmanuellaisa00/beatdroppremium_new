package com.beatdrop.kt.ui.screens

/**
 * Format a duration in milliseconds as "M:SS".
 * Top-level so existing call sites (AlbumScreen, ArtistScreen, ...) keep
 * working without per-file private copies.
 */
fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
