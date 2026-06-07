package com.beatdrop.kt.extraction

import com.beatdrop.kt.youtube.OnlineResult
import com.beatdrop.kt.youtube.OnlineSearch
import com.beatdrop.kt.youtube.ResolvedStream
import com.beatdrop.kt.youtube.YouTubePlaylist
import com.beatdrop.kt.youtube.getStream

/**
 * Boundary for sideload-only extraction features.
 *
 * The UI / playback layers should depend on this contract, not directly on
 * Innertube/NewPipe/WebView/Piped implementation details. That lets BeatDrop
 * reorder, disable, or hotfix resolver strategies without contaminating core
 * local-library playback.
 */
interface ExtractionEngine {
    suspend fun search(query: String, maxResults: Int = 20): List<OnlineResult>
    suspend fun resolveAudio(videoId: String): ResolvedStream
    suspend fun resolveVideo(videoId: String): ResolvedStream
    suspend fun playlist(playlistId: String, maxItems: Int = 100): List<OnlineResult>
}

/** Current adapter over the existing extraction stack. Incrementally migrate
 * callers from direct YoutubeService/OnlineSearch access to this engine. */
object ExistingExtractionEngine : ExtractionEngine {
    override suspend fun search(query: String, maxResults: Int): List<OnlineResult> =
        OnlineSearch.provider.search(query).take(maxResults)

    override suspend fun resolveAudio(videoId: String): ResolvedStream = getStream(videoId)

    // Today video resolution reuses the same resolver; the V2 downloader can
    // later expose explicit muxed/video-only strategy through this method.
    override suspend fun resolveVideo(videoId: String): ResolvedStream = getStream(videoId)

    override suspend fun playlist(playlistId: String, maxItems: Int): List<OnlineResult> =
        YouTubePlaylist.fetchPlaylist(playlistId, maxItems = maxItems).videos
}
