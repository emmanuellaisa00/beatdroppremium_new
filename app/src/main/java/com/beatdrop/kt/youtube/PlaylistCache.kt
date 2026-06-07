package com.beatdrop.kt.youtube

import android.content.Context
import com.beatdrop.kt.DebugLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Disk-backed cache for fetched playlists & albums.
 *
 * The user kept hitting two pain points:
 *   1. Re-opening the same album/playlist re-fetched it every time
 *      (~1-3 s of staring at a loading spinner for content we already
 *      had).
 *   2. Tapping a track inside a playlist screen would `playFeaturedPlaylist`
 *      which RE-fetched the same playlist over the network just to call
 *      `prepareAndPlayOnline` with the result. That made the 'Next' button
 *      misbehave too — between the tap and the network coming back,
 *      `onlineContext` was either empty or stale, so Next walked the
 *      wrong list.
 *
 * Both go away with a simple shared cache keyed on `playlistId`. TTL is
 * generous (24 h) because YT playlists don't churn aggressively day to
 * day. Records survive process death via the on-disk JSON dump.
 *
 * Memory cap: the in-memory map is unbounded but each entry is
 * just ~100 OnlineResult records (~25 KB at most), so 100 cached
 * playlists ≈ 2.5 MB. Acceptable.
 *
 * Thread-safe via ConcurrentHashMap; persistence is best-effort.
 */
object PlaylistCache {

    /** A cached fetch — playlist info + when it was fetched. */
    data class Entry(
        val fetchedAt: Long,
        val info: PlaylistInfo,
    )

    private const val TTL_MS = 24L * 60 * 60 * 1000  // 24 hours

    private val gson = Gson()
    private val mem = ConcurrentHashMap<String, Entry>()
    @Volatile private var diskFile: File? = null

    fun init(context: Context) {
        diskFile = File(context.filesDir, "playlist_cache.json")
        loadFromDisk()
    }

    /**
     * Get a cached entry if it exists and is fresh, else null.
     * Callers should fall back to YouTubePlaylist.fetchPlaylist on null.
     */
    fun get(playlistId: String): PlaylistInfo? {
        val e = mem[playlistId] ?: return null
        if (System.currentTimeMillis() - e.fetchedAt > TTL_MS) {
            // Expired — drop and force a refetch.
            mem.remove(playlistId)
            return null
        }
        return e.info
    }

    /** Store a freshly-fetched playlist. */
    fun put(playlistId: String, info: PlaylistInfo) {
        if (info.videos.isEmpty()) return    // never cache failures
        mem[playlistId] = Entry(System.currentTimeMillis(), info)
        // Best-effort persist; don't block the caller.
        runCatching { persistToDisk() }
    }

    /** Drop a specific cache entry — used when the user explicitly refreshes. */
    fun invalidate(playlistId: String) {
        mem.remove(playlistId)
        runCatching { persistToDisk() }
    }

    fun clear() {
        mem.clear()
        runCatching { persistToDisk() }
    }

    private fun loadFromDisk() {
        val file = diskFile ?: return
        if (!file.exists()) return
        try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, Entry>>() {}.type
            val map: Map<String, Entry> = gson.fromJson(json, type) ?: return
            // Strip expired entries on load — no point keeping yesterday's data.
            val now = System.currentTimeMillis()
            mem.putAll(map.filter { now - it.value.fetchedAt <= TTL_MS })
            DebugLog.i("playlistCache", "loaded ${mem.size} entries from disk")
        } catch (e: Exception) {
            DebugLog.w("playlistCache", "disk load failed: ${e.message}")
        }
    }

    private fun persistToDisk() {
        val file = diskFile ?: return
        try {
            file.writeText(gson.toJson(mem.toMap()))
        } catch (_: Exception) { }
    }
}
