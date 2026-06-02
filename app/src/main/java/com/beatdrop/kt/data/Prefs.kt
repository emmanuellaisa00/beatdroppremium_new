package com.beatdrop.kt.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "beatdrop_prefs")

/**
 * DataStore-backed persistence — replaces the RN mmkv / async-storage usage.
 * Stores: liked track ids, playlists (name -> track ids), settings, play counts.
 */
class Prefs(private val context: Context) {

    private object Keys {
        val LIKED = stringPreferencesKey("liked")           // JSON array of ids
        val PLAYLISTS = stringPreferencesKey("playlists")   // JSON {name: [ids]}
        val PLAY_COUNTS = stringPreferencesKey("play_counts") // JSON {id: count}
        val THEME = stringPreferencesKey("theme")            // "system" | "dark" | "light"
        val HAPTICS = booleanPreferencesKey("haptics")
        val DEFAULT_SHUFFLE = booleanPreferencesKey("default_shuffle")
        val AUTO_DJ = booleanPreferencesKey("auto_dj")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val RESOLVER_BACKEND_URL = stringPreferencesKey("resolver_backend_url")
        val STREAM_QUALITY = stringPreferencesKey("stream_quality") // "auto" | "high" | "medium" | "low"
        val MUSIC_SEARCH_ENABLED = booleanPreferencesKey("music_search_enabled")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        // Cached on-device audio features: JSON {trackId: {"bpm":128,"key":"8A"}}
        val TRACK_FEATURES = stringPreferencesKey("track_features")
        // ── New keys for SnapTube features ──
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val DOWNLOAD_SPEED_LIMIT = intPreferencesKey("download_speed_limit") // KB/s, 0 = unlimited
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_DIR_PATH = stringPreferencesKey("download_dir_path")
        val PRIVATE_PIN = stringPreferencesKey("private_pin")  // hashed PIN
        val SEARCH_PLATFORM = stringPreferencesKey("search_platform") // "YouTube"
    }

    // ── liked ──
    val likedFlow: Flow<Set<String>> = context.dataStore.data.map { p ->
        jsonArrayToSet(p[Keys.LIKED])
    }
    suspend fun setLiked(ids: Set<String>) {
        context.dataStore.edit { it[Keys.LIKED] = JSONArray(ids.toList()).toString() }
    }

