package com.beatdrop.kt.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.beatdrop.kt.ui.screens.*

@Composable
fun BeatDropNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Splash.route, modifier = modifier) {

        // ── Entry flow ──
        composable(Screen.Splash.route) {
            SplashScreen(onDone = {
                navController.safeNavigate(Screen.Discover.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onGetStarted = {
                navController.safeNavigate(Screen.Discover.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        // ── Tab: Discover ──
        composable(Screen.Discover.route) {
            DiscoverScreen(
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                onOpenSearch = { navController.safeNavigate(Screen.Search.route) },
                onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
                onOpenTrending = { navController.safeNavigate(Screen.Trending.route) },
                onOpenOnlineCollection = { id -> navController.safeNavigate(Screen.OnlineCollection.createRoute(id.sanitize())) },
                onOpenPlaylist = { id -> navController.safeNavigate(Screen.PlaylistDetail.createRoute(id.sanitize())) },
            )
        }

        // ── Tab: Search ──
        composable(Screen.Search.route) {
            SearchScreen(
                onSearch = { q -> navController.safeNavigate(Screen.SearchResults.createRoute(q.sanitize())) },
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                onOpenPlaylist = { id -> navController.safeNavigate(Screen.PlaylistDetail.createRoute(id.sanitize())) },
                onOpenGenre = { name -> navController.safeNavigate(Screen.GenreDetail.createRoute(name.sanitize())) },
                onOpenOnlineCollection = { id -> navController.safeNavigate(Screen.OnlineCollection.createRoute(id.sanitize())) },
                onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
            )
        }

        // ── Tab: Library ──
        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
                onOpenSearch = { navController.safeNavigate(Screen.Search.route) },
                onOpenPlaylists = { navController.safeNavigate(Screen.Playlists.route) },
                onOpenDownloads = { navController.safeNavigate(Screen.Downloads.route) },
                onOpenStats = { navController.safeNavigate(Screen.Stats.route) },
                onOpenSettings = { navController.safeNavigate(Screen.Settings.route) },
            )
        }

        // ── Tab: Radio ──
        composable(Screen.Radio.route) {
            RadioScreen(onExpandPlayer = { navController.safeNavigate(Screen.NowPlaying.route) })
        }

        // ── Search results ──
        composable(Screen.SearchResults.route, listOf(navArgument("query") { type = NavType.StringType })) { entry ->
            val query = entry.safeString("query").replace("_", " ")
            SearchScreen(
                initialQuery = query,
                onSearch = { /* already on results */ },
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                onOpenPlaylist = { id -> navController.safeNavigate(Screen.PlaylistDetail.createRoute(id.sanitize())) },
                onOpenGenre = { name -> navController.safeNavigate(Screen.GenreDetail.createRoute(name.sanitize())) },
                onOpenOnlineCollection = { id -> navController.safeNavigate(Screen.OnlineCollection.createRoute(id.sanitize())) },
                onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
            )
        }

        // ── Detail: Album ──
        composable(Screen.AlbumDetail.route, listOf(navArgument("albumId") { type = NavType.StringType })) { entry ->
            val albumId = entry.safeString("albumId")
            if (albumId.isBlank()) {
                navController.safePop()
            } else {
                AlbumDetailScreen(
                    onBack = { navController.safePop() },
                    onPlay = { navController.safeNavigate(Screen.NowPlaying.route) },
                    onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                )
            }
        }

        // ── Detail: Artist ──
        composable(Screen.ArtistDetail.route, listOf(navArgument("artistName") { type = NavType.StringType })) { entry ->
            val name = entry.safeString("artistName").decodeRoute()
            if (name.isBlank()) {
                navController.safePop()
            } else {
                ArtistScreen(
                    artistName = name,
                    onBack = { navController.safePop() },
                    onPlay = { navController.safeNavigate(Screen.NowPlaying.route) },
                    onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                    onOpenArtist = { n -> navController.safeNavigate(Screen.ArtistDetail.createRoute(n.sanitize())) },
                )
            }
        }

        // ── Detail: Playlist ──
        composable(Screen.PlaylistDetail.route, listOf(navArgument("playlistId") { type = NavType.StringType })) { entry ->
            val name = entry.safeString("playlistId").decodeRoute()
            if (name.isBlank()) {
                navController.safePop()
            } else {
                PlaylistDetailScreen(
                    playlistName = name,
                    onBack = { navController.safePop() },
                    onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
                    onOpenArtist = { n -> navController.safeNavigate(Screen.ArtistDetail.createRoute(n.sanitize())) },
                    onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                )
            }
        }

        // ── Detail: Online Collection ──
        composable(Screen.OnlineCollection.route, listOf(navArgument("collectionId") { type = NavType.StringType })) { entry ->
            val id = entry.safeString("collectionId")
            if (id.isBlank()) {
                navController.safePop()
            } else {
                OnlineAlbumScreen(
                    collectionId = id,
                    onBack = { navController.safePop() },
                    onExpandPlayer = { navController.safeNavigate(Screen.NowPlaying.route) },
                    onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                )
            }
        }

        // ── Detail: Channel ──
        composable(Screen.ChannelDetail.route, listOf(navArgument("channelId") { type = NavType.StringType })) { entry ->
            val id = entry.safeString("channelId")
            if (id.isBlank()) {
                navController.safePop()
            } else {
                ChannelScreen(
                    channelId = id,
                    onBack = { navController.safePop() },
                    onExpandPlayer = { navController.safeNavigate(Screen.NowPlaying.route) },
                    onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                    onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                )
            }
        }

        // ── Detail: Genre ──
        composable(Screen.GenreDetail.route, listOf(navArgument("genreName") { type = NavType.StringType })) { entry ->
            val name = entry.safeString("genreName").decodeRoute()
            if (name.isBlank()) {
                navController.safePop()
            } else {
                GenreDetailScreen(
                    genreName = name,
                    onBack = { navController.safePop() },
                    onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                    onOpenArtist = { n -> navController.safeNavigate(Screen.ArtistDetail.createRoute(n.sanitize())) },
                    onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
                )
            }
        }

        // ── Player ──
        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                onBack = { navController.safePop() },
                onOpenLyrics = { navController.safeNavigate(Screen.Lyrics.route) },
                onOpenQueue = { navController.safeNavigate(Screen.Queue.route) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
            )
        }
        composable(Screen.Queue.route) {
            QueueScreen(onClose = { navController.safePop() })
        }
        composable(Screen.Lyrics.route) {
            LyricsScreen(onBack = { navController.safePop() })
        }

        // ── Library sub-screens ──
        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onBack = { navController.safePop() },
                onTrackClick = { navController.safeNavigate(Screen.NowPlaying.route) },
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
            )
        }
        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                onBack = { navController.safePop() },
                onOpen = { name -> navController.safeNavigate(Screen.PlaylistDetail.createRoute(name.sanitize())) },
            )
        }
        composable(Screen.Stats.route) { StatsScreen(onBack = { navController.safePop() }) }
        composable(Screen.Trending.route) {
            TrendingScreen(
                onBack = { navController.safePop() },
                onExpandPlayer = { navController.safeNavigate(Screen.NowPlaying.route) },
                onOpenAlbum = { id -> navController.safeNavigate(Screen.AlbumDetail.createRoute(id.sanitize())) },
                onOpenArtist = { name -> navController.safeNavigate(Screen.ArtistDetail.createRoute(name.sanitize())) },
            )
        }

        // ── Settings ──
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.safePop() },
                onOpenEq = { navController.safeNavigate(Screen.Eq.route) },
                onOpenStorage = { navController.safeNavigate(Screen.Storage.route) },
                onOpenPrivateFolder = { navController.safeNavigate(Screen.PrivateFolder.route) },
                onOpenDebugLog = { navController.safeNavigate(Screen.DebugLog.route) },
            )
        }
        composable(Screen.Eq.route) { EqScreen(onBack = { navController.safePop() }) }
        composable(Screen.Storage.route) { StorageScreen(onBack = { navController.safePop() }) }
        composable(Screen.PrivateFolder.route) { PrivateFolderScreen(onBack = { navController.safePop() }) }
        composable(Screen.DebugLog.route) { DebugLogScreen(onBack = { navController.safePop() }) }

        // ── Utility ──
        composable(Screen.ClipUrl.route) {
            ClipUrlScreen(onBack = { navController.safePop() }, onExpandPlayer = { navController.safeNavigate(Screen.NowPlaying.route) })
        }
        composable(Screen.VideoPlayer.route, listOf(navArgument("videoId") { type = NavType.StringType })) { entry ->
            val id = entry.safeString("videoId")
            if (id.isBlank()) {
                navController.safePop()
            } else {
                VideoPlayerScreen(videoPath = id, onBack = { navController.safePop() })
            }
        }
        composable(Screen.PlaylistDownload.route, listOf(navArgument("playlistId") { type = NavType.StringType })) { entry ->
            val id = entry.safeString("playlistId")
            if (id.isBlank()) {
                navController.safePop()
            } else {
                PlaylistDownloadScreen(playlistId = id, onBack = { navController.safePop() })
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// NAV-SAFE HELPERS
// ═══════════════════════════════════════════════════

/**
 * Navigate with crash protection — catches all exceptions
 * (double-navigate, graph not ready, route not found, etc.).
 */
fun NavHostController.safeNavigate(
    route: String,
    builder: androidx.navigation.NavOptionsBuilder.() -> Unit = {},
) {
    try {
        navigate(route, builder)
    } catch (_: Exception) {
        // Silently swallow navigation failures — user can tap again
    }
}

/**
 * Pop back stack with crash protection.
 */
fun NavHostController.safePop() {
    try {
        popBackStack()
    } catch (_: Exception) {
        // Can't pop — stay on current screen
    }
}

/**
 * Safely extract a string argument, returns "" if missing.
 */
private fun androidx.navigation.NavBackStackEntry.safeString(key: String): String {
    return try {
        arguments?.getString(key) ?: ""
    } catch (_: Exception) {
        ""
    }
}

/**
 * Sanitize a string for use in a route path.
 * Removes characters that could break URL routing.
 */
fun String.sanitize(): String {
    return replace("/", "_")
        .replace("#", "")
        .replace("?", "")
        .replace("&", "")
        .replace("=", "")
        .replace("{", "")
        .replace("}", "")
        .replace("|", "")
        .replace("\\", "_")
        .trim()
        .ifBlank { "unknown" }
}

/**
 * Decode a route path segment back to readable text.
 */
fun String.decodeRoute(): String {
    return replace("_", " ").trim().ifBlank { "" }
}
