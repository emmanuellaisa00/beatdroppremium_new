package com.beatdrop.kt.youtube

import android.app.Application
import android.net.Uri
import com.beatdrop.kt.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ─── HTTP Client ──────────────────────────────────────────────────────────────
private val okHttp = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

// ─── Constants (mirrored from YoutubeService.ts) ──────────────────────────────
private const val YT_KEY    = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
private const val YT_PLAYER = "https://www.youtube.com/youtubei/v1/player"
private const val YT_SEARCH = "https://www.youtube.com/youtubei/v1/search"

private val INVIDIOUS_INSTANCES = listOf(
    "https://invidious.io.lol",
    "https://invidious.fdn.fr",
    "https://vid.puffyan.us",
    "https://invidious.slipfox.xyz",
)

// Innertube player clients — same fallback chain as the React Native app
private data class YtClient(
    val name: String,
    val clientName: String,
    val clientVersion: String,
    val headers: Map<String, String>,
    val extraContext: JSONObject = JSONObject(),
)

private val YT_CLIENTS = listOf(
    YtClient(
        name = "MWEB", clientName = "MWEB", clientVersion = "2.20241202.07.00",
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
            "X-Youtube-Client-Name" to "2",
            "X-Youtube-Client-Version" to "2.20241202.07.00",
            "Origin" to "https://m.youtube.com",
        )
    ),
    YtClient(
        name = "WEB_EMBEDDED", clientName = "WEB_EMBEDDED_PLAYER", clientVersion = "2.20241202.07.00",
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "X-Youtube-Client-Name" to "56",
            "X-Youtube-Client-Version" to "2.20241202.07.00",
            "Origin" to "https://www.youtube.com",
        )
    ),
    YtClient(
        name = "ANDROID_VR", clientName = "ANDROID_VR", clientVersion = "1.60.19",
        headers = mapOf(
            "User-Agent" to "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            "X-Youtube-Client-Name" to "28",
            "X-Youtube-Client-Version" to "1.60.19",
        ),
        extraContext = JSONObject().put("deviceMake", "Oculus").put("deviceModel", "Quest 3").put("androidSdkVersion", 32)
    ),
)

// ─── URL Cache (4-hour TTL) ───────────────────────────────────────────────────
private data class CacheEntry(val url: String, val cachedAt: Long)
private val urlCache = ConcurrentHashMap<String, CacheEntry>()
private const val URL_TTL_MS = 4 * 60 * 60 * 1000L

private fun getCachedUrl(videoId: String): String? {
    val e = urlCache[videoId] ?: return null
    if (System.currentTimeMillis() - e.cachedAt > URL_TTL_MS) { urlCache.remove(videoId); return null }
    return e.url
}
private fun setCachedUrl(videoId: String, url: String) {
    urlCache[videoId] = CacheEntry(url, System.currentTimeMillis())
}

// ─── Application context holder (set in BeatDropApp.onCreate) ─────────────────
object YoutubeService {
    internal var app: Application? = null
    fun init(application: Application) { app = application }

    val downloadDir: File?
        get() = app?.getExternalFilesDir(null)?.let { File(it, "BeatDrop/Downloads") }?.also { it.mkdirs() }
}

// ─── Search ───────────────────────────────────────────────────────────────────
/**
 * Innertube /search — no API key quota, no registration.
 * Mirrors searchYoutube() from YoutubeService.ts.
 */
suspend fun searchYoutube(query: String, maxResults: Int = 20): List<OnlineResult> =
    withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("query", musicifyQuery(query.trim()))
            put("context", JSONObject().put("client", JSONObject().apply {
                put("clientName", "MWEB"); put("clientVersion", "2.20241202.07.00")
                put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
            }))
            put("params", "EgWKAQIIAQ%3D%3D") // music category filter
        }.toString()

        val req = Request.Builder()
            .url("$YT_SEARCH?key=$YT_KEY&prettyPrint=false")
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

        extractVideoRenderers(json)
            .mapNotNull { parseInnertubeRenderer(it) }
            .filter { !it.isLive && it.durationSecs <= 600 && (it.durationSecs == 0 || it.durationSecs >= 60) }
            .sortedByDescending { musicRelevanceScore(it) }
            .take(maxResults)
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

// ─── Stream URL resolution (5-strategy fallback chain) ───────────────────────
/**
 * Mirrors getStreamUrl() from YoutubeService.ts.
 * Strategy 1: YoutubeExtractor (WebView / BotGuard-immune)
 * Strategy 2-4: Innertube /player (MWEB, WEB_EMBEDDED, ANDROID_VR)
 * Strategy 5: Invidious public instances
 */
