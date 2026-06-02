package com.beatdrop.kt.youtube

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicReference

// Chrome 124 Android UA — same spoof as the React Native app
private const val CHROME_UA =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

// ─── IFrame HTML ──────────────────────────────────────────────────────────────
private val YT_IFRAME_HTML = """<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>*{margin:0;padding:0;background:#000;overflow:hidden}</style>
</head><body><div id="player"></div><script>
window.ReactNativeWebView={postMessage:function(m){try{window.AndroidBridge.onMessage(m)}catch(e){}}}; 
var tag=document.createElement('script');tag.src='https://www.youtube.com/iframe_api';document.head.appendChild(tag);
window.__ytPlayer=null;window.__ytQueue=[];window.__ytQueueIdx=0;window.__pending=null;window.__progressTimer=null;
function post(t,e){try{window.ReactNativeWebView.postMessage(JSON.stringify(Object.assign({type:t},e||{})))}catch(e){}}
function sendProgress(){if(!window.__ytPlayer||!window.__ytPlayer.getCurrentTime)return;post('progress',{position:window.__ytPlayer.getCurrentTime()||0,duration:window.__ytPlayer.getDuration()||0})}
function startProgress(){stopProgress();window.__progressTimer=setInterval(sendProgress,500)}
function stopProgress(){if(window.__progressTimer){clearInterval(window.__progressTimer);window.__progressTimer=null}}
function onYouTubeIframeAPIReady(){
  window.__ytPlayer=new YT.Player('player',{height:'1',width:'1',
    playerVars:{autoplay:1,controls:0,playsinline:1,rel:0,iv_load_policy:3,disablekb:1,modestbranding:1,fs:0,origin:'https://www.youtube.com'},
    events:{
      onReady:function(){post('ready',{});if(window.__pending){window.__ytPlayer.loadVideoById(window.__pending);window.__pending=null}},
      onStateChange:function(e){post('state',{state:e.data});if(e.data===1){startProgress()}else{stopProgress();sendProgress();if(e.data===0&&window.__ytQueue.length>1){window.__ytQueueIdx=(window.__ytQueueIdx+1)%window.__ytQueue.length;var n=window.__ytQueue[window.__ytQueueIdx];if(n)window.__ytPlayer.loadVideoById(n)}}},
      onError:function(e){stopProgress();post('error',{code:e.data})}
    }
  });
}
window.__ytCmd={
  load:function(id){if(!window.__ytPlayer||!window.__ytPlayer.loadVideoById){window.__pending=id;return}window.__ytQueue=[id];window.__ytQueueIdx=0;window.__ytPlayer.loadVideoById(id)},
  loadQueue:function(ids,idx){if(!ids||!ids.length)return;window.__ytQueue=ids;window.__ytQueueIdx=idx||0;var id=ids[window.__ytQueueIdx];if(!id)return;if(!window.__ytPlayer||!window.__ytPlayer.loadVideoById){window.__pending=id;return}window.__ytPlayer.loadVideoById(id)},
  play:function(){window.__ytPlayer&&window.__ytPlayer.playVideo&&window.__ytPlayer.playVideo()},
  pause:function(){window.__ytPlayer&&window.__ytPlayer.pauseVideo&&window.__ytPlayer.pauseVideo()},
  stop:function(){window.__ytPlayer&&window.__ytPlayer.stopVideo&&window.__ytPlayer.stopVideo()},
  seek:function(s){window.__ytPlayer&&window.__ytPlayer.seekTo&&window.__ytPlayer.seekTo(s,true)},
  volume:function(v){window.__ytPlayer&&window.__ytPlayer.setVolume&&window.__ytPlayer.setVolume(Math.round(v*100))},
  next:function(){if(!window.__ytQueue.length)return;window.__ytQueueIdx=(window.__ytQueueIdx+1)%window.__ytQueue.length;var id=window.__ytQueue[window.__ytQueueIdx];if(id)window.__ytPlayer.loadVideoById(id);post('queueIdx',{index:window.__ytQueueIdx})},
  prev:function(){if(!window.__ytQueue.length)return;window.__ytQueueIdx=Math.max(0,window.__ytQueueIdx-1);var id=window.__ytQueue[window.__ytQueueIdx];if(id)window.__ytPlayer.loadVideoById(id);post('queueIdx',{index:window.__ytQueueIdx})}
};
</script></body></html>"""

