package com.beatdrop.kt.youtube

/**
 * A "collection" of online tracks the user can open as a detail screen
 * with Play / Shuffle / Download buttons and a scrollable track list.
 *
 * Albums and playlists are structurally identical from the UI's point
 * of view (cover + title + author + a playlistId you can fetch tracks
 * from) so a single OnlineAlbumScreen can render both, parametrised
 * by this interface.
 *
 * Implemented as a sealed interface so the screen can choose slightly
 * different copy per kind (e.g. 'Album · 2024' vs 'Playlist · 56
 * tracks') without a free-form `kind: String` parameter.
 */
sealed interface PlayableCollection {
    val playlistId: String        // YouTube playlistId used to fetch tracks
    val title: String
    val author: String            // artist name (album) or curator (playlist)
    val coverUrl: String?         // HD square cover
    val secondaryLine: String     // 'Album · 2024' / 'Playlist · 56 tracks'
    val kindLabel: String         // 'Album' / 'Playlist' — used in nav-bar subtitle

    data class Album(val album: OnlineAlbum) : PlayableCollection {
        override val playlistId    get() = album.audioPlaylistId
        override val title         get() = album.title
        override val author        get() = album.artist
        override val coverUrl      get() = album.thumbnailUrl
        override val secondaryLine get() = buildString {
            append("Album")
            if (album.artist.isNotBlank()) append(" · ").append(album.artist)
            if (album.year.isNotBlank())   append(" · ").append(album.year)
        }
        override val kindLabel get() = "Album"
    }

    data class Playlist(val playlist: OnlinePlaylist) : PlayableCollection {
        override val playlistId    get() = playlist.playlistId
        override val title         get() = playlist.title
        override val author        get() = playlist.author
        override val coverUrl      get() = playlist.thumbnailUrl
        override val secondaryLine get() = buildString {
            append("Playlist")
            if (playlist.author.isNotBlank()) append(" · ").append(playlist.author)
            if (playlist.trackCount > 0)      append(" · ").append("${playlist.trackCount} tracks")
        }
        override val kindLabel get() = "Playlist"
    }

    /**
     * A curated featured playlist (Made-For-You / Browse Categories).
     * Carries the accent colour so the cover backdrop can tint to match
     * the original card the user tapped.
     */
    data class Featured(val featured: com.beatdrop.kt.youtube.MadeForYou.FeaturedPlaylist) : PlayableCollection {
        override val playlistId    get() = featured.playlistId
        override val title         get() = featured.title
        override val author        get() = featured.subtitle
        override val coverUrl: String? get() = null   // cover comes from first track after fetch
        override val secondaryLine get() = featured.subtitle
        override val kindLabel get() = "Playlist"
    }
}
