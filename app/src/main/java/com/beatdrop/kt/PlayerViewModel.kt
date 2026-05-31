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

    private val _sort = MutableStateFlow(SortMode.TITLE_ASC)
    val sort: StateFlow<SortMode> = _sort.asStateFlow()
    fun setSort(s: SortMode) { _sort.value = s }

    // ── Playback ──────────────────────────────────────────────────────────────
    private val _current   = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position  = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

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
    private val _haptics = MutableStateFlow(false)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()
    fun setHaptics(v: Boolean) { _haptics.value = v; viewModelScope.launch { prefs.setHaptics(v) } }

    private val _theme = MutableStateFlow("light")
    val theme: StateFlow<String> = _theme.asStateFlow()
    fun setTheme(v: String) { _theme.value = v; viewModelScope.launch { prefs.setTheme(v) } }

    private val _defaultShuffle = MutableStateFlow(false)
    val defaultShuffle: StateFlow<Boolean> = _defaultShuffle.asStateFlow()
    fun setDefaultShuffle(v: Boolean) { _defaultShuffle.value = v; viewModelScope.launch { prefs.setDefaultShuffle(v) } }

    private val _autoDjEnabled = MutableStateFlow(false)
    val autoDjEnabled: StateFlow<Boolean> = _autoDjEnabled.asStateFlow()
    fun setAutoDjEnabled(v: Boolean) { _autoDjEnabled.value = v; viewModelScope.launch { prefs.setAutoDj(v) } }
    @Volatile private var isCrossfading = false

    private fun observePrefs() {
        prefs.likedFlow.onEach { _liked.value = it }.launchIn(viewModelScope)
        prefs.playlistsFlow.onEach { _playlists.value = it }.launchIn(viewModelScope)
        prefs.playCountsFlow.onEach { _playCounts.value = it }.launchIn(viewModelScope)
        prefs.hapticsFlow.onEach { _haptics.value = it }.launchIn(viewModelScope)
        prefs.themeFlow.onEach { _theme.value = it }.launchIn(viewModelScope)
        prefs.defaultShuffleFlow.onEach { _defaultShuffle.value = it }.launchIn(viewModelScope)
        prefs.autoDjFlow.onEach { _autoDjEnabled.value = it }.launchIn(viewModelScope)
    }

    // ── Queue / shuffle / repeat ──────────────────────────────────────────────
    private val _queue   = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    fun toggleShuffle() { _shuffle.value = !_shuffle.value; controller?.shuffleModeEnabled = _shuffle.value }

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
            _tracks.value.firstOrNull { it.id == id }?.let { out.add(it) }
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
        observePrefs()
        val token = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        val future = MediaController.Builder(getApplication(), token).buildAsync()
        future.addListener({
            controller = future.get(); attach(); startTicker()
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun attach() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { _isPlaying.value = p }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) { syncCurrent() }
            override fun onTimelineChanged(t: Timeline, reason: Int) { refreshQueueFromController() }
            override fun onPlaybackStateChanged(state: Int) {
                _duration.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
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
                _tracks.value = batch
                if (!_loaded.value) _loaded.value = true
            }
        }
    }

    // ── Playback ──────────────────────────────────────────────────────────────
    fun play(track: Track) {
        val c = controller ?: return
        val list = filteredSorted()
        val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        c.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare(); c.play()
        _current.value = track; _duration.value = track.durationMs
        loadLyrics(track); refreshQueueFromController()
        viewModelScope.launch { prefs.incrementPlayCount(track.id) }
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

    fun togglePlay() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms); _position.value = ms }

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
                    if (dur > 0) {
                        _duration.value = dur
                        if (_autoDjEnabled.value && (dur - pos) <= 5000L && (dur - pos) > 1000L && !isCrossfading) {
                            triggerAutoDjTransition()
                        }
                    }
                    if (_lyrics.value.isNotEmpty())
                        _activeLyric.value = LrcParser.activeIndex(_lyrics.value, _position.value)
                    delay(120)
                } else delay(400)
            }
        }
    }

    private fun triggerAutoDjTransition() {
        if (isCrossfading) return
        isCrossfading = true
        viewModelScope.launch {
            val cur = _current.value ?: run { isCrossfading = false; return@launch }
            val list = _tracks.value
            if (list.isEmpty()) { isCrossfading = false; return@launch }
            val candidates = list.filter { it.id != cur.id }
            val nextTrack = candidates.firstOrNull { it.artist.equals(cur.artist, ignoreCase = true) }
                ?: candidates.firstOrNull { it.album.equals(cur.album, ignoreCase = true) }
                ?: candidates.randomOrNull()
                ?: cur
            loadDeckB(nextTrack)
            val steps = 40
            val delayMs = 100L // 4 seconds crossfade
            for (i in 0..steps) {
                val ratio = i.toFloat() / steps
                controller?.volume = 1f - ratio
                deckB?.volume = ratio
                delay(delayMs)
            }
            val c = controller
            if (c != null) {
                c.setMediaItem(nextTrack.toMediaItem())
                c.prepare()
                c.volume = 1f
                c.play()
                _current.value = nextTrack
                _duration.value = nextTrack.durationMs
                loadLyrics(nextTrack)
            }
            deckB?.pause()
            deckB?.volume = 0f
            isCrossfading = false
        }
    }

    // ── DJ Mode ───────────────────────────────────────────────────────────────
    private var deckB: ExoPlayer? = null
    private val _deckATrack   = MutableStateFlow<Track?>(null)
    val deckATrack: StateFlow<Track?> = _deckATrack.asStateFlow()
    private val _deckBTrack   = MutableStateFlow<Track?>(null)
    val deckBTrack: StateFlow<Track?> = _deckBTrack.asStateFlow()
    private val _deckAPlaying = MutableStateFlow(false)
    val deckAPlaying: StateFlow<Boolean> = _deckAPlaying.asStateFlow()
    private val _deckBPlaying = MutableStateFlow(false)
    val deckBPlaying: StateFlow<Boolean> = _deckBPlaying.asStateFlow()
    private val _crossfade    = MutableStateFlow(0f)
    val crossfade: StateFlow<Float> = _crossfade.asStateFlow()

    private fun ensureDeckB(): ExoPlayer {
        return deckB ?: ExoPlayer.Builder(getApplication()).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { _deckBPlaying.value = playing }
            })
            deckB = p
        }
    }
    fun loadDeckA(track: Track) { play(track); _deckATrack.value = track; _deckAPlaying.value = true; applyCrossfade(_crossfade.value) }
    fun loadDeckB(track: Track) {
        val p = ensureDeckB()
        p.setMediaItem(MediaItem.Builder().setMediaId(track.id).setUri(track.uri).build())
        p.prepare(); p.playWhenReady = true; _deckBTrack.value = track; applyCrossfade(_crossfade.value)
    }
    fun toggleDeckA() { val c = controller ?: return; if (c.isPlaying) c.pause() else c.play(); _deckAPlaying.value = c.isPlaying }
    fun toggleDeckB() { val p = deckB ?: return; p.playWhenReady = !p.isPlaying }
    fun setCrossfade(v: Float) { val x = v.coerceIn(0f, 1f); _crossfade.value = x; applyCrossfade(x) }
    private fun applyCrossfade(x: Float) { controller?.volume = (1f - x); deckB?.volume = x }

    // ── Online Search ─────────────────────────────────────────────────────────
    private val _onlineQuery   = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()
    fun setOnlineQuery(q: String) { _onlineQuery.value = q }

    private val _onlineResults = MutableStateFlow<List<OnlineResult>>(emptyList())
    val onlineResults: StateFlow<List<OnlineResult>> = _onlineResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isFetchingStream = MutableStateFlow(false)
    val isFetchingStream: StateFlow<Boolean> = _isFetchingStream.asStateFlow()

    private val _fetchingVideoId = MutableStateFlow<String?>(null)
    val fetchingVideoId: StateFlow<String?> = _fetchingVideoId.asStateFlow()

    private val _onlineMessage = MutableStateFlow<String?>(null)
    val onlineMessage: StateFlow<String?> = _onlineMessage.asStateFlow()
    fun clearOnlineMessage() { _onlineMessage.value = null }

    // Suggestions for the search bar
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    val searchConfigured: Boolean get() = OnlineSearch.isConfigured

    fun runOnlineSearch() {
        val q = _onlineQuery.value.trim()
        if (q.isBlank()) return
        _isSearching.value = true; _suggestions.value = emptyList()
        viewModelScope.launch {
            val res = runCatching { OnlineSearch.provider.search(q) }.getOrElse {
                _onlineMessage.value = "Couldn't search the catalog: ${it.message}"; emptyList()
            }
            _onlineResults.value = res; _isSearching.value = false
        }
    }

    fun loadSuggestions() {
        val q = _onlineQuery.value.trim()
        if (q.length < 2) { _suggestions.value = emptyList(); return }
        viewModelScope.launch {
            val s = runCatching { OnlineSearch.provider.suggestions(q) }.getOrElse { emptyList() }
            _suggestions.value = s
        }
    }

    // ── YouTube playback — resolve URL then play via ExoPlayer ────────────────
    // Cache of ytTrack objects so syncCurrent() can find them by mediaId
    private val _ytTrackCache = mutableMapOf<String, Track>()

    fun playOnline(result: OnlineResult) {
        viewModelScope.launch {
            _isFetchingStream.value = true
            _fetchingVideoId.value = result.videoId
            val track = runCatching { youtubeResultToTrack(result) }.getOrElse {
                _onlineMessage.value = "Couldn't stream this song: ${it.message}"
                _isFetchingStream.value = false; _fetchingVideoId.value = null; return@launch
            }
            _ytTrackCache[track.id] = track
            val c = controller ?: run { _isFetchingStream.value = false; _fetchingVideoId.value = null; return@launch }
            c.setMediaItem(track.toMediaItem()); c.prepare(); c.play()
            _current.value = track; _duration.value = track.durationMs
            loadLyrics(track)
            _isFetchingStream.value = false
            _fetchingVideoId.value = null
        }
    }

    fun playOnlineByMetadata(title: String, artist: String, coverUrl: String?) {
        viewModelScope.launch {
            _isFetchingStream.value = true
            val query = "$artist $title"
            val results = runCatching { OnlineSearch.provider.search(query) }.getOrNull()
            val bestMatch = results?.firstOrNull()
            if (bestMatch != null) {
                val cleanMatch = if (bestMatch.thumbnailUrl.isNullOrBlank()) {
                    bestMatch.copy(thumbnailUrl = coverUrl)
                } else bestMatch
                playOnline(cleanMatch)
            } else {
                _onlineMessage.value = "Could not find a stream for: $title"
                _isFetchingStream.value = false
            }
        }
    }

    // ── Downloads ─────────────────────────────────────────────────────────────
    private val _downloadJobs = MutableStateFlow<Map<String, DownloadJob>>(emptyMap())
    val downloadJobs: StateFlow<Map<String, DownloadJob>> = _downloadJobs.asStateFlow()

    fun downloadJobFor(videoId: String): DownloadJob? = _downloadJobs.value[videoId]

    private fun updateJob(job: DownloadJob) {
        _downloadJobs.value = _downloadJobs.value + (job.videoId to job)
    }

    fun downloadOnline(result: OnlineResult) {
        val existing = _downloadJobs.value[result.videoId]
        if (existing != null && existing.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)) return

        updateJob(DownloadJob(result.videoId, result.title, DownloadStatus.QUEUED))
        viewModelScope.launch(Dispatchers.IO) {
            updateJob(DownloadJob(result.videoId, result.title, DownloadStatus.DOWNLOADING))
            runCatching {
                downloadYoutubeTrack(result) { progress ->
                    updateJob(DownloadJob(result.videoId, result.title, DownloadStatus.DOWNLOADING, progress.percent))
                }
            }.onSuccess { track ->
                // Add downloaded track to library
                _tracks.value = _tracks.value + track
                updateJob(DownloadJob(result.videoId, result.title, DownloadStatus.COMPLETED, 100, track))
            }.onFailure { err ->
                updateJob(DownloadJob(result.videoId, result.title, DownloadStatus.FAILED, 0, null, err.message))
                _onlineMessage.value = "Couldn't save to library: ${err.message}"
            }
        }
    }

    fun cancelDownload(videoId: String) {
        // Mark as cancelled — OkHttp will clean up on next use
        _downloadJobs.value = _downloadJobs.value - videoId
    }

    fun retryDownload(result: OnlineResult) {
        _downloadJobs.value = _downloadJobs.value - result.videoId
        downloadOnline(result)
    }

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

private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title).setArtist(artist).setAlbumTitle(album)
                .setArtworkUri(artworkUri).build()
        )
        .build()
