package com.beatdrop.kt.youtube

/**
 * Online search result — mirrors YoutubeSearchResult from YoutubeService.ts.
 * Added durationSecs + isLive compared to the original shell.
 */
data class OnlineResult(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationText: String,
    val durationSecs: Int = 0,
    val isLive: Boolean = false,
    val sourcePlatform: String = "YouTube",
    val sourceUrl: String? = null,             // Original platform URL
)

/** Pluggable search backend — the UI only talks to this interface. */
interface SearchProvider {
    suspend fun search(query: String): List<OnlineResult>
    suspend fun suggestions(query: String): List<String> = emptyList()
}

object NotConfiguredProvider : SearchProvider {
    override suspend fun search(query: String): List<OnlineResult> = emptyList()
}

/**
 * Production search backend — picks between two backends based on the
 * `OnlineSearch.musicMode` switch:
 *
 *   musicMode = true  → YouTube Music's `music.youtube.com/youtubei/v1/search`
 *                       (WEB_REMIX client). Returns curated SONG results
 *                       only — no reaction videos, no lyric channels, no
 *                       fan covers. This is what users want for a music app.
 *
 *   musicMode = false → Generic youtube.com/youtubei/v1/search (MWEB).
 *                       Returns any video. Useful for podcasts, interviews,
 *                       live sets, mixes.
 *
 * Suggestions always come from the public YouTube suggest endpoint —
 * works identically for both modes.
 */
class InnertubeSearchProvider : SearchProvider {
    override suspend fun search(query: String): List<OnlineResult> =
        if (OnlineSearch.musicMode) searchYoutubeMusic(query) else searchYoutube(query)

    override suspend fun suggestions(query: String): List<String> =
        getSearchSuggestions(query)
}

/** Injection point. Set in BeatDropApp.onCreate(). */
object OnlineSearch {
    @Volatile var provider: SearchProvider = NotConfiguredProvider
    /** When true, search uses YouTube Music (curated songs only). */
    @Volatile var musicMode: Boolean = true
    /** Active search platform: "YouTube" */
    @Volatile var searchPlatform: String = "YouTube"
    val isConfigured: Boolean get() = provider !== NotConfiguredProvider
}

/**
 * Search filters — duration, date, quality, type.
 */
data class SearchFilter(
    val duration: DurationFilter = DurationFilter.ANY,
    val type: TypeFilter = TypeFilter.ANY,
    val sortBy: SortFilter = SortFilter.RELEVANCE,
)

enum class DurationFilter(val label: String) {
    ANY("Any duration"),
    SHORT("Under 4 min"),
    MEDIUM("4–20 min"),
    LONG("Over 20 min"),
}

enum class TypeFilter(val label: String) {
    ANY("Any type"),
    SONG("Songs"),
    VIDEO("Videos"),
    LIVE("Live"),
}

enum class SortFilter(val label: String) {
    RELEVANCE("Relevance"),
    DATE("Upload date"),
    VIEWS("View count"),
}

/** Filter a list of OnlineResult by the given SearchFilter. */
fun applySearchFilter(results: List<OnlineResult>, filter: SearchFilter): List<OnlineResult> {
    var filtered = results
    filtered = when (filter.duration) {
        DurationFilter.SHORT -> filtered.filter { it.durationSecs in 1..240 }
        DurationFilter.MEDIUM -> filtered.filter { it.durationSecs in 240..1200 }
        DurationFilter.LONG -> filtered.filter { it.durationSecs > 1200 }
        else -> filtered
    }
    filtered = when (filter.type) {
        TypeFilter.LIVE -> filtered.filter { it.isLive }
        TypeFilter.SONG -> filtered.filter { !it.isLive && it.durationSecs in 60..600 }
        else -> filtered
    }
    return filtered
}

/**
 * Stream-quality preference. Read by `YoutubeService.resolveBestAudio()` and
 * `resolveBestMuxed()` to cap bitrate. Persists in DataStore via Prefs.
 *
 *   "auto"   = pick the highest-bitrate audio available (default)
 *   "high"   = cap at ~256 kbps (typical for music)
 *   "medium" = cap at ~128 kbps (data saver)
 *   "low"    = cap at ~64 kbps (emergency / very tight data)
 */
object QualityPreference {
    @Volatile var preferred: String = "auto"

    /** Returns max bitrate in bps for the current preference; 0 = unlimited. */
    fun maxBitrate(): Long = when (preferred) {
        "high"   -> 256_000L
        "medium" -> 128_000L
        "low"    -> 64_000L
        else     -> 0L            // "auto" — no cap
    }
}