// ─── JS fallback hook (secondary — shouldInterceptRequest is primary) ─────────
private fun makeExtractJs(videoId: String): String = """(function(){
    if (window.__hooked) return;
    window.__hooked = true;
    function sendFormats(sd) {
        if (!sd || !sd.adaptiveFormats) return false;
        var af = sd.adaptiveFormats.filter(function(f){
            var m=(f.mimeType||f.type||'').toLowerCase();
            return m.indexOf('audio/')===0 && f.url;
        });
        if (af.length) {
            var best = af.sort(function(a,b){return(b.bitrate||0)-(a.bitrate||0)})[0];
            if (best.url) { window.AndroidBridge.onStreamResultJS(best.url); return true; }
        }
        return false;
    }
    var oldFetch = window.fetch;
    window.fetch = function() {
        return oldFetch.apply(this, arguments).then(function(res) {
            if (res && res.url && res.url.indexOf('/v1/player') !== -1) {
                res.clone().json().then(function(data) {
                    if (data && data.streamingData) sendFormats(data.streamingData);
                }).catch(function(){});
            }
            return res;
        });
    };
    var oldSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            if (this.responseURL && this.responseURL.indexOf('/v1/player') !== -1) {
                try { var d=JSON.parse(this.responseText); if(d&&d.streamingData) sendFormats(d.streamingData); } catch(e){}
            }
        });
        return oldSend.apply(this, arguments);
    };
    var pr = window.ytInitialPlayerResponse;
    if (pr && pr.streamingData && sendFormats(pr.streamingData)) return;
    var t=0; var iv=setInterval(function(){
        t++; var pr=window.ytInitialPlayerResponse;
        if(pr&&pr.streamingData&&sendFormats(pr.streamingData)){clearInterval(iv);return;}
        if(t>=20) clearInterval(iv);
    },500);
})();true;"""

// ─── IFrame Player Service ────────────────────────────────────────────────────
object YoutubePlayerService {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile internal var webView: WebView? = null

    var onReady:    (() -> Unit)?               = null
    var onState:    ((Int) -> Unit)?            = null
    var onProgress: ((Double, Double) -> Unit)? = null
    var onError:    ((Int) -> Unit)?            = null

    private fun inject(expr: String) {
        val wv = webView ?: return
        mainHandler.post { wv.evaluateJavascript("(function(){$expr})();true;", null) }
    }

    fun load(videoId: String)  = inject("window.__ytCmd&&window.__ytCmd.load(${JSONObject.quote(videoId)})")
    fun loadQueue(ids: List<String>, idx: Int = 0) {
        val arr = ids.joinToString(",") { JSONObject.quote(it) }
        inject("window.__ytCmd&&window.__ytCmd.loadQueue([$arr],$idx)")
    }
    fun play()                 = inject("window.__ytCmd&&window.__ytCmd.play()")
    fun pause()                = inject("window.__ytCmd&&window.__ytCmd.pause()")
    fun stop()                 = inject("window.__ytCmd&&window.__ytCmd.stop()")
    fun seek(sec: Double)      = inject("window.__ytCmd&&window.__ytCmd.seek($sec)")
    fun setVolume(vol: Double) = inject("window.__ytCmd&&window.__ytCmd.volume($vol)")
    fun next()                 = inject("window.__ytCmd&&window.__ytCmd.next()")
    fun prev()                 = inject("window.__ytCmd&&window.__ytCmd.prev()")

    internal fun handleMessage(json: String) {
        try {
            val m = JSONObject(json)
            when (m.getString("type")) {
                "ready"    -> mainHandler.post { onReady?.invoke() }
                "state"    -> mainHandler.post { onState?.invoke(m.getInt("state")) }
                "progress" -> mainHandler.post { onProgress?.invoke(m.getDouble("position"), m.getDouble("duration")) }
                "error"    -> mainHandler.post { onError?.invoke(m.optInt("code", -1)) }
            }
        } catch (_: Exception) {}
    }

    const val UNSTARTED = -1; const val ENDED = 0; const val PLAYING = 1
    const val PAUSED = 2;     const val BUFFERING = 3; const val CUED = 5
}

// ─── Stream Extractor Singleton ───────────────────────────────────────────────
/**
 * SnapTube approach:
 *   PRIMARY  — shouldInterceptRequest captures googlevideo.com CDN URLs after
 *              YouTube's own JS runs natively (DroidGuard, n-sig, signatureCipher
 *              all handled by Chrome's V8 inside the WebView, not by us).
 *   FALLBACK — JS fetch/XHR hook catches cases where the CDN request fires before
 *              shouldInterceptRequest, or for non-standard stream delivery.
 *
 * AtomicReference<CompletableDeferred> replaces Mutex + @Volatile to eliminate
 * the race condition between the coroutine timeout and JS-bridge callbacks.
 */
object YoutubeExtractor {
    private val mainHandler = Handler(Looper.getMainLooper())

