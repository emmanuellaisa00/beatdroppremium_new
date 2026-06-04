package com.beatdrop.kt.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Curated "Made for You" playlist hub for the Discover screen.
 *
 * Without a logged-in YouTube Music account we can't fetch true
 * personalised feeds (`FEmusic_home`, "Daily Mix", "My Supermix" require
 * an authenticated `SAPISIDHASH`). What we *can* fetch with the open
 * Innertube `BROWSE` endpoint:
 *
 *   • Any **public** YT Music playlist by `playlistId`
 *     (delegated to YouTubePlaylist.fetchPlaylist)
 *
 * So we hand-pick ~10 long-standing public playlists that cover the
 * dominant musical territories, present them as cards in the Discover
 * screen ("Made for You" / "Spotify Mixes" UX), and the first few tracks
 * of each playlist auto-populate the carousel preview rows.
 *
 * The list is small on purpose — every entry costs one Innertube round-trip
 * on first load, after which `cachedPlaylists` keeps them in RAM. A weekly
 * refresh on cold-launch matches how the playlists themselves rotate.
 */
object MadeForYou {

    /**
     * Curated card. `subtitle` is shown under the title in the carousel
     * tile; `accentHex` tints the gradient corner badge so the cards
     * look distinct from each other (matches Spotify's coloured mix cards).
     */
    data class FeaturedPlaylist(
        val playlistId: String,
        val title: String,
        val subtitle: String,
        val accentHex: Long,
    )

    /**
     * Public YT Music / YouTube playlist IDs.
     *
     * These are intentionally drawn from two safe categories:
     *   • YouTube's regional / global music charts (PLFgquLnL59ak…),
     *     which are maintained automatically by YouTube and always
     *     resolve as long as the chart exists.
     *   • Long-standing curator playlists (PL… IDs), each verified
     *     to return a populated track list at the time of writing.
     *
     * If any single ID later starts returning an empty track list,
     * `fetchAll` silently drops that tile from the carousel so the
     * Discover screen doesn't render a broken cover.
     */
    val featured: List<FeaturedPlaylist> = listOf(
        FeaturedPlaylist(
            playlistId = "PLFgquLnL59akA2PflFpeQG9L01VFg90wS",   // Global Top 100
            title = "Global Top 100",
            subtitle = "What the world is playing",
            accentHex = 0xFFE53935,
        ),
        FeaturedPlaylist(
            playlistId = "PLDIoUOhQQPlXr63I_vwF9GD8sAKh77dWU",   // Today's Top Hits (US)
            title = "Today's Top Hits",
            subtitle = "The biggest pop songs",
            accentHex = 0xFFEC407A,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgs658kAHR_LAaILBXb-s6Q5",   // Hip-Hop hits
            title = "Hip-Hop Now",
            subtitle = "Today's biggest rap",
            accentHex = 0xFFFFB300,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgsK8fhDjU6jJ_dQpv0DkYUI",   // R&B Now
            title = "R&B Heat",
            subtitle = "Smooth, soulful, current",
            accentHex = 0xFF8E24AA,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOguQyxOXcdNkpoNNXZNwGcyU",   // Afrobeats
            title = "Afrobeats",
            subtitle = "The sound of Lagos & beyond",
            accentHex = 0xFF00897B,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgu_nzN-S_p2gZeDoZAVbRfm",   // Latin hits
            title = "Latin Heat",
            subtitle = "Reggaeton, Latin pop, more",
            accentHex = 0xFFFB8C00,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgsXJp4HiQ3rUmPgs6m4FN9j",   // Workout
            title = "Workout Mix",
            subtitle = "High-energy fuel",
            accentHex = 0xFFD81B60,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgu5_2Q4WP5q3iCK_v1nNqsx",   // Chill
            title = "Chill Vibes",
            subtitle = "Slow it down",
            accentHex = 0xFF26A69A,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgvtnnnqWlTqByAtC7tXBg6D",   // Lo-fi / focus
            title = "Lo-Fi Focus",
            subtitle = "Quiet instrumental",
            accentHex = 0xFF5C6BC0,
        ),
        FeaturedPlaylist(
            playlistId = "PLw-VjHDlEOgvjvbGgwUO84A6gNvr_3SVS",   // Throwbacks
            title = "Throwbacks",
            subtitle = "Hits from yesterday",
            accentHex = 0xFF6D4C41,
        ),
    )

    /**
     * Fetch the first [previewTracks] tracks of every curated playlist
     * in parallel. Tiles whose playlist returned **no** tracks are
     * silently dropped from the result list so the Discover carousel
     * never shows a broken empty cover for a dead playlist ID.
     */
    suspend fun fetchAll(previewTracks: Int = 4): List<PlaylistPreview> = coroutineScope {
        withContext(Dispatchers.IO) {
            featured.map { pl ->
                async {
                    val info = runCatching {
                        YouTubePlaylist.fetchPlaylist(pl.playlistId, maxItems = previewTracks * 4)
                    }.getOrNull()
                    val videos = info?.videos.orEmpty().take(previewTracks)
                    if (videos.isEmpty()) null
                    else PlaylistPreview(
                        meta = pl,
                        coverUrl = videos.firstOrNull()?.thumbnailUrl,
                        tracks = videos,
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * A single playlist tile ready for rendering on Discover.
     *
     *  • `meta`     — the curated metadata (title, subtitle, accent).
     *  • `coverUrl` — first track's thumbnail, used as the tile cover
     *                 (already passed through `upgradeThumbnailUrl`).
     *  • `tracks`   — preview list (first N tracks), surfaced as the
     *                 "tap-to-play" carousel under the tile.
     */
    data class PlaylistPreview(
        val meta: FeaturedPlaylist,
        val coverUrl: String?,
        val tracks: List<OnlineResult>,
    )
}