suspend fun getStreamUrl(videoId: String): String {
    getCachedUrl(videoId)?.let { return it }

    // Strategy 1 — WebView extractor (Snaptube / BotGuard-immune)
    if (YoutubeExtractor.isConfigured) {
        try {
            val url = YoutubeExtractor.extractStreamUrl(videoId, 14_000)
            if (!url.isNullOrBlank()) { setCachedUrl(videoId, url); return url }
        } catch (_: Exception) {}
    }

    // Strategy 2-4 — Innertube player API
    for (client in YT_CLIENTS) {
        try {
            val body = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().put("client", JSONObject().apply {
                    put("clientName", client.clientName)
                    put("clientVersion", client.clientVersion)
                    put("hl", "en"); put("gl", "US")
                    client.extraContext.keys().forEach { k -> put(k, client.extraContext.get(k)) }
                }))
                put("playbackContext", JSONObject().put("contentPlaybackContext",
                    JSONObject().put("html5Preference", "HTML5_PREF_WANTS")))
                put("contentCheckOk", true); put("racyCheckOk", true)
            }.toString()

            val req = Request.Builder()
                .url("$YT_PLAYER?key=$YT_KEY&prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .apply { client.headers.forEach { (k, v) -> header(k, v) } }
                .header("Content-Type", "application/json")
                .build()

            val data = okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body!!.string())
            } ?: continue
            if (data.optJSONObject("playabilityStatus")?.optString("status") != "OK") continue
            val url = getBestAudioUrl(data.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats"))
            if (!url.isNullOrBlank()) { setCachedUrl(videoId, url); return url }
        } catch (_: Exception) {}
    }

    // Strategy 5 — Invidious
    for (instance in INVIDIOUS_INSTANCES) {
        try {
            val data = okHttp.newCall(
                Request.Builder().url("$instance/api/v1/videos/$videoId?fields=adaptiveFormats,formatStreams").build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body!!.string())
            } ?: continue
            val url = getBestAudioUrl(data.optJSONArray("adaptiveFormats"))
                ?: data.optJSONArray("formatStreams")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("url") else null
                }
            if (!url.isNullOrBlank()) { setCachedUrl(videoId, url); return url }
        } catch (_: Exception) {}
    }

    throw Exception("Could not load this track. Try a different song or check your connection.")
}

// ─── Download ─────────────────────────────────────────────────────────────────
data class DownloadProgress(val bytesWritten: Long, val contentLength: Long, val percent: Int)