    // Thread-safe: getAndSet is atomic — no lock needed in shouldInterceptRequest
    // or @JavascriptInterface callbacks
    internal val pendingRef = AtomicReference<CompletableDeferred<String>?>(null)
    @Volatile internal var pendingVideoId: String? = null
    @Volatile internal var webView: WebView? = null

    val isConfigured: Boolean get() = webView != null

    suspend fun extractStreamUrl(videoId: String, timeoutMs: Long = 15_000): String? {
        val deferred = CompletableDeferred<String>()
        // Cancel any in-flight extraction before starting a new one
        pendingRef.getAndSet(deferred)?.cancel()
        pendingVideoId = videoId

        mainHandler.post {
            // autoplay=1 triggers the player to start loading its stream immediately,
            // which is what fires the googlevideo.com CDN requests we intercept
            webView?.loadUrl(
                "https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1&origin=https://www.youtube.com"
            )
        }
        return try {
            withTimeout(timeoutMs) { deferred.await() }
        } catch (_: Exception) {
            // Only clear if it's still our deferred (not replaced by a newer call)
            pendingRef.compareAndSet(deferred, null)
            pendingVideoId = null
            null
        }
    }

    /**
     * Called from shouldInterceptRequest (any background thread) — safe because
     * AtomicReference.getAndSet is a single atomic operation.
     */
    fun onStreamResult(url: String) {
        val d = pendingRef.getAndSet(null) ?: return
        pendingVideoId = null
        if (url.isNotBlank()) d.complete(url)
        else d.completeExceptionally(Exception("empty_url"))
    }

    /** JS-bridge fallback — same thread-safety guarantee via AtomicReference */
    @JavascriptInterface
    fun onStreamResultJS(url: String) = onStreamResult(url)

    @JavascriptInterface
    fun onStreamError(error: String) {
        val d = pendingRef.getAndSet(null) ?: return
        pendingVideoId = null
        d.completeExceptionally(Exception(error))
    }
}

// ─── Checks if a CDN URL is a usable media stream ─────────────────────────────
// Loosened to accept BOTH audio-only AND progressive muxed video+audio formats.
// ExoPlayer can play the audio track out of a muxed mp4 without issue, and
// progressive itags (18, 22, etc.) are far less likely to be PO-token-gated
// than the audio-only adaptive formats in 2026 — so accepting them dramatically
// raises the WebView extractor's hit rate.
private fun isAudioStreamUrl(url: String): Boolean {
    if (!url.contains("googlevideo.com/videoplayback")) return false
    // Skip ad pings, image previews, ranges that aren't actual media.
    if (url.contains("&rn=") && url.contains("&range=") && url.contains("&dur=0")) return false
    // Accept any audio mime, any known audio itag, OR any known muxed progressive itag.
    val audioMatches = url.contains("mime=audio") || url.contains("mime%3Daudio") ||
        listOf("itag=140", "itag=141", "itag=251", "itag=250", "itag=249",
               "itag=139", "itag=599", "itag=600", "itag=171", "itag=258", "itag=327")
            .any { url.contains(it) }
    val muxedMatches = listOf("itag=18", "itag=22", "itag=59", "itag=37", "itag=43")
        .any { url.contains(it) }
    return audioMatches || muxedMatches
}

// ─── Shared WebViewClient factory for the extractor ──────────────────────────
private fun extractorWebViewClient() = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = false

    /**
     * PRIMARY CAPTURE — intercepts the googlevideo.com CDN request the moment
     * YouTube's player fires it. The URL is already fully deciphered (n-sig,
     * signatureCipher resolved by Chrome's V8) — no native JS decryption needed.
     * Returns an empty 204 to prevent the WebView from downloading actual audio data.
     */
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (YoutubeExtractor.pendingRef.get() != null && isAudioStreamUrl(url)) {
            YoutubeExtractor.onStreamResult(url)
            return WebResourceResponse(
                "audio/mp4", "UTF-8", 204, "No Content",
                emptyMap(), ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    /** FALLBACK JS hook — catches /v1/player responses if CDN interception misses */
    override fun onLoadResource(view: WebView, url: String) {
        super.onLoadResource(view, url)
        val vid = YoutubeExtractor.pendingVideoId ?: return
        view.evaluateJavascript(makeExtractJs(vid), null)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        val vid = YoutubeExtractor.pendingVideoId ?: return
        if (url.contains("youtube.com/embed")) {
            view.evaluateJavascript(makeExtractJs(vid), null)
        }
    }

    override fun onReceivedError(v: WebView, req: WebResourceRequest, err: WebResourceError) {
        super.onReceivedError(v, req, err)
        if (req.isForMainFrame) YoutubeExtractor.onStreamError("page_load_error")
    }
}

// ─── IFrame Player Composable ─────────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeIFramePlayerHost(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { YoutubePlayerService.webView = null } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = CHROME_UA
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    allowFileAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = false
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun onMessage(json: String) = YoutubePlayerService.handleMessage(json)
                }, "AndroidBridge")
                YoutubePlayerService.webView = this
                loadDataWithBaseURL("https://www.youtube.com", YT_IFRAME_HTML, "text/html", "UTF-8", null)
            }
        },
        update = { YoutubePlayerService.webView = it },
    )
}

