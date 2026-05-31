package com.beatdrop.kt.lyrics

import com.beatdrop.kt.data.Track
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Online lyrics from LRCLIB (https://lrclib.net) — free, open, no API key.
 * Faithful Kotlin port of the RN LrcLibProvider: exact-match first, then search
 * fallback; prefers synced lyrics, falls back to plain text converted to timed
 * lines. Uses HttpURLConnection so no extra dependency is required.
 */
object LrcLibProvider {

    private const val BASE = "https://lrclib.net/api"
    private const val TIMEOUT_MS = 6000

    /** @return parsed timed lyric lines, or empty if none found / offline. */
    fun fetch(track: Track): List<LyricLine> {
        val durationSec = (track.durationMs / 1000).toInt()

        // 1) Exact match: artist + title + album + duration
        runCatching {
            val url = "$BASE/get?" +
                "track_name=${enc(track.title)}" +
                "&artist_name=${enc(track.artist)}" +
                "&album_name=${enc(track.album)}" +
                if (durationSec > 0) "&duration=$durationSec" else ""
            val obj = getJsonObject(url)
            if (obj != null) {
                fromObject(obj, durationSec)?.let { return it }
            }
        }

        // 2) Search fallback (strip "(feat. …)", "[Official]", "- audio", etc.)
        val cleanTitle = track.title
            .replace(Regex("""\s*[(\[].*?[)\]]"""), "")
            .replace(Regex("""\s*-\s*(official|audio|video|lyric|music).*""", RegexOption.IGNORE_CASE), "")
            .trim()

        runCatching {
            val url = "$BASE/search?track_name=${enc(cleanTitle)}&artist_name=${enc(track.artist)}"
            val arr = getJsonArray(url)
            if (arr != null && arr.length() > 0) {
                // Prefer entries with synced lyrics, then closest duration.
                var best: JSONObject? = null
                var bestScore = Long.MAX_VALUE
                var bestHasSynced = false
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val hasSynced = !o.optString("syncedLyrics").isNullOrBlank()
                    val dur = o.optInt("duration", 0)
                    val score = if (durationSec > 0) abs(dur - durationSec).toLong() else 0L
                    val better = when {
                        hasSynced && !bestHasSynced -> true
                        hasSynced == bestHasSynced -> score < bestScore
                        else -> false
                    }
                    if (best == null || better) { best = o; bestScore = score; bestHasSynced = hasSynced }
                }
                best?.let { fromObject(it, durationSec)?.let { lines -> return lines } }
            }
        }

        // 3) Broad Search Query fallback (q=Artist+Title)
        runCatching {
            val url = "$BASE/search?q=${enc(track.artist + " " + cleanTitle)}"
            val arr = getJsonArray(url)
            if (arr != null && arr.length() > 0) {
                // Prefer entries with synced lyrics, then closest duration.
                var best: JSONObject? = null
                var bestScore = Long.MAX_VALUE
                var bestHasSynced = false
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val hasSynced = !o.optString("syncedLyrics").isNullOrBlank()
                    val dur = o.optInt("duration", 0)
                    val score = if (durationSec > 0) abs(dur - durationSec).toLong() else 0L
                    val better = when {
                        hasSynced && !bestHasSynced -> true
                        hasSynced == bestHasSynced -> score < bestScore
                        else -> false
                    }
                    if (best == null || better) { best = o; bestScore = score; bestHasSynced = hasSynced }
                }
                best?.let { fromObject(it, durationSec)?.let { lines -> return lines } }
            }
        }

        return emptyList()
    }

    private fun fromObject(o: JSONObject, durationSec: Int): List<LyricLine>? {
        val synced = o.optString("syncedLyrics")
        if (!synced.isNullOrBlank()) return LrcParser.parse(synced)
        val plain = o.optString("plainLyrics")
        if (!plain.isNullOrBlank()) {
            val lines = plain.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            return plainToTimed(lines, if (durationSec > 0) durationSec else 180)
        }
        return null
    }

    /** Convert plain lines to weighted timed lines (port of RN plainToLines). */
    private fun plainToTimed(lines: List<String>, durationSec: Int): List<LyricLine> {
        if (lines.isEmpty()) return emptyList()
        val dur = maxOf(durationSec.toDouble(), lines.size * 1.5)
        val intro = minOf(dur * 0.05, 8.0)
        val outro = minOf(dur * 0.04, 6.0)
        val avail = dur - intro - outro
        val minHold = 1.2
        val weights = lines.map { maxOf(0.5, minOf(2.5, it.length / 30.0)) }
        val total = weights.sum().takeIf { it > 0 } ?: 1.0
        val out = ArrayList<LyricLine>()
        var cursor = intro
        for (i in lines.indices) {
            val hold = maxOf(minHold, (weights[i] / total) * avail)
            out.add(LyricLine((cursor * 1000).toLong(), lines[i]))
            cursor += hold
        }
        return out
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun httpGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "BeatDrop/1.0 (Android; +https://lrclib.net)")
        }
        return try {
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun getJsonObject(url: String): JSONObject? =
        httpGet(url)?.let { runCatching { JSONObject(it) }.getOrNull() }

    private fun getJsonArray(url: String): JSONArray? =
        httpGet(url)?.let { runCatching { JSONArray(it) }.getOrNull() }
}