suspend fun downloadYoutubeTrack(
    result: OnlineResult,
    onProgress: (DownloadProgress) -> Unit = {},
): Track = withContext(Dispatchers.IO) {
    val dir = YoutubeService.downloadDir
        ?: throw Exception("External storage not available")

    // Resolve stream URL (WebView first, then HTTP fallbacks)
    var streamUrl = ""
    var fileExt = "m4a"
    if (YoutubeExtractor.isConfigured) {
        try {
            streamUrl = YoutubeExtractor.extractStreamUrl(result.videoId, 14_000) ?: ""
        } catch (_: Exception) {}
    }
    if (streamUrl.isBlank()) {
        for (client in YT_CLIENTS) {
            try {
                val body = JSONObject().apply {
                    put("videoId", result.videoId)
                    put("context", JSONObject().put("client", JSONObject().apply {
                        put("clientName", client.clientName); put("clientVersion", client.clientVersion)
                        put("hl", "en"); put("gl", "US")
                        client.extraContext.keys().forEach { k -> put(k, client.extraContext.get(k)) }
                    }))
                    put("contentCheckOk", true); put("racyCheckOk", true)
                }.toString()
                val req = Request.Builder()
                    .url("$YT_PLAYER?key=$YT_KEY&prettyPrint=false")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .apply { client.headers.forEach { (k, v) -> header(k, v) } }
                    .header("Content-Type", "application/json")
                    .build()
                val data = okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) null else JSONObject(resp.body!!.string())
                } ?: continue
                if (data.optJSONObject("playabilityStatus")?.optString("status") != "OK") continue
                val formats = data.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
                val fmt = getBestAudioFormat(formats)
                if (fmt != null) {
                    streamUrl = fmt.optString("url", "")
                    val mime = (fmt.optString("mimeType") + fmt.optString("type")).lowercase()
                    fileExt = when {
                        mime.contains("webm") || mime.contains("opus") -> "opus"
                        mime.contains("ogg") -> "ogg"
                        else -> "m4a"
                    }
                    if (streamUrl.isNotBlank()) break
                }
            } catch (_: Exception) {}
        }
    }
    if (streamUrl.isBlank()) streamUrl = getStreamUrl(result.videoId)

    // Download audio file
    val safeTitle = result.title.replace(Regex("[^a-zA-Z0-9 \\-_]"), "").take(80).trim()
    val fileName = "${safeTitle}_${result.videoId}.$fileExt"
    val filePath = File(dir, fileName)

    val req = Request.Builder().url(streamUrl).build()
    okHttp.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) throw Exception("Download failed (HTTP ${resp.code})")
        val body = resp.body ?: throw Exception("Empty response body")
        val contentLength = body.contentLength()
        var bytesWritten = 0L
        FileOutputStream(filePath).use { fos ->
            body.byteStream().use { inp ->
                val buf = ByteArray(8192)
                var read: Int
                while (inp.read(buf).also { read = it } != -1) {
                    fos.write(buf, 0, read)
                    bytesWritten += read
                    val pct = if (contentLength > 0) ((bytesWritten * 100) / contentLength).toInt() else 0
                    onProgress(DownloadProgress(bytesWritten, contentLength, pct))
                }
            }
        }
    }
    if (!filePath.exists() || filePath.length() < 1024)
        throw Exception("Download produced an empty or corrupt file.")

    // Download thumbnail
    val thumbUrl = result.thumbnailUrl
    var artworkPath: String? = thumbUrl
    if (!thumbUrl.isNullOrBlank()) {
        try {
            val artFile = File(dir, "${safeTitle}_${result.videoId}.jpg")
            okHttp.newCall(Request.Builder().url(thumbUrl).build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    FileOutputStream(artFile).use { it.write(resp.body!!.bytes()) }
                    artworkPath = "file://${artFile.absolutePath}"
                }
            }
        } catch (_: Exception) {}
    }

    val (parsedTitle, parsedArtist) = parseTitle(result.title, result.author)
    val meta = runCatching { enrichTrackMetadata(parsedTitle, parsedArtist) }.getOrNull()
    Track(
        id        = "dl_${result.videoId}",
        uri       = Uri.fromFile(filePath),
        title     = meta?.title ?: parsedTitle,
        artist    = meta?.artist ?: parsedArtist,
        album     = meta?.album ?: result.author,
        albumId   = 0L,
        durationMs = result.durationSecs * 1000L,
        data      = filePath.absolutePath,
        dateAdded = System.currentTimeMillis(),
        artworkOverride = meta?.artwork ?: artworkPath,
    )
}

// ─── iTunes metadata enrichment ──────────────────────────────────────────────
data class EnrichedMeta(val artwork: String?, val album: String?, val artist: String?, val title: String? = null)

private val metaCache = ConcurrentHashMap<String, EnrichedMeta>()

suspend fun enrichTrackMetadata(title: String, artist: String): EnrichedMeta =
    withContext(Dispatchers.IO) {
        val key = "${artist.lowercase()}::${title.lowercase()}"
        metaCache[key]?.let { return@withContext it }
        try {
            val q = Uri.encode("$artist $title")
            val data = okHttp.newCall(
                Request.Builder().url("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=5").build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body!!.string())
            }
            val results = data?.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val match = (0 until results.length()).map { results.getJSONObject(it) }
                    .firstOrNull { it.optString("artistName").lowercase().startsWith(artist.lowercase().take(4)) }
                    ?: results.getJSONObject(0)
                val art = match.optString("artworkUrl100", "")
                    .replace("100x100bb", "600x600bb").replace("100x100", "600x600")
                val meta = EnrichedMeta(
                    artwork = art.ifEmpty { null },
                    album   = match.optString("collectionName").ifEmpty { null },
                    artist  = match.optString("artistName").ifEmpty { null },
                    title   = match.optString("trackName").ifEmpty { null },
                )
                metaCache[key] = meta
                return@withContext meta
            }
        } catch (_: Exception) {}
        EnrichedMeta(null, null, null, null).also { metaCache[key] = it }
    }

