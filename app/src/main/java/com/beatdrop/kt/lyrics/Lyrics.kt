package com.beatdrop.kt.lyrics

import com.beatdrop.kt.data.Track
import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

/**
 * Port of the RN LyricsEngine sidecar-.lrc logic: looks for a ".lrc" file with
 * the same stem next to the audio file and parses [mm:ss.xx] timestamps.
 */
object LrcParser {
    private val tag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun findAndParse(track: Track): List<LyricLine> {
        val path = track.data ?: return emptyList()
        val dot = path.lastIndexOf('.')
        if (dot <= 0) return emptyList()
        val lrc = File(path.substring(0, dot) + ".lrc")
        if (!lrc.exists() || !lrc.canRead()) return emptyList()
        return runCatching { parse(lrc.readText()) }.getOrDefault(emptyList())
    }

    fun parse(content: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        content.lineSequence().forEach { raw ->
            val matches = tag.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val text = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val f = m.groupValues[3]
                val frac = when (f.length) { 0 -> 0L; 1 -> f.toLong() * 100; 2 -> f.toLong() * 10; else -> f.take(3).toLong() }
                out.add(LyricLine(min * 60_000 + sec * 1000 + frac, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /**
     * Index of the line that should be highlighted for the given playback
     * position. Returns -1 if [posMs] is before the first lyric line.
     *
     * O(log n) binary search — significantly cheaper than the previous O(n)
     * linear scan when called ~8×/sec on long-form tracks (200+ lines).
     * Replacing the scan was the main reason synced highlighting could
     * sometimes drift on slower devices: a busy main thread might skip a
     * tick, and a linear walk costs more in those skip windows.
     */
    fun activeIndex(lines: List<LyricLine>, posMs: Long): Int {
        if (lines.isEmpty() || posMs < lines.first().timeMs) return -1
        var lo = 0
        var hi = lines.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= posMs) lo = mid + 1 else hi = mid - 1
        }
        // hi now points to the last line whose timeMs <= posMs.
        return hi
    }
}
