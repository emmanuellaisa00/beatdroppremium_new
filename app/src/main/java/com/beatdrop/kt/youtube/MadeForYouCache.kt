package com.beatdrop.kt.youtube

import android.content.Context
import com.beatdrop.kt.DebugLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Disk-backed cache for the Discover screen's 'Made for You' carousel.
 *
 * Why this exists: Discover fires 10 parallel playlist fetches on every
 * cold open. Even with parallelism it waits for the slowest one — often
 * 3-6 s on cellular. The user reported this as 'Discover is taking
 * forever to fetch'.
 *
 * Solution: cache the resolved PlaylistPreview list on disk with a
 * 12-hour TTL. On cold launch:
 *   • Cached entries are served INSTANTLY (Discover renders fully
 *     populated within a single frame).
 *   • A background refresh kicks in only if cache is stale or empty,
 *     so the user sees content immediately and the refresh lands
 *     invisibly when it's done.
 *
 * TTL is 12h (vs PlaylistCache's 24h) because Discover *is* supposed
 * to feel curated/fresh — a once-daily refresh keeps tile covers
 * tracking with new releases.
 */
object MadeForYouCache {

    private const val TTL_MS = 12L * 60 * 60 * 1000   // 12 hours

    private data class CachedSnapshot(
        val fetchedAt: Long,
        val previews: List<MadeForYou.PlaylistPreview>,
    )

    private val gson = Gson()
    @Volatile private var diskFile: File? = null
    @Volatile private var memSnap: CachedSnapshot? = null

    fun init(context: Context) {
        diskFile = File(context.filesDir, "made_for_you.json")
        loadFromDisk()
    }

    /** Returns cached previews if fresh, else null. */
    fun getFresh(): List<MadeForYou.PlaylistPreview>? {
        val s = memSnap ?: return null
        if (System.currentTimeMillis() - s.fetchedAt > TTL_MS) return null
        return s.previews
    }

    /**
     * Returns ANY cached previews regardless of freshness — used for
     * 'show stale instantly, refresh in background' UX where seeing
     * yesterday's tiles is better than seeing a loading spinner.
     */
    fun getAny(): List<MadeForYou.PlaylistPreview>? = memSnap?.previews

    fun put(previews: List<MadeForYou.PlaylistPreview>) {
        if (previews.isEmpty()) return    // never cache failures
        memSnap = CachedSnapshot(System.currentTimeMillis(), previews)
        runCatching { persistToDisk() }
    }

    fun isStale(): Boolean {
        val s = memSnap ?: return true
        return System.currentTimeMillis() - s.fetchedAt > TTL_MS
    }

    private fun loadFromDisk() {
        val file = diskFile ?: return
        if (!file.exists()) return
        try {
            val json = file.readText()
            val type = object : TypeToken<CachedSnapshot>() {}.type
            memSnap = gson.fromJson(json, type)
            DebugLog.i("madeForYouCache",
                "loaded ${memSnap?.previews?.size ?: 0} previews from disk")
        } catch (e: Exception) {
            DebugLog.w("madeForYouCache", "disk load failed: ${e.message}")
        }
    }

    private fun persistToDisk() {
        val file = diskFile ?: return
        try {
            file.writeText(gson.toJson(memSnap))
        } catch (_: Exception) { }
    }
}
