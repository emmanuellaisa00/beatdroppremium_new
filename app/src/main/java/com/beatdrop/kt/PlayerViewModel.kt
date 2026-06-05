package com.beatdrop.kt

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.exoplayer.ExoPlayer
import com.beatdrop.kt.data.*
import com.beatdrop.kt.lyrics.LrcParser
import com.beatdrop.kt.lyrics.LyricLine
import com.beatdrop.kt.lyrics.LyricsEngine
import com.beatdrop.kt.playback.PlaybackService
import com.beatdrop.kt.youtube.*
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo   = MediaRepository(app)
    private val prefs  = Prefs(app)

    // ── Library ───────────────────────────────────────────────────────────────
    private val _tracks  = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    // Library sort mode — default RECENT (recently added first), persisted
    // across app launches via Prefs.librarySortFlow. The init { } block
    // below hydrates _sort from disk before the UI renders so the user
    // never sees the wrong default flash.
    private val _sort = MutableStateFlow(SortMode.RECENT)
    val sort: StateFlow<SortMode> = _sort.asStateFlow()
    fun setSort(s: SortMode) {
        _sort.value = s
        viewModelScope.launch { prefs.setLibrarySort(s.name) }
    }

    // ── Playback ──────────────────────────────────────────────────────────────
    private val _current   = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position  = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    /**
     * Last known buffered position from the underlying player, in ms.
     * Used by NowPlayingScreen to draw a small accent dot on the seek
     * bar showing how far the buffer has filled past the playhead.
     */
    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _duration  = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // ── Lyrics ────────────────────────────────────────────────────────────────
    private val _lyrics        = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()
    private val _activeLyric   = MutableStateFlow(-1)
    val activeLyric: StateFlow<Int> = _activeLyric.asStateFlow()

    // ── Liked / Playlists / Counts ────────────────────────────────────────────
    private val _liked = MutableStateFlow<Set<String>>(emptySet())
    val liked: StateFlow<Set<String>> = _liked.asStateFlow()
    fun isLiked(id: String) = _liked.value.contains(id)
    fun toggleLike(id: String) {
        val next = _liked.value.toMutableSet().apply { if (!add(id)) remove(id) }
        _liked.value = next
        viewModelScope.launch { prefs.setLiked(next) }
    }

    private val _playlists = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<String>>> = _playlists.asStateFlow()
    fun createPlaylist(name: String) {
        if (name.isBlank() || _playlists.value.containsKey(name)) return
        persistPlaylists(_playlists.value + (name.trim() to emptyList()))
    }
    fun deletePlaylist(name: String) { persistPlaylists(_playlists.value - name) }
    fun addToPlaylist(name: String, trackId: String) {
        val cur = _playlists.value[name] ?: return
        if (trackId in cur) return
        persistPlaylists(_playlists.value + (name to (cur + trackId)))
    }
    fun removeFromPlaylist(name: String, trackId: String) {
        val cur = _playlists.value[name] ?: return
        persistPlaylists(_playlists.value + (name to (cur - trackId)))
    }
    fun playlistTracks(name: String): List<Track> {
        val ids = _playlists.value[name] ?: return emptyList()
        val byId = _tracks.value.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
    private fun persistPlaylists(m: Map<String, List<String>>) {
        _playlists.value = m
        viewModelScope.launch { prefs.setPlaylists(m) }
    }

    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts.asStateFlow()

    // ── Settings ──────────────────────────────────────────────────────────────
    private val _haptics = MutableStateFlow(true)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()
    fun setHaptics(v: Boolean) { _haptics.value = v; viewModelScope.launch { prefs.setHaptics(v) } }

    private val _theme = MutableStateFlow("dark")
    val theme: StateFlow<String> = _theme.asStateFlow()
    fun setTheme(v: String) { _theme.value = v; viewModelScope.launch { prefs.setTheme(v) } }

    private val _defaultShuffle = MutableStateFlow(false)
    val defaultShuffle: StateFlow<Boolean> = _defaultShuffle.asStateFlow()
    fun setDefaultShuffle(v: Boolean) { _defaultShuffle.value = v; viewModelScope.launch { prefs.setDefaultShuffle(v) } }

    private val _autoDjEnabled = MutableStateFlow(false)
    val autoDjEnabled: StateFlow<Boolean> = _autoDjEnabled.asStateFlow()
    fun setAutoDjEnabled(v: Boolean) { _autoDjEnabled.value = v; viewModelScope.launch { prefs.setAutoDj(v) } }

    // ── Auto-DJ: user-tunable crossfade length (4..12 s, default 8 s) ────────
    private val _crossfadeMs = MutableStateFlow(8_000)
    val crossfadeMs: StateFlow<Int> = _crossfadeMs.asStateFlow()
    fun setCrossfadeMs(v: Int) {
        val clamped = v.coerceIn(4_000, 12_000)
        _crossfadeMs.value = clamped
        viewModelScope.launch { prefs.setCrossfadeMs(clamped) }
    }

    // ── What's New version tracking ───────────────────────────────────────
    /** Returns the last app versionCode the user has dismissed What's New for. */
    suspend fun lastSeenWhatsNew(): Int = prefs.lastSeenWhatsNewFlow.first()
    /** Record that the user has now seen What's New for [versionCode]. */
    fun markWhatsNewSeen(versionCode: Int) {
        viewModelScope.launch { prefs.setLastSeenWhatsNew(versionCode) }
    }

    // ── Privacy / Terms acceptance ───────────────────────────────────────
    suspend fun termsAcceptedVersion(): Int = prefs.termsAcceptedVersionFlow.first()
    fun acceptTerms(versionCode: Int) {
        viewModelScope.launch { prefs.setTermsAcceptedVersion(versionCode) }
    }

    // ── User-selected app language ──────────────────────────────────────
    private val _language = MutableStateFlow("")
    val language: StateFlow<String> = _language.asStateFlow()
    fun setLanguage(tag: String) {
        _language.value = tag
        viewModelScope.launch { prefs.setLanguage(tag) }
        // Apply at runtime. AppCompatDelegate.setApplicationLocales
        // re-creates the activity stack with the new locale, so the
        // entire UI re-resolves stringResource() against the chosen
        // values-XX/strings.xml.
        val locales = if (tag.isBlank()) {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            androidx.core.os.LocaleListCompat.forLanguageTags(tag)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
    }

    // ── Resolver backend (optional self-hosted yt-dlp proxy) ─────────────────
    private val _resolverBackend = MutableStateFlow("")
    val resolverBackend: StateFlow<String> = _resolverBackend.asStateFlow()
    fun setResolverBackend(v: String) {
        val trimmed = v.trim()
        _resolverBackend.value = trimmed
        com.beatdrop.kt.youtube.ResolverBackend.baseUrl = trimmed.ifBlank { null }
        viewModelScope.launch { prefs.setResolverBackend(trimmed) }
    }

    // ── Stream quality ("auto" | "high" | "medium" | "low") ──────────────────
    private val _streamQuality = MutableStateFlow("auto")
    val streamQuality: StateFlow<String> = _streamQuality.asStateFlow()
    fun setStreamQuality(v: String) {
        _streamQuality.value = v
        com.beatdrop.kt.youtube.QualityPreference.preferred = v
        viewModelScope.launch { prefs.setStreamQuality(v) }
    }

    // ── Music-mode search (WEB_REMIX vs generic YouTube) ─────────────────────
    private val _musicSearchEnabled = MutableStateFlow(true)
    val musicSearchEnabled: StateFlow<Boolean> = _musicSearchEnabled.asStateFlow()
    fun setMusicSearchEnabled(v: Boolean) {
        _musicSearchEnabled.value = v
        com.beatdrop.kt.youtube.OnlineSearch.musicMode = v
        viewModelScope.launch { prefs.setMusicSearchEnabled(v) }
    }

    // ── Auto-DJ runtime state ────────────────────────────────────────────────
    @Volatile private var isCrossfading = false
    private var autoMixSequence = 0L  // monotonic — guards against stale finally blocks
    @Volatile private var crossfadeJob: kotlinx.coroutines.Job? = null
    // Repeat mode to restore after the fade (we force REPEAT_MODE_ONE during it).
    @Volatile private var savedRepeatMode: Int = Player.REPEAT_MODE_OFF
    // The next track Auto-Mix has chosen, exposed so Now Playing can show a
    // "Mixing in: <title>" badge during the fade.
    private val _mixingNext = MutableStateFlow<Track?>(null)
    val mixingNext: StateFlow<Track?> = _mixingNext.asStateFlow()
    // Ring buffer of last N played track ids — used to avoid immediate repeats.
    private val recentlyPlayedIds = ArrayDeque<String>(8)
    private fun pushRecent(id: String) {
        if (recentlyPlayedIds.lastOrNull() == id) return
        recentlyPlayedIds.addLast(id)
        while (recentlyPlayedIds.size > 6) recentlyPlayedIds.removeFirst()
    }
    // Online-played ring — separate from local so cross-source transitions
    // don't mistakenly exclude valid candidates.
    private val onlineRecentlyPlayedIds = ArrayDeque<String>(8)
    /**
     * Records that [videoId] just started playing online. Also feeds the
     * smart-shuffle CollabGraph with this track's title/artist so the graph
     * grows as the session goes on (artists who appear together in titles
     * become linked and start boosting each other in pickNext).
     */
    private fun pushRecentOnline(videoId: String, title: String = "", artist: String = "") {
        if (title.isNotBlank() && artist.isNotBlank()) {
            runCatching {
                com.beatdrop.kt.playback.OnlineSmartShuffle.CollabGraph.observe(title, artist)
            }
        }
        if (videoId.isBlank()) return
        if (onlineRecentlyPlayedIds.lastOrNull() == videoId) return
        onlineRecentlyPlayedIds.addLast(videoId)
        while (onlineRecentlyPlayedIds.size > 6) onlineRecentlyPlayedIds.removeFirst()
        viewModelScope.launch { prefs.setOnlineRecentlyPlayed(onlineRecentlyPlayedIds.toList()) }
    }
    // Cached per-track features (BPM, key) populated by the analyzer.
    private val _trackFeatures = MutableStateFlow<Map<String, com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures>>(emptyMap())
    val trackFeatures: StateFlow<Map<String, com.beatdrop.kt.playback.TrackAnalyzer.TrackFeatures>> = _trackFeatures.asStateFlow()
    // Per-track analyzer scheduling: don't re-analyze in flight or already-cached tracks.
    private val analyzingIds = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    // ── Loudness gain cache (per-track volume multiplier 0.5..1.0) ───────────
    // Populated by LoudnessAnalyzer in the same background pass as the BPM/key
    // analyzer. Auto-Mix uses these to balance Deck B against Deck A during
    // the crossfade so a quiet track doesn't dip in the blend and a loud one
    // doesn't overpower.  No entry = play at native 1.0.
    private val loudnessGains = java.util.concurrent.ConcurrentHashMap<String, Float>()

    // ── Predictive prefetch tracking ─────────────────────────────────────────
    // Auto-Mix peeks at the next candidate ~30s before the current track ends
    // and warms its features cache + (for online tracks) its resolved URL via
    // the playback cache. By the time the fade fires at 8s remaining, B's
    // first range request is already buffered → no audible gap on slow nets.
    // ── Discover screen cache (prevents re-fetch on every tab switch) ────
    private val _cachedTrending = MutableStateFlow<List<com.beatdrop.kt.youtube.OnlineResult>>(emptyList())
    val cachedTrending: StateFlow<List<com.beatdrop.kt.youtube.OnlineResult>> = _cachedTrending.asStateFlow()
    private val _cachedPopHits = MutableStateFlow<List<com.beatdrop.kt.youtube.OnlineResult>>(emptyList())
    val cachedPopHits: StateFlow<List<com.beatdrop.kt.youtube.OnlineResult>> = _cachedPopHits.asStateFlow()
    private val _cachedHiphop = MutableStateFlow<List<com.beatdrop.kt.youtube.OnlineResult>>(emptyList())
    val cachedHiphop: StateFlow<List<com.beatdrop.kt.youtube.OnlineResult>> = _cachedHiphop.asStateFlow()
    // "Made for You" curated playlist hub for Discover.
    private val _madeForYou = MutableStateFlow<List<com.beatdrop.kt.youtube.MadeForYou.PlaylistPreview>>(emptyList())
    val madeForYou: StateFlow<List<com.beatdrop.kt.youtube.MadeForYou.PlaylistPreview>> = _madeForYou.asStateFlow()
    private val _madeForYouLoading = MutableStateFlow(false)
    val madeForYouLoading: StateFlow<Boolean> = _madeForYouLoading.asStateFlow()

    /**
     * Lazy load the curated "Made for You" playlist tiles. Cached for the
     * lifetime of the ViewModel; call again only when you genuinely want
     * a refresh.
     */
    fun loadMadeForYou(force: Boolean = false) {
        if (!force && (_madeForYou.value.isNotEmpty() || _madeForYouLoading.value)) return
        _madeForYouLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = runCatching { com.beatdrop.kt.youtube.MadeForYou.fetchAll() }
                .getOrDefault(emptyList())
            _madeForYou.value = list
            _madeForYouLoading.value = false
        }
    }

    /**
     * Open a curated playlist: fetches its full track list and starts
     * playback at the first track. Used by the Made-for-You card tap.
     *
     * Failure modes — all surfaced to the user, never silent:
     *   • Fetch returned no tracks (dead playlist ID, geo-block, etc.)
     *     → falls back to a search by the playlist's display title so
     *     the user still gets music in roughly the right genre, then
     *     posts a soft message describing what happened.
     *   • Fetch threw → onlineMessage is populated with the error
     *     so the Now Playing snackbar surfaces it.
     */
    fun playFeaturedPlaylist(playlistId: String, startFromTrackId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = runCatching {
                com.beatdrop.kt.youtube.YouTubePlaylist.fetchPlaylist(playlistId, maxItems = 100)
            }.getOrNull()
            // Strip livestreams and long-form videos (1-hr mixes etc.).
            // If the user explicitly tapped a track by id, never drop it —
            // play it regardless of duration.
            val filtered = info?.videos?.filter {
                it.videoId == startFromTrackId ||
                    (!it.isLive && it.durationSecs in 60..600)
            }.orEmpty()

            // ── Primary path: playlist returned tracks ─────────────────────
            if (filtered.isNotEmpty()) {
                // Smart-context adoption (Mode B): if the user tapped the
                // playlist/album generally (no specific start track) AND the
                // currently-playing song is somewhere inside this playlist,
                // prefer to start from THAT position rather than from index 0.
                // Combined with the same-track guard inside
                // prepareAndPlayOnline this means: tapping an album whose
                // first track is what you're already playing won't restart
                // it; tapping an album you're in the middle of won't jump
                // you back to track 1 — the playlist just becomes your new
                // queue and you keep listening.
                val currentVideoId = _current.value?.sourceVideoId
                val startIdx = when {
                    startFromTrackId != null ->
                        filtered.indexOfFirst { it.videoId == startFromTrackId }.coerceAtLeast(0)
                    currentVideoId != null && filtered.any { it.videoId == currentVideoId } ->
                        filtered.indexOfFirst { it.videoId == currentVideoId }
                    else -> 0
                }
                withContext(Dispatchers.Main) {
                    prepareAndPlayOnline(filtered[startIdx], filtered, startIdx)
                }
                return@launch
            }

            // ── Fallback: derive a title hint and search instead ───────────
            // Look up the curated metadata so the fallback search is sensible
            // (e.g. dead 'Afrobeats' playlist → search 'Afrobeats hits').
            val curated = com.beatdrop.kt.youtube.MadeForYou.featured
                .firstOrNull { it.playlistId == playlistId }
            val fallbackQuery = curated?.let { "${it.title} hits" } ?: playlistId
            DebugLog.w("playlist",
                "playlistId=$playlistId returned empty — falling back to search '$fallbackQuery'")
            val searchResults = runCatching {
                com.beatdrop.kt.youtube.searchYoutubeMusic(fallbackQuery, maxResults = 50)
            }.getOrDefault(emptyList())

            if (searchResults.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    prepareAndPlayOnline(searchResults.first(), searchResults, 0)
                    // Soft note so the user knows what happened (snackbar in
                    // SearchScreen / NowPlaying picks this up).
                    _onlineMessage.value =
                        "Loaded ${curated?.title ?: "playlist"} from a fresh search."
                }
            } else {
                withContext(Dispatchers.Main) {
                    _onlineMessage.value =
                        "Couldn't load ${curated?.title ?: "playlist"}. Check your connection and try again."
                }
            }
        }
    }
    private val _discoverLoading = MutableStateFlow(false)
    val discoverLoading: StateFlow<Boolean> = _discoverLoading.asStateFlow()
    private val _discoverLastFetch = MutableStateFlow(0L)
    val discoverLastFetch: StateFlow<Long> = _discoverLastFetch.asStateFlow()

    /** Fetch or return cached discover data (re-fetches every 5 minutes) */
    fun getDiscoverData(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (_discoverLoading.value) return
        // 5-minute hard cache. If a fresh fetch was attempted in the last
        // 5 seconds it's almost certainly a rapid tab-switch — drop it.
        // (B9 fix: dedup rapid repeat calls instead of silently no-op'ing
        //  through the loading guard.)
        if (!forceRefresh && now - _discoverLastFetch.value < 300_000L && _cachedTrending.value.isNotEmpty()) return
        if (now - _discoverLastFetch.value < 5_000L && _cachedTrending.value.isNotEmpty()) return
        _discoverLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Discover surfaces should contain real songs (60 s – 10 min)
                // — not 1-hr DJ sets, lofi-radio livestreams, or 8-min remixes
                // labelled 'extended cut'. The duration filter is applied
                // after fetch since search() returns whatever ranking YT
                // gives us. Fetch larger pools so we still end up with ~10–15
                // entries per row after filtering.
                val isSingleSong: (OnlineResult) -> Boolean = { r ->
                    !r.isLive && r.durationSecs in 60..600
                }
                val trending = com.beatdrop.kt.youtube.searchYoutube("trending music hits", 30)
                    .filter(isSingleSong).take(15)
                val pop = com.beatdrop.kt.youtube.searchYoutube("popular pop songs hits", 25)
                    .filter(isSingleSong).take(10)
                val hiphop = com.beatdrop.kt.youtube.searchYoutube("latest rap hiphop hits", 25)
                    .filter(isSingleSong).take(10)
                _cachedTrending.value = trending
                _cachedPopHits.value = pop
                _cachedHiphop.value = hiphop
                _discoverLastFetch.value = System.currentTimeMillis()
                // Persist to DataStore only if data changed
                val newCache = com.beatdrop.kt.data.Prefs.DiscoverCache(trending, pop, hiphop, _discoverLastFetch.value)
                val idSet = { l: List<OnlineResult> -> l.map { it.videoId }.toSet() }
                if (idSet(_cachedTrending.value) != idSet(trending) ||
                    idSet(_cachedPopHits.value) != idSet(pop) ||
                    idSet(_cachedHiphop.value) != idSet(hiphop)) {
                    runCatching { prefs.setDiscoverCache(newCache) }
                }
            }
            _discoverLoading.value = false
        }
    }

    /** Load discover cache from disk — call once on startup */
    fun loadDiscoverCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cache = prefs.discoverCacheFlow.first()
                if (cache.trending.isNotEmpty()) {
                    _cachedTrending.value = cache.trending
                    _cachedPopHits.value = cache.pop
                    _cachedHiphop.value = cache.hiphop
                    _discoverLastFetch.value = cache.timestamp
                }
            }
        }
    }

    @Volatile private var prefetchedNextId: String? = null      // current track id we've prefetched for
    @Volatile private var prefetchInFlight: Boolean = false

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    fun deleteHistoryQuery(q: String) { viewModelScope.launch { prefs.deleteSearchQuery(q) } }

    private fun observePrefs() {
        prefs.likedFlow.onEach { _liked.value = it }.launchIn(viewModelScope)
        prefs.playlistsFlow.onEach { _playlists.value = it }.launchIn(viewModelScope)
        prefs.playCountsFlow.onEach { _playCounts.value = it }.launchIn(viewModelScope)
        prefs.hapticsFlow.onEach { _haptics.value = it }.launchIn(viewModelScope)
        prefs.librarySortFlow.onEach { name ->
            // Tolerate stale enum names (e.g. user downgraded; we renamed
            // an enum) by falling back to the RECENT default.
            _sort.value = runCatching { SortMode.valueOf(name) }.getOrDefault(SortMode.RECENT)
        }.launchIn(viewModelScope)
        prefs.languageFlow.onEach { _language.value = it }.launchIn(viewModelScope)
        prefs.themeFlow.onEach { _theme.value = it }.launchIn(viewModelScope)
        prefs.defaultShuffleFlow.onEach { _defaultShuffle.value = it }.launchIn(viewModelScope)
        prefs.autoDjFlow.onEach { _autoDjEnabled.value = it }.launchIn(viewModelScope)
        prefs.crossfadeMsFlow.onEach { _crossfadeMs.value = it }.launchIn(viewModelScope)
        prefs.resolverBackendFlow.onEach {
            _resolverBackend.value = it
            com.beatdrop.kt.youtube.ResolverBackend.baseUrl = it.ifBlank { null }
        }.launchIn(viewModelScope)
        prefs.streamQualityFlow.onEach {
            _streamQuality.value = it
            com.beatdrop.kt.youtube.QualityPreference.preferred = it
        }.launchIn(viewModelScope)
        prefs.musicSearchEnabledFlow.onEach {
            _musicSearchEnabled.value = it
            com.beatdrop.kt.youtube.OnlineSearch.musicMode = it
        }.launchIn(viewModelScope)
        prefs.trackFeaturesFlow.onEach { _trackFeatures.value = it }.launchIn(viewModelScope)
        prefs.searchHistoryFlow.onEach { _searchHistory.value = it }.launchIn(viewModelScope)
        prefs.wifiOnlyFlow.onEach { com.beatdrop.kt.youtube.DownloadManagerV2.wifiOnly = it }.launchIn(viewModelScope)
        prefs.downloadSpeedLimitFlow.onEach { com.beatdrop.kt.youtube.DownloadManagerV2.speedLimitKBps = it }.launchIn(viewModelScope)
        prefs.maxConcurrentDownloadsFlow.onEach { com.beatdrop.kt.youtube.DownloadManagerV2.maxConcurrentDownloads = it }.launchIn(viewModelScope)
        prefs.downloadDirPathFlow.onEach { com.beatdrop.kt.youtube.DownloadManagerV2.downloadDirPath = it.ifBlank { null } }.launchIn(viewModelScope)
        prefs.privatePinFlow.onEach { _privatePin.value = it }.launchIn(viewModelScope)
        prefs.searchPlatformFlow.onEach { com.beatdrop.kt.youtube.OnlineSearch.searchPlatform = it }.launchIn(viewModelScope)
        prefs.smartShuffleFlow.onEach { _smartShuffle.value = it }.launchIn(viewModelScope)
        prefs.onlineRecentlyPlayedOrderedFlow.onEach { onlineRecentlyPlayedIds.clear(); onlineRecentlyPlayedIds.addAll(it) }.launchIn(viewModelScope)
    }

    // ── Queue / shuffle / repeat ──────────────────────────────────────────────
    private val _queue   = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    fun toggleShuffle() { _shuffle.value = !_shuffle.value; controller?.shuffleModeEnabled = _shuffle.value }

    private val _smartShuffle = MutableStateFlow(false)
    val smartShuffle: StateFlow<Boolean> = _smartShuffle.asStateFlow()
    fun toggleSmartShuffle() {
        val v = !_smartShuffle.value
        _smartShuffle.value = v
        viewModelScope.launch { prefs.setSmartShuffle(v) }
        if (v) {
            if (_current.value?.isStreaming == true && onlineContext.isNotEmpty()) {
                smartShuffleOnlineContext()
            }
            _smartShuffleMessage.value = "Smart Shuffle on — picks the best next track based on artist, style, and your taste."
        } else {
            _smartShuffleMessage.value = null
        }
    }

    private val _smartShuffleMessage = MutableStateFlow<String?>(null)
    val smartShuffleMessage: StateFlow<String?> = _smartShuffleMessage.asStateFlow()
    fun clearSmartShuffleMessage() { _smartShuffleMessage.value = null }

    private val _repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()
    fun cycleRepeat() {
        val next = when (_repeat.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeat.value = next; controller?.repeatMode = next
    }

    private fun refreshQueueFromController() {
        val c = controller ?: return
        val out = ArrayList<Track>()
        for (i in 0 until c.mediaItemCount) {
            val id = c.getMediaItemAt(i).mediaId
            val t = _tracks.value.firstOrNull { it.id == id } ?: _ytTrackCache[id]
            t?.let { out.add(it) }
        }
        _queue.value = out
    }
    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) { c.seekTo(index, 0L); c.play() }
    }
    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) {
            c.moveMediaItem(from, to); refreshQueueFromController()
        }
    }
    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) { c.removeMediaItem(index); refreshQueueFromController() }
    }
    fun shuffleAll() {
        val all = _tracks.value; if (all.isEmpty()) return
        val list = all.shuffled(); playList(list, list.first().id)
        _shuffle.value = true; controller?.shuffleModeEnabled = true
    }

    private var controller: MediaController? = null

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()
    fun setVolume(v: Float) {
        val x = v.coerceIn(0f, 1f)
        _volume.value = x
        controller?.volume = x
    }

    // ── Derived lists (memoized) ──────────────────────────────────────────────
    val filteredTracks: StateFlow<List<Track>> =
        combine(_tracks, _query.debounce(150), _sort) { tracks, query, sort ->
            val q = query.trim()
            val base = if (q.isBlank()) tracks else tracks.filter {
                it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
            }
            repo.sort(base, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumGroups: StateFlow<List<AlbumGroup>> =
        _tracks.map { repo.groupAlbums(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistGroups: StateFlow<List<ArtistGroup>> =
        _tracks.map { repo.groupArtists(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filteredSorted(): List<Track> = filteredTracks.value
    fun albums(): List<AlbumGroup>   = albumGroups.value.ifEmpty  { repo.groupAlbums(_tracks.value) }
    fun artists(): List<ArtistGroup> = artistGroups.value.ifEmpty { repo.groupArtists(_tracks.value) }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    fun connect() {
        com.beatdrop.kt.util.NetworkMonitor.init(getApplication())
        observePrefs()
        observeDownloadCompletions()
        loadDiscoverCache()
        val token = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        val future = MediaController.Builder(getApplication(), token).buildAsync()
        future.addListener({
            controller = future.get(); attach(); startTicker()
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    @Volatile private var onlineTransitionInProgress = false
    // Guards against infinite re-resolve loops on a permanently-dead stream.
    @Volatile private var lastRecoveredVideoId: String? = null

    private fun attach() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) {
                _isPlaying.value = p
                DebugLog.d("player", "isPlaying=$p")
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val name = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"; Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"; Player.STATE_ENDED -> "ENDED"; else -> "$playbackState"
                }
                // Skip ENDED log for temp online tracks (Uri.EMPTY) -- those immediately
                // transition to ENDED because the placeholder has no media source.
                if (name == "ENDED" && _current.value?.uri == Uri.EMPTY) return
                DebugLog.d("player", "state=$name")
                _duration.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
                _volume.value = controller?.volume ?: 1f

                // ── Online auto-advance on natural end ──────────────────────
                // Local tracks auto-advance via ExoPlayer's internal queue.
                // Online tracks are loaded one-at-a-time via setMediaItem so
                // STATE_ENDED would leave the player IDLE → silence. Bridge
                // the gap: if an online track ends and we have an online
                // context with a next entry, call next() to chain into it.
                // The next track's stream URL is already warmed by
                // triggerNextPrefetch(), so the gap is < 200 ms in practice.
                if (playbackState == Player.STATE_ENDED) {
                    val cur = _current.value
                    if (cur?.isStreaming == true && onlineContext.isNotEmpty() &&
                        onlineContextIndex + 1 in onlineContext.indices &&
                        !onlineTransitionInProgress && !isCrossfading
                    ) {
                        DebugLog.i("player", "online STATE_ENDED → auto-advance")
                        next()
                    }
                }
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                // Don't let ExoPlayer callbacks clobber an in-flight online track switch.
                // When prepareAndPlayOnline sets a temp track then calls setMediaItem,
                // ExoPlayer can fire this with the OLD item during the transition.
                if (onlineTransitionInProgress) return
                syncCurrent()
            }
            override fun onTimelineChanged(t: Timeline, reason: Int) { refreshQueueFromController() }
            override fun onPlayerError(error: PlaybackException) {
                DebugLog.e("player", "onPlayerError code=${error.errorCode} ${error.message}")
                // 403 / expired-token recovery: googlevideo URLs expire and are
                // IP/client-bound. On a bad HTTP status for a streamed track, drop
                // the cached URL and silently re-resolve + resume once.
                val cur = _current.value
                // Also recover from ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE (2004) —
                // googlevideo CDN returns a wrong content type when client identity
                // headers are missing/mismatched, not a 4xx status.
                val recoverableError = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE
                if (recoverableError &&
                    cur != null && cur.isStreaming && cur.sourceVideoId != null &&
                    cur.sourceVideoId != lastRecoveredVideoId
                ) {
                    lastRecoveredVideoId = cur.sourceVideoId
                    val resumeAt = controller?.currentPosition ?: 0L
                    // CRITICAL: clear the broken media item from the controller
                    // BEFORE we kick off the re-resolve. Otherwise ExoPlayer
                    // keeps re-buffering the rejected URL in parallel with our
                    // WebView extraction (the second onPlayerError code=2004
                    // mid-extraction in your debug log) — which then gets
                    // suppressed by lastRecoveredVideoId, leaving the player
                    // dead-IDLE while we work.
                    controller?.let {
                        onlineTransitionInProgress = true
                        it.stop()
                        it.clearMediaItems()
                    }
                    viewModelScope.launch {
                        // markForWebViewRetry() tells getStream() to SKIP
                        // Strategy 2 (Innertube) and go straight to Strategy 3
                        // (WebView extractor) which uses real Chrome cookies +
                        // visitorData and is not subject to PO-token enforcement.
                        DebugLog.i("player", "recovery → WebView re-resolve for ${cur.sourceVideoId}")
                        markForWebViewRetry(cur.sourceVideoId!!)
                        val fresh = runCatching {
                            youtubeResultToTrack(
                                OnlineResult(
                                    videoId = cur.sourceVideoId!!,
                                    title = cur.title, author = cur.artist,
                                    thumbnailUrl = cur.artworkOverride,
                                    durationText = "", durationSecs = (cur.durationMs / 1000).toInt(),
                                )
                            )
                        }.getOrNull()
                        if (fresh != null) {
                            _ytTrackCache[fresh.id] = fresh
                            val c = controller ?: run { onlineTransitionInProgress = false; return@launch }
                            c.setMediaItem(fresh.toMediaItem()); c.prepare()
                            c.seekTo(resumeAt); c.play()
                            onlineTransitionInProgress = false
                            _current.value = fresh
                        } else {
                            onlineTransitionInProgress = false
                            _onlineMessage.value = "Couldn't play this track — may be region-blocked or require sign-in. Try another."
                        }
                    }
                    return
                }

                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network error. Check your connection."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                        "Stream unavailable (HTTP error). Try another song."
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Track not found. It may have been removed."
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Cannot play this audio format."
                    else -> "Playback error: ${error.message}"
                }
                _onlineMessage.value = msg
            }
        })
    }

    private fun syncCurrent() {
        val id = controller?.currentMediaItem?.mediaId ?: return
        // Check local tracks first, then youtube cache
        val t = _tracks.value.firstOrNull { it.id == id } ?: _ytTrackCache[id] ?: return
        _current.value = t; _duration.value = t.durationMs
        loadLyrics(t)
    }

    @Volatile private var libraryLoadStarted = false
    fun loadLibrary() {
        if (libraryLoadStarted) return
        libraryLoadStarted = true
        viewModelScope.launch(Dispatchers.IO) {
            repo.loadTracksStreaming(batchSize = 60) { batch ->
                // Merge persistent download history into the library so
                // tracks the user downloaded NEVER disappear after restart,
                // even when the download lives in an app-private folder
                // that the system MediaStore doesn't index.
                _tracks.value = mergeWithDownloads(batch)
                if (!_loaded.value) _loaded.value = true
            }
        }
    }

    /**
     * Take the MediaStore scan result and union it with the persisted
     * DownloadHistory.completedRecords() — synthesising Track entries
     * for downloads whose file path doesn't appear in the MediaStore
     * batch. Deduped by file path so a download that lives in the
     * Music/ folder (which MediaStore does index) is only counted once.
     */
    private fun mergeWithDownloads(scanned: List<Track>): List<Track> {
        val knownPaths = scanned.mapNotNull { it.data }.toHashSet()
        val downloads = com.beatdrop.kt.data.DownloadHistory.completedRecords()
        if (downloads.isEmpty()) return scanned
        val synthesised = downloads.mapNotNull { rec ->
            val path = rec.filePath ?: return@mapNotNull null
            if (path in knownPaths) return@mapNotNull null
            Track(
                id = "dl_${rec.videoId}",
                uri = android.net.Uri.fromFile(java.io.File(path)),
                title = rec.title,
                artist = rec.artist,
                album = rec.artist,           // album metadata isn't tracked per-download
                albumId = 0L,
                durationMs = rec.durationSecs * 1000L,
                data = path,
                dateAdded = rec.downloadedAt / 1000L,   // store as Unix seconds to match MediaStore
                artworkOverride = rec.thumbnailUrl,
            )
        }
        return scanned + synthesised
    }

    // ── Playback ──────────────────────────────────────────────────────────────
    fun play(track: Track) {
        cancelAutoMix()
        val c = controller ?: return
        val list = filteredSorted()
        val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        c.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        // Apply this track's loudness gain if we have it; default to 1.0.
        c.volume = (loudnessGains[track.id] ?: 1.0f).let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
        c.play()
        _current.value = track; _duration.value = track.durationMs
        prefetchedNextId = null   // re-arm prefetch for the new current track
        pushRecent(track.id)
        loadLyrics(track); refreshQueueFromController()
        viewModelScope.launch { prefs.incrementPlayCount(track.id) }
        scheduleAnalysis(track)
    }

    fun playList(tracks: List<Track>, startId: String) {
        val c = controller ?: return
        val idx = tracks.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        c.setMediaItems(tracks.map { it.toMediaItem() }, idx, 0L)
        c.prepare(); c.play()
        _current.value = tracks.getOrNull(idx); refreshQueueFromController()
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        val at = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItem(at, track.toMediaItem())
        if (c.mediaItemCount == 1) { c.prepare(); c.play() }
        refreshQueueFromController()
    }

    fun addToQueueEnd(track: Track) {
        val c = controller ?: return
        c.addMediaItem(track.toMediaItem())
        if (c.mediaItemCount == 1) { c.prepare(); c.play() }
        refreshQueueFromController()
    }

    /** Queue an online result as "play next" — resolves stream URL in background */
    fun playOnlineNext(result: OnlineResult) {
        viewModelScope.launch {
            val track = runCatching { youtubeResultToTrack(result) }.getOrNull() ?: return@launch
            _ytTrackCache[track.id] = track
            playNext(track)
        }
    }

    /** Queue an online result at end of queue */
    fun addOnlineToQueue(result: OnlineResult) {
        viewModelScope.launch {
            val track = runCatching { youtubeResultToTrack(result) }.getOrNull() ?: return@launch
            _ytTrackCache[track.id] = track
            addToQueueEnd(track)
        }
    }

    fun togglePlay() {
        cancelAutoMix()
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    fun next() {
        cancelAutoMix()
        // Online context wins over ExoPlayer's internal queue — see
        // onlineContext field comment for why.
        val cur = _current.value
        if (cur?.isStreaming == true && onlineContext.isNotEmpty()) {
            val nextIdx = onlineContextIndex + 1
            if (nextIdx in onlineContext.indices) {
                val nextResult = onlineContext[nextIdx]

                // ── Fast path: warm stream URL already in cache ─────────
                // If the upcoming-tracks prefetch (kicked off when the
                // current track started AND when the context was first set)
                // has already resolved this videoId's stream URL, skip the
                // full prepareAndPlayOnline path which costs:
                //   • controller.stop()           ~50 ms IPC
                //   • controller.clearMediaItems() ~50 ms IPC
                //   • re-publishing temp track to _current
                // and instead just set the resolved media item and prepare
                // directly. UI updates still happen optimistically below.
                val cachedStream = com.beatdrop.kt.youtube.getCachedStream(nextResult.videoId)
                val c = controller
                if (cachedStream != null && c != null) {
                    // Update onlineContext index + optimistic Now Playing
                    // immediately, then run the real swap on the controller.
                    onlineContextIndex = nextIdx
                    val tempTrack = Track(
                        id = "yt_${nextResult.videoId}",
                        uri = Uri.EMPTY,
                        title = nextResult.title,
                        artist = nextResult.author,
                        album = nextResult.author,
                        albumId = 0L,
                        durationMs = nextResult.durationSecs * 1000L,
                        data = null,
                        dateAdded = System.currentTimeMillis(),
                        artworkOverride = nextResult.thumbnailUrl,
                    )
                    _current.value = tempTrack
                    _duration.value = nextResult.durationSecs * 1000L
                    _position.value = 0L
                    _bufferedPosition.value = 0L
                    _activeLyric.value = -1
                    _lyrics.value = emptyList()
                    _fetchingVideoId.value = nextResult.videoId

                    viewModelScope.launch {
                        // youtubeResultToTrack reads from urlCache first, so
                        // this is essentially synchronous (~1-5 ms).
                        val nextTrack = runCatching { youtubeResultToTrack(nextResult) }.getOrNull()
                        if (nextTrack != null) {
                            _ytTrackCache[nextTrack.id] = nextTrack
                            c.setMediaItem(nextTrack.toMediaItem())
                            c.prepare()
                            c.play()
                            _current.value = nextTrack
                            _duration.value = nextTrack.durationMs
                            _fetchingVideoId.value = null
                            pushRecent("yt_${nextTrack.sourceVideoId}")
                            pushRecentOnline(nextTrack.sourceVideoId ?: "", nextTrack.title, nextTrack.artist)
                            loadLyrics(nextTrack)
                            // Prefetch the NEW upcoming entries for the
                            // *new* current position so Next still feels
                            // instant on the next tap.
                            prefetchUpcomingFromContext()
                            triggerNextOnlinePrefetch()
                            DebugLog.i("next", "✅ fast-path next → ${nextTrack.title} (URL was cached)")
                        } else {
                            // Cache hit on getCachedStream but resolve still
                            // failed — fall back to the slow path.
                            DebugLog.w("next", "fast-path next failed, falling back to prepareAndPlayOnline")
                            prepareAndPlayOnline(nextResult, onlineContext, nextIdx)
                        }
                    }
                    return
                }

                // ── Slow path: URL not cached yet — full resolve ────────
                // Pre-queue media item in ExoPlayer too so lock-screen
                // controls show 'Next: <title>'.
                viewModelScope.launch {
                    val nextTrack = runCatching { youtubeResultToTrack(nextResult) }.getOrNull()
                    if (nextTrack != null) {
                        _ytTrackCache[nextTrack.id] = nextTrack
                        val ctrl = controller
                        if (ctrl != null && ctrl.mediaItemCount > 0) {
                            val at = ctrl.currentMediaItemIndex + 1
                            if (at <= ctrl.mediaItemCount && ctrl.getMediaItemAt(at.coerceAtMost(ctrl.mediaItemCount - 1)).mediaId != nextTrack.id) {
                                ctrl.addMediaItem(at, nextTrack.toMediaItem())
                            }
                        }
                    }
                }
                prepareAndPlayOnline(nextResult, onlineContext, nextIdx)
                return
            }
            // End of online context — fall through to ExoPlayer (no-op for
            // single-item case, advance if a local track somehow got mixed in).
        }
        controller?.seekToNextMediaItem()
    }

    fun prev() {
        cancelAutoMix()
        // Apple Music / Spotify behaviour: tap prev within first 3s of a
        // track = previous track; later than 3s = restart current track.
        val c = controller
        val cur = _current.value
        if (cur?.isStreaming == true && onlineContext.isNotEmpty()) {
            val pos = c?.currentPosition ?: 0L
            if (pos > 3_000L) {
                c?.seekTo(0L)
                _position.value = 0L
                return
            }
            val prevIdx = onlineContextIndex - 1
            if (prevIdx in onlineContext.indices) {
                val prevResult = onlineContext[prevIdx]
                prepareAndPlayOnline(prevResult, onlineContext, prevIdx)
                return
            }
            // Beginning of online context — just restart current.
            c?.seekTo(0L)
            _position.value = 0L
            return
        }
        c?.seekToPreviousMediaItem()
    }
    fun seekTo(ms: Long) {
        cancelAutoMix()
        controller?.seekTo(ms)
        _position.value = ms
        // Recompute the active lyric line immediately on seek; otherwise the
        // highlight stays stale for up to one ticker interval (~120 ms) which
        // is visually jarring when the user taps a line to jump to it.
        if (_lyrics.value.isNotEmpty()) {
            _activeLyric.value = LrcParser.activeIndex(_lyrics.value, ms)
        }
    }

    private fun loadLyrics(track: Track) {
        viewModelScope.launch {
            _activeLyric.value = -1; _lyrics.value = emptyList(); _lyricsLoading.value = true
            val lines = withContext(Dispatchers.IO) { LyricsEngine.fetch(getApplication(), track) }
            if (_current.value?.id == track.id) {
                _lyrics.value = lines; _lyricsLoading.value = false
            }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    val dur = c.duration
                    _position.value = pos
                    _bufferedPosition.value = c.bufferedPosition.coerceAtLeast(pos)
                    if (dur > 0) {
                        _duration.value = dur
                        if (_autoDjEnabled.value && !isCrossfading) {
                            val tail = dur - pos
                            val fadeMs = _crossfadeMs.value.toLong()
                            // Predictive prefetch: ~30 s before end, peek at the
                            // next track Auto-Mix is likely to pick, warm its
                            // features cache + loudness probe + (for online
                            // tracks) the playback cache via getStream(). Fires
                            // once per current track (guarded by prefetchedNextId).
                            if (tail in 28_000L..32_000L) {
                                triggerPrefetch()
                            }
                            // Auto-Mix trigger: start crossfading exactly `crossfadeMs`
                            // before the track's natural end. Tight 350 ms guard band
                            // absorbs ticker jitter without firing twice.
                            if (tail in (fadeMs - 350L)..(fadeMs + 350L)) {
                                triggerAutoMix(fadeMs)
                            }
                        }
                    }
                    if (_lyrics.value.isNotEmpty())
                        _activeLyric.value = LrcParser.activeIndex(_lyrics.value, _position.value)
                    delay(120)
                } else delay(400)
            }
        }
    }

    /**
     * The Auto-Mix transition — the heart of the SnapTube-style auto-DJ.
     *
     * KEY DESIGN POINTS (and the bugs they fix from the previous version):
     *
     * 1. **No restart of the next track at the end of the fade.** The previous
     *    code did `controller.setMediaItem(Y); prepare(); play()` after the fade,
     *    which started Y from position 0 — so the user heard Y's first 4 s once
     *    on Deck B, then again on the main player. The new version captures
     *    Deck B's `currentPosition` at the moment of swap and seeks the main
     *    player to that position. **Audio is continuous; the intro plays once.**
     *
     * 2. **Equal-power curve** (`cos²θ + sin²θ = 1`) instead of linear, so total
     *    perceived loudness stays constant through the blend — no centre dip.
     *
     * 3. **The current track is held in `REPEAT_MODE_ONE` during the fade** so
     *    when X reaches its natural end mid-crossfade, ExoPlayer doesn't
     *    auto-advance into a random next library track and race with us.
     *
     * 4. **User actions abort the fade cleanly.** A skip/back/seek/pause cancels
     *    `crossfadeJob`, restores main-player volume to 1.0, stops Deck B, and
     *    clears `isCrossfading` so a later Auto-Mix can re-arm.
     */
    private fun triggerAutoMix(fadeMs: Long) {
        if (isCrossfading) return
        val cur = _current.value ?: return
        val library = _tracks.value
        if (library.isEmpty()) return
        // ── Smart Shuffle hybrid path ──────────────────────────────────────
        // When Smart Shuffle is on and the current track is online, use the
        // hybrid picker that can mix local and online candidates.  When the
        // current track is local, still use the pure-local scorer unless the
        // user has an online context open (in which case we blend).
        if (cur.isStreaming) {
            // Online track: use the hybrid picker
            val recentOnline = onlineRecentlyPlayedIds.toSet()
            val (localPick, onlinePick) = com.beatdrop.kt.playback.AutoMixEngine.pickNextHybrid(
                localPool = library,
                onlinePool = onlineContext,
                currentLocal = null,
                currentOnline = OnlineResult(
                    videoId = cur.sourceVideoId ?: cur.id.removePrefix("yt_"),
                    title = cur.title, author = cur.artist,
                    thumbnailUrl = cur.artworkOverride,
                    durationText = "", durationSecs = (cur.durationMs / 1000).toInt(),
                ),
                likedIds = _liked.value,
                playCounts = _playCounts.value,
                recentlyPlayedLocalIds = recentlyPlayedIds.toSet(),
                recentlyPlayedOnlineIds = recentOnline,
                featuresById = _trackFeatures.value,
            )
            if (onlinePick != null) {
                triggerOnlineAutoMix(onlinePick, fadeMs)
                return
            }
            // Online mode: smart shuffle picks online only — don't fall through to local
            return
        }

        // ── Local track: use the hybrid picker when online context exists ────
        val recent = (recentlyPlayedIds + cur.id).toSet()
        val recentOnline = onlineRecentlyPlayedIds.toSet()
        val next = if (onlineContext.isNotEmpty() && _smartShuffle.value) {
            val (localPick, onlinePick) = com.beatdrop.kt.playback.AutoMixEngine.pickNextHybrid(
                localPool = library,
                onlinePool = onlineContext,
                currentLocal = cur,
                currentOnline = null,
                likedIds = _liked.value,
                playCounts = _playCounts.value,
                recentlyPlayedLocalIds = recent,
                recentlyPlayedOnlineIds = recentOnline,
                featuresById = _trackFeatures.value,
            )
            if (onlinePick != null) {
                triggerOnlineAutoMix(onlinePick, fadeMs)
                return
            }
            localPick
        } else {
            com.beatdrop.kt.playback.AutoMixEngine.pickNext(
                current = cur, library = library, likedIds = _liked.value,
                playCounts = _playCounts.value, recentlyPlayedIds = recent,
                featuresById = _trackFeatures.value,
            )
        } ?: return

        isCrossfading = true
        _mixingNext.value = next
        val mainPlayer = controller ?: run { isCrossfading = false; _mixingNext.value = null; return }
        savedRepeatMode = mainPlayer.repeatMode
        mainPlayer.repeatMode = Player.REPEAT_MODE_ONE   // pin X in place

        // Surface the Auto-Mix pick in the MediaSession queue so lock-screen
        // / Bluetooth controls show "Next: <Y>" and a working ⏭ button. If
        // the user hits Bluetooth-next during the fade, vm.next() runs
        // cancelAutoMix() → seekToNextMediaItem() lands on Y cleanly.
        //
        // We insert Y at (currentMediaItemIndex + 1) so seekToNextMediaItem()
        // lands on it regardless of any existing playlist queue length.
        // After handoff the queue gets replaced with [Y] anyway (see below)
        // so we don't accumulate ghost entries across many transitions.
        runCatching {
            val curIdx = mainPlayer.currentMediaItemIndex
            val insertAt = (curIdx + 1).coerceAtMost(mainPlayer.mediaItemCount)
            // Avoid duplicate inserts if user re-triggers via skip → re-pick.
            val alreadyNext = insertAt < mainPlayer.mediaItemCount &&
                mainPlayer.getMediaItemAt(insertAt).mediaId == next.id
            if (!alreadyNext) mainPlayer.addMediaItem(insertAt, next.toMediaItem())
        }

        val deck = ensureDeckB()

        // Loudness gains — apply per-track so a quiet track doesn't dip in
        // the blend and a loud one doesn't overpower. Default to 1.0 (no
        // change) when the analyzer hasn't run for a track yet.
        val gainA = (loudnessGains[cur.id] ?: 1.0f).let { if (it.isNaN()) 1.0f else it }
        val gainB = (loudnessGains[next.id] ?: 1.0f).let { if (it.isNaN()) 1.0f else it }

        crossfadeJob = viewModelScope.launch {
            try {
                // If prefetch already set the media item on Deck B, don't reset
                // it — that would discard the warmed buffer.
                val deckHasNext = deck.currentMediaItem?.mediaId == next.id
                if (!deckHasNext) {
                    deck.volume = 0f
                    deck.setMediaItem(MediaItem.Builder().setMediaId(next.id).setUri(next.uri).build())
                    deck.prepare()
                }
                deck.playWhenReady = true

                // ── Update Now Playing UI at fade START, not at handoff ──────
                // Previously _current.value = next happened AFTER the 60-tick
                // fade loop, so the user heard the new song for the full
                // fadeMs (e.g. 13 s) while Now Playing still showed the old
                // one. Move metadata + lyrics + cover refresh to the moment
                // Deck B starts producing audio — the user's ears and eyes
                // now align within ~17 ms of each other.
                _current.value = next
                _duration.value = next.durationMs
                _position.value = deck.currentPosition.coerceAtLeast(0L)
                prefetchedNextId = null
                pushRecent(next.id)
                loadLyrics(next)
                prefs.incrementPlayCount(next.id)
                scheduleAnalysis(next)
                DebugLog.i("automix", "🎵 UI switched to ${next.title} at fade start")

                // Equal-power crossfade — 60 ticks regardless of duration → ~17ms cadence.
                // Each side scaled by its loudness gain (ReplayGain-style match).
                val ticks = 60
                val stepMs = (fadeMs / ticks).coerceAtLeast(8L)
                for (i in 0..ticks) {
                    val t = i.toFloat() / ticks                       // 0..1
                    val theta = (t * Math.PI / 2.0).toFloat()         // 0..π/2

                    // If X finished and looped natively mid-fade, mute it to prevent the intro bleeding in.
                    if (mainPlayer.currentPosition < 1000L && t > 0.1f) {
                        mainPlayer.volume = 0f
                    } else {
                        mainPlayer.volume = (kotlin.math.cos(theta) * gainA).toFloat().let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
                    }

                    deck.volume       = (kotlin.math.sin(theta) * gainB).toFloat().let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
                    // Keep the seek bar moving with Deck B's actual position
                    // so the user sees the new song scrubbing forward during
                    // the fade, not frozen at 0:00 until handoff.
                    _position.value = deck.currentPosition.coerceAtLeast(0L)
                    delay(stepMs)
                }
                // Hand off Deck B → main player at exactly Deck B's current position.
                val handoffPos = deck.currentPosition.coerceAtLeast(0L)
                deck.playWhenReady = false
                deck.volume = 0f
                mainPlayer.repeatMode = savedRepeatMode
                val nextIdx = mainPlayer.currentMediaItemIndex + 1

                // We MUST NOT do two seeks back-to-back (like seekToDefaultPosition then seekTo)
                // because the first seek clears ExoPlayer's pre-warmed buffer and the second seek
                // forces a cold I/O fetch, which causes the 1-second gap!
                // Instead, we just seek directly to the exact millisecond Deck B is at.
                if (nextIdx >= mainPlayer.mediaItemCount || mainPlayer.getMediaItemAt(nextIdx).mediaId != next.id) {
                    mainPlayer.addMediaItem(nextIdx, next.toMediaItem())
                }

                mainPlayer.seekTo(nextIdx, handoffPos)
                mainPlayer.prepare()
                mainPlayer.play()
                // Keep the B deck gain for the new track
                mainPlayer.volume = gainB.let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
                DebugLog.i("automix", "✅ audio handoff ${cur.title} → ${next.title} @${handoffPos}ms")
            } catch (e: Exception) {
                DebugLog.e("automix", "crossfade failed", e)
                // Roll back to a clean state.
                runCatching { mainPlayer.volume = 1f }
                runCatching { mainPlayer.repeatMode = savedRepeatMode }
                runCatching { deck.playWhenReady = false }
            } finally {
                isCrossfading = false
                _mixingNext.value = null
                crossfadeJob = null
            }
        }
    }


    /**
     * Online Auto-Mix transition — resolves the next track's stream URL and
     * hands off playback.  Since we can't use Deck B for online streams
     * (ResolvingDataSource requires the main player's pipeline), we do a
     * "soft handoff": resolve → set next media item → seek to next.
     *
     * The 200 MB playback cache makes re-resolution nearly instant for
     * recently-played tracks, so the gap is usually < 200 ms.
     */
    private fun triggerOnlineAutoMix(nextResult: OnlineResult, fadeMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        val transitionId = ++autoMixSequence
        val synth = Track(
            id = "yt_${nextResult.videoId}",
            uri = android.net.Uri.EMPTY,
            title = nextResult.title,
            artist = nextResult.author,
            album = nextResult.author,
            albumId = 0L,
            durationMs = nextResult.durationSecs * 1000L,
            data = null,
            dateAdded = System.currentTimeMillis(),
            artworkOverride = nextResult.thumbnailUrl,
        )
        _mixingNext.value = synth
        val mainPlayer = controller ?: run { isCrossfading = false; _mixingNext.value = null; return }
        savedRepeatMode = mainPlayer.repeatMode

        crossfadeJob = viewModelScope.launch {
            try {
                // ── Optimistic UI update at fade-out START ───────────────────
                // Publish the synthesised metadata immediately so Now Playing
                // shows the upcoming track's title/artist/cover the moment
                // the fade-out begins. The user's ears and eyes stay in sync
                // throughout the transition. The real Track instance lands
                // after stream resolution and replaces synth without flicker
                // (same id namespace 'yt_<videoId>').
                _current.value = synth
                _duration.value = synth.durationMs
                _position.value = 0L
                _activeLyric.value = -1
                _lyrics.value = emptyList()
                _fetchingVideoId.value = nextResult.videoId
                DebugLog.i("automix", "🎵 online UI switched to ${nextResult.title} at fade-out start")

                val track = runCatching { youtubeResultToTrack(nextResult) }.getOrElse { err ->
                    DebugLog.e("automix", "online resolve failed: ${err.message}")
                    isCrossfading = false; _mixingNext.value = null
                    _fetchingVideoId.value = null
                    return@launch
                }
                _ytTrackCache[track.id] = track

                // Short fade-out → handoff → fade-in
                val ticks = 30; val stepMs = (fadeMs / ticks).coerceAtLeast(16L)
                for (i in 0..ticks) {
                    mainPlayer.volume = (1f - i.toFloat() / ticks).coerceIn(0f, 1f)
                    delay(stepMs)
                }
                mainPlayer.clearMediaItems()
                mainPlayer.repeatMode = savedRepeatMode
                mainPlayer.setMediaItem(track.toMediaItem())
                mainPlayer.prepare(); mainPlayer.play(); mainPlayer.volume = 1f
                _current.value = track; _duration.value = track.durationMs
                _fetchingVideoId.value = null
                prefetchedNextId = null
                pushRecent("yt_${track.sourceVideoId}")
                pushRecentOnline(track.sourceVideoId ?: "", track.title, track.artist)
                loadLyrics(track)
                prefs.incrementPlayCount("yt_${track.sourceVideoId}")
                _onlineMessage.value = null
                val newIdx = onlineContext.indexOfFirst { it.videoId == nextResult.videoId }
                if (newIdx >= 0) onlineContextIndex = newIdx
                DebugLog.i("automix", "online ✅ handoff → ${nextResult.title}")
            } catch (e: Exception) {
                DebugLog.e("automix", "online crossfade failed", e)
                runCatching { mainPlayer.volume = 1f }
                runCatching { mainPlayer.repeatMode = savedRepeatMode }
            } finally {
                if (autoMixSequence == transitionId) {
                    isCrossfading = false; _mixingNext.value = null; crossfadeJob = null
                }
            }
        }
    }

    /**
     * Reorder the current onlineContext into a smart-shuffle sequence.
     * The currently-playing track stays at index 0; everything else is
     * reordered via OnlineSmartShuffle.buildSmartQueue().
     */
    private fun smartShuffleOnlineContext() {
        if (onlineContext.size <= 1) return
        val curIdx = onlineContextIndex.coerceIn(0, onlineContext.size - 1)
        val likedIds = _liked.value.mapNotNull { id ->
            if (id.startsWith("yt_")) id.removePrefix("yt_") else null
        }.toSet()
        val reordered = com.beatdrop.kt.playback.OnlineSmartShuffle.buildSmartQueue(
            pool = onlineContext, likedVideoIds = likedIds, startIndex = curIdx,
        )
        onlineContext = reordered
        onlineContextIndex = 0
    }

    /** Cancel a fade-in-progress (called when the user skips, seeks, or pauses). */
    private fun cancelAutoMix() {
        val job = crossfadeJob ?: return
        crossfadeJob = null
        job.cancel()
        runCatching { controller?.volume = 1f }
        runCatching { controller?.repeatMode = savedRepeatMode }
        runCatching { deckB?.playWhenReady = false }
        runCatching { deckB?.volume = 0f }
        isCrossfading = false
        _mixingNext.value = null
    }

    // ── Auto-DJ secondary player (used only for the crossfade buffer) ────────
    // This player is NEVER user-facing. The DJ Mode UI was removed; only
    // Auto-Mix uses it, and only during the brief crossfade window.
    private var deckB: ExoPlayer? = null
    private fun ensureDeckB(): ExoPlayer {
        return deckB ?: ExoPlayer.Builder(getApplication()).build().also { deckB = it }
    }

    // ── On-device track analyzer scheduler ───────────────────────────────────
    /**
     * Analyze a track for BPM + key + loudness on a background thread, cache
     * the results. Idempotent: skips tracks already cached or being analyzed.
     * Called opportunistically as you play things — by the time Auto-Mix wants
     * to score candidates, the tracks you actually listen to are already cached.
     */
    private fun scheduleAnalysis(track: Track) {
        if (track.isStreaming) return                         // local files only
        val path = track.data ?: return
        val haveFeat = _trackFeatures.value.containsKey(track.id)
        val haveLoud = loudnessGains.containsKey(track.id)
        if (haveFeat && haveLoud) return
        if (!analyzingIds.add(track.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!haveFeat) {
                    val feat = com.beatdrop.kt.playback.TrackAnalyzer.estimate(path, track.durationMs)
                    if (feat != null) {
                        prefs.putTrackFeatures(track.id, feat)
                        DebugLog.d("analyzer", "${track.title} → ${feat.bpm} BPM, ${feat.keyCamelot}")
                    }
                }
                if (!haveLoud) {
                    val gain = com.beatdrop.kt.playback.LoudnessAnalyzer.analyze(path)
                    if (gain != null) {
                        loudnessGains[track.id] = gain
                        DebugLog.d("loudness", "${track.title} → gain=${"%.2f".format(gain)}")
                    }
                }
            } catch (e: Exception) {
                DebugLog.w("analyzer", "${track.title}: ${e.message}")
            } finally {
                analyzingIds.remove(track.id)
            }
        }
    }

    /**
     * Predictive prefetch — fires ~30 s before the current track ends. Picks
     * the candidate Auto-Mix is likely to choose, then warms:
     *   • TrackAnalyzer features cache (so scoring is informed)
     *   • LoudnessAnalyzer gain (so the fade is volume-matched)
     *   • The playback cache via Deck B prepare (so the first range request
     *     for the crossfade is already in flight when the fade triggers)
     *
     * Guard: only fires once per current track (prefetchedNextId). User skips
     * reset this via cancelAutoMix → next current track gets a fresh prefetch.
     */
    private fun triggerPrefetch() {
        val cur = _current.value ?: return
        if (prefetchInFlight) return
        if (prefetchedNextId == cur.id) return
        val library = _tracks.value
        if (library.isEmpty()) return
        if (cur.isStreaming) {
            // Online prefetch: warm the playback cache by resolving next URL early
            if (onlineContext.isEmpty()) return
            prefetchInFlight = true
            prefetchedNextId = cur.id
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val recentOnline = onlineRecentlyPlayedIds.toSet()
                    val nextOnline = com.beatdrop.kt.playback.OnlineSmartShuffle.pickNext(
                        OnlineResult(
                            videoId = cur.sourceVideoId ?: cur.id.removePrefix("yt_"),
                            title = cur.title, author = cur.artist,
                            thumbnailUrl = cur.artworkOverride,
                            durationText = "", durationSecs = (cur.durationMs / 1000).toInt(),
                        ),
                        onlineContext,
                        recentOnline,
                        _liked.value.mapNotNull { if (it.startsWith("yt_")) it.removePrefix("yt_") else null }.toSet(),
                    ) ?: return@launch
                    // Warm the cache by resolving the URL (result goes into 200 MB playback cache)
                    runCatching { com.beatdrop.kt.youtube.getStream(nextOnline.videoId) }
                    DebugLog.i("prefetch", "warmed online: ${nextOnline.title}")
                } catch (_: Exception) {}
                finally { prefetchInFlight = false }
            }
            return
        }
        prefetchInFlight = true
        prefetchedNextId = cur.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recent = (recentlyPlayedIds + cur.id).toSet()
                val candidate = com.beatdrop.kt.playback.AutoMixEngine.pickNext(
                    current = cur, library = library, likedIds = _liked.value,
                    playCounts = _playCounts.value, recentlyPlayedIds = recent,
                    featuresById = _trackFeatures.value,
                ) ?: return@launch
                if (candidate.isStreaming) return@launch
                scheduleAnalysis(candidate)  // populates BPM+key+loudness if missing
                // Warm the playback cache by asking the deck to prepare the URI
                // briefly. The actual play happens at fade trigger; this just
                // gets the first range request in flight + cached on disk.
                val path = candidate.data
                if (path != null) {
                    runCatching {
                        val deck = ensureDeckB()
                        withContext(Dispatchers.Main) {
                            deck.volume = 0f
                            deck.setMediaItem(MediaItem.Builder().setMediaId(candidate.id).setUri(candidate.uri).build())
                            deck.prepare()
                            // Don't playWhenReady — we just want the buffer warmed.
                            deck.playWhenReady = false
                        }
                    }
                    DebugLog.i("prefetch", "warmed ${candidate.title}")
                }
            } finally {
                prefetchInFlight = false
            }
        }
    }

    // ── Online Search ─────────────────────────────────────────────────────────
    private val _onlineQuery   = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()
    fun setOnlineQuery(q: String) { _onlineQuery.value = q }

    private val _onlineResults = MutableStateFlow<List<OnlineResult>>(emptyList())
    val onlineResults: StateFlow<List<OnlineResult>> = _onlineResults.asStateFlow()

    // Typed search results — albums and playlists fetched in parallel with
    // the song results so the search screen can render
    // 'Albums · Playlists · Songs' (Spotify-style sectioned results).
    private val _albumResults = MutableStateFlow<List<com.beatdrop.kt.youtube.OnlineAlbum>>(emptyList())
    val albumResults: StateFlow<List<com.beatdrop.kt.youtube.OnlineAlbum>> = _albumResults.asStateFlow()
    private val _playlistResults = MutableStateFlow<List<com.beatdrop.kt.youtube.OnlinePlaylist>>(emptyList())
    val playlistResults: StateFlow<List<com.beatdrop.kt.youtube.OnlinePlaylist>> = _playlistResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _fetchingVideoId = MutableStateFlow<String?>(null)
    val fetchingVideoId: StateFlow<String?> = _fetchingVideoId.asStateFlow()

    private val _onlineMessage = MutableStateFlow<String?>(null)
    val onlineMessage: StateFlow<String?> = _onlineMessage.asStateFlow()
    fun clearOnlineMessage() { _onlineMessage.value = null }

    // Suggestions for the search bar
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    val searchConfigured: Boolean get() = OnlineSearch.isConfigured

    // ── Local library search ─────────────────────────────────────────────
    // Cheap in-memory filter over _tracks. The hybrid SearchScreen mode
    // calls this in parallel with runOnlineSearch so users see their own
    // library results above the YT catalog matches.
    private val _localSearchResults = MutableStateFlow<List<Track>>(emptyList())
    val localSearchResults: StateFlow<List<Track>> = _localSearchResults.asStateFlow()

    fun runLocalSearch(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _localSearchResults.value = emptyList()
            return
        }
        _localSearchResults.value = _tracks.value.asSequence()
            .filter { t ->
                t.title.contains(q, ignoreCase = true) ||
                    t.artist.contains(q, ignoreCase = true) ||
                    t.album.contains(q, ignoreCase = true)
            }
            .take(20)
            .toList()
    }

    fun runOnlineSearch() {
        val q = _onlineQuery.value.trim()
        if (q.isBlank()) return
        _isSearching.value = true; _suggestions.value = emptyList()
        // Reset typed results so the previous query's albums/playlists don't
        // linger while the new search is in flight.
        _albumResults.value = emptyList()
        _playlistResults.value = emptyList()
        DebugLog.i("search", "query=\"$q\" (provider configured=${OnlineSearch.isConfigured})")
        viewModelScope.launch {
            // Save search query in local history
            runCatching { prefs.addSearchQuery(q) }
            // Songs (primary) + Albums + Playlists fan-out in parallel; whoever
            // finishes first updates its own flow. The screen is responsible
            // for ordering them in the UI (Top result / Albums / Playlists /
            // Songs).  Albums + playlists are best-effort: any failure leaves
            // its flow empty rather than failing the whole search.
            val songsJob = async(Dispatchers.IO) {
                runCatching { OnlineSearch.provider.search(q) }.getOrElse {
                    DebugLog.e("search", "songs failed", it); emptyList()
                }
            }
            val albumsJob = async(Dispatchers.IO) {
                runCatching { com.beatdrop.kt.youtube.searchYoutubeMusicAlbums(q) }.getOrDefault(emptyList())
            }
            val playlistsJob = async(Dispatchers.IO) {
                runCatching { com.beatdrop.kt.youtube.searchYoutubeMusicPlaylists(q) }.getOrDefault(emptyList())
            }
            val res = songsJob.await()
            _onlineResults.value = res
            _albumResults.value = albumsJob.await()
            _playlistResults.value = playlistsJob.await()
            _isSearching.value = false
            DebugLog.i("search", "${res.size} songs · ${_albumResults.value.size} albums · ${_playlistResults.value.size} playlists")
            if (res.isEmpty() && _albumResults.value.isEmpty() && _playlistResults.value.isEmpty()) {
                // Surface a helpful message only if everything came back empty.
                _onlineMessage.value = "No results for \"$q\". Try different keywords."
            }
        }
    }

    /**
     * Open a YT Music album: fetches its track list (album = playlist with
     * OLAK5uy_ prefix) and starts playback at the first track. Used by the
     * OnlineAlbumScreen "Play" button and by tapping an album row.
     */
    fun playAlbum(album: com.beatdrop.kt.youtube.OnlineAlbum, startVideoId: String? = null) {
        playFeaturedPlaylist(album.audioPlaylistId, startVideoId)
    }

    fun loadSuggestions() {
        val q = _onlineQuery.value.trim()
        if (q.length < 2) { _suggestions.value = emptyList(); return }
        viewModelScope.launch {
            val s = runCatching { OnlineSearch.provider.suggestions(q) }.getOrElse { emptyList() }
            _suggestions.value = s
        }
    }

    // ── Live typed suggestions ──────────────────────────────────────────
    // A debounced parallel fetch of small song/album/playlist samples so
    // the search field can show rich, typed result rows while the user is
    // still typing — Apple-Music / Spotify-iOS pattern. The list is a flat
    // ordered set: top song, then top album, then top playlist, then a
    // few more songs. Tap any row → instant play (song) or open detail
    // (album / playlist). No 'press enter to search' step needed.
    sealed interface LiveSuggestion {
        val title: String
        val subtitle: String
        val thumbnailUrl: String?
        data class Song    (val result: OnlineResult)                                  : LiveSuggestion {
            override val title: String get() = result.title
            override val subtitle: String get() = "Song · ${result.author}"
            override val thumbnailUrl: String? get() = result.thumbnailUrl
        }
        data class Album   (val album: com.beatdrop.kt.youtube.OnlineAlbum)            : LiveSuggestion {
            override val title: String get() = album.title
            override val subtitle: String get() = buildString {
                append("Album")
                if (album.artist.isNotBlank()) append(" · ").append(album.artist)
            }
            override val thumbnailUrl: String? get() = album.thumbnailUrl
        }
        data class Playlist(val playlist: com.beatdrop.kt.youtube.OnlinePlaylist)      : LiveSuggestion {
            override val title: String get() = playlist.title
            override val subtitle: String get() = buildString {
                append("Playlist")
                if (playlist.author.isNotBlank()) append(" · ").append(playlist.author)
            }
            override val thumbnailUrl: String? get() = playlist.thumbnailUrl
        }
    }

    private val _liveSuggestions = MutableStateFlow<List<LiveSuggestion>>(emptyList())
    val liveSuggestions: StateFlow<List<LiveSuggestion>> = _liveSuggestions.asStateFlow()
    @Volatile private var liveSuggestionsJob: kotlinx.coroutines.Job? = null

    /**
     * Cancel any in-flight typed-suggestions fetch, wait 250 ms (debounce),
     * then fire songs + albums + playlists in parallel and assemble a
     * flat ordered list. Called from SearchScreen on every keystroke.
     */
    fun loadLiveSuggestions() {
        val q = _onlineQuery.value.trim()
        if (q.length < 2) {
            _liveSuggestions.value = emptyList()
            liveSuggestionsJob?.cancel()
            liveSuggestionsJob = null
            return
        }
        liveSuggestionsJob?.cancel()
        liveSuggestionsJob = viewModelScope.launch {
            delay(250)   // debounce
            val songsJob = async(Dispatchers.IO) {
                runCatching { com.beatdrop.kt.youtube.searchYoutubeMusic(q, maxResults = 6) }
                    .getOrDefault(emptyList())
            }
            val albumsJob = async(Dispatchers.IO) {
                runCatching { com.beatdrop.kt.youtube.searchYoutubeMusicAlbums(q, maxResults = 3) }
                    .getOrDefault(emptyList())
            }
            val playlistsJob = async(Dispatchers.IO) {
                runCatching { com.beatdrop.kt.youtube.searchYoutubeMusicPlaylists(q, maxResults = 3) }
                    .getOrDefault(emptyList())
            }
            val songs     = songsJob.await()
            val albums    = albumsJob.await()
            val playlists = playlistsJob.await()
            // Ordered: top song, top album, top playlist, then alternating
            // songs to fill out to ~8 rows total. Matches the inline-suggestion
            // pattern Spotify and Apple Music both use.
            val mixed = buildList {
                songs.firstOrNull()?.let { add(LiveSuggestion.Song(it)) }
                albums.firstOrNull()?.let { add(LiveSuggestion.Album(it)) }
                playlists.firstOrNull()?.let { add(LiveSuggestion.Playlist(it)) }
                songs.drop(1).take(5).forEach { add(LiveSuggestion.Song(it)) }
                albums.drop(1).take(2).forEach { add(LiveSuggestion.Album(it)) }
                playlists.drop(1).take(2).forEach { add(LiveSuggestion.Playlist(it)) }
            }
            _liveSuggestions.value = mixed
        }
    }

    // ── YouTube playback — resolve URL then play via ExoPlayer ────────────────
    // Cache of ytTrack objects so syncCurrent() can find them by mediaId
    // LRU-evicted at 50 entries to prevent unbounded memory growth
    private val _ytTrackCache = object : LinkedHashMap<String, Track>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Track>?): Boolean = size > 50
    }

    // Last failed online result — allows retry from UI
    private val _lastFailedOnline = MutableStateFlow<OnlineResult?>(null)
    val lastFailedOnline: StateFlow<OnlineResult?> = _lastFailedOnline.asStateFlow()

    // ── Online playback context (skip-next/skip-prev across a list) ─────────
    // ExoPlayer's seekToNextMediaItem only walks its own MediaItem list. For
    // online tracks we use setMediaItem (single item) because each one needs
    // a fresh resolve before it can be played. To make skip-forward/back work
    // we track the originating list (search results, trending grid, etc.)
    // here and override next()/prev() to advance through it, calling
    // prepareAndPlayOnline again on the new item.
    @Volatile private var onlineContext: List<OnlineResult> = emptyList()
    @Volatile private var onlineContextIndex: Int = -1

    /** Convenience: play a single online result (context=just this track).
     *  pushRecentOnline is called inside prepareAndPlayOnline(). */
    fun playOnline(result: OnlineResult) {
        prepareAndPlayOnline(result)
    }

    /** Retry the last failed online playback */
    fun retryOnlinePlay() {
        val result = _lastFailedOnline.value ?: return
        _lastFailedOnline.value = null
        prepareAndPlayOnline(result)
    }

    /**
     * Play an online result. When called with [context] (the surrounding list
     * the user tapped from — search results, a Trending grid, a Discover
     * carousel, etc.) and [contextIndex], the [next]/[prev] transport
     * controls will advance through that list, resolving each track on
     * demand. Without a context, skip-next/prev are no-ops for online tracks
     * (same as before).
     */
    fun prepareAndPlayOnline(
        result: OnlineResult,
        context: List<OnlineResult> = listOf(result),
        contextIndex: Int = context.indexOfFirst { it.videoId == result.videoId }.coerceAtLeast(0),
    ) {
        // ── Same-track guard (Mode B, loosened) ─────────────────────────────
        // If the user taps a result whose videoId matches the currently-
        // loaded track, do NOT cut audio or zero the seek bar. Instead:
        //   • Update onlineContext + onlineContextIndex so next/prev walk
        //     through the freshly-tapped surface and the lock-screen
        //     'Next' slot reflects what the user now expects.
        //   • If the player is paused, resume playback from the *current*
        //     position (no restart). This matches Spotify behaviour: tap
        //     a track you already had open → it just resumes.
        //   • If already playing, leave it alone.
        //   • Return early so the instant-cut block below never runs.
        //
        // The previous version of this guard required
        // controller.isPlaying == true, which meant tapping the same song
        // while paused fell through to the full restart path. Now any
        // 'same videoId' tap is honoured regardless of play/pause state.
        val currentVideoId = _current.value?.sourceVideoId
        val c = controller
        if (currentVideoId != null && currentVideoId == result.videoId && c != null) {
            onlineContext = context
            onlineContextIndex = context.indexOfFirst { it.videoId == result.videoId }
                .let { if (it >= 0) it else contextIndex.coerceIn(0, context.size - 1) }
            if (!c.isPlaying) {
                // Resume from wherever we were paused — never seek to 0.
                runCatching { c.play() }
                DebugLog.i("play",
                    "same-track guard: ${result.title} was paused — resumed @${c.currentPosition}ms, ctx size=${context.size}, idx=$onlineContextIndex")
            } else {
                DebugLog.i("play",
                    "same-track guard: ${result.title} already playing — context updated (size=${context.size}, idx=$onlineContextIndex)")
            }
            return
        }

        onlineContext = context
        onlineContextIndex = contextIndex
        DebugLog.i("play", "tap → \"${result.title}\" [${result.videoId}] (ctx=${context.size}, idx=$contextIndex)")

        // Kick off upcoming-tracks prefetch the moment the context is set,
        // even before the current track's URL has resolved. By the time
        // the user hits Next, the next 3 entries already have warm URLs.
        prefetchUpcomingFromContext()

        // ── Instant track switch ────────────────────────────────────────
        // Cut the previous track's audio and zero the seek bar BEFORE any
        // other UI state changes — otherwise the user briefly sees the
        // new track's title with the old track's position bar still
        // scrubbing along (the 'transition limbo' the user reported).
        cancelAutoMix()
        controller?.let {
            runCatching { it.stop() }
            runCatching { it.clearMediaItems() }
        }
        _position.value = 0L
        _bufferedPosition.value = 0L
        _activeLyric.value = -1
        _lyrics.value = emptyList()

        // Show track info in Now Playing instantly — no blocking overlay
        val tempTrack = Track(
            id = "yt_${result.videoId}",
            uri = Uri.EMPTY,
            title = result.title,
            artist = result.author,
            album = result.author,
            albumId = 0L,
            durationMs = result.durationSecs * 1000L,
            data = null,
            dateAdded = System.currentTimeMillis(),
            artworkOverride = result.thumbnailUrl,
        )
        _current.value = tempTrack
        _duration.value = result.durationSecs * 1000L   // show full bar length immediately
        _fetchingVideoId.value = result.videoId
        _lastFailedOnline.value = null
        lastRecoveredVideoId = null   // allow 403-recovery for this fresh play
        onlineTransitionInProgress = true

        viewModelScope.launch {
            val track = runCatching { youtubeResultToTrack(result) }.getOrElse { err ->
                val msg = when {
                    err.message?.contains("403") == true ->
                        "Playback blocked (403). YouTube may be restricting this content. Try another song."
                    err.message?.contains("timeout", true) == true ->
                        "Connection timed out. Check your internet and try again."
                    err.message?.contains("Could not load") == true ->
                        "No playable stream found. Try a different song."
                    else ->
                        "Couldn't stream this song: ${err.message}"
                }
                DebugLog.e("play", "resolve failed", err)
                _onlineMessage.value = msg
                _lastFailedOnline.value = result
                _fetchingVideoId.value = null
                onlineTransitionInProgress = false
                return@launch
            }
            _ytTrackCache[track.id] = track
            val c = controller ?: run {
                DebugLog.e("play", "controller is null — MediaController not connected yet")
                _fetchingVideoId.value = null
                onlineTransitionInProgress = false
                return@launch
            }
            DebugLog.i("play", "handing to ExoPlayer (ua=${track.streamUserAgent?.take(24)}…, hdrs=${track.streamHeaders.keys})")
            c.setMediaItem(track.toMediaItem()); c.prepare(); c.play()
            // Allow syncCurrent to run again — the new item is now active in ExoPlayer
            onlineTransitionInProgress = false
            _current.value = track; _duration.value = track.durationMs
            pushRecent("yt_${track.sourceVideoId}")
            pushRecentOnline(track.sourceVideoId ?: "", track.title, track.artist)
            loadLyrics(track)
            _fetchingVideoId.value = null
            // ── Predetermine + warm next track ──────────────────────────────
            // The moment a new online track starts, kick off resolution of
            // whatever OnlineSmartShuffle thinks should play next. By the
            // time STATE_ENDED fires, that URL is already in the 200 MB
            // playback cache → the auto-advance gap is <200 ms instead of
            // the 1-3 s a cold resolve would take. Cheap (deduped via
            // prefetchedVisibleIds), fire-and-forget.
            triggerNextOnlinePrefetch()
        }
    }

    /**
     * Pre-resolve the predicted next online track so STATE_ENDED hand-off
     * (or a user-initiated skip) is near-instant. Picks the next via
     * OnlineSmartShuffle, falling back to the literal next entry in
     * onlineContext if the scorer returns nothing.
     */
    private fun triggerNextOnlinePrefetch() {
        val cur = _current.value ?: return
        if (!cur.isStreaming || onlineContext.isEmpty()) return
        val curVid = cur.sourceVideoId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Smart-shuffle pick from the current pool (no likes — pure
            // content scoring per the new OnlineSmartShuffle).
            val curResult = onlineContext.firstOrNull { it.videoId == curVid }
                ?: OnlineResult(
                    videoId = curVid, title = cur.title, author = cur.artist,
                    thumbnailUrl = cur.artworkOverride,
                    durationText = "", durationSecs = (cur.durationMs / 1000).toInt(),
                )
            val nextPick = com.beatdrop.kt.playback.OnlineSmartShuffle.pickNext(
                current = curResult,
                pool = onlineContext,
                recentlyPlayedIds = onlineRecentlyPlayedIds.toSet(),
            )
            // Prefetch the next 3 LITERAL entries in onlineContext (covers
            // 'user hits Next twice quickly' — second Next gets a warm URL
            // too) AND the smart-shuffle pick (covers Auto-Mix). All four
            // are deduped + cap-limited via prefetchedVisibleIds so we
            // never issue duplicate getStream calls within the cache TTL.
            val literalNext3 = (1..3).mapNotNull { onlineContext.getOrNull(onlineContextIndex + it) }
            (listOfNotNull(nextPick) + literalNext3)
                .distinctBy { it.videoId }
                .take(4)
                .forEach { prefetchOnlineUrl(it.videoId) }
        }
    }

    /**
     * Kick off prefetching the moment a new onlineContext is set, even
     * BEFORE playback of the first track has started. By the time the
     * user taps Next for the first time, the second/third tracks already
     * have their URLs warm in the playback cache, so Next is near-instant
     * instead of the 200-800 ms cold-resolve it used to take.
     */
    private fun prefetchUpcomingFromContext() {
        if (onlineContext.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val upcoming = (1..3).mapNotNull {
                onlineContext.getOrNull(onlineContextIndex + it)
            }
            upcoming.distinctBy { it.videoId }
                .forEach { prefetchOnlineUrl(it.videoId) }
        }
    }

    fun playOnlineByMetadata(title: String, artist: String, coverUrl: String?) {
        viewModelScope.launch {
            val query = "$artist $title"
            val results = runCatching { OnlineSearch.provider.search(query) }.getOrNull()
            val bestMatch = results?.firstOrNull()
            if (bestMatch != null) {
                val cleanMatch = if (bestMatch.thumbnailUrl.isNullOrBlank()) {
                    bestMatch.copy(thumbnailUrl = coverUrl)
                } else bestMatch

                // Set temp track immediately so Now Playing shows the right info
                val tempTrack = Track(
                    id = "yt_${cleanMatch.videoId}",
                    uri = Uri.EMPTY,
                    title = title,
                    artist = artist,
                    album = artist,
                    albumId = 0L,
                    durationMs = 0L,
                    data = null,
                    dateAdded = System.currentTimeMillis(),
                    artworkOverride = coverUrl,
                )
                _current.value = tempTrack
                _fetchingVideoId.value = cleanMatch.videoId
                onlineTransitionInProgress = true
                
                val track = runCatching { youtubeResultToTrack(cleanMatch) }.getOrElse {
                    _onlineMessage.value = "Couldn't stream this song: ${it.message}"
                    _fetchingVideoId.value = null
                    onlineTransitionInProgress = false
                    return@launch
                }
                _ytTrackCache[track.id] = track
                val c = controller ?: run {
                    _fetchingVideoId.value = null
                    onlineTransitionInProgress = false
                    return@launch
                }
                c.setMediaItem(track.toMediaItem()); c.prepare(); c.play()
                onlineTransitionInProgress = false
                _current.value = track; _duration.value = track.durationMs
                pushRecent("yt_${track.sourceVideoId}")
                pushRecentOnline(track.sourceVideoId ?: "", track.title, track.artist)
                loadLyrics(track)
                _fetchingVideoId.value = null
            } else {
                _onlineMessage.value = "Could not find a stream for: $title"
            }
        }
    }

    // ── Downloads ─────────────────────────────────────────────────────────────
    // Delegate to DownloadManager — its CoroutineScope outlives this ViewModel
    // so downloads survive configuration changes and app backgrounding.
    // DownloadService (foreground) keeps the process alive for the full transfer.

    val downloadJobs: StateFlow<Map<String, DownloadJob>> = DownloadManager.jobs

    fun downloadJobFor(videoId: String): DownloadJob? = DownloadManager.jobs.value[videoId]

    /** Check if an online track has been downloaded locally */
    fun isOnlineDownloaded(videoId: String): Boolean {
        return _tracks.value.any { it.id == "dl_$videoId" }
    }

    /** Get the local Track for a downloaded online video, or null */
    fun downloadedTrackFor(videoId: String): Track? {
        return _tracks.value.firstOrNull { it.id == "dl_$videoId" }
    }

    fun downloadOnline(result: OnlineResult) {
        DownloadManager.enqueue(result, getApplication())
    }

    /**
     * Predictive prefetch for visible search results. Resolves [videoId]'s
     * stream URL in the background and lets it land in the 200 MB playback
     * cache so the *first* range request when the user taps is already
     * warmed → apparent tap-to-play latency drops to ~0 ms.
     *
     * Cheap: cached upstream (urlCache, 90-min TTL), guarded by a small
     * in-memory set so we never resolve the same id twice while the row
     * is on-screen.  Idempotent.  Fire-and-forget — errors are swallowed.
     */
    private val prefetchedVisibleIds = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    )
    fun prefetchOnlineUrl(videoId: String) {
        if (videoId.isBlank()) return
        if (!prefetchedVisibleIds.add(videoId)) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { com.beatdrop.kt.youtube.getStream(videoId) }
        }
    }

    /** Download with full metadata resolution — fetches accurate duration/title before enqueueing */
    fun downloadOnlineWithMetadata(videoId: String) {
        if (videoId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val track = _current.value
            val result = runCatching {
                OnlineSearch.provider.search("${track?.title} ${track?.artist}")
            }.getOrDefault(emptyList())
                .firstOrNull { it.videoId == videoId }
                ?.copy(thumbnailUrl = track?.artworkOverride)
                ?: com.beatdrop.kt.youtube.OnlineResult(
                    videoId = videoId,
                    title = track?.title ?: "",
                    author = track?.artist ?: "",
                    thumbnailUrl = track?.artworkOverride,
                    durationText = "",
                    durationSecs = ((track?.durationMs ?: 0L) / 1000L).toInt(),
                )
            DownloadManager.enqueue(result, getApplication())
        }
    }

    fun cancelDownload(videoId: String) {
        DownloadManager.cancel(videoId, getApplication())
    }

    fun retryDownload(result: OnlineResult) {
        DownloadManager.retry(result, getApplication())
    }

    // Observe completed downloads → add track to the local library immediately
    private fun observeDownloadCompletions() {
        DownloadManager.trackReady
            .onEach { track ->
                if (_tracks.value.none { it.id == track.id }) {
                    _tracks.value = _tracks.value + track
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Private Folder PIN ──────────────────────────────────────────────────────
    private val _privatePin = MutableStateFlow<String?>(null)
    val privatePin: StateFlow<String?> = _privatePin.asStateFlow()
    fun setPrivatePin(pin: String) {
        _privatePin.value = pin
        viewModelScope.launch { prefs.setPrivatePin(pin) }
    }

    // ── URL-based play/download (from clipboard, share menu, deep links) ───────
    /**
     * Play a video from a URL (YouTube etc.).
     * Extracts the video ID and delegates to playOnline.
     */
    fun playOnlineByUrl(url: String) {
        val detected = com.beatdrop.kt.util.ClipboardWatcher.parseUrl(url) ?: return
        if (detected.isPlaylist) return // Use PlaylistDownloadScreen for playlists
        val videoId = detected.videoId ?: return
        val result = com.beatdrop.kt.youtube.OnlineResult(
            videoId = videoId,
            title = "",
            author = detected.platform,
            thumbnailUrl = com.beatdrop.kt.youtube.ytThumbHd(videoId),
            durationText = "",
            durationSecs = 0,
            sourcePlatform = detected.platform,
        )
        // Start playing immediately — title/duration fill in once resolved.
        playOnline(result)
        // B11 fix: in the background, build a real "Up Next" context so the
        // user can skip after a paste. We wait briefly for the real title to
        // land (via _current updates from prepareAndPlayOnline), then search
        // on it. Previously this searched on a 5-char prefix of the literal
        // string "Loading…" which returned garbage.
        viewModelScope.launch(Dispatchers.IO) {
            // Poll _current for up to 8s waiting for the real title to populate.
            var seedTitle = ""
            var seedAuthor = ""
            repeat(16) {
                val cur = _current.value
                if (cur?.sourceVideoId == videoId && cur.title.isNotBlank() && cur.title != "Loading…") {
                    seedTitle = cur.title
                    seedAuthor = cur.artist
                    return@repeat
                }
                kotlinx.coroutines.delay(500)
            }
            val seedQuery = "$seedTitle $seedAuthor".trim()
            if (seedQuery.isBlank()) return@launch
            val related = runCatching { OnlineSearch.provider.search(seedQuery) }
                .getOrDefault(emptyList())
                .filter { it.videoId != videoId }
            if (related.isEmpty()) return@launch
            val context = (listOf(result.copy(title = seedTitle, author = seedAuthor)) + related)
                .distinctBy { it.videoId }
            onlineContext = context
            onlineContextIndex = 0
        }
    }

    /**
     * Download a video from a URL.
     */
    fun downloadOnlineByUrl(url: String) {
        val detected = com.beatdrop.kt.util.ClipboardWatcher.parseUrl(url) ?: return
        if (detected.isPlaylist) return
        val videoId = detected.videoId ?: return
        val result = com.beatdrop.kt.youtube.OnlineResult(
            videoId = videoId,
            title = "Downloading…",
            author = detected.platform,
            thumbnailUrl = com.beatdrop.kt.youtube.ytThumbHd(videoId),
            durationText = "",
            durationSecs = 0,
            sourcePlatform = detected.platform,
        )
        downloadOnline(result)
    }

    // ── Debug log (on-screen diagnostics) ──────────────────────────────────────
    val debugLog: StateFlow<List<DebugLog.Entry>> = DebugLog.entries
    fun clearDebugLog() = DebugLog.clear()
    fun dumpDebugLog(): String = DebugLog.dump()

    // ── Sleep timer ───────────────────────────────────────────────────────────
    private val _sleepMinutesLeft = MutableStateFlow(0)
    val sleepMinutesLeft: StateFlow<Int> = _sleepMinutesLeft.asStateFlow()
    private var sleepJob: kotlinx.coroutines.Job? = null

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) { _sleepMinutesLeft.value = 0; return }
        _sleepMinutesLeft.value = minutes
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) { delay(60_000); left--; _sleepMinutesLeft.value = left }
            controller?.pause()
        }
    }
    fun cancelSleepTimer() { sleepJob?.cancel(); _sleepMinutesLeft.value = 0 }

    override fun onCleared() {
        sleepJob?.cancel(); controller?.release(); deckB?.release(); super.onCleared()
    }
}

