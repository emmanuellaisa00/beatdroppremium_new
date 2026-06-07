package com.beatdrop.kt.navigation

sealed class Screen(val route: String) {

    // ── Entry flow ──
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")

    // ── Tab screens ──
    data object Discover : Screen("discover")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Radio : Screen("radio")

    // ── Search ──
    data object SearchResults : Screen("search_results/{query}") {
        fun createRoute(q: String) = "search_results/${q.replace("/", "_")}"
    }

    // ── Detail: Album ──
    data object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    // ── Detail: Artist ──
    data object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(name: String) = "artist/${name.replace("/", "_")}"
    }

    // ── Detail: Playlist ──
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(id: String) = "playlist/$id"
    }

    // ── Detail: Online collection ──
    data object OnlineCollection : Screen("collection/{collectionId}") {
        fun createRoute(id: String) = "collection/$id"
    }

    // ── Detail: Channel ──
    data object ChannelDetail : Screen("channel/{channelId}") {
        fun createRoute(id: String) = "channel/$id"
    }

    // ── Detail: Genre ──
    data object GenreDetail : Screen("genre/{genreName}") {
        fun createRoute(name: String) = "genre/${name.replace(" ", "_")}"
    }

    // ── Player ──
    data object NowPlaying : Screen("now_playing")
    data object Queue : Screen("queue")
    data object Lyrics : Screen("lyrics")

    // ── Library sub ──
    data object Downloads : Screen("downloads")
    data object Playlists : Screen("playlists")
    data object Stats : Screen("stats")
    data object Trending : Screen("trending")

    // ── Settings ──
    data object Settings : Screen("settings")
    data object Eq : Screen("eq")
    data object Storage : Screen("storage")
    data object PrivateFolder : Screen("private_folder")
    data object DebugLog : Screen("debug_log")

    // ── Utility ──
    data object ClipUrl : Screen("clip_url")
    data object PlaylistDownload : Screen("playlist_download/{playlistId}") {
        fun createRoute(id: String) = "playlist_download/$id"
    }
    data object VideoPlayer : Screen("video_player/{videoId}") {
        fun createRoute(id: String) = "video_player/$id"
    }
}
