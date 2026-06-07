package com.beatdrop.kt.youtube

/**
 * Search-result row representing a YouTube Music *album* (or single).
 *
 * Albums on YT Music are backed by a "browseId" of the form `MPRE…` plus a
 * sibling "audioPlaylistId" of the form `OLAK5uy_…`. The audioPlaylistId is
 * what we need to fetch the album's track list — it's a regular playlist as
 * far as our existing `YouTubePlaylist.fetchPlaylist` is concerned.
 *
 * @property browseId         YT Music browseId (MPREb_… etc.) – stable per album.
 * @property audioPlaylistId  Playlist ID (`OLAK5uy_…`) used to fetch tracks.
 * @property title            Album title.
 * @property artist           Primary artist (display string).
 * @property year             Release year as string ("2024") or blank.
 * @property thumbnailUrl     HD square cover, already through upgradeThumbnailUrl().
 */
data class OnlineAlbum(
    val browseId: String,
    val audioPlaylistId: String,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnailUrl: String?,
)

/**
 * Search-result row representing a YouTube Music *playlist*.
 *
 * @property playlistId    `PL…` or `RDCLAK5uy_…` ID to fetch via YouTubePlaylist.
 * @property title         Playlist title.
 * @property author        Curator or owner ("YouTube Music", a channel name, …).
 * @property trackCount    Approximate track count if YT supplies it; -1 if unknown.
 * @property thumbnailUrl  HD cover image.
 */
data class OnlinePlaylist(
    val playlistId: String,
    val title: String,
    val author: String,
    val trackCount: Int,
    val thumbnailUrl: String?,
)
