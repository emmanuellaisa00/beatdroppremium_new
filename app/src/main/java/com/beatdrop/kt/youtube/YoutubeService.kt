package com.beatdrop.kt.youtube

import android.app.Application
import android.net.Uri
import com.beatdrop.kt.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// ─── HTTP Clients ─────────────────────────────────────────────────────────────

// Search/API client — generous read timeout for large JSON payloads on slow networks
private val okHttp = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

// Dedicated download client — no read timeout ceiling so large files complete
// on slow connections without SocketTimeoutException mid-transfer
private val downloadHttp = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)    // 0 = infinite — OkHttp will not timeout reads
    .followRedirects(true)
    .build()

// ─── Constants ────────────────────────────────────────────────────────────────

private const val YT_PLAYER = "https://www.youtube.com/youtubei/v1/player"
private const val YT_SEARCH = "https://www.youtube.com/youtubei/v1/search"
private const val IOS_UA    = "com.google.ios.youtube/20.03.02 (iPhone16,2; U; CPU iOS 18_2_1 like Mac OS X;)"
private const val SVC_CHROME_UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

// Parallel chunk count and minimum file size to bother chunking
private const val CHUNK_COUNT         = 4
private const val CHUNK_MIN_BYTES     = 1_048_576L  // 1 MB

// Updated Invidious instances (alive as of mid-2026)
private val INVIDIOUS_INSTANCES = listOf(
    "https://inv.nadeko.net",
    "https://invidious.nerdvpn.de",
    "https://invidious.f5.si",
    "https://inv.thepixora.com",
    "https://yewtu.be",
    "https://invidious.fdn.fr",
)

private data class YtClient(
    val name: String,
    val clientName: String,
    val clientVersion: String,
    val headers: Map<String, String>,
    val extraContext: JSONObject = JSONObject(),
    val bodyExtra: JSONObject = JSONObject(),   // injected at top level of player body
)

private val YT_CLIENTS = listOf(
    // ── ANDROID_VR (Oculus Quest) — NO PO TOKEN REQUIRED (2026) ─────────────
    // This is the single most reliable client as of 2026. YouTube does not
    // require Proof of Origin tokens for this client. Only limitation: "Made
    // for kids" videos are unavailable (irrelevant for a music player).
    YtClient(
        name = "ANDROID_VR", clientName = "ANDROID_VR", clientVersion = "1.57.29",
        headers = mapOf(
            "User-Agent"               to "com.google.android.apps.youtube.vr.oculus/1.57.29 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            "X-Youtube-Client-Name"    to "28",
            "X-Youtube-Client-Version" to "1.57.29",
        ),
        extraContext = JSONObject().apply {
            put("osName", "Android"); put("osVersion", "12L")
            put("androidSdkVersion", 32)
            put("deviceMake", "Oculus"); put("deviceModel", "Quest 3")
        },
    ),
    // ── WEB_EMBEDDED — NO PO TOKEN REQUIRED, embeddable videos only ─────────
    YtClient(
        name = "WEB_EMBEDDED", clientName = "WEB_EMBEDDED_PLAYER", clientVersion = "2.20241202.07.00",
        headers = mapOf(
            "User-Agent"               to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "X-Youtube-Client-Name"    to "56",
            "X-Youtube-Client-Version" to "2.20241202.07.00",
            "Origin"                   to "https://www.youtube.com",
        )
    ),
    // ── TV_EMBEDDED — works for age-restricted/embed-locked videos ───────────
    YtClient(
        name = "TV_EMBEDDED", clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER", clientVersion = "2.0",
        headers = mapOf(
            "User-Agent"               to "Mozilla/5.0 (SMART-TV; LINUX; Tizen 5.0) AppleWebKit/537.36",
            "X-Youtube-Client-Name"    to "85",
            "X-Youtube-Client-Version" to "2.0",
            "Origin"                   to "https://www.youtube.com",
            "Referer"                  to "https://www.youtube.com/",
        ),
        bodyExtra = JSONObject().put("thirdParty", JSONObject().put("embedUrl", "https://www.youtube.com/")),
    ),
    // ── ANDROID — may require PO token (GVS) in 2026, but sometimes works ───
    YtClient(
        name = "ANDROID", clientName = "ANDROID", clientVersion = "19.09.37",
        headers = mapOf(
            "User-Agent"               to "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip",
            "X-Youtube-Client-Name"    to "3",
            "X-Youtube-Client-Version" to "19.09.37",
        ),
        extraContext = JSONObject()
            .put("osName", "Android").put("osVersion", "11").put("androidSdkVersion", 30),
    ),
    // ── IOS — may require PO token (GVS) in 2026, but sometimes works ───────
    YtClient(
        name = "IOS", clientName = "IOS", clientVersion = "20.03.02",
        headers = mapOf(
            "User-Agent"               to IOS_UA,
            "X-Youtube-Client-Name"    to "5",
            "X-Youtube-Client-Version" to "20.03.02",
        ),
        extraContext = JSONObject().apply {
            put("deviceMake", "Apple"); put("deviceModel", "iPhone16,2")
            put("osName", "iPhone");   put("osVersion", "18.2.1.22C161")
        }
    ),
    // ── MWEB — may require PO token (GVS) in 2026, kept as last resort ──────
    YtClient(
        name = "MWEB", clientName = "MWEB", clientVersion = "2.20241202.07.00",
        headers = mapOf(
            "User-Agent"               to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
            "X-Youtube-Client-Name"    to "2",
            "X-Youtube-Client-Version" to "2.20241202.07.00",
            "Origin"                   to "https://m.youtube.com",
        )
    ),
)

