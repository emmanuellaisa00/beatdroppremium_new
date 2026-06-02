package com.beatdrop.kt.youtube

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Function as RhinoFunction
import org.mozilla.javascript.ScriptableObject
import java.util.concurrent.TimeUnit

/**
 * Native YouTube signature + n-throttle decipher.
 *
 * Most YouTube formats in 2024-2026 ship a `signatureCipher` (the URL signature
 * is obfuscated) and/or an `n` query parameter that is throttled unless it is
 * transformed by a function defined in the player's `base.js`. yt-dlp / NewPipe
 * solve this by extracting those two JS functions from base.js and running them.
 *
 * We do the same: download base.js once (cached per player version), extract the
 * `sig` transform and the `nsig` transform as standalone JS snippets, and
 * evaluate them with Mozilla Rhino. This lets us accept ciphered formats that the
 * pure-Kotlin path previously had to discard.
 */
object YoutubeCipher {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()

    // Cached, compiled-from-source JS functions keyed by player JS URL.
    private data class PlayerFns(val sigJs: String?, val nsigJs: String?)
    @Volatile private var cachedUrl: String? = null
    @Volatile private var cachedFns: PlayerFns? = null

    // nsig results are deterministic per (player, n) → cache to avoid re-running JS
    private val nCache = HashMap<String, String>()

    /**
     * Resolve a single format object into a directly-playable URL.
     * Handles three cases:
     *   1. Plain `url` (no cipher)            → return as-is (after nsig fix)
     *   2. `signatureCipher` / `cipher`       → decipher signature, then nsig fix
     * Returns null if it cannot be resolved.
     */
    suspend fun resolveFormatUrl(format: org.json.JSONObject): String? {
        val plain = format.optString("url").ifBlank { null }
        val cipher = format.optString("signatureCipher")
            .ifBlank { format.optString("cipher").ifBlank { null } }

        val baseUrl: String
        val sigParam: String?
        val sigValue: String?

        if (plain != null) {
            baseUrl = plain
            sigParam = null
            sigValue = null
        } else if (cipher != null) {
            val params = parseQuery(cipher)
            val u = params["url"] ?: return null
            val s = params["s"]            // the obfuscated signature
            val sp = params["sp"] ?: "signature"  // query key the signature goes into
            val decoded = if (s != null) decipherSignature(s) ?: return null else null
            baseUrl = u
            sigParam = sp
            sigValue = decoded
        } else {
            return null
        }

        var finalUrl = baseUrl
        if (sigValue != null && sigParam != null) {
            finalUrl = appendQuery(finalUrl, sigParam, sigValue)
        }

        // Fix the n-throttle parameter so the CDN doesn't rate-limit us to a crawl.
        finalUrl = fixNParam(finalUrl)
        return finalUrl
    }

    /** Pre-warm the cipher for a given player JS URL (called once per resolution). */
    suspend fun ensurePlayer(playerJsUrl: String) {
        if (cachedUrl == playerJsUrl && cachedFns != null) return
        mutex.withLock {
            if (cachedUrl == playerJsUrl && cachedFns != null) return
            val js = downloadBaseJs(playerJsUrl) ?: return
            val fns = PlayerFns(
                sigJs = extractSigFunction(js),
                nsigJs = extractNSigFunction(js),
            )
            cachedFns = fns
            cachedUrl = playerJsUrl
            nCache.clear()
        }
    }

    // ── Signature decipher ────────────────────────────────────────────────────
    private suspend fun decipherSignature(s: String): String? = withContext(Dispatchers.Default) {
        val sigJs = cachedFns?.sigJs ?: return@withContext null
        runJs(sigJs, "beatdropSig", s)
    }

    // ── n-throttle transform ──────────────────────────────────────────────────
    private suspend fun fixNParam(url: String): String = withContext(Dispatchers.Default) {
        val nsigJs = cachedFns?.nsigJs ?: return@withContext url
        val n = Uri.parse(url).getQueryParameter("n") ?: return@withContext url
        val key = (cachedUrl ?: "") + "|" + n
        val transformed = synchronized(nCache) { nCache[key] }
            ?: runJs(nsigJs, "beatdropNsig", n)?.also {
                synchronized(nCache) { nCache[key] = it }
            }
            ?: return@withContext url
        if (transformed.isBlank() || transformed.startsWith("enhanced_except")) return@withContext url
        appendQuery(stripQuery(url, "n"), "n", transformed)
    }

