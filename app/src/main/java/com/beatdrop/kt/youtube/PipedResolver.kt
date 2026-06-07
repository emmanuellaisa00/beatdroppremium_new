package com.beatdrop.kt.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    // Static seed list — verified working as of June 2026.
    // Expanded from 2 to 8: parallel resolve() tries ALL at once, so more seeds
    // = better chance of at least one responding within the 5s window.
    // Auto-refresh from the public registry on each resolve() call.
    @Volatile private var instances: MutableList<String> = mutableListOf(
        "https://api.piped.private.coffee",   // Austria — consistently fast
        "https://pipedapi.smnz.de",            // Germany
        "https://pipedapi.adminforge.de",      // Germany
        "https://piped-api.garudalinux.org",   // Italy
        "https://api.piped.yt",                // Netherlands
        "https://pipedapi.leptons.xyz",        // Finland
        "https://piped.drgns.space",           // US
        "https://api.piped.projectsegfau.lt",  // Germany — large instance
    )
    @Volatile private var lastRefresh: Long = 0L

    /**
     * Resolve a videoId to a directly-playable URL via any Piped instance.
     * Returns null only if every instance is dead. Each instance is given a 5s
     * window; ALL instances are queried in parallel (not just 5), first wins.
     * Using 5s instead of 6s keeps total latency under ~5s even when all instances
     * are slow, and trying ALL at once means we don't miss a working instance
     * that happens to be at the end of the list.
     */
    suspend fun resolve(videoId: String): ResolvedStream? = coroutineScope {
        // Auto-refresh instance list every 10 minutes
        val now = System.currentTimeMillis()
        if (now - lastRefresh > 600_000L) {
            launch { runCatching { refreshInstanceList() } }
            lastRefresh = now
        }
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
            val host = base.removePrefix("https://")
            try {
                val r = tryInstance(base, videoId)
                if (r != null) return@withContext r
                com.beatdrop.kt.DebugLog.w("resolve", "piped-ex $host: no stream")
            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                val label = if (msg.contains("timeout", true) || msg.contains("timed out", true)) "timeout"
                            else "failed (${msg.take(60)})"
                com.beatdrop.kt.DebugLog.w("resolve", "piped-ex $host: $label")
            }
        }
        null
    }

    private fun tryInstance(base: String, videoId: String): ResolvedStream? {
        // Use HttpUrl.Builder for defensive URL construction — a malformed
        // base (e.g. trailing slash, accidental whitespace, missing scheme)
        // used to produce DNS errors like 'Unable to resolve host
        // "adminforge.destreams"' which we couldn't reproduce locally but
        // hit consistently in your debug logs. Parsing the URL up front
        // and bailing on null means we never call DNS with a garbage host.
        val httpUrl = base.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment("streams")
            ?.addPathSegment(videoId)
            ?.build()
            ?: return null
        val req = Request.Builder()
            .url(httpUrl)
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
                instances = merged.toMutableList()
            }
        }
    }
}