// ─── Resolved stream (URL + the exact headers/UA that URL must be fetched with) ─
/**
 * A googlevideo CDN URL is bound to the client identity that resolved it. ExoPlayer
 * MUST replay the same User-Agent (and Referer/Origin for web/embed clients) or the
 * CDN returns 403. We therefore carry the headers alongside the URL.
 */
data class ResolvedStream(
    val url: String,
    val userAgent: String,
    val headers: Map<String, String> = emptyMap(),
)

// ─── URL Cache (90-minute TTL — googlevideo tokens expire fairly fast) ─────────
private data class CacheEntry(val stream: ResolvedStream, val cachedAt: Long)
private val urlCache = ConcurrentHashMap<String, CacheEntry>()
private const val URL_TTL_MS = 90 * 60 * 1000L

private fun getCachedStream(videoId: String): ResolvedStream? {
    val e = urlCache[videoId] ?: return null
    if (System.currentTimeMillis() - e.cachedAt > URL_TTL_MS) { urlCache.remove(videoId); return null }
    return e.stream
}
private fun setCachedStream(videoId: String, stream: ResolvedStream) {
    urlCache[videoId] = CacheEntry(stream, System.currentTimeMillis())
}

/** Drop a cached URL — used when ExoPlayer reports a 403 so the next attempt re-resolves. */
fun invalidateStreamCache(videoId: String) { urlCache.remove(videoId) }

// ─── Application context ──────────────────────────────────────────────────────
object YoutubeService {
    internal var app: Application? = null
    fun init(application: Application) { app = application }
    val downloadDir: File?
        get() = app?.getExternalFilesDir(null)
            ?.let { File(it, "BeatDrop/Downloads") }
            ?.also { it.mkdirs() }
}

// ─── Search ───────────────────────────────────────────────────────────────────
suspend fun searchYoutube(query: String, maxResults: Int = 50): List<OnlineResult> =
    withContext(Dispatchers.IO) {
        val cleanQuery   = query.trim()
        // Always produce a meaningfully different second query for more coverage
        val musicQuery   = musicifyQuery(cleanQuery)

        suspend fun innertubeSearch(q: String): Pair<List<JSONObject>, String?> {
            val body = JSONObject().apply {
                put("query", q)
                put("context", JSONObject().put("client", JSONObject().apply {
                    put("clientName", "MWEB"); put("clientVersion", "2.20241202.07.00")
                    put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
                }))
            }.toString()

            val req = Request.Builder()
                .url("$YT_SEARCH?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15")
                .header("X-Youtube-Client-Name", "2")
                .header("X-Youtube-Client-Version", "2.20241202.07.00")
                .header("Origin", "https://m.youtube.com")
                .header("Referer", "https://m.youtube.com/")
                .build()

            val json = okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Search failed (${resp.code})")
                JSONObject(resp.body!!.string())
            }
            val renderers = extractVideoRenderers(json)
            val contToken = extractContinuationToken(json)
            return Pair(renderers, contToken)
        }

        // Fetch continuation page for more results
        suspend fun innertubeContinuation(token: String): List<JSONObject> {
            val body = JSONObject().apply {
                put("continuation", token)
                put("context", JSONObject().put("client", JSONObject().apply {
                    put("clientName", "MWEB"); put("clientVersion", "2.20241202.07.00")
                    put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
                }))
            }.toString()

            val req = Request.Builder()
                .url("$YT_SEARCH?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15")
                .header("X-Youtube-Client-Name", "2")
                .header("X-Youtube-Client-Version", "2.20241202.07.00")
                .header("Origin", "https://m.youtube.com")
                .header("Referer", "https://m.youtube.com/")
                .build()

            val json = okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                JSONObject(resp.body!!.string())
            }
            return extractVideoRenderers(json)
        }

        // Fetch first page + continuation in parallel with music query
        val plainResult = runCatching { innertubeSearch(cleanQuery) }.getOrElse { Pair(emptyList(), null) }
        val plain       = plainResult.first
        val contToken   = plainResult.second

        // Always run the music query (musicifyQuery now always returns something different)
        val music = runCatching { innertubeSearch(musicQuery).first }.getOrElse { emptyList() }

        // Fetch continuation page for more results if available
        val continuation = if (contToken != null) {
            runCatching { innertubeContinuation(contToken) }.getOrElse { emptyList() }
        } else emptyList()

        val seen   = mutableSetOf<String>()
        val merged = (plain + music + continuation).filter { vr ->
            val id = vr.optString("videoId")
            if (id.isBlank() || !seen.add(id)) false else true
        }

        merged
            .mapNotNull { parseInnertubeRenderer(it) }
            // Fixed filter: don't kill results with unknown duration; raise cap to 60 min
            .filter { !it.isLive && (it.durationSecs == 0 || it.durationSecs in 15..3600) }
            .sortedByDescending { musicRelevanceScore(it) }
            .take(maxResults)
    }

