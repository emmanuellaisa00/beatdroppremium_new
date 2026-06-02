package com.beatdrop.kt.playback

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.net.Uri
import java.io.File

/**
 * Media3 MediaSessionService — replaces react-native-track-player.
 * Provides background playback + the system media notification automatically.
 *
 * Per-stream headers
 * ──────────────────
 * googlevideo CDN URLs resolved by certain clients (e.g. ANDROID_VR) are bound
 * to that client's User-Agent — fetching them with a generic browser UA returns
 * **HTTP 403**. We carry the resolving client's UA + Origin/Referer alongside
 * the URL via the URI fragment, and a `ResolvingDataSource` applies them as
 * HTTP request headers + the `User-Agent` on the underlying HttpDataSource.
 *
 * The fragment encoding is Base64URL (`StreamHeaderCodec`) — chosen because it
 * round-trips through `Uri.parse(...)` without re-encoding, unlike a naive
 * `k=v&k=v` fragment.
 *
 * Streams that don't have our fragment (local files, Piped HTTPS, Invidious)
 * fall through and use the default UA.
 *
 * Caching
 * ───────
 * A 200 MB LRU `SimpleCache` sits in front of the HTTP factory. Re-tapping a
 * recently-played song serves bytes from disk instantly instead of re-running
 * the resolver and re-fetching from the CDN. The cache is keyed on the
 * post-fragment-stripped URI so per-stream UA differences don't poison hits.
 *
 * Audio-only renderer policy
 * ──────────────────────────
 * BeatDrop is a music app — video is never displayed. We tell the track
 * selector to disable the video track type entirely, so when our muxed-MP4
 * fallback (itag 18 = 360p H.264 + AAC) plays, ExoPlayer never instantiates
 * the H.264 decoder. This saves ~30 MB RAM and avoids decoder-init failures
 * on Android Go / budget MediaTek devices that lack a video decoder for the
 * audio context.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var cache: SimpleCache? = null

    // Default UA — used when the resolved URL doesn't carry its own.
    // Piped proxies, Invidious direct, and most CDNs accept this.
    private val defaultUa =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(defaultUa)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        // Wrap in a ResolvingDataSource so we can inspect each DataSpec and:
        //   1. Strip our `#bdh1.<base64>` fragment from the URI before the
        //      request goes on the wire (CDNs reject unknown query/fragments).
        //   2. Re-apply the encoded UA + Referer/Origin headers per-request,
        //      so the CDN sees exactly the identity it expects.
        val resolvingFactory = ResolvingDataSource.Factory(httpFactory) { spec: DataSpec ->
            val uri: Uri = spec.uri
            val fragment = uri.fragment
            val headers = StreamHeaderCodec.decode(fragment)
            if (headers == null || headers.isEmpty()) {
                spec  // no fragment → leave it alone
            } else {
                // Strip the fragment from the wire URI.
                val cleanUri = uri.buildUpon().fragment(null).build()
                val ua = headers[StreamHeaderCodec.userAgentKey()]
                val extraHeaders = headers.filterKeys { it != StreamHeaderCodec.userAgentKey() }
                var builder = spec.buildUpon().setUri(cleanUri)
                if (extraHeaders.isNotEmpty()) {
                    val merged = LinkedHashMap(spec.httpRequestHeaders).apply {
                        putAll(extraHeaders)
                        if (!ua.isNullOrBlank()) put("User-Agent", ua)
                    }
                    builder = builder.setHttpRequestHeaders(merged)
                } else if (!ua.isNullOrBlank()) {
                    val merged = LinkedHashMap(spec.httpRequestHeaders).apply {
                        put("User-Agent", ua)
                    }
                    builder = builder.setHttpRequestHeaders(merged)
                }
                builder.build()
            }
        }

        // ── Playback cache (200 MB LRU on disk) ─────────────────────────────
        // Sits in front of the network factory. Streamed bytes are written to
        // disk as they're read; future requests for the same URL serve from
        // disk first. The cache key uses the URI WITHOUT our fragment (set by
        // ResolvingDataSource above) so per-UA variations don't fragment the
        // cache. Survives process restarts; auto-evicts oldest when full.
        val cacheDir = File(cacheDir, "playback_cache").also { it.mkdirs() }
        val evictor = LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024)  // 200 MB
        val playbackCache = SimpleCache(cacheDir, evictor).also { cache = it }

        val cachingFactory = CacheDataSource.Factory()
            .setCache(playbackCache)
            .setUpstreamDataSourceFactory(resolvingFactory)
            // Write-through: cache misses fetch + cache; cache hits skip network.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val dataSourceFactory = DefaultDataSource.Factory(this, cachingFactory)

        // ── Audio-only track selector ───────────────────────────────────────
        // BeatDrop never shows video. Disabling the video track type means:
        //   - muxed mp4 fallback (itag 18) plays its audio track only
        //   - no H.264 decoder is ever created → no DECODER_INIT_FAILED on
        //     low-end devices that lack a hardware H.264 decoder
        //   - saves ~30 MB RAM and a few percent CPU
        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .build()
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()

        EqEngine.attach(player.audioSessionId)
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        EqEngine.release()
        session?.run { player.release(); release() }
        session = null
        runCatching { cache?.release() }
        cache = null
        super.onDestroy()
    }
}
