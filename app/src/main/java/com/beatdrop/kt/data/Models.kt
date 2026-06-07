package com.beatdrop.kt.data

// ───── Domain models matching the HTML prototype ─────

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String = "0:00",
    val coverIndex: Int = 1,
    val isPlaying: Boolean = false,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverIndex: Int,
    val year: Int = 2024,
    val songCount: Int = 0,
    val duration: String = "",
    val tracks: List<Track> = emptyList(),
)

data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val coverIndex: Int = 1,
)

data class Genre(
    val title: String,
    val tileIndex: Int,
)

// ───── Sample data matching the HTML ─────

object SampleData {

    val homeQuickAccess = listOf(
        Playlist("liked", "Liked Songs", "", 1),
        Playlist("daily1", "Daily Mix 1", "", 4),
        Playlist("discover", "Discover Weekly", "", 2),
        Playlist("release", "Release Radar", "", 5),
    )

    val recentlyPlayed = listOf(
        Album("hardstone", "Hardstone Psycho", "Don Toliver", 1, 2024, 18, "56 min"),
        Album("utopia", "UTOPIA", "Travis Scott", 2, 2023, 19, "52 min"),
        Album("gnx", "GNX", "Kendrick Lamar", 3, 2024, 12, "40 min"),
        Album("carter3", "Tha Carter III", "Lil Wayne", 4, 2008, 16, "72 min"),
        Album("music", "MUSIC", "Playboi Carti", 5, 2025, 24, "65 min"),
    )

    val madeForYou = listOf(
        Playlist("discover", "Discover Weekly", "Your weekly mixtape", 2),
        Playlist("release", "Release Radar", "Catch the latest", 5),
        Playlist("daily1", "Daily Mix 1", "Travis Scott, Don T…", 4),
        Playlist("repeat", "On Repeat", "Songs you can't stop", 6),
    )

    val topPodcasts = listOf(
        Playlist("daily-pod", "The Daily", "The New York Times", 7),
        Playlist("lex", "Lex Fridman", "Long-form interviews", 3),
        Playlist("huberman", "Huberman Lab", "Science of everyday life", 8),
    )

    val libraryQuickAccess = listOf(
        Playlist("liked", "Liked Songs", "", 1),
        Playlist("downloads", "Downloads", "", 5),
        Playlist("hardstone", "Hardstone Psycho", "", 1),
        Playlist("utopia", "UTOPIA", "", 2),
        Playlist("discover", "Discover Weekly", "", 2),
        Playlist("daily1", "Daily Mix 1", "", 4),
    )

    val libraryTracks = listOf(
        Track("1", "4×4", "Don Toliver", "Hardstone Psycho", "3:22", 1, true),
        Track("2", "HYAENA", "Travis Scott", "UTOPIA", "4:08", 2),
        Track("3", "THANK GOD", "Travis Scott", "UTOPIA", "3:36", 3),
        Track("4", "MODERN JAM", "Travis Scott, Teezo Touchdown", "", "3:11", 4),
        Track("5", "If Looks Could Kill", "Destroy Lonely", "", "2:54", 5),
        Track("6", "PHILLY", "Playboi Carti", "", "3:04", 1),
    )

    val albumTracks = listOf(
        Track("1", "4×4", "Don Toliver", "Hardstone Psycho", "3:22", 1),
        Track("2", "Inside", "Don Toliver", "Hardstone Psycho", "2:48", 1),
        Track("3", "Glock In My Purse", "Don Toliver", "Hardstone Psycho", "3:01", 1),
        Track("4", "Bus Stop", "Don Toliver, Charlie Wilson", "Hardstone Psycho", "3:15", 1),
        Track("5", "Hardstone Psycho", "Don Toliver", "Hardstone Psycho", "2:34", 1),
        Track("6", "Ice Age", "Don Toliver", "Hardstone Psycho", "3:42", 1),
        Track("7", "Brother Stone", "Don Toliver", "Hardstone Psycho", "3:55", 1),
    )

    val genres = listOf(
        Genre("Pop", 1),
        Genre("Hip-Hop", 4),
        Genre("Chill", 3),
        Genre("Indie", 5),
        Genre("Electronic", 2),
        Genre("R&B", 6),
        Genre("Jazz", 7),
        Genre("Workout", 8),
    )

    val browseAll = listOf(
        Genre("New Releases", 2),
        Genre("Charts", 1),
        Genre("Mood", 6),
        Genre("Decades", 3),
    )

    val addOptions = listOf(
        Triple("Create a Playlist", "Start fresh with an empty playlist", "playlist"),
        Triple("Blend with a friend", "Make a shared playlist that updates daily", "blend"),
        Triple("Scan a Code", "Use your camera to import a track", "scan"),
        Triple("Paste a Link", "BeatDrop catalogue links", "link"),
        Triple("Import from Device", "Add music files from your phone", "import"),
    )

    val lyricsLines = listOf(
        "Headlights catching the rain",
        "Slow drive, no plans tonight",
        "Radio low, just the hum and the wind",
        "Lights low, the world feels far away",
        "Hold the night, the silence carries on",
        "Every step a beat, every breath a song",
        "Out where the city ends and the sky begins",
        "We turn it up, we let it ride",
        "Four by four, the road keeps rolling on",
        null,  // gap indicator
        "Sunset glass, the dashboard's glow",
        "Nothing here can touch us now",
        "Tell me when you've had enough",
        "Tell me when the night is gone",
        "Four by four, we keep rolling on",
    )

    val defaultAlbum = Album(
        "hardstone", "Hardstone Psycho", "Don Toliver", 1,
        2024, 18, "56 min", albumTracks
    )

    val defaultTrack = Track("1", "4×4", "Don Toliver", "Hardstone Psycho", "3:22", 1, true)
}