// ─── Continuation token extraction ───────────────────────────────────────────
private fun extractContinuationToken(obj: JSONObject): String? {
    // Recursively search for continuationCommand.token or continuationEndpoint
    return findContinuationToken(obj)
}

private fun findContinuationToken(obj: JSONObject): String? {
    if (obj.has("continuationCommand")) {
        val token = obj.optJSONObject("continuationCommand")?.optString("token")
        if (!token.isNullOrBlank()) return token
    }
    if (obj.has("token") && obj.optString("request") == "CONTINUATION_REQUEST_TYPE_SEARCH") {
        val token = obj.optString("token")
        if (token.isNotBlank()) return token
    }
    // Check continuationItemRenderer pattern
    if (obj.has("continuationEndpoint")) {
        val token = obj.optJSONObject("continuationEndpoint")
            ?.optJSONObject("continuationCommand")?.optString("token")
        if (!token.isNullOrBlank()) return token
    }
    obj.keys().forEach { key ->
        when (val v = obj.opt(key)) {
            is JSONObject -> {
                val t = findContinuationToken(v)
                if (t != null) return t
            }
            is JSONArray -> {
                val t = findContinuationTokenArr(v)
                if (t != null) return t
            }
        }
    }
    return null
}

private fun findContinuationTokenArr(arr: JSONArray): String? {
    for (i in 0 until arr.length()) {
        when (val v = arr.opt(i)) {
            is JSONObject -> {
                val t = findContinuationToken(v)
                if (t != null) return t
            }
            is JSONArray -> {
                val t = findContinuationTokenArr(v)
                if (t != null) return t
            }
        }
    }
    return null
}

// ─── Search suggestions ───────────────────────────────────────────────────────
suspend fun getSearchSuggestions(query: String): List<String> =
    withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val urls = listOf(
            "https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&ds=yt&q=${Uri.encode(query)}&callback=f",
            "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${Uri.encode(query)}",
        )
        for (url in urls) {
            try {
                val text = okHttp.newCall(Request.Builder().url(url).build()).execute()
                    .use { it.body!!.string() }
                val m = Regex("\\[.*]", RegexOption.DOT_MATCHES_ALL).find(text)?.value ?: continue
                val arr = JSONArray(m)
                if (arr.length() < 2) continue
                val second = arr.get(1)
                val suggestions = when {
                    second is JSONArray && second.length() > 0 && second.get(0) is String ->
                        (0 until second.length()).map { second.getString(it) }
                    second is JSONArray ->
                        (0 until second.length()).mapNotNull {
                            (second.get(it) as? JSONArray)?.getString(0)
                        }
                    else -> continue
                }
                if (suggestions.isNotEmpty()) return@withContext suggestions.take(8)
            } catch (_: Exception) {}
        }
        emptyList()
    }

// ─── Stream URL resolution ────────────────────────────────────────────────────
/**
 * Strategy order (updated for 2026 PO Token enforcement):
 *   1. WebView extractor  — YouTube's own JS resolves everything natively.
 *   2. ANDROID_VR         — No PO token required (Oculus Quest client).
 *   3. WEB_EMBEDDED       — No PO token required (embeddable videos only).
 *   4. TV_EMBEDDED        — Works for age-restricted / embed-locked videos.
 *   5. ANDROID / IOS      — May work intermittently without PO token.
 *   6. MWEB               — Web fallback (PO token usually required).
 *   7. Invidious          — Public instances as last resort.
 */
/** Backward-compatible accessor — returns just the URL (used by the downloader). */
suspend fun getStreamUrl(videoId: String): String = getStream(videoId).url

/**
 * Resolve a directly-playable audio stream + the headers it must be fetched with.
 *
 * Now handles ciphered formats: when a /player response returns `signatureCipher`
 * (the common case in 2026), we download the player's base.js once, extract the
 * signature + n-throttle transforms, and run them with Rhino — the same approach
 * yt-dlp / NewPipe use. This is the piece that makes playback reliable like SnapTube.
 */
