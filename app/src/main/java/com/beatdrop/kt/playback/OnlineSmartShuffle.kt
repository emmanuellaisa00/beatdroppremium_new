package com.beatdrop.kt.playback

import com.beatdrop.kt.youtube.OnlineResult

/**
 * OnlineSmartShuffle — content-based scorer for the online (streaming) pool.
 *
 * Philosophy: pick what *fits this moment* — not what the user "liked" months
 * ago. Smart shuffle is rebuilt around three signals derived from the currently
 * playing track itself:
 *
 *   1. Artist continuity        — same author, or someone the current artist
 *                                 has collaborated with in this session.
 *   2. Genre / mood vibe        — genre tokens parsed from the title
 *                                 ("lofi", "drill", "afrobeats", "phonk", …).
 *   3. Sonic shape              — duration & era similarity to the current track.
 *
 * The `likedVideoIds` parameter is preserved in the signature for binary
 * compatibility with existing call-sites but is **deliberately ignored** —
 * smart shuffle no longer biases on the user's Liked list (that list still
 * powers the Liked Songs playlist, just not the queue).
 *
 * Scoring weights:
 *   +25   same artist as current
 *   +20   collab edge — candidate.author appears in current.title OR
 *         current.author appears in candidate.title OR both are linked via
 *         the in-session collab graph (CollabGraph.observe)
 *   +4    per matching genre/mood token (capped at +16)
 *   +2    per matching content-word in titles (capped at +10)
 *   ×8    duration similarity in [0,1]   ( 1 − |Δsec|/180 )
 *   +6    era similarity if both titles carry a 4-digit year
 *   −30   live stream
 *   −∞    in the recently-played LRU ring
 *
 * Top score wins; ties broken by collab-graph degree (more-connected first).
 */
object OnlineSmartShuffle {

    // ── Genre / mood vocabulary ──────────────────────────────────────────────
    // Single-word tokens that strongly suggest a genre or mood. Compared after
    // lowercasing and stripping punctuation from the title.
    private val genreTokens = setOf(
        // hip-hop family
        "rap", "hiphop", "hip-hop", "trap", "drill", "boom-bap", "grime", "phonk",
        // african / latin / caribbean
        "afrobeats", "afrobeat", "amapiano", "afropop", "highlife", "dancehall",
        "reggae", "reggaeton", "latin", "bachata", "salsa", "kompa", "kwaito",
        // electronic
        "edm", "house", "techno", "trance", "dnb", "dubstep", "garage",
        "future-bass", "synthwave", "vaporwave", "ambient", "downtempo",
        // pop / rock / etc.
        "pop", "k-pop", "kpop", "j-pop", "jpop", "indie", "rock", "punk",
        "metal", "emo", "alt", "alternative", "shoegaze",
        // chill / instrumental
        "lofi", "lo-fi", "chill", "chillhop", "study", "sleep", "ambient",
        "instrumental", "piano", "acoustic", "classical", "jazz", "blues",
        "soul", "funk", "rnb", "r&b",
        // mood
        "sad", "happy", "love", "heartbreak", "party", "workout", "gym",
        "focus", "morning", "night", "vibes", "vibe",
        // gospel / spiritual
        "gospel", "worship", "praise", "hymn",
        // variant tags
        "remix", "cover", "edit", "extended", "slowed", "reverb", "nightcore",
        "sped-up", "speed-up", "mashup", "bootleg", "vip", "rework",
        "live", "session", "unplugged", "demo",
    )

    // Words to ignore when scoring title-token overlap (otherwise every track
    // matches on "the", "feat", "official", …).
    private val stopWords = setOf(
        "the", "a", "an", "of", "to", "in", "on", "at", "for", "and", "or",
        "but", "is", "it", "with", "from", "by", "as", "this", "that",
        "feat", "ft", "featuring", "prod", "produced", "official",
        "video", "audio", "lyric", "lyrics", "hd", "hq", "mv", "mp3",
        "free", "download", "full", "version", "new", "song", "music",
        "&", "x", "vs", "vs.",
    )

    // Collab markers — substrings that, when followed by an artist token,
    // mark a collaboration relationship.
    private val collabMarkers = listOf(
        " feat. ", " feat ", " ft. ", " ft ", " featuring ",
        " with ", " w/ ",
        " x ", " × ", " & ", " and ",
        " prod. by ", " prod ", " produced by ",
    )

    // ── In-session collab graph ──────────────────────────────────────────────
    /**
     * Self-building adjacency map of artists who have appeared together (in
     * a title's "feat./ft./x/&" markers) during this session. Cheap, in-memory,
     * resets on process death. Reflects the actual flow the user is in.
     */
    object CollabGraph {
        private val edges: MutableMap<String, MutableSet<String>> = mutableMapOf()

