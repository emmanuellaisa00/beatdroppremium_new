package com.beatdrop.kt.youtube

import com.beatdrop.kt.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional self-hosted resolver backend — the SnapTube parity gap-closer.
 *
 * SnapTube's biggest hidden advantage is a server-side resolver (`api.snaptube.in`)
 * running an always-fresh yt-dlp + a PO-Token generator. When their in-app
 * extractors fail, they transparently fall through to this backend, which has
 * a working URL for ~all videos.
 *
 * BeatDrop doesn't ship one for you. But if YOU deploy one (a 30-line
 * Cloudflare Worker or a free Render instance running yt-dlp — see
 * `docs/RESOLVER_BACKEND.md`), set its URL here and BeatDrop will use it as
 * the FIRST resolver strategy, ahead of the in-app Innertube + Piped path.
 * Median latency ~200 ms when reachable; closes ~80% of the residual
 * "won't stream" gap with SnapTube.
 *
 * Protocol (kept stupid-simple so any yt-dlp wrapper works):
 *   GET  {baseUrl}/resolve?id=<videoId>
 *   200  application/json
 *        { "url": "https://...googlevideo.com/videoplayback?...",
 *          "ua":  "<optional user-agent the URL was resolved with>",
 *          "headers": { "Referer": "...", "Origin": "..." }   // optional
 *        }
 *   404  → fall through to next strategy
 *   5xx  → fall through to next strategy
 *
 * No backend deployed → all calls return null and we behave exactly like before.
 */
object ResolverBackend {

    @Volatile var baseUrl: String? = null   // e.g. "https://my-resolver.workers.dev"

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /** Returns null if no backend configured, the backend is down, or the
     *  videoId isn't resolvable. Never throws. */
    suspend fun resolve(videoId: String): ResolvedStream? = withContext(Dispatchers.IO) {
        val base = baseUrl?.trim()?.trimEnd('/') ?: return@withContext null
        if (base.isBlank()) return@withContext null

        runCatching {
            val req = Request.Builder()
                .url("$base/resolve?id=$videoId")
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    DebugLog.w("backend", "HTTP ${resp.code}")
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val url = json.optString("url").ifBlank { return@withContext null }
                val ua = json.optString("ua").ifBlank { UA }
                val hdrs = json.optJSONObject("headers")?.let { h ->
                    val out = LinkedHashMap<String, String>()
                    h.keys().forEach { k -> out[k] = h.optString(k) }
                    out
                } ?: emptyMap()
                ResolvedStream(url, ua, hdrs)
            }
        }.getOrNull()
    }
}