    // ── base.js download ──────────────────────────────────────────────────────
    private fun downloadBaseJs(playerJsUrl: String): String? = try {
        val url = if (playerJsUrl.startsWith("http")) playerJsUrl
        else "https://www.youtube.com$playerJsUrl"
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (_: Exception) { null }

    /**
     * Discover the player JS URL. Two strategies:
     *   1. Scrape the embed/watch page for "/s/player/<hash>/.../base.js".
     *   2. Fall back to a known-good recent player path.
     */
    suspend fun discoverPlayerJsUrl(): String? = withContext(Dispatchers.Default) {
        runCatching {
            val pageReq = Request.Builder()
                .url("https://www.youtube.com/iframe_api")
                .build()
            val body = http.newCall(pageReq).execute().use { it.body?.string() } ?: return@runCatching null
            val hash = Regex("player\\\\?/([0-9a-fA-F]{8,})\\\\?/").find(body)?.groupValues?.get(1)
                ?: Regex("player/([0-9a-fA-F]{8,})/").find(body)?.groupValues?.get(1)
            if (hash != null) "https://www.youtube.com/s/player/$hash/player_ias.vflset/en_US/base.js" else null
        }.getOrNull()
    }

    // ── JS function extraction (yt-dlp-style regexes) ─────────────────────────

    /**
     * Extract the signature-decipher function and its helper object as runnable JS.
     *
     * Uses the current yt-dlp signature-function discovery patterns. NOTE: YouTube's
     * 2026 player obfuscation frequently defeats pure-regex extraction (yt-dlp itself
     * has moved to a full JS runtime for this reason). This path is therefore a
     * best-effort *secondary*; the WebView extractor (V8) is the robust primary.
     */
    private fun extractSigFunction(js: String): String? {
        // Find the name of the function that takes the obfuscated sig.
        val sigFnName =
            // a=a.split("");...;return a.join("")  (classic)
            Regex("""(?:\b|[^a-zA-Z0-9${'$'}])([a-zA-Z0-9${'$'}]{2,})\s*=\s*function\(\s*([a-zA-Z0-9${'$'}]+)\s*\)\s*\{\s*\2\s*=\s*\2\.split\(\s*""\s*\)""")
                .find(js)?.groupValues?.get(1)
            // ...&&(b=NAME(decodeURIComponent(b)))  /  c&&d.set(x,(0,NAME)(...))
            ?: Regex("""\b[a-zA-Z0-9${'$'}]+\s*&&\s*[a-zA-Z0-9${'$'}]+\.set\([^,]+,\s*(?:\([^)]*\)\s*)?\(?\s*0?,?\s*([a-zA-Z0-9${'$'}]+)\s*\)?\(""")
                .find(js)?.groupValues?.get(1)?.ifBlank { null }
            ?: Regex("""(?:["'])?([a-zA-Z0-9${'$'}]+)(?:["'])?\s*:\s*function\(\s*[a-zA-Z]\s*\)\s*\{\s*[a-zA-Z]\s*=\s*[a-zA-Z]\.split\(\s*""\s*\)""")
                .find(js)?.groupValues?.get(1)
            ?: findSigNameViaCaller(js)
            ?: return null

        val fnBody = extractFunctionBody(js, sigFnName) ?: return null
        // The decipher fn references a helper object like `Xy.AB(a,3)` — extract it.
        val helperName = Regex(""";([a-zA-Z0-9${'$'}]{2,})\.""").find(fnBody)?.groupValues?.get(1)
        val helperObj = helperName?.let { extractObject(js, it) } ?: ""

        return buildSigRunner(helperObj, sigFnName, fnBody)
    }

    private fun buildSigRunner(helperObj: String, name: String, body: String): String = """
        $helperObj
        function $name${body}
        function beatdropSig(s){ return $name(s); }
    """.trimIndent()

    private fun findSigNameViaCaller(js: String): String? {
        val caller = Regex("""\b([a-zA-Z0-9${'$'}]+)\s*\(\s*decodeURIComponent""").find(js)
        return caller?.groupValues?.get(1)
    }

    /** Extract the n-throttle transform function as runnable JS (current yt-dlp patterns). */
    private fun extractNSigFunction(js: String): String? {
        val nFnName =
            // a.get("n"))&&(b=NAME[idx](b)   /   c=a.get(b))&&(c=narray[idx](c)
            Regex("""(?:\.get\(\s*"n"\s*\)\s*\)|=\s*[a-zA-Z]\.get\([a-zA-Z]\)\s*\))\s*&&\s*\(\s*[a-zA-Z]\s*=\s*([a-zA-Z0-9${'$'}]+)(?:\[(\d+)\])?\(""")
                .find(js)?.let { m ->
                    val raw = m.groupValues[1]
                    val idx = m.groupValues.getOrNull(2)
                    if (!idx.isNullOrBlank()) resolveArrayRef(js, raw, idx.toInt()) else raw
                }
            // var NAME=function(a){var b=a.split("")...
            ?: Regex("""([a-zA-Z0-9${'$'}]+)\s*=\s*function\(\s*[a-zA-Z]\s*\)\s*\{\s*var\s*[a-zA-Z]\s*=\s*[a-zA-Z]\.split\(""")
                .find(js)?.groupValues?.get(1)
            // ;NAME=function(a){...  preceded by enhanced_except marker nearby
            ?: Regex(""";\s*([a-zA-Z0-9_${'$'}]+)\s*=\s*function\([a-zA-Z0-9_${'$'}]+\)\s*\{[^{}]*enhanced_except""")
                .find(js)?.groupValues?.get(1)
            ?: return null

        val body = extractFunctionBody(js, nFnName) ?: return null
        return """
            function $nFnName${body}
            function beatdropNsig(n){ try { return $nFnName(n); } catch(e){ return n; } }
        """.trimIndent()
    }

    private fun resolveArrayRef(js: String, arrName: String, idx: Int): String? {
        val arr = Regex("""var\s+${Regex.escape(arrName)}\s*=\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(js)?.groupValues?.get(1) ?: return null
        val items = arr.split(',').map { it.trim() }
        return items.getOrNull(idx)
    }

    /**
     * Returns the full "(args){...}" body for a named function, regardless of
     * declaration style (function f(){}, f=function(){}, var f=function(){},
     * or "f":function(){} inside an object literal).
     */
    private fun extractFunctionBody(js: String, name: String): String? {
        val escaped = Regex.escape(name)
        val patterns = listOf(
            Regex("""function\s+$escaped\s*\("""),
            Regex("""\b$escaped\s*=\s*function\s*\("""),
            Regex("""var\s+$escaped\s*=\s*function\s*\("""),
            Regex("""["']?$escaped["']?\s*:\s*function\s*\("""),
        )
        for (re in patterns) {
            val m = re.find(js) ?: continue
            // Locate the parameter list "(" that this match ends on.
            val parenOpen = js.indexOf('(', m.range.first)
            if (parenOpen < 0) continue
            val parenClose = js.indexOf(')', parenOpen)
            if (parenClose < 0) continue
            val braceOpen = js.indexOf('{', parenClose)
            if (braceOpen < 0) continue
            val braceClose = matchBrace(js, braceOpen) ?: continue
            val args = js.substring(parenOpen, parenClose + 1)
            return "$args${js.substring(braceOpen, braceClose + 1)}"
        }
        return null
    }

    /** Extract a helper object literal `var X={...};` as runnable JS. */
    private fun extractObject(js: String, name: String): String? {
        val escaped = Regex.escape(name)
        val m = Regex("""var\s+$escaped\s*=\s*\{""").find(js) ?: return null
        val open = js.indexOf('{', m.range.first)
        val close = matchBrace(js, open) ?: return null
        return "var $name=${js.substring(open, close + 1)};"
    }

    private fun matchBrace(s: String, openIdx: Int): Int? {
        var depth = 0
        var i = openIdx
        var inStr: Char? = null
        var prev = ' '
        while (i < s.length) {
            val c = s[i]
            if (inStr != null) {
                if (c == inStr && prev != '\\') inStr = null
            } else {
                when (c) {
                    '"', '\'', '`' -> inStr = c
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) return i }
                }
            }
            prev = c
            i++
        }
        return null
    }

    // ── Rhino execution ───────────────────────────────────────────────────────
    private fun runJs(source: String, entry: String, arg: String): String? {
        val ctx = RhinoContext.enter()
        return try {
            ctx.optimizationLevel = -1 // interpreted mode (required on Android — no bytecode gen)
            val scope: ScriptableObject = ctx.initSafeStandardObjects()
            ctx.evaluateString(scope, source, "yt", 1, null)
            val fn = scope.get(entry, scope) as? RhinoFunction ?: return null
            val result = fn.call(ctx, scope, scope, arrayOf<Any>(arg))
            RhinoContext.toString(result)
        } catch (_: Exception) {
            null
        } finally {
            RhinoContext.exit()
        }
    }

    // ── small utils ───────────────────────────────────────────────────────────
    private fun parseQuery(q: String): Map<String, String> =
        q.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx < 0) null else Uri.decode(it.substring(0, idx)) to Uri.decode(it.substring(idx + 1))
        }.toMap()

    private fun appendQuery(url: String, key: String, value: String): String {
        val sep = if (url.contains('?')) '&' else '?'
        return "$url$sep${Uri.encode(key)}=${Uri.encode(value)}"
    }

    private fun stripQuery(url: String, key: String): String {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name == key) continue
            for (v in uri.getQueryParameters(name)) builder.appendQueryParameter(name, v)
        }
        return builder.build().toString()
    }
}