suspend fun getStream(videoId: String): ResolvedStream = withContext(Dispatchers.IO) {
    com.beatdrop.kt.DebugLog.i("resolve", "getStream($videoId) start")
    getCachedStream(videoId)?.let {
        com.beatdrop.kt.DebugLog.i("resolve", "cache hit → ${com.beatdrop.kt.DebugLog.shortUrl(it.url)}")
        return@withContext it
    }

    // Prime the cipher once (best-effort) so ciphered formats from the API resolve.
    val playerJsUrl = runCatching { YoutubeCipher.discoverPlayerJsUrl() }.getOrNull()
    if (playerJsUrl != null) {
        com.beatdrop.kt.DebugLog.d("cipher", "base.js = ${playerJsUrl.substringAfterLast('/')}")
        runCatching { YoutubeCipher.ensurePlayer(playerJsUrl) }
            .onFailure { com.beatdrop.kt.DebugLog.w("cipher", "ensurePlayer failed: ${it.message}") }
    } else {
        com.beatdrop.kt.DebugLog.w("cipher", "could not discover base.js (ciphered formats may be skipped)")
    }

    // Strategy 1–5 — Innertube /player clients FIRST (fast, instant, no PO token).
    for (client in YT_CLIENTS) {
        try {
            val body = buildPlayerBody(videoId, client)
            val req = Request.Builder()
                .url("$YT_PLAYER?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .apply { client.headers.forEach { (k, v) -> header(k, v) } }
                .header("Content-Type", "application/json")
                .build()

            val (code, data) = okHttp.newCall(req).execute().use { resp ->
                resp.code to (if (!resp.isSuccessful) null else JSONObject(resp.body!!.string()))
            }
            if (data == null) {
                com.beatdrop.kt.DebugLog.w("resolve", "${client.name}: HTTP $code")
                continue
            }
            val status = data.optJSONObject("playabilityStatus")?.optString("status")
            if (status != "OK") {
                val reason = data.optJSONObject("playabilityStatus")?.optString("reason")
                com.beatdrop.kt.DebugLog.w("resolve", "${client.name}: playability=$status ${reason ?: ""}".trim())
                continue
            }

            val streamingData = data.optJSONObject("streamingData")
            if (streamingData == null) {
                com.beatdrop.kt.DebugLog.w("resolve", "${client.name}: OK but no streamingData")
                continue
            }
            val url = resolveBestAudio(streamingData.optJSONArray("adaptiveFormats"))
                ?: resolveBestAudio(streamingData.optJSONArray("formats"))
            if (!url.isNullOrBlank()) {
                val ua = client.headers["User-Agent"] ?: IOS_UA
                val extra = buildMap {
                    client.headers["Origin"]?.let { put("Origin", it) }
                    client.headers["Referer"]?.let { put("Referer", it) }
                }
                com.beatdrop.kt.DebugLog.i("resolve", "✅ ${client.name} resolved → ${com.beatdrop.kt.DebugLog.shortUrl(url)}")
                val s = ResolvedStream(url, ua, extra)
                setCachedStream(videoId, s); return@withContext s
            } else {
                com.beatdrop.kt.DebugLog.w("resolve", "${client.name}: OK but no playable audio format (all ciphered & undecipherable?)")
            }
        } catch (e: Exception) {
            com.beatdrop.kt.DebugLog.w("resolve", "${client.name}: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    // Strategy 6 — WebView extractor (fallback).
    if (YoutubeExtractor.isConfigured) {
        com.beatdrop.kt.DebugLog.i("resolve", "trying WebView extractor (≤9s)…")
        try {
            val url = YoutubeExtractor.extractStreamUrl(videoId, 9_000)
            if (!url.isNullOrBlank()) {
                com.beatdrop.kt.DebugLog.i("resolve", "✅ WebView resolved → ${com.beatdrop.kt.DebugLog.shortUrl(url)}")
                val s = ResolvedStream(url, SVC_CHROME_UA, mapOf(
                    "Referer" to "https://www.youtube.com/",
                    "Origin"  to "https://www.youtube.com",
                ))
                setCachedStream(videoId, s); return@withContext s
            } else {
                com.beatdrop.kt.DebugLog.w("resolve", "WebView returned no URL (timed out or no CDN request)")
            }
        } catch (e: Exception) {
            com.beatdrop.kt.DebugLog.w("resolve", "WebView: ${e.message}")
        }
    } else {
        com.beatdrop.kt.DebugLog.w("resolve", "WebView extractor not configured")
    }

    // Strategy 7 — Invidious public instances.
    for (instance in INVIDIOUS_INSTANCES) {
        try {
            val data = okHttp.newCall(
                Request.Builder()
                    .url("$instance/api/v1/videos/$videoId?fields=adaptiveFormats,formatStreams")
                    .build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body!!.string())
            }
            if (data == null) {
                com.beatdrop.kt.DebugLog.w("resolve", "invidious ${instance.removePrefix("https://")}: failed")
                continue
            }
            val url = getBestAudioUrl(data.optJSONArray("adaptiveFormats"))
                ?: data.optJSONArray("formatStreams")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("url") else null
                }
            if (!url.isNullOrBlank()) {
                com.beatdrop.kt.DebugLog.i("resolve", "✅ invidious resolved → ${com.beatdrop.kt.DebugLog.shortUrl(url)}")
                val s = ResolvedStream(url, IOS_UA)
                setCachedStream(videoId, s); return@withContext s
            }
        } catch (e: Exception) {
            com.beatdrop.kt.DebugLog.w("resolve", "invidious ${instance.removePrefix("https://")}: ${e.message}")
        }
    }

    com.beatdrop.kt.DebugLog.e("resolve", "❌ all strategies failed for $videoId")
    throw Exception("Could not load this track. Try a different song or check your connection.")
}

// ─── Download ─────────────────────────────────────────────────────────────────
data class DownloadProgress(val bytesWritten: Long, val contentLength: Long, val percent: Int)

/**
 * SnapTube-style download:
 *   - Resolves the stream URL using the same strategy chain as getStreamUrl
 *   - Probes content-length + Accept-Ranges to decide between chunked/serial
 *   - Parallel chunked download (CHUNK_COUNT simultaneous HTTP Range requests)
 *     for files above CHUNK_MIN_BYTES — same technique as SnapTube's engine
 *   - Uses downloadHttp (no read timeout) so slow connections don't time out mid-file
 */
suspend fun downloadYoutubeTrack(
    result: OnlineResult,
    onProgress: (DownloadProgress) -> Unit = {},
): Track = withContext(Dispatchers.IO) {
    val dir = YoutubeService.downloadDir
        ?: throw Exception("External storage not available")

    val stream = getStream(result.videoId)
    val streamUrl = stream.url
    // Replay the resolving client's UA/headers so the CDN doesn't 403 the download.
    val dlUa = stream.userAgent
    val dlHeaders = stream.headers

    val safeTitle = result.title.replace(Regex("[^a-zA-Z0-9 \\-_]"), "").take(80).trim()

    // HEAD probe: get content-length, Range support, AND the authoritative content-type
    val head = downloadHttp.newCall(
        Request.Builder().url(streamUrl).head()
            .header("User-Agent", dlUa)
            .apply { dlHeaders.forEach { (k, v) -> header(k, v) } }
            .build()
    ).execute()
    val contentLength  = head.header("Content-Length")?.toLongOrNull() ?: 0L
    val acceptsRanges  = head.header("Accept-Ranges")
        ?.equals("bytes", ignoreCase = true) == true
    val ctype = (head.header("Content-Type") ?: "").lowercase()

    // Pick the right container extension. Prefer the server's Content-Type, then the
    // URL's mime= marker, then opus itags, defaulting to m4a (AAC).
    val isWebmOpus = ctype.contains("webm") || ctype.contains("opus") ||
        streamUrl.contains("mime=audio%2Fwebm") || streamUrl.contains("mime=audio/webm") ||
        listOf("itag=251", "itag=250", "itag=249", "itag=171", "itag=600", "itag=599")
            .any { streamUrl.contains(it) }
    val fileExt = if (isWebmOpus) "opus" else "m4a"
    val fileName = "${safeTitle}_${result.videoId}.$fileExt"
    val filePath = File(dir, fileName)

    if (acceptsRanges && contentLength >= CHUNK_MIN_BYTES) {
        downloadChunked(streamUrl, filePath, contentLength, dlUa, dlHeaders, onProgress)
    } else {
        downloadSerial(streamUrl, filePath, contentLength, dlUa, dlHeaders, onProgress)
    }

    check(filePath.exists() && filePath.length() >= 1024) {
        "Download produced an empty or corrupt file."
    }

    // Thumbnail
    val artworkPath = result.thumbnailUrl?.let { thumbUrl ->
        runCatching {
            val artFile = File(dir, "${safeTitle}_${result.videoId}.jpg")
            downloadHttp.newCall(Request.Builder().url(thumbUrl).build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    FileOutputStream(artFile).use { it.write(resp.body!!.bytes()) }
                    "file://${artFile.absolutePath}"
                } else null
            }
        }.getOrNull()
    } ?: result.thumbnailUrl

    val (parsedTitle, parsedArtist) = parseTitle(result.title, result.author)

    Track(
        id        = "dl_${result.videoId}",
        uri       = Uri.fromFile(filePath),
        title     = parsedTitle,
        artist    = parsedArtist,
        album     = result.author,
        albumId   = 0L,
        durationMs = result.durationSecs * 1000L,
        data      = filePath.absolutePath,
        dateAdded = System.currentTimeMillis(),
        artworkOverride = artworkPath,
    )
}

/**
 * Parallel chunked download — CHUNK_COUNT simultaneous Range requests.
 * FileChannel.write(ByteBuffer, position) is thread-safe for non-overlapping
 * positions, so each coroutine writes its chunk at its own offset without locks.
 */
private suspend fun downloadChunked(
    url: String,
    file: File,
    contentLength: Long,
    ua: String,
    extraHeaders: Map<String, String>,
    onProgress: (DownloadProgress) -> Unit,
) = withContext(Dispatchers.IO) {
    val chunkSize = (contentLength + CHUNK_COUNT - 1) / CHUNK_COUNT
    val written   = AtomicLong(0L)

    RandomAccessFile(file, "rw").use { raf ->
        raf.setLength(contentLength)
        val channel = raf.channel

        (0 until CHUNK_COUNT).map { i ->
            async(Dispatchers.IO) {
                val start = i * chunkSize
                val end   = minOf(start + chunkSize - 1, contentLength - 1)

                val req = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$start-$end")
                    .header("User-Agent", ua)
                    .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
                    .build()

                downloadHttp.newCall(req).execute().use { resp ->
                    check(resp.code in 200..206) { "Chunk $i HTTP ${resp.code}" }
                    var pos = start
                    val buf = ByteArray(65_536)
                    resp.body!!.byteStream().use { inp ->
                        while (true) {
                            val n = inp.read(buf)
                            if (n == -1) break
                            // Positional write — thread-safe, no mutex needed
                            channel.write(ByteBuffer.wrap(buf, 0, n), pos)
                            pos += n
                            val total = written.addAndGet(n.toLong())
                            val pct   = ((total * 100) / contentLength).toInt()
                            onProgress(DownloadProgress(total, contentLength, pct))
                        }
                    }
                }
            }
        }.awaitAll()
    }
}

/** Serial fallback for servers that don't support Range requests or small files */
private suspend fun downloadSerial(
    url: String,
    file: File,
    contentLength: Long,
    ua: String,
    extraHeaders: Map<String, String>,
    onProgress: (DownloadProgress) -> Unit,
) = withContext(Dispatchers.IO) {
    val req = Request.Builder().url(url)
        .header("User-Agent", ua)
        .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
        .build()
    downloadHttp.newCall(req).execute().use { resp ->
        check(resp.isSuccessful) { "Download failed (HTTP ${resp.code})" }
        val body = resp.body ?: throw Exception("Empty response body")
        val len  = if (contentLength > 0) contentLength else body.contentLength()
        var done = 0L
        FileOutputStream(file).use { fos ->
            body.byteStream().use { inp ->
                val buf = ByteArray(65_536)
                while (true) {
                    val n = inp.read(buf)
                    if (n == -1) break
                    fos.write(buf, 0, n)
                    done += n
                    val pct = if (len > 0) ((done * 100) / len).toInt() else 0
                    onProgress(DownloadProgress(done, len, pct))
                }
            }
        }
    }
}

// ─── Convert search result → Track (for streaming) ───────────────────────────
suspend fun youtubeResultToTrack(result: OnlineResult): Track {
    val stream = getStream(result.videoId)
    val (title, artist) = parseTitle(result.title, result.author)
    return Track(
        id        = "yt_${result.videoId}",
        uri       = Uri.parse(stream.url),
        title     = title,
        artist    = artist,
        album     = result.author,
        albumId   = 0L,
        durationMs = result.durationSecs * 1000L,
        data      = null,
        dateAdded = System.currentTimeMillis(),
        artworkOverride = result.thumbnailUrl,
        streamUserAgent = stream.userAgent,
        streamHeaders   = stream.headers,
        sourceVideoId   = result.videoId,
    )
}

// ─── Private helpers ──────────────────────────────────────────────────────────

private fun buildPlayerBody(videoId: String, client: YtClient): String =
    JSONObject().apply {
        put("videoId", videoId)
        put("context", JSONObject().put("client", JSONObject().apply {
            put("clientName", client.clientName)
            put("clientVersion", client.clientVersion)
            put("hl", "en"); put("gl", "US")
            client.extraContext.keys().forEach { k -> put(k, client.extraContext.get(k)) }
        }))
        // html5Preference only makes sense for web/TV clients, not native Android/iOS/VR
        if (client.clientName != "IOS" && client.clientName != "ANDROID" && client.clientName != "ANDROID_VR") {
            put("playbackContext", JSONObject().put("contentPlaybackContext",
                JSONObject().put("html5Preference", "HTML5_PREF_WANTS")))
        }
        // ANDROID client needs CgIQBg parameter to bypass 403 integrity checks
        if (client.clientName == "ANDROID") {
            put("params", "CgIQBg")
        }
        put("contentCheckOk", true); put("racyCheckOk", true)
        // Inject any client-specific top-level fields (e.g. thirdParty embed URL)
        client.bodyExtra.keys().forEach { k -> put(k, client.bodyExtra.get(k)) }
    }.toString()

/** Plain-URL selector — used for Invidious (already deciphered). */
private fun getBestAudioUrl(formats: JSONArray?): String? =
    getBestAudioFormat(formats)?.let { f ->
        f.optString("url").ifBlank { null }
    }

/**
 * Selects the highest-bitrate audio format and resolves it to a playable URL,
 * deciphering `signatureCipher` formats via YoutubeCipher when needed.
 * This is what lets us play the (now-common) protected formats SnapTube handles.
 */
private suspend fun resolveBestAudio(formats: JSONArray?): String? {
    if (formats == null) return null
    val audio = (0 until formats.length()).map { formats.getJSONObject(it) }
        .filter { f ->
            val mt = (f.optString("mimeType") + f.optString("type")).lowercase()
            mt.contains("audio/")
        }
        .sortedByDescending { f -> f.optLong("bitrate").coerceAtLeast(f.optLong("averageBitrate")) }

    // Try formats best-first; first one that resolves wins.
    for (f in audio) {
        // Plain URL? Still run it through the cipher so the n-throttle param is fixed.
        val resolved = runCatching { YoutubeCipher.resolveFormatUrl(f) }.getOrNull()
        if (!resolved.isNullOrBlank()) return resolved
    }
    return null
}

/**
 * Plain-URL-only selector (no decipher) — retained for Invidious responses which
 * already provide fully-resolved URLs.
 */
private fun getBestAudioFormat(formats: JSONArray?): JSONObject? {
    if (formats == null) return null
    val all = (0 until formats.length()).map { formats.getJSONObject(it) }
    val withUrl = all.filter { f ->
        val mt = (f.optString("mimeType") + f.optString("type")).lowercase()
        mt.contains("audio/") && f.optString("url").isNotBlank()
    }.sortedByDescending { f ->
        f.optLong("bitrate").coerceAtLeast(f.optLong("averageBitrate"))
    }
    return withUrl.firstOrNull()
}

/**
 * Always produce a meaningfully different second query so we always get
 * double coverage. Never returns the same string as input.
 */
private fun musicifyQuery(q: String): String {
    val lower = q.lowercase()
    val hasAudioHint = lower.contains("official audio") || lower.contains("lyrics") ||
        lower.contains("audio") || lower.contains("music video") || lower.contains("topic")
    val hasDash = lower.contains(" - ") || lower.contains(" – ")
    return when {
        hasAudioHint && !lower.contains("official audio") -> "$q official audio"
        hasAudioHint -> "$q lyrics"
        hasDash      -> "$q official audio"   // "Artist - Song" → "Artist - Song official audio"
        q.length > 40 -> "$q song"
        else          -> "$q official audio"
    }
}

private fun musicRelevanceScore(r: OnlineResult): Int {
    var s = 0
    val t = r.title.lowercase(); val c = r.author.lowercase()
    if (t.contains("official audio"))   s += 40
    if (t.contains("lyrics"))           s += 30
    if (t.contains("lyric video"))      s += 25
    if (t.contains("audio"))            s += 15
    if (t.contains("cover"))            s += 5
    if (c.contains("vevo"))             s += 35
    if (c.contains("records"))          s += 10
    if (c.contains("music"))            s += 5
    if (t.contains("reaction"))         s -= 50
    if (t.contains("interview"))        s -= 40
    if (t.contains("behind the scene")) s -= 30
    if (t.contains("review"))           s -= 20
    if (t.contains("tutorial"))         s -= 40
    if (Regex("^.{2,40}\\s[-–—]\\s.{2,60}$").matches(r.title)) s += 20
    if (r.durationSecs in 120..420) s += 10
    return s
}

// ─── Video renderer extraction (fixed: no early return, more renderer types) ──

/**
 * Recursively extracts video renderer objects from the Innertube JSON response.
 * Handles: videoRenderer, videoWithContextRenderer, compactVideoRenderer,
 *          gridVideoRenderer, reelItemRenderer, playlistVideoRenderer.
 * Does NOT early-return after finding a renderer — continues recursion to
 * capture nested/grouped results (e.g., shelves, carousels).
 */
internal fun extractVideoRenderers(obj: JSONObject, out: MutableList<JSONObject> = mutableListOf()): List<JSONObject> {
    // Check all known renderer types — add to list but keep recursing
    val rendererKeys = listOf(
        "videoRenderer",
        "videoWithContextRenderer",
        "compactVideoRenderer",
        "gridVideoRenderer",
        "reelItemRenderer",
        "playlistVideoRenderer",
    )
    for (key in rendererKeys) {
        if (obj.has(key)) {
            val renderer = obj.optJSONObject(key)
            if (renderer != null && renderer.has("videoId")) {
                out.add(renderer)
            }
        }
    }

    // Continue recursion into all children (no early return)
    obj.keys().forEach { key ->
        when (val v = obj.opt(key)) {
            is JSONObject -> extractVideoRenderers(v, out)
            is JSONArray  -> extractVideoRenderers(v, out)
        }
    }
    return out
}

private fun extractVideoRenderers(arr: JSONArray, out: MutableList<JSONObject> = mutableListOf()): List<JSONObject> {
    for (i in 0 until arr.length()) {
        when (val v = arr.opt(i)) {
            is JSONObject -> extractVideoRenderers(v, out)
            is JSONArray  -> extractVideoRenderers(v, out)
        }
    }
    return out
}

/**
 * Parses an Innertube video renderer into an OnlineResult.
 * Handles multiple JSON structures for title, author, duration, and thumbnails
 * across different renderer types (videoRenderer, compactVideoRenderer, etc.).
 *
 * isLive detection uses badge/overlay checks — NOT empty lengthText (which was
 * falsely marking videos with missing duration metadata as live streams).
 */
internal fun parseInnertubeRenderer(vr: JSONObject): OnlineResult? {
    val videoId  = vr.optString("videoId").ifEmpty { return null }

    // ── Title extraction (multiple paths for different renderers) ────────────
    val rawTitle = htmlDecode(
        vr.optJSONObject("headline")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("title")?.optString("simpleText")
            ?: vr.optJSONObject("title")?.optString("text")
            ?: ""
    ).ifEmpty { return null }

    // ── Author extraction (multiple paths) ──────────────────────────────────
    val rawAuthor =
        vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("channelThumbnailSupportedRenderers")
                ?.optJSONObject("channelThumbnailWithLinkRenderer")
                ?.optJSONObject("accessibility")?.optJSONObject("accessibilityData")?.optString("label")
            ?: ""

    // ── Duration extraction (multiple paths for different renderer types) ────
    val lengthText =
        // Standard videoRenderer path
        vr.optJSONObject("lengthText")?.optString("simpleText")
            ?: vr.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("lengthText")?.optJSONObject("accessibility")
                ?.optJSONObject("accessibilityData")?.optString("label")
            // thumbnailOverlays path (common in compactVideoRenderer / gridVideoRenderer)
            ?: extractDurationFromOverlays(vr)
            // timestamp on thumbnail (reelItemRenderer style)
            ?: vr.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                ?.optJSONObject("text")?.optString("simpleText")
            ?: ""

    val duration = parseTimestamp(lengthText)

    // ── Live detection (proper badge/overlay checks, NOT empty lengthText) ───
    val isLive = detectLiveStream(vr)

    // ── Thumbnail extraction ────────────────────────────────────────────────
    val thumbsArr  = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
    val thumb = if (thumbsArr != null && thumbsArr.length() > 0)
        thumbsArr.getJSONObject(thumbsArr.length() - 1).optString("url",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
    else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    val (cleanTitle, cleanArtist) = parseTitle(rawTitle, rawAuthor)
    return OnlineResult(
        videoId = videoId, title = cleanTitle, author = cleanArtist,
        thumbnailUrl = thumb, durationText = formatTime(duration),
        durationSecs = duration, isLive = isLive,
    )
}

/**
 * Extract duration from thumbnailOverlays — used by compactVideoRenderer,
 * gridVideoRenderer, and other mobile renderer types.
 */
private fun extractDurationFromOverlays(vr: JSONObject): String? {
    val overlays = vr.optJSONArray("thumbnailOverlays") ?: return null
    for (i in 0 until overlays.length()) {
        val overlay = overlays.optJSONObject(i) ?: continue
        val timeStatus = overlay.optJSONObject("thumbnailOverlayTimeStatusRenderer") ?: continue
        val text = timeStatus.optJSONObject("text")
        val simpleText = text?.optString("simpleText")
        if (!simpleText.isNullOrBlank()) return simpleText
        val runs = text?.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            return runs.optJSONObject(0)?.optString("text")
        }
        // Accessibility fallback
        val accLabel = text?.optJSONObject("accessibility")
            ?.optJSONObject("accessibilityData")?.optString("label")
        if (!accLabel.isNullOrBlank()) return accLabel
    }
    return null
}

/**
 * Properly detects live streams by checking badges and thumbnail overlays.
 * Does NOT rely on empty lengthText (which incorrectly flagged normal videos).
 */
private fun detectLiveStream(vr: JSONObject): Boolean {
    // Check badges array
    val badges = vr.optJSONArray("badges")
    if (badges != null) {
        for (i in 0 until badges.length()) {
            val badge = badges.optJSONObject(i) ?: continue
            val style = badge.optJSONObject("metadataBadgeRenderer")?.optString("style") ?: ""
            val label = badge.optJSONObject("metadataBadgeRenderer")?.optString("label") ?: ""
            if (style.contains("LIVE", true) || label.contains("LIVE", true)) return true
        }
    }
    // Check thumbnail overlays for LIVE status
    val overlays = vr.optJSONArray("thumbnailOverlays")
    if (overlays != null) {
        for (i in 0 until overlays.length()) {
            val overlay = overlays.optJSONObject(i) ?: continue
            val timeStatus = overlay.optJSONObject("thumbnailOverlayTimeStatusRenderer") ?: continue
            val style = timeStatus.optString("style")
            if (style.contains("LIVE", true)) return true
            val text = timeStatus.optJSONObject("text")?.optString("simpleText") ?: ""
            if (text.equals("LIVE", true)) return true
        }
    }
    // Check ownerBadges
    val ownerBadges = vr.optJSONArray("ownerBadges")
    if (ownerBadges != null) {
        val str = ownerBadges.toString()
        if (str.contains("LIVE_NOW", true)) return true
    }
    return false
}

private fun parseTitle(raw: String, channelTitle: String): Pair<String, String> {
    var title  = raw
    var artist = channelTitle
        .replace(Regex("VEVO|Official|Music|Channel|TV|Topic", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*[-–—]\\s*"), "").trim()
    val dash = Regex("^(.+?)\\s*[-–—]\\s*(.+?)(?:\\s*[\\(\\[].*)?$").find(raw)
    if (dash != null) { artist = dash.groupValues[1].trim(); title = dash.groupValues[2].trim() }
    title = title
        .replace(Regex("\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[Official.*?]",  RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Audio.*?\\)",   RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Lyric.*?\\)",   RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Music Video\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(ft\\..*?\\)",   RegexOption.IGNORE_CASE), "")
        .trim()
    return Pair(title.ifEmpty { raw }, artist.ifEmpty { channelTitle })
}

private fun parseTimestamp(s: String): Int {
    if (s.isBlank()) return 0
    // Handle accessibility labels like "3 minutes, 45 seconds"
    val accMatch = Regex("(\\d+)\\s*minute").find(s)
    val accSec   = Regex("(\\d+)\\s*second").find(s)
    if (accMatch != null) {
        val min = accMatch.groupValues[1].toIntOrNull() ?: 0
        val sec = accSec?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return min * 60 + sec
    }
    // Handle hour accessibility labels
    val accHour = Regex("(\\d+)\\s*hour").find(s)
    if (accHour != null) {
        val hr  = accHour.groupValues[1].toIntOrNull() ?: 0
        val min = Regex("(\\d+)\\s*minute").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val sec = accSec?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return hr * 3600 + min * 60 + sec
    }
    // Standard "M:SS" or "H:MM:SS" format
    val parts = s.split(":").mapNotNull { it.trim().toIntOrNull() }
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0
    }
}

private fun formatTime(s: Int): String {
    if (s <= 0) return "0:00"
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

private fun htmlDecode(s: String) = s
    .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    .replace("&quot;", "\"").replace("&#39;", "'")
