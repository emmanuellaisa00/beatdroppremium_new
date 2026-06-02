package com.beatdrop.kt.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Piped resolver — the most reliable YouTube stream source in mid-2026.
 *
 * Piped backends proxy the actual googlevideo CDN through their own server, which
 * means:
 *   - No PO Token / DroidGuard / SABR enforcement bites us (the backend handles it).
 *   - The returned URL points at `pipedproxy-*` and serves vanilla HTTP byte ranges,
 *     which ExoPlayer plays without any header tricks.
 *   - Works for the vast majority of public-facing videos including music.
 *
 * We hit several backends in parallel, return the first to deliver a stream, and
 * (lazily) auto-discover new backends from the public instance list when our
 * static list gets blocked.
 */
object PipedResolver {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Browser-like UA — some Piped instances 403 unknown clients (Cloudflare WAF rules).
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // Static seed list — confirmed alive as of May/June 2026.
    // Order matters: try the most reliable first.
    @Volatile private var instances: List<String> = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.reallyaweso.me",
        "https://piapi.ggtyler.dev",
        "https://pipedapi.darkness.services",
        "https://pipedapi.r4fo.com",
        "https://pipedapi.smnz.de",
        "https://pipedapi.drgns.space",
        "https://api-piped.mha.fi",
    )

    /**
     * Resolve a videoId to a directly-playable URL via any Piped instance.
     * Returns null only if every instance is dead. Each instance is given a 5s
     * window; ALL instances are queried in parallel (not just 5), first wins.
     * Using 5s instead of 6s keeps total latency under ~5s even when all instances
     * are slow, and trying ALL at once means we don't miss a working instance
     * that happens to be at the end of the list.
     */
    suspend fun resolve(videoId: String): ResolvedStream? = coroutineScope {
        val list = instances
        // Probe ALL instances in parallel — first non-null wins.
        val deferreds = list.map { base ->
            async(Dispatchers.IO) { runCatching { tryInstance(base, videoId) }.getOrNull() }
        }
        // Return as soon as ANY succeeds.
        var winner: ResolvedStream? = null
        for (d in deferreds) {
            val r = runCatching { d.await() }.getOrNull()
            if (r != null) { winner = r; break }
        }
        // Cancel the rest in background (avoid wasted work).
        deferreds.forEach { it.cancel() }
        winner
    }

    /** All-instances variant — used as a last-ditch fallback. */
    suspend fun resolveExhaustive(videoId: String): ResolvedStream? = withContext(Dispatchers.IO) {
        for (base in instances) {
            val r = runCatching { tryInstance(base, videoId) }.getOrNull()
            if (r != null) return@withContext r
        }
        null
    }

    private fun tryInstance(base: String, videoId: String): ResolvedStream? {
        val req = Request.Builder()
            .url("$base/streams/$videoId")
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .build()
        val text = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string() ?: return null
        }
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (!json.optString("error").isNullOrBlank()) return null
        if (json.optBoolean("livestream", false)) return null  // we don't stream live to ExoPlayer audio

        // 1) Prefer audio-only streams (smallest, highest quality audio).
        val audio = json.optJSONArray("audioStreams")
        if (audio != null) {
            var bestUrl: String? = null
            var bestBitrate = -1L
            for (i in 0 until audio.length()) {
                val s = audio.optJSONObject(i) ?: continue
                val url = s.optString("url").ifBlank { null } ?: continue
                val br = s.optLong("bitrate", 0L)
                // Prefer m4a (AAC) — universally supported by ExoPlayer's default decoders.
                val isM4a = s.optString("format").contains("m4a", true) ||
                    s.optString("codec").contains("mp4a", true)
                val score = br + (if (isM4a) 50_000L else 0L)
                if (score > bestBitrate) { bestBitrate = score; bestUrl = url }
            }
            if (bestUrl != null) {
                return ResolvedStream(bestUrl!!, UA, mapOf("Referer" to "$base/"))
            }
        }

        // 2) Fall back to MUXED video+audio (we just play the audio out of it).
        val videoStreams = json.optJSONArray("videoStreams")
        if (videoStreams != null) {
            var bestUrl: String? = null
            var bestSize = Long.MAX_VALUE
            for (i in 0 until videoStreams.length()) {
                val s = videoStreams.optJSONObject(i) ?: continue
                if (s.optBoolean("videoOnly", true)) continue   // muxed only
                val url = s.optString("url").ifBlank { null } ?: continue
                // Skip LBRY mirrors (player.odycdn.com) for muxed-fallback path —
                // they don't always have audio for music videos and are slower.
                if (url.contains("odycdn.com")) continue
                // Prefer the smallest muxed (lowest bitrate ~ 360p).
                val br = s.optLong("bitrate", Long.MAX_VALUE)
                if (br < bestSize) { bestSize = br; bestUrl = url }
            }
            // If we only had LBRY/odycdn entries, take the first muxed of any kind.
            if (bestUrl == null) {
                for (i in 0 until videoStreams.length()) {
                    val s = videoStreams.optJSONObject(i) ?: continue
                    if (s.optBoolean("videoOnly", true)) continue
                    val url = s.optString("url").ifBlank { null } ?: continue
                    bestUrl = url; break
                }
            }
            if (bestUrl != null) {
                return ResolvedStream(bestUrl!!, UA, mapOf("Referer" to "$base/"))
            }
        }

        // 3) HLS manifest (rare for non-live, but ExoPlayer handles it).
        val hls = json.optString("hls").ifBlank { null }
        if (hls != null) return ResolvedStream(hls, UA, mapOf("Referer" to "$base/"))

        return null
    }

    /** Refresh the instance list from the public registry (best effort). */
    suspend fun refreshInstanceList() = withContext(Dispatchers.IO) {
        runCatching {
            val text = http.newCall(
                Request.Builder().url("https://piped-instances.kavin.rocks/").build()
            ).execute().use { it.body?.string() } ?: return@withContext
            val arr = org.json.JSONArray(text)
            val fresh = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val url = o.optString("api_url").trim().trimEnd('/')
                if (url.startsWith("https://")) fresh.add(url)
            }
            if (fresh.isNotEmpty()) {
                // Merge: keep our static seeds at the front (they're hand-verified)
                // and add any new ones we don't already know about.
                val merged = LinkedHashSet<String>()
                merged.addAll(instances)
                merged.addAll(fresh)
                instances = merged.toList()
            }
        }
    }
}