    // ── playlists ──
    val playlistsFlow: Flow<Map<String, List<String>>> = context.dataStore.data.map { p ->
        jsonToPlaylists(p[Keys.PLAYLISTS])
    }
    suspend fun setPlaylists(map: Map<String, List<String>>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, JSONArray(v)) }
        context.dataStore.edit { it[Keys.PLAYLISTS] = obj.toString() }
    }

    // ── play counts (for Stats) ──
    val playCountsFlow: Flow<Map<String, Int>> = context.dataStore.data.map { p ->
        jsonToCounts(p[Keys.PLAY_COUNTS])
    }
    suspend fun incrementPlayCount(id: String) {
        context.dataStore.edit { prefs ->
            val map = jsonToCounts(prefs[Keys.PLAY_COUNTS]).toMutableMap()
            map[id] = (map[id] ?: 0) + 1
            val obj = JSONObject(); map.forEach { (k, v) -> obj.put(k, v) }
            prefs[Keys.PLAY_COUNTS] = obj.toString()
        }
    }

    // ── settings ──
    val themeFlow: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "light" }
    suspend fun setTheme(v: String) { context.dataStore.edit { it[Keys.THEME] = v } }

    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS] ?: false }
    suspend fun setHaptics(v: Boolean) { context.dataStore.edit { it[Keys.HAPTICS] = v } }

    val defaultShuffleFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEFAULT_SHUFFLE] ?: false }
    suspend fun setDefaultShuffle(v: Boolean) { context.dataStore.edit { it[Keys.DEFAULT_SHUFFLE] = v } }

    val autoDjFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DJ] ?: false }
    suspend fun setAutoDj(v: Boolean) { context.dataStore.edit { it[Keys.AUTO_DJ] = v } }

    // Crossfade duration in milliseconds (default 8 s). UI slider exposes 4..12 s.
    val crossfadeMsFlow: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFADE_MS] ?: 8_000 }
    suspend fun setCrossfadeMs(v: Int) { context.dataStore.edit { it[Keys.CROSSFADE_MS] = v.coerceIn(4_000, 12_000) } }

    // Optional self-hosted resolver backend URL (Cloudflare Worker / Render etc.)
    // Empty string = disabled. When set, used as Strategy 0 in the YouTube resolver.
    val resolverBackendFlow: Flow<String> = context.dataStore.data.map { it[Keys.RESOLVER_BACKEND_URL] ?: "" }
    suspend fun setResolverBackend(v: String) {
        context.dataStore.edit { it[Keys.RESOLVER_BACKEND_URL] = v.trim() }
    }

    // Stream quality preference. "auto" picks the highest available; others cap.
    val streamQualityFlow: Flow<String> = context.dataStore.data.map { it[Keys.STREAM_QUALITY] ?: "auto" }
    suspend fun setStreamQuality(v: String) {
        context.dataStore.edit { it[Keys.STREAM_QUALITY] = v }
    }

    // Use YouTube Music's curated music search (WEB_REMIX client) instead of
    // generic YouTube search. Filters out reactions / lyric channels / fan covers.
    val musicSearchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.MUSIC_SEARCH_ENABLED] ?: true }
    suspend fun setMusicSearchEnabled(v: Boolean) {
        context.dataStore.edit { it[Keys.MUSIC_SEARCH_ENABLED] = v }
    }

    // ── Cached per-track audio features (BPM + Camelot key) ────────────────────
    // Stored as JSON {trackId: {"bpm":<int>, "key":"<camelot>"}}. Analyzed once
    // per track by TrackAnalyzer and looked up by AutoMixEngine.
    val trackFeaturesFlow: Flow<Map<String, com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures>> =
        context.dataStore.data.map { p -> jsonToFeatures(p[Keys.TRACK_FEATURES]) }

    suspend fun putTrackFeatures(id: String, feat: com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures) {
        context.dataStore.edit { prefs ->
            val map = jsonToFeatures(prefs[Keys.TRACK_FEATURES]).toMutableMap()
            map[id] = feat
            val obj = JSONObject()
            map.forEach { (k, v) ->
                obj.put(k, JSONObject().put("bpm", v.bpm).put("key", v.keyCamelot))
            }
            prefs[Keys.TRACK_FEATURES] = obj.toString()
        }
    }

    private fun jsonToFeatures(s: String?): Map<String, com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures> {
        if (s.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(s); val out = HashMap<String, com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures>()
            o.keys().forEach { k ->
                val f = o.optJSONObject(k) ?: return@forEach
                out[k] = com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures(
                    bpm = f.optInt("bpm", 0),
                    keyCamelot = f.optString("key", "")
                )
            }
            out
        }.getOrDefault(emptyMap())
    }

    // ── search history ──
    val searchHistoryFlow: Flow<List<String>> = context.dataStore.data.map { p ->
        jsonArrayToList(p[Keys.SEARCH_HISTORY])
    }
    suspend fun addSearchQuery(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val list = jsonArrayToList(prefs[Keys.SEARCH_HISTORY]).toMutableList()
            list.remove(trimmed)
            list.add(0, trimmed)
            val limited = list.take(15) // limit to top 15 queries
            prefs[Keys.SEARCH_HISTORY] = JSONArray(limited).toString()
        }
    }
    suspend fun deleteSearchQuery(q: String) {
        context.dataStore.edit { prefs ->
            val list = jsonArrayToList(prefs[Keys.SEARCH_HISTORY]).toMutableList()
            list.remove(q)
            prefs[Keys.SEARCH_HISTORY] = JSONArray(list).toString()
        }
    }

    // ── New SnapTube-style settings ──────────────────────────────────────────

    val wifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY_DOWNLOADS] ?: false }
    suspend fun setWifiOnly(v: Boolean) { context.dataStore.edit { it[Keys.WIFI_ONLY_DOWNLOADS] = v } }

    val downloadSpeedLimitFlow: Flow<Int> = context.dataStore.data.map { it[Keys.DOWNLOAD_SPEED_LIMIT] ?: 0 }
    suspend fun setDownloadSpeedLimit(v: Int) { context.dataStore.edit { it[Keys.DOWNLOAD_SPEED_LIMIT] = v.coerceAtLeast(0) } }

    val maxConcurrentDownloadsFlow: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3 }
    suspend fun setMaxConcurrentDownloads(v: Int) { context.dataStore.edit { it[Keys.MAX_CONCURRENT_DOWNLOADS] = v.coerceIn(1, 5) } }

    val downloadDirPathFlow: Flow<String> = context.dataStore.data.map { it[Keys.DOWNLOAD_DIR_PATH] ?: "" }
    suspend fun setDownloadDirPath(v: String) { context.dataStore.edit { it[Keys.DOWNLOAD_DIR_PATH] = v } }

    val privatePinFlow: Flow<String?> = context.dataStore.data.map { it[Keys.PRIVATE_PIN] }
    suspend fun setPrivatePin(v: String) { context.dataStore.edit { it[Keys.PRIVATE_PIN] = v } }

    val searchPlatformFlow: Flow<String> = context.dataStore.data.map { it[Keys.SEARCH_PLATFORM] ?: "YouTube" }
    suspend fun setSearchPlatform(v: String) { context.dataStore.edit { it[Keys.SEARCH_PLATFORM] = v } }

    // ── helpers ──
    private fun jsonArrayToList(s: String?): List<String> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val a = JSONArray(s); (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun jsonArrayToSet(s: String?): Set<String> {
        if (s.isNullOrBlank()) return emptySet()
        return runCatching {
            val a = JSONArray(s); (0 until a.length()).map { a.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }
    private fun jsonToPlaylists(s: String?): Map<String, List<String>> {
        if (s.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(s); val out = LinkedHashMap<String, List<String>>()
            o.keys().forEach { k -> val a = o.getJSONArray(k); out[k] = (0 until a.length()).map { a.getString(it) } }
            out
        }.getOrDefault(emptyMap())
    }
    private fun jsonToCounts(s: String?): Map<String, Int> {
        if (s.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(s); val out = HashMap<String, Int>()
            o.keys().forEach { k -> out[k] = o.getInt(k) }
            out
        }.getOrDefault(emptyMap())
    }
}
