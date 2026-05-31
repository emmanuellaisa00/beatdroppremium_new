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

    // ── helpers ──
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