private fun Track.toMediaItem(): MediaItem {
    // For online streams (sourceVideoId != null) carry the resolving client's
    // UA + headers in a Base64URL fragment. PlaybackService's ResolvingDataSource
    // strips the fragment from the wire URI and re-applies the values as HTTP
    // request headers — necessary because googlevideo CDN URLs are bound to the
    // exact UA that resolved them (a mismatch is the classic 403 cause).
    //
    // Streams that don't need headers (local files, Piped proxies, Invidious
    // direct URLs) pass an empty header map and get a plain URI.
    val finalUri: android.net.Uri = run {
        if (sourceVideoId == null) return@run uri
        val hdrs = LinkedHashMap<String, String>()
        if (!streamUserAgent.isNullOrBlank()) hdrs[com.beatdrop.kt.playback.StreamHeaderCodec.userAgentKey()] = streamUserAgent!!
        hdrs.putAll(streamHeaders)
        if (hdrs.isEmpty()) return@run uri
        val frag = com.beatdrop.kt.playback.StreamHeaderCodec.encode(hdrs)
        if (frag.isBlank()) uri else uri.buildUpon().fragment(frag).build()
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(finalUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title).setArtist(artist).setAlbumTitle(album)
                .setArtworkUri(artworkUri).build()
        )
        .build()
}
