package com.beatdrop.kt.lyrics

import android.content.Context
import com.beatdrop.kt.data.Track

/**
 * Unified lyrics fetcher with a three-tier fallback chain.
 *
 *   1. Sidecar .lrc next to audio file
 *   2. App cache .lrc (written after any previous online fetch)
 *   3. LrcLib.net (synced + plain timed)
 *
 * Any result from tier 3 is automatically written to the app cache so the
 * next play is instant and offline-safe.
 *
 * If all tiers miss, returns an empty list — the UI then shows its
 * 'No synced lyrics available' state. Previous versions had a tier-4
 * placeholder that synthesised fake timed lyrics from the title + artist
 * words ('Song Title — Artist Name' scrolling across the screen), which
 * users were correctly reading as 'the app thinks the song's title IS
 * its lyrics' — confusing and wrong. Removed.
 */
object LyricsEngine {

    fun fetch(ctx: Context, track: Track): List<LyricLine> {
        // Tier 1 — user-provided sidecar .lrc
        LrcParser.findAndParse(track).takeIf { it.isNotEmpty() }?.let { return it }

        // Tier 2 — previously cached online fetch
        LyricsCache.read(ctx, track)?.let { return it }

        // Tier 3 — LrcLib.net (free, no key)
        val online = LrcLibProvider.fetch(track)
        if (online.isNotEmpty()) {
            LyricsCache.write(ctx, track, online)
            return online
        }

        // No real lyrics anywhere — let the UI show its empty state.
        return emptyList()
    }
}
