package com.beatdrop.kt.util

import android.content.ClipboardManager
import android.content.Context

/**
 * Watches the system clipboard for YouTube/video URLs and reports them.
 * Used to auto-detect when a user copies a video link from another app.
 */
object ClipboardWatcher {

    // Patterns for supported video URLs
    private val URL_PATTERNS = listOf(
        // YouTube
        Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?music\.youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/playlist\?list=([a-zA-Z0-9_-]+)"""),
        // TikTok
        Regex("""(?:https?://)?(?:www\.|vm\.)?tiktok\.com/[@\w/.]+"""),
        // Instagram
        Regex("""(?:https?://)?(?:www\.)?instagram\.com/(?:reel|p)/([\w-]+)"""),
        // Facebook
        Regex("""(?:https?://)?(?:www\.|m\.)?facebook\.com/.*/videos/"""),
        Regex("""(?:https?://)?fb\.watch/([\w-]+)"""),
        // Twitter/X
        Regex("""(?:https?://)?(?:www\.|mobile\.)?(?:twitter\.com|x\.com)/\w+/status/(\d+)"""),
    )

    data class DetectedUrl(
        val url: String,
        val platform: String,
        val videoId: String?,    // YouTube video ID or playlist ID
        val isPlaylist: Boolean,
    )

    /** Last clipboard text we reported — prevents duplicate notifications. */
    private var lastReportedText: String? = null

    /**
     * Check clipboard for a video URL. Returns null if nothing found.
     * Should be called in onResume() or onWindowFocusChanged().
     */
    fun checkClipboard(context: Context): DetectedUrl? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        if (!cm.hasPrimaryClip()) return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return null
        if (text == lastReportedText) return null

        val detected = parseUrl(text)
        if (detected != null) lastReportedText = text
        return detected
    }

    /** Parse a URL string into a DetectedUrl. */
    fun parseUrl(text: String): DetectedUrl? {
        for (pattern in URL_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val url = match.value
            val platform = when {
                url.contains("youtube.com") || url.contains("youtu.be") || url.contains("music.youtube.com") -> "YouTube"
                url.contains("tiktok.com") -> "TikTok"
                url.contains("instagram.com") -> "Instagram"
                url.contains("facebook.com") || url.contains("fb.watch") -> "Facebook"
                url.contains("twitter.com") || url.contains("x.com") -> "Twitter/X"
                else -> "Unknown"
            }
            val isPlaylist = url.contains("/playlist?list=")
            val videoId = if (isPlaylist) {
                Regex("""list=([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.get(1)
            } else {
                match.groupValues.getOrNull(1)
            }
            return DetectedUrl(url = url, platform = platform, videoId = videoId, isPlaylist = isPlaylist)
        }
        return null
    }

    /** Reset so the same URL can be detected again (e.g., after user dismisses). */
    fun reset() { lastReportedText = null }
}