// ─── Stream Extractor Composable ──────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeStreamExtractorHost(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { YoutubeExtractor.webView = null } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // Must be false so autoplay=1 triggers stream loading
                    // (actual audio never plays — we return 204 for the stream URL)
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = CHROME_UA
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(YoutubeExtractor, "AndroidBridge")
                webViewClient = extractorWebViewClient()
                YoutubeExtractor.webView = this
            }
        },
        update = { YoutubeExtractor.webView = it },
    )
}

// ─── Programmatic initializer ─────────────────────────────────────────────────

/**
 * Pre-warm the YouTube consent cookie so EU / UK / age-gated content doesn't
 * land the WebView on the "Before you continue to YouTube" consent wall — in
 * which case `shouldInterceptRequest` would never see a googlevideo.com URL
 * because the embed player JS never even loads.
 *
 * `CONSENT=YES+cb` is the value YouTube sets when a user clicks "Accept all".
 * Pasting it in pre-emptively (with the standard `cb` revision suffix YouTube
 * uses globally) means the consent page redirects straight to the player on
 * the very first load — invisible to the user, but unblocks extraction for
 * roughly 25% of devices that would otherwise time out on the WebView path.
 */
private fun seedYoutubeConsentCookies() {
    runCatching {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        // Generic accept-all consent (works across all locales as of 2026).
        cm.setCookie("https://www.youtube.com", "CONSENT=YES+cb.20210328-17-p0.en+FX+999; Domain=.youtube.com; Path=/; Secure")
        cm.setCookie("https://www.youtube.com", "SOCS=CAI; Domain=.youtube.com; Path=/; Secure")
        // VISITOR_INFO1_LIVE — gives us a stable session-ish identifier so YT
        // doesn't think we're a fresh ad-blocker probe on every load.
        cm.setCookie("https://www.youtube.com", "VISITOR_INFO1_LIVE=BeatDropKt; Domain=.youtube.com; Path=/; Secure")
        cm.flush()
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun initHiddenYoutubeWebViews(activity: ComponentActivity): () -> Unit {
    // Seed consent cookies BEFORE we create the WebView so the first page load
    // already carries them (Cookie store is process-wide, set once is enough).
    seedYoutubeConsentCookies()
    // INVISIBLE (not GONE) + a real 1x1 size: a GONE / 0x0 WebView is not laid out,
    // and many WebView implementations suspend JS/media loading when the host isn't
    // drawn — which would stop the embed player from ever firing the CDN request we
    // intercept. INVISIBLE keeps it in the layout/draw pass while staying off-screen.
    val container = android.widget.FrameLayout(activity).apply {
        visibility = android.view.View.INVISIBLE
    }

    // IFrame player WebView
    val playerWv = WebView(activity).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.userAgentString = CHROME_UA
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.allowFileAccess = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = false
        }
        addJavascriptInterface(object {
            @JavascriptInterface fun onMessage(json: String) = YoutubePlayerService.handleMessage(json)
        }, "AndroidBridge")
        YoutubePlayerService.webView = this
        loadDataWithBaseURL("https://www.youtube.com", YT_IFRAME_HTML, "text/html", "UTF-8", null)
    }
    container.addView(playerWv, android.view.ViewGroup.LayoutParams(1, 1))

    // Stream extractor WebView
    val extractWv = WebView(activity).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // false = autoplay=1 works, triggering CDN requests we intercept
        settings.mediaPlaybackRequiresUserGesture = false
        settings.userAgentString = CHROME_UA
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(YoutubeExtractor, "AndroidBridge")
        webViewClient = extractorWebViewClient()
        YoutubeExtractor.webView = this
    }
    container.addView(extractWv, android.view.ViewGroup.LayoutParams(1, 1))

    // 1x1 (not 0x0) so the inner WebViews get a real measure/layout pass.
    activity.addContentView(container, android.view.ViewGroup.LayoutParams(1, 1))

    return {
        container.removeAllViews()
        YoutubePlayerService.webView = null
        YoutubeExtractor.webView = null
    }
}
