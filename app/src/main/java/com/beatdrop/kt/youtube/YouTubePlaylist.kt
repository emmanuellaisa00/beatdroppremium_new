package com.beatdrop.kt.youtube

import com.beatdrop.kt.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube playlist parser — fetches all videos in a playlist for batch download.
 */
object YouTubePlaylist {

    private const val YT_BROWSE = "https://www.youtube.com/youtubei/v1/browse"

    /**
     * Fetch all videos in a YouTube playlist.
     * Handles pagination (continuation tokens) for large playlists.
     */
    suspend fun fetchPlaylist(
        playlistId: String,
        maxItems: Int = 200,
        forceRefresh: Boolean = false,
    ): PlaylistInfo = withContext(Dispatchers.IO) {
        // ── Cache hit (24-h TTL, disk-backed) ───────────────────────────
        // Fixes the 'Discover takes forever' / 'why does this re-fetch
        // every time I open it' user pain points. If we already have
        // the playlist in cache and the caller didn't explicitly ask for
        // a refresh, skip the whole Innertube round-trip.
        if (!forceRefresh) {
            PlaylistCache.get(playlistId)?.let { cached ->
                com.beatdrop.kt.DebugLog.i("playlist",
                    "cache hit: $playlistId (${cached.videos.size} videos)")
                return@withContext cached
            }
        }

        val body = JSONObject().apply {
            put("browseId", "VL$playlistId")
            put("context", JSONObject().put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20240501.01.00")
                put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
            }))
        }.toString()

        val req = Request.Builder()
            .url("$YT_BROWSE?prettyPrint=false")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240501.01.00")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .build()

        val json = try {
            com.beatdrop.kt.youtube.okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext PlaylistInfo(playlistId, "", emptyList())
                JSONObject(resp.body!!.string())
            }
        } catch (e: Exception) {
            DebugLog.e("playlist", "fetch failed: ${e.message}")
            return@withContext PlaylistInfo(playlistId, "", emptyList())
        }

        // Extract playlist title
        val title = extractPlaylistTitle(json)

        // Extract video items
        val items = mutableListOf<OnlineResult>()
        extractPlaylistItems(json, items)

        // Handle continuation (pagination)
        var contToken = extractContinuation(json)
        var attempts = 0
        while (contToken != null && items.size < maxItems && attempts < 10) {
            attempts++
            val contBody = JSONObject().apply {
                put("continuation", contToken)
                put("context", JSONObject().put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240501.01.00")
                    put("hl", "en"); put("gl", "US")
                }))
            }.toString()

            val contReq = Request.Builder()
                .url("$YT_BROWSE?prettyPrint=false")
                .post(contBody.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("X-Youtube-Client-Name", "67")
                .build()

            try {
                val response = com.beatdrop.kt.youtube.okHttp.newCall(contReq).execute()
                val contJson = response.use { resp ->
                    if (!resp.isSuccessful) null
                    else JSONObject(resp.body!!.string())
                }
                if (contJson == null) break
                extractPlaylistItems(contJson, items)
                contToken = extractContinuation(contJson)
            } catch (_: Exception) { break }
        }

        val result = PlaylistInfo(playlistId, title, items.take(maxItems))
        // Store on success (cache.put no-ops on empty results — so a
        // partial / failed fetch never poisons the cache).
        PlaylistCache.put(playlistId, result)
        result
    }

    private fun extractPlaylistTitle(json: JSONObject): String {
        val header = json.optJSONObject("header")
            ?: json.optJSONObject("microformat")?.optJSONObject("microformatDataRenderer")
        return header?.optJSONObject("musicDetailHeaderRenderer")
            ?.optJSONObject("title")?.optString("simpleText")
            ?: header?.optJSONObject("playlistHeaderRenderer")
                ?.optJSONObject("title")?.optString("simpleText")
            ?: header?.optString("title")
            ?: ""
    }

    private fun extractPlaylistItems(json: JSONObject, out: MutableList<OnlineResult>) {
        val items = mutableListOf<JSONObject>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    if (o.has("musicResponsiveListItemRenderer")) {
                        o.optJSONObject("musicResponsiveListItemRenderer")?.let { items.add(it) }
                    }
                    if (o.has("playlistVideoRenderer")) {
                        o.optJSONObject("playlistVideoRenderer")?.let { items.add(it) }
                    }
                    o.keys().forEach { walk(o.opt(it)) }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(json)

        for (item in items) {
            try {
                val videoId = item
                    .optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    ?: item.optJSONObject("playlistItemData")?.optString("videoId")
                    ?: item.optString("videoId").ifBlank { null }
                    ?: continue

                val flex = item.optJSONArray("flexColumns")
                val title = flex?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: continue

                var artist = ""
                var durationText = ""
                val secondaryRuns = flex?.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")
                if (secondaryRuns != null) {
                    val parts = (0 until secondaryRuns.length()).mapNotNull {
                        secondaryRuns.optJSONObject(it)?.optString("text")
                    }
                    val cleaned = parts.filter { it.isNotBlank() && it != " • " }
                    if (cleaned.isNotEmpty()) artist = cleaned[0]
                    cleaned.lastOrNull { it.matches(Regex("\\d+:\\d{2}(?::\\d{2})?")) }?.let { durationText = it }
                }

                val durationSecs = if (durationText.isNotBlank()) {
                    val p = durationText.split(":").mapNotNull { it.toIntOrNull() }
                    when (p.size) { 2 -> p[0]*60+p[1]; 3 -> p[0]*3600+p[1]*60+p[2]; else -> 0 }
                } else 0

                val thumb = upgradeThumbnailUrl(
                    item.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")?.let {
                            if (it.length() > 0) it.getJSONObject(it.length()-1).optString("url") else null
                        }
                ) ?: ytThumbHd(videoId)

                out.add(OnlineResult(
                    videoId = videoId, title = title, author = artist,
                    thumbnailUrl = thumb, durationText = durationText,
                    durationSecs = durationSecs, isLive = false,
                ))
            } catch (_: Exception) { continue }
        }
    }

    private fun extractContinuation(json: JSONObject): String? {
        fun walk(o: Any?): String? {
            when (o) {
                is JSONObject -> {
                    o.optJSONObject("continuationItemRenderer")
                        ?.optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")?.let { return it }
                    o.keys().forEach { walk(o.opt(it))?.let { return it } }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))?.let { return it }
            }
            return null
        }
        return walk(json)
    }
}

data class PlaylistInfo(
    val playlistId: String,
    val title: String,
    val videos: List<OnlineResult>,
)
