package com.beatdrop.kt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app, on-screen debug log. Lets us diagnose search → resolve → play on a real
 * device without adb. Everything funnels through here; the Settings screen shows it
 * live and can copy/share the buffer.
 *
 * No Android dependencies, no logcat noise gating — safe to call from any thread,
 * any module (youtube/, playback/, VM, UI).
 */
object DebugLog {
    enum class Level { D, I, W, E }

    data class Entry(
        val time: Long,
        val level: Level,
        val tag: String,
        val msg: String,
    ) {
        val clock: String get() = TS.format(Date(time))
    }

    private const val MAX = 500
    private val TS = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    @Volatile var enabled: Boolean = true

    fun d(tag: String, msg: String) = log(Level.D, tag, msg)
    fun i(tag: String, msg: String) = log(Level.I, tag, msg)
    fun w(tag: String, msg: String) = log(Level.W, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) =
        log(Level.E, tag, if (t != null) "$msg — ${t.javaClass.simpleName}: ${t.message}" else msg)

    @Synchronized
    private fun log(level: Level, tag: String, msg: String) {
        if (!enabled) return
        val safeMsg = sanitize(msg)
        val e = Entry(System.currentTimeMillis(), level, tag, safeMsg)
        val cur = _entries.value
        val next = if (cur.size >= MAX) cur.subList(cur.size - MAX + 1, cur.size) + e else cur + e
        _entries.value = next
        // Mirror to logcat too, harmless if unread.
        try {
            when (level) {
                Level.D -> android.util.Log.d("BeatDrop/$tag", safeMsg)
                Level.I -> android.util.Log.i("BeatDrop/$tag", safeMsg)
                Level.W -> android.util.Log.w("BeatDrop/$tag", safeMsg)
                Level.E -> android.util.Log.e("BeatDrop/$tag", safeMsg)
            }
        } catch (_: Throwable) {}
    }

    fun clear() { _entries.value = emptyList() }

    /** Plain-text dump for copy/share. */
    fun dump(): String = buildString {
        appendLine("BeatDrop debug log — ${_entries.value.size} entries")
        _entries.value.forEach { e ->
            appendLine("${e.clock} ${e.level} [${e.tag}] ${e.msg}")
        }
    }

    /** Redacts stream URLs / credentials before logs are stored, copied, or shared. */
    fun sanitize(raw: String): String {
        var out = raw
        out = URL_RE.replace(out) { m -> shortUrl(m.value) }
        out = TOKEN_RE.replace(out) { r -> "${r.groupValues[1]}=<redacted>" }
        return out
    }

    /** Redacts long/signed URLs so shared diagnostics are useful but not leaky. */
    fun shortUrl(url: String): String {
        val uri = runCatching { android.net.Uri.parse(url) }.getOrNull()
        val host = uri?.host ?: return if (url.length <= 80) url else "<url:${url.length} chars>"
        val path = uri.path?.take(40).orEmpty()
        return "$host$path…<redacted:${url.length} chars>"
    }

    private val URL_RE = Regex("""https?://[^\s)\]\}"']+""")
    private val TOKEN_RE = Regex("""(?i)(sig|signature|lsig|token|key|cookie|authorization|po[_-]?token|params)=([^\s&]+)""")
}