        /** Call once per track that starts playing. Parses title for collabs. */
        fun observe(title: String, primaryArtist: String) {
            val primary = primaryArtist.normalizeArtist()
            if (primary.isBlank()) return
            for (other in extractCollabArtists(title)) {
                val o = other.normalizeArtist()
                if (o.isBlank() || o == primary) continue
                edges.getOrPut(primary) { mutableSetOf() }.add(o)
                edges.getOrPut(o) { mutableSetOf() }.add(primary)
            }
        }

        fun areLinked(a: String, b: String): Boolean {
            val an = a.normalizeArtist(); val bn = b.normalizeArtist()
            if (an.isBlank() || bn.isBlank()) return false
            return edges[an]?.contains(bn) == true
        }

        fun degree(artist: String): Int =
            edges[artist.normalizeArtist()]?.size ?: 0

        fun clear() = edges.clear()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Pick the single best next track from [pool] given [current].
     *
     * [likedVideoIds] is accepted for source-compatibility but ignored. Smart
     * shuffle no longer biases on the Liked list.
     */
    fun pickNext(
        current: OnlineResult,
        pool: List<OnlineResult>,
        recentlyPlayedIds: Set<String>,
        @Suppress("UNUSED_PARAMETER") likedVideoIds: Set<String> = emptySet(),
    ): OnlineResult? {
        if (pool.isEmpty()) return null

        var best: OnlineResult? = null
        var bestScore = Float.NEGATIVE_INFINITY
        var bestDegree = -1
        for (c in pool) {
            if (c.videoId == current.videoId) continue
            if (c.videoId in recentlyPlayedIds) continue
            val s = score(current, c, recentlyPlayedIds)
            if (s > bestScore) {
                bestScore = s; best = c
                bestDegree = CollabGraph.degree(c.author)
            } else if (s == bestScore) {
                // Tie-break: prefer the more collab-connected artist.
                val d = CollabGraph.degree(c.author)
                if (d > bestDegree) { best = c; bestDegree = d }
            }
        }
        return best
            ?: pool.firstOrNull { it.videoId != current.videoId && it.videoId !in recentlyPlayedIds }
    }

    /**
     * Score a single [candidate] against [current]. Pure function.
     *
     * [likedVideoIds] is accepted for source-compatibility but ignored.
     */
    fun score(
        current: OnlineResult,
        candidate: OnlineResult,
        recentlyPlayedIds: Set<String>,
        @Suppress("UNUSED_PARAMETER") likedVideoIds: Set<String> = emptySet(),
    ): Float {
        if (candidate.videoId == current.videoId) return Float.NEGATIVE_INFINITY
        if (candidate.videoId in recentlyPlayedIds) return Float.NEGATIVE_INFINITY

        var s = 0f

        // ── 1. Artist continuity ────────────────────────────────────────
        val curArtist = current.author.normalizeArtist()
        val candArtist = candidate.author.normalizeArtist()
        if (curArtist.isNotEmpty() && curArtist == candArtist) {
            s += 25f
        }

        // ── 2. Collab edge — three ways to qualify ──────────────────────
        val curTitleLower = current.title.lowercase()
        val candTitleLower = candidate.title.lowercase()
        val collab =
            (candArtist.isNotEmpty() && candArtist in curTitleLower) ||
            (curArtist.isNotEmpty() && curArtist in candTitleLower) ||
            CollabGraph.areLinked(curArtist, candArtist)
        if (collab && curArtist != candArtist) s += 20f

        // ── 3. Genre / mood token match ─────────────────────────────────
        val curGenres = extractGenreTokens(current.title)
        val candGenres = extractGenreTokens(candidate.title)
        val genreOverlap = curGenres.intersect(candGenres).size
        s += (genreOverlap * 4f).coerceAtMost(16f)

        // ── 4. Title content-word overlap (non-stopwords, non-genre) ────
        val curContent = contentTokens(current.title) - curGenres
        val candContent = contentTokens(candidate.title) - candGenres
        val contentOverlap = curContent.intersect(candContent).size
        s += (contentOverlap * 2f).coerceAtMost(10f)

        // ── 5. Duration similarity (sonic shape) ────────────────────────
        if (current.durationSecs > 0 && candidate.durationSecs > 0) {
            val delta = kotlin.math.abs(candidate.durationSecs - current.durationSecs)
            val sim = (1f - delta / 180f).coerceIn(0f, 1f)
            s += sim * 8f
        }

        // ── 6. Era similarity ───────────────────────────────────────────
        val curYear = extractYear(current.title)
        val candYear = extractYear(candidate.title)
        if (curYear != null && candYear != null) {
            val ydelta = kotlin.math.abs(candYear - curYear)
            when {
                ydelta == 0 -> s += 6f
                ydelta <= 2 -> s += 4f
                ydelta <= 5 -> s += 2f
            }
        }

        // ── 7. Hard quality penalty ─────────────────────────────────────
        if (candidate.isLive) s -= 30f

        return s
    }