// ─── Convert search result → Track (for streaming) ───────────────────────────
suspend fun youtubeResultToTrack(result: OnlineResult): Track {
    val streamUrl = getStreamUrl(result.videoId)
    val (title, artist) = parseTitle(result.title, result.author)
    val meta = enrichTrackMetadata(title, artist)
    return Track(
        id        = "yt_${result.videoId}",
        uri       = Uri.parse(streamUrl),
        title     = meta.title   ?: title,
        artist    = meta.artist  ?: artist,
        album     = meta.album   ?: result.author,
        albumId   = 0L,
        durationMs = result.durationSecs * 1000L,
        data      = null,
        dateAdded = System.currentTimeMillis(),
        artworkOverride = meta.artwork ?: result.thumbnailUrl,
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
private fun musicifyQuery(q: String): String {
    val lower = q.lowercase()
    return if (lower.contains("official audio") || lower.contains("lyrics") ||
        lower.contains("audio") || lower.contains("music video"))
        q else "$q official audio"
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
    if (Regex("^.{2,40}\\s[-–—]\\s.{2,60}\$").matches(r.title)) s += 20
    if (r.durationSecs in 120..420) s += 10
    return s
}

// Recursively extract videoRenderer nodes — mirrors extractVideoRenderers() in TS
internal fun extractVideoRenderers(obj: JSONObject, out: MutableList<JSONObject> = mutableListOf()): List<JSONObject> {
    if (obj.has("videoRenderer") && obj.optJSONObject("videoRenderer")?.has("videoId") == true) {
        out.add(obj.getJSONObject("videoRenderer")); return out
    }
    if (obj.has("videoWithContextRenderer") && obj.optJSONObject("videoWithContextRenderer")?.has("videoId") == true) {
        out.add(obj.getJSONObject("videoWithContextRenderer")); return out
    }
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

internal fun parseInnertubeRenderer(vr: JSONObject): OnlineResult? {
    val videoId = vr.optString("videoId").ifEmpty { return null }
    val rawTitle = htmlDecode(
        vr.optJSONObject("headline")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("title")?.optString("simpleText") ?: ""
    ).ifEmpty { return null }
    val rawAuthor = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
        ?: vr.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
    val lengthText = vr.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
        ?: vr.optJSONObject("lengthText")?.optString("simpleText")
        ?: vr.optJSONObject("lengthText")?.optJSONObject("accessibility")
            ?.optJSONObject("accessibilityData")?.optString("label") ?: ""
    val duration = parseTimestamp(lengthText)
    val thumbsArr = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
    val thumb = if (thumbsArr != null && thumbsArr.length() > 0)
        thumbsArr.getJSONObject(thumbsArr.length() - 1).optString("url",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
    else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    val isLive = lengthText.isEmpty()

    // Clean up YouTube-specific cruft so results look like a music catalog
    val (cleanTitle, cleanArtist) = parseTitle(rawTitle, rawAuthor)
    return OnlineResult(
        videoId = videoId, title = cleanTitle, author = cleanArtist,
        thumbnailUrl = thumb, durationText = formatTime(duration),
        durationSecs = duration, isLive = isLive,
    )
}

private fun getBestAudioUrl(formats: JSONArray?): String? {
    if (formats == null) return null
    return getBestAudioFormat(formats)?.optString("url")?.ifEmpty { null }
}

private fun getBestAudioFormat(formats: JSONArray?): JSONObject? {
    if (formats == null) return null
    val audio = (0 until formats.length())
        .map { formats.getJSONObject(it) }
        .filter { f ->
            val mt = (f.optString("mimeType") + f.optString("type")).lowercase()
            mt.startsWith("audio/") && f.optString("url").isNotBlank()
        }
        .sortedByDescending { f -> f.optLong("bitrate").coerceAtLeast(f.optLong("averageBitrate")) }
    return audio.firstOrNull()
}

private fun parseTitle(raw: String, channelTitle: String): Pair<String, String> {
    var title = raw
    var artist = channelTitle
        .replace(Regex("VEVO|Official|Music|Channel|TV|Topic", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*[-–—]\\s*"), "")
        .trim()
    val dash = Regex("^(.+?)\\s*[-–—]\\s*(.+?)(?:\\s*[\\(\\[].*)?$").find(raw)
    if (dash != null) { artist = dash.groupValues[1].trim(); title = dash.groupValues[2].trim() }
    title = title
        .replace(Regex("\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[Official.*?]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Audio.*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Lyric.*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Music Video\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(ft\\..*?\\)", RegexOption.IGNORE_CASE), "")
        .trim()
    return Pair(title.ifEmpty { raw }, artist.ifEmpty { channelTitle })
}

private fun parseTimestamp(s: String): Int {
    if (s.isBlank()) return 0
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
