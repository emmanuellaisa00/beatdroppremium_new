package com.beatdrop.kt.data

import android.net.Uri

/**
 * Port of the RN Track type.
 * artworkOverride: used for YouTube tracks where albumId has no MediaStore entry.
 */
data class Track(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val data: String?,          // file path — used for sibling .lrc lookup
    val dateAdded: Long,
    val artworkOverride: String? = null,  // YouTube thumbnail or downloaded art path
    // For streamed (yt_) tracks: the exact UA/headers the googlevideo CDN URL was
    // resolved with. ExoPlayer must replay these or the CDN returns 403.
    val streamUserAgent: String? = null,
    val streamHeaders: Map<String, String> = emptyMap(),
    // The source videoId (for 403 re-resolution). Empty for local files.
    val sourceVideoId: String? = null,
) {
    val artworkUri: Uri
        get() = if (!artworkOverride.isNullOrBlank()) Uri.parse(artworkOverride)
                else Uri.parse("content://media/external/audio/albumart/$albumId")

    val isYoutube: Boolean get() = id.startsWith("yt_") || id.startsWith("dl_")
    val isDownloaded: Boolean get() = id.startsWith("dl_")
    val isStreaming: Boolean get() = id.startsWith("yt_")
}

data class AlbumGroup(val album: String, val artist: String, val artworkUri: Uri, val tracks: List<Track>)
data class ArtistGroup(val artist: String, val trackCount: Int, val tracks: List<Track>)

enum class SortMode(val label: String) {
    TITLE_ASC("Title A–Z"),
    TITLE_DESC("Title Z–A"),
    ARTIST("Artist"),
    RECENT("Recently added"),
}