    /**
     * Build a full smart-shuffled queue from [pool] using iterative greedy
     * selection. Each pick is scored against the previously-picked track, so
     * the queue forms a vibe-following chain rather than a static ranking.
     *
     * The track at [startIndex] (or the first non-live track if null) anchors
     * position 0. [likedVideoIds] is ignored.
     */
    fun buildSmartQueue(
        pool: List<OnlineResult>,
        @Suppress("UNUSED_PARAMETER") likedVideoIds: Set<String> = emptySet(),
        startIndex: Int? = null,
    ): List<OnlineResult> {
        if (pool.size <= 1) return pool.toList()

        val remaining = pool.toMutableList()
        val result = mutableListOf<OnlineResult>()
        val recent = LinkedHashSet<String>()

        // ── Pick the starter ─────────────────────────────────────────────
        val first = if (startIndex != null && startIndex in pool.indices) {
            remaining.removeAt(startIndex)
        } else {
            // Default starter: first non-live track, else first.
            val starter = remaining.firstOrNull { !it.isLive } ?: remaining.first()
            remaining.remove(starter)
            starter
        }
        result.add(first); recent.add(first.videoId)

        // ── Greedy vibe-chain ────────────────────────────────────────────
        var cur = first
        while (remaining.isNotEmpty()) {
            val next = pickNext(cur, remaining, recent) ?: remaining.first()
            remaining.remove(next)
            result.add(next); recent.add(next.videoId)

            // Keep the recent ring ≤ 6 so the chain can revisit themes later.
            if (recent.size > 6) {
                val oldest = recent.iterator().next()
                recent.remove(oldest)
            }
            cur = next
        }
        return result
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun String.normalizeArtist(): String =
        trim().lowercase()
            // Strip "Topic" suffix from auto-generated YouTube Music channels.
            .removeSuffix(" - topic").removeSuffix(" topic")
            // Strip VEVO/Official trailing tags.
            .removeSuffix("vevo").removeSuffix(" official").removeSuffix(" official channel")
            .trim()

    /** Extract genre/mood tokens present in a title. */
    private fun extractGenreTokens(title: String): Set<String> {
        val lower = title.lowercase()
        val out = mutableSetOf<String>()
        for (g in genreTokens) {
            if (g.contains('-') || g.contains('&')) {
                if (lower.contains(g)) out.add(g)
            } else {
                // Word-boundary check so "rap" doesn't match "wrap" etc.
                val r = Regex("\\b" + Regex.escape(g) + "\\b")
                if (r.containsMatchIn(lower)) out.add(g)
            }
        }
        return out
    }

    /** Lowercased content tokens from a title (≥3 chars, not stopwords). */
    private fun contentTokens(title: String): Set<String> {
        return title.lowercase()
            .split(Regex("[\\s\\-_/(),\\[\\]|.:;\"'!?]+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }

    /**
     * Pull out artist names that appear after a collab marker. Stops at the
     * first " - " (the title/song-name separator) so we don't suck the song
     * name into the artist list.
     */
    internal fun extractCollabArtists(title: String): List<String> {
        // Cut off the song-name part — typical pattern "Artist - Song".
        val artistPart = title.substringBefore(" - ", missingDelimiterValue = title)
        if (artistPart.isBlank()) return emptyList()
        val lower = artistPart.lowercase()
        val out = mutableListOf<String>()
        for (m in collabMarkers) {
            // Marker as written has leading/trailing spaces ("  ft. ", " x ").
            // Strip them when searching so a marker at the very start still hits.
            val needle = m.trim()
            if (needle.isEmpty()) continue
            // Word-boundary search: require either start-of-string or space before,
            // and a space after, so " x " doesn't fire inside "Maxwell".
            val pattern = Regex("(?:^|\\s)" + Regex.escape(needle) + "\\s+")
            for (match in pattern.findAll(lower)) {
                val tail = artistPart.substring(match.range.last + 1)
                val name = tail
                    .takeWhile { it !in setOf('(', '[', '|') }
                    .split(Regex("[,;]"))
                    .firstOrNull()
                    .orEmpty()
                    .trim()
                if (name.length in 2..40) out.add(name)
            }
        }
        return out
    }

    /** Find a 4-digit year (1950–2099) in a title, if any. */
    private fun extractYear(title: String): Int? {
        val m = Regex("\\b(19[5-9]\\d|20[0-9]\\d)\\b").find(title) ?: return null
        return m.value.toIntOrNull()
    }
}
