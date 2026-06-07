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
 * YouTube trending / discovery feed.
 * Fetches trending music videos and curated categories from YouTube's browse endpoint.
 */
object YouTubeTrending {

    private const val YT_BROWSE = "https://www.youtube.com/youtubei/v1/browse"

    // Browse IDs for YouTube Music categories
    private const val TRENDING_BROWSE_ID = "FEmusic_trending"
    private const val NEW_RELEASES_BROWSE_ID = "FEmusic_new_releases"
    private const val TOP_HITS_BROWSE_ID = "FEmusic_charts"
    private const val MOODS_GENRES_BROWSE_ID = "FEmusic_moods_and_genres"

    /**
     * Fetch trending music (the YouTube Music "Trending" tab).
     * Returns a list of OnlineResult entries.
     */
    suspend fun fetchTrending(maxResults: Int = 30): List<OnlineResult> =
        fetchBrowseResults(TRENDING_BROWSE_ID, maxResults)

    /**
     * Fetch new releases from YouTube Music.
     */
    suspend fun fetchNewReleases(maxResults: Int = 20): List<OnlineResult> =
        fetchBrowseResults(NEW_RELEASES_BROWSE_ID, maxResults)

    /**
     * Fetch top charts / top hits.
     */
    suspend fun fetchTopHits(maxResults: Int = 20): List<OnlineResult> =
        fetchBrowseResults(TOP_HITS_BROWSE_ID, maxResults)

    /**
     * Fetch curated playlists by mood/genre.
     */
    suspend fun fetchMoodsAndGenres(): List<MoodGenre> =
        withContext(Dispatchers.IO) {
            val body = buildBrowseBody(MOODS_GENRES_BROWSE_ID)
            val req = buildBrowseRequest(body)
            try {
                val json = com.beatdrop.kt.youtube.okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    JSONObject(resp.body!!.string())
                }
                parseMoodGenres(json)
            } catch (e: Exception) {
                DebugLog.w("trending", "moods fetch failed: ${e.message}")
                emptyList()
            }
        }

    /**
     * Fetch the latest videos from a specific channel ID.
     */
    suspend fun fetchChannelVideos(channelId: String, maxResults: Int = 15): List<OnlineResult> =
        withContext(Dispatchers.IO) {
            val body = buildBrowseBody(channelId)
            val req = buildBrowseRequest(body)
            try {
                val json = com.beatdrop.kt.youtube.okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    JSONObject(resp.body!!.string())
                }
                parseVideoRenderers(json, maxResults)
            } catch (e: Exception) {
                DebugLog.w("trending", "channel $channelId: ${e.message}")
                emptyList()
            }
        }

    private suspend fun fetchBrowseResults(browseId: String, maxResults: Int): List<OnlineResult> =
        withContext(Dispatchers.IO) {
            val body = buildBrowseBody(browseId)
            val req = buildBrowseRequest(body)
            try {
                val json = com.beatdrop.kt.youtube.okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    JSONObject(resp.body!!.string())
                }
                parseVideoRenderers(json, maxResults)
            } catch (e: Exception) {
                DebugLog.w("trending", "browse $browseId: ${e.message}")
                emptyList()
            }
        }

    private fun buildBrowseBody(browseId: String): String {
        return JSONObject().apply {
            put("browseId", browseId)
            put("context", JSONObject().put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20240501.01.00")
                put("hl", "en")
                put("gl", "US")
                put("utcOffsetMinutes", 0)
            }))
        }.toString()
    }

    private fun buildBrowseRequest(body: String): Request {
        return Request.Builder()
            .url("$YT_BROWSE?prettyPrint=false")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240501.01.00")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .build()
    }

    private fun parseVideoRenderers(json: JSONObject, maxResults: Int): List<OnlineResult> {
        val items = mutableListOf<JSONObject>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    for (key in listOf("musicResponsiveListItemRenderer", "videoRenderer",
                        "compactVideoRenderer", "gridVideoRenderer")) {
                        o.optJSONObject(key)?.let { items.add(it) }
                    }
                    o.keys().forEach { walk(o.opt(it)) }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(json)

        return items.mapNotNull { item ->
            try {
                // Try music renderer format first
                val videoId = item
                    .optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    ?: item.optString("videoId").ifBlank { null }
                    ?: return@mapNotNull null

                val title = item.optJSONArray("flexColumns")
                    ?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: return@mapNotNull null

                val secondaryRuns = item.optJSONArray("flexColumns")?.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")
                var artist = ""
                var durationText = ""
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
                        } ?: item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let {
                            if (it.length() > 0) it.getJSONObject(it.length()-1).optString("url") else null
                        }
                ) ?: ytThumbHd(videoId)

                OnlineResult(
                    videoId = videoId, title = title, author = artist,
                    thumbnailUrl = thumb, durationText = durationText,
                    durationSecs = durationSecs, isLive = false,
                )
            } catch (_: Exception) { null }
        }.take(maxResults)
    }

    private fun parseMoodGenres(json: JSONObject): List<MoodGenre> {
        val out = mutableListOf<MoodGenre>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    val renderer = o.optJSONObject("musicNavigationButtonRenderer")
                        ?: o.optJSONObject("musicTwoRowItemRenderer")
                    if (renderer != null) {
                        val title = renderer.optJSONObject("text")?.optString("simpleText")
                            ?: renderer.optJSONObject("title")?.optJSONArray("runs")
                                ?.optJSONObject(0)?.optString("text")
                            ?: return
                        val browseId = renderer.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")?.optString("browseId")
                            ?: return
                        val icon = renderer.optJSONObject("icon")?.optString("iconType") ?: "MUSIC"
                        out.add(MoodGenre(title, browseId, icon))
                    }
                    o.keys().forEach { walk(o.opt(it)) }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(json)
        return out
    }
}

data class MoodGenre(
    val title: String,
    val browseId: String,
    val iconType: String,
)
