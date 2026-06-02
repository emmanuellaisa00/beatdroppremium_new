package com.beatdrop.kt.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 MediaSessionService — replaces react-native-track-player.
 * Provides background playback + the system media notification automatically.
 *
 * googlevideo CDN URLs are bound to the client (User-Agent / Origin / Referer)
 * that resolved them; replaying a mismatched UA causes a 403. The ViewModel
 * encodes the resolving client's UA + headers into the stream URI's fragment
 * (#bdua=...&bdh_Referer=...). A [ResolvingDataSource] here parses that fragment,
 * strips it from the URL, and re-applies the values as HTTP request headers.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    private val fallbackUa =
        "com.google.android.apps.youtube.vr.oculus/1.57.29 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(fallbackUa)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)

        val baseFactory = DefaultDataSource.Factory(this, httpFactory)

        val resolvingFactory = ResolvingDataSource.Factory(baseFactory) { dataSpec ->
            applyStreamHeaders(dataSpec)
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()

        EqEngine.attach(player.audioSessionId)
        session = MediaSession.Builder(this, player).build()
    }

    /**
     * Reads the #bd... fragment from the URI, removes it, and turns the encoded
     * key/values into request headers (incl. User-Agent override) on the DataSpec.
     */
    private fun applyStreamHeaders(spec: DataSpec): DataSpec {
        val uri = spec.uri
        val fragment = uri.fragment ?: return spec
        if (!fragment.contains("bdua=") && !fragment.contains("bdh_")) return spec

        val headers = HashMap<String, String>(spec.httpRequestHeaders)
        fragment.split('&').forEach { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) return@forEach
            val k = Uri.decode(pair.substring(0, eq))
            val v = Uri.decode(pair.substring(eq + 1))
            when {
                k == "bdua" -> headers["User-Agent"] = v
                k.startsWith("bdh_") -> headers[k.removePrefix("bdh_")] = v
            }
        }

        // Strip our fragment so the CDN sees a clean URL.
        val cleanUri: Uri = uri.buildUpon().fragment(null).build()
        return spec.buildUpon()
            .setUri(cleanUri)
            .setHttpRequestHeaders(headers)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        EqEngine.release()
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
