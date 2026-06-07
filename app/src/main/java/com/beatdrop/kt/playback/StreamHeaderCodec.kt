package com.beatdrop.kt.playback

import android.util.Base64

/**
 * Encodes per-stream HTTP headers (User-Agent + Origin/Referer) into a single
 * URI-fragment-safe token and back.
 *
 * Why a fragment + Base64url:
 *  - googlevideo CDN URLs are tied to the client identity that resolved them;
 *    ExoPlayer must replay the same UA/headers or the CDN may return 403.
 *  - Media3's ResolvingDataSource can only see the DataSpec's URI, so we smuggle
 *    the headers in the URI fragment (it never reaches the network — we strip it).
 *  - Base64url (A–Z a–z 0–9 - _), padding removed, is NEVER percent-encoded by
 *    android.net.Uri inside a fragment, so encode→toString→parse→getFragment is
 *    an exact round-trip. A naive "k=v&k=v" fragment is not (%/&/= ambiguity).
 *
 * Blob format (before Base64): key \u001F value \u001E key \u001F value ...
 * The User-Agent uses the reserved key "__ua__".
 */
object StreamHeaderCodec {
    const val PREFIX = "bdh1."          // marks a fragment we own
    private const val UA_KEY = "__ua__"
    private const val UNIT = '\u001F'   // key/value separator
    private const val REC  = '\u001E'   // record separator
    private const val B64 = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    fun encode(headers: Map<String, String>): String {
        if (headers.isEmpty()) return ""
        val blob = headers.entries.joinToString(REC.toString()) { (k, v) -> "$k$UNIT$v" }
        return PREFIX + Base64.encodeToString(blob.toByteArray(Charsets.UTF_8), B64)
    }

    /** Returns null if the fragment isn't one of ours. UA (if any) is under key "__ua__". */
    fun decode(fragment: String?): Map<String, String>? {
        if (fragment == null || !fragment.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.decode(fragment.removePrefix(PREFIX), B64)
            val blob = String(raw, Charsets.UTF_8)
            val out = LinkedHashMap<String, String>()
            for (rec in blob.split(REC)) {
                val i = rec.indexOf(UNIT)
                if (i <= 0) continue
                out[rec.substring(0, i)] = rec.substring(i + 1)
            }
            out
        } catch (_: Exception) {
            null
        }
    }

    fun userAgentKey() = UA_KEY
}
