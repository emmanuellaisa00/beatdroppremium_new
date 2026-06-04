package com.beatdrop.kt.playback

import com.beatdrop.kt.youtube.OnlineResult

/**
 * OnlineSmartShuffle — scoring engine for online (streaming) tracks.
 *
 * Picks the best next track from a pool of OnlineResults using only signals
 * available without downloading/decoding the audio (no BPM/key analysis).
 *
 * Scoring weights:
 *   +40   same author/artist — strongest proxy for style continuity
 *   +15   duration within ±30% of current (similar "energy" length)
 *   +25   in liked/downloaded set
 *   +10   source platform affinity (from same search/playlist context)
 *   −∞    in recently-played ring (last 6 tracks) → never pick
 *   +5    title keyword overlap (remix, acoustic, live, etc.)
 *   −20   live stream (unpredictable quality, often not music)
 *   +5    ideal duration sweet-spot (90–420 s)
 *
 * Returns null if the pool is empty or only contains recently-played tracks.
 */
object OnlineSmartShuffle {

    // Keywords indicating a remix/variety variant — mild bonus for diversity
    private val remixKeywords = setOf(
        "remix", "acoustic", "cover", "edit", "extended",
        "instrumental", "lo-fi", "lofi", "slowed", "sped up", "nightcore",
        "radio edit", "clean", "explicit",
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Pick the single best next track from [pool] given [current].
     */
    fun pickNext(
        current: OnlineResult,
        pool: List<OnlineResult>,
        recentlyPlayedIds: Set<String>,
        likedVideoIds: Set<String> = emptySet(),
    ): OnlineResult? {
        if (pool.isEmpty()) return null

        val candidates = pool.asSequence()
            .filter { it.videoId != current.videoId }
            .filter { it.videoId !in recentlyPlayedIds }

        var best: OnlineResult? = null
        var bestScore = Float.NEGATIVE_INFINITY
        for (c in candidates) {
            val s = score(current, c, recentlyPlayedIds, likedVideoIds)
            if (s > bestScore) { bestScore = s; best = c }
        }
        return best
            ?: pool.firstOrNull { it.videoId != current.videoId && it.videoId !in recentlyPlayedIds }
    }

    /**
     * Score a single [candidate] against [current].  Pure function.
     */
    fun score(
        current: OnlineResult,
        candidate: OnlineResult,
        recentlyPlayedIds: Set<String>,
        likedVideoIds: Set<String>,
    ): Float {
        // Hard filter: never repeat the same video
        if (candidate.videoId == current.videoId) return Float.NEGATIVE_INFINITY
        if (candidate.videoId in recentlyPlayedIds) return Float.NEGATIVE_INFINITY

        var s = 0f

        // ── Artist / source affinity ──────────────────────────────────────
        if (candidate.author.equals(current.author, ignoreCase = true)) s += 40f
        if (candidate.sourcePlatform == current.sourcePlatform) s += 10f

        // ── Duration proximity (±30% sweet spot) ──────────────────────────
        if (current.durationSecs > 0 && candidate.durationSecs > 0) {
            val ratio = candidate.durationSecs.toDouble() /
                current.durationSecs.coerceAtLeast(1).toDouble()
            if (ratio in 0.7..1.3) s += 15f
            if (ratio in 0.9..1.1) s += 5f    // extra for very close durations
        }

        // ── Liked/downloaded bonus ────────────────────────────────────────
        if (candidate.videoId in likedVideoIds) s += 25f

        // ── Title keyword overlap — gentle topic continuity ───────────────
        val curToks = current.title.lowercase()
            .split(" ", "-", "(", ")", "[", "]", "feat.", "ft.", ",").toSet()
            .filter { it.length > 2 }
        val candToks = candidate.title.lowercase()
            .split(" ", "-", "(", ")", "[", "]", "feat.", "ft.", ",").toSet()
            .filter { it.length > 2 }
        val overlap = curToks.intersect(candToks)
        if (overlap.isNotEmpty()) {
            val overlapRatio = overlap.size.toFloat() /
                maxOf(curToks.size, candToks.size).coerceAtLeast(1)
            s += 10f * overlapRatio
        }

        // Mild bonus if candidate has a remix/variety keyword but current doesn't
        val curHasRemix = curToks.any { it in remixKeywords }
        val candHasRemix = candToks.any { it in remixKeywords }
        if (candHasRemix != curHasRemix) s += 3f

        // ── Quality heuristics ────────────────────────────────────────────
        if (candidate.isLive) s -= 20f

        // Duration sweet-spot: 1.5–7 min is ideal for music listening
        when {
            candidate.durationSecs in 90..420 -> s += 5f
            candidate.durationSecs > 600 -> s -= 10f       // >10 min = likely a mix/set
            candidate.durationSecs in 1..30 -> s -= 15f    // <30 s = likely not music
        }

        return s
    }

    /**
     * Build a full smart-shuffled queue from [pool] using iterative greedy
     * selection.
     *
     * Algorithm:
     *   1. Pick the best "starter" track (liked, good duration, non-live).
     *   2. For each remaining slot, score all remaining candidates against
     *      the last-picked track; take the highest-scoring one.
     *   3. Maintain the 6-track recently-played ring across the chain.
     *
     * Returns the pool in smart-shuffle order.  If [startIndex] is provided
     * the track at that index anchors the first position.
     */
    fun buildSmartQueue(
        pool: List<OnlineResult>,
        likedVideoIds: Set<String> = emptySet(),
        startIndex: Int? = null,
    ): List<OnlineResult> {
        if (pool.size <= 1) return pool.toList()

        val remaining = pool.toMutableList()
        val result = mutableListOf<OnlineResult>()
        val recent = mutableSetOf<String>()

        // ── Step 1: pick starter ──────────────────────────────────────────
        val first = if (startIndex != null && startIndex in pool.indices) {
            remaining.removeAt(startIndex)
        } else {
            val starter = remaining.maxByOrNull { r ->
                var s = 0f
                if (r.videoId in likedVideoIds) s += 30f
                if (r.durationSecs in 120..300) s += 15f
                if (r.isLive) s -= 100f
                s
            } ?: remaining.first()
            remaining.remove(starter)
            starter
        }
        result.add(first)
        recent.add(first.videoId)

        // ── Step 2: greedy chain ──────────────────────────────────────────
        var cur = first
        while (remaining.isNotEmpty()) {
            val picked = pickNext(cur, remaining, recent, likedVideoIds)
            val next = if (picked != null) {
                remaining.remove(picked)
                picked
            } else {
                remaining.removeFirst()
            }
            result.add(next)
            recent.add(next.videoId)

            // Keep the recent ring ≤ 6 entries
            if (recent.size > 6) {
                val oldest = result.getOrNull(result.size - 7)
                oldest?.let { recent.remove(it.videoId) }
            }
            cur = next
        }

        return result
    }
}
