package com.beatdrop.kt.lyrics

import android.content.Context
import com.beatdrop.kt.data.Track
import java.io.File
import java.security.MessageDigest

/**
 * On-disk LRU cache for fetched lyrics. Saves as .lrc in app cache so
 * offline playback always has lyrics without re-fetching.
 */
object LyricsCache {
    private fun hash(track: Track): String {
        val src = "${track.title.lowercase().trim()}|${track.artist.lowercase().trim()}"
        val md = MessageDigest.getInstance("MD5")
        return md.digest(src.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun dir(ctx: Context): File =
        File(ctx.cacheDir, "beatdrop_lyrics").also { it.mkdirs() }

    private fun file(ctx: Context, track: Track): File =
        File(dir(ctx), "${hash(track)}.lrc")

    fun read(ctx: Context, track: Track): List<LyricLine>? {
        val f = file(ctx, track)
        if (!f.exists() || f.length() == 0L) return null
        return runCatching { LrcParser.parse(f.readText()) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    fun write(ctx: Context, track: Track, lines: List<LyricLine>) {
        if (lines.isEmpty()) return
        val f = file(ctx, track)
        val text = lines.joinToString("\n") { line ->
            val min = line.timeMs / 60000
            val sec = (line.timeMs % 60000) / 1000
            val ms = (line.timeMs % 1000) / 10
            String.format("[%02d:%02d.%02d]%s", min, sec, ms, line.text)
        }
        runCatching { f.writeText(text) }
    }

    fun clear(ctx: Context) {
        runCatching { dir(ctx).listFiles()?.forEach { it.delete() } }
    }
}
