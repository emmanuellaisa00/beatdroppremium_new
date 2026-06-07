package com.beatdrop.kt.youtube

/**
 * YouTube thumbnail URL helpers.
 *
 * YouTube serves multiple resolutions of every video thumbnail from a
 * predictable URL pattern:
 *
 *   https://i.ytimg.com/vi/<videoId>/default.jpg        (120 ×  90)
 *   https://i.ytimg.com/vi/<videoId>/mqdefault.jpg      (320 × 180)
 *   https://i.ytimg.com/vi/<videoId>/hqdefault.jpg      (480 × 360)
 *   https://i.ytimg.com/vi/<videoId>/sddefault.jpg      (640 × 480)
 *   https://i.ytimg.com/vi/<videoId>/maxresdefault.jpg  (1280 × 720)  ← may 404
 *
 * `maxresdefault` is generated only for videos uploaded in high quality
 * (basically all modern music videos), so it's safe to default-prefer when
 * a fallback URL is needed. For YT-Music-style square covers we also rewrite
 * the `=wXXX-hXXX` size segment to ask for a larger frame.
 */
internal const val YT_THUMB_BEST_PATH = "maxresdefault.jpg"
internal const val YT_THUMB_SAFE_PATH = "sddefault.jpg"

/**
 * Build a high-resolution fallback thumbnail URL for [videoId].
 * Prefer over the legacy `hqdefault.jpg` literal at every call site.
 */
fun ytThumbHd(videoId: String): String =
    "https://i.ytimg.com/vi/$videoId/$YT_THUMB_BEST_PATH"

/**
 * Upgrade any YouTube/YT-Music thumbnail URL to a higher resolution, if we
 * can recognise the pattern. Safe to call on any string — non-matching
 * URLs are returned unchanged.
 *
 *  1. i.ytimg.com/vi/<id>/<any>.jpg   →  i.ytimg.com/vi/<id>/maxresdefault.jpg
 *  2. lh3.googleusercontent.com/.../=w120-h120-...  →  =w720-h720-l90-rj
 *     (this is the YT Music album-cover CDN; clamp 'w'/'h' params higher)
 */
fun upgradeThumbnailUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    // 1. i.ytimg.com pattern
    val ytImg = Regex("""(https?://i\.ytimg\.com/vi/[A-Za-z0-9_-]{11})/[a-z0-9]+\.jpg""")
    ytImg.matchEntire(url.substringBefore('?'))?.let {
        return "${it.groupValues[1]}/$YT_THUMB_BEST_PATH"
    }
    // 2. googleusercontent =wXX-hXX-... pattern (YT Music covers)
    val ggc = Regex("""(.+googleusercontent\.com/[^=]+=)w\d+-h\d+(-[a-z\d-]+)?""")
    ggc.matchEntire(url)?.let {
        return "${it.groupValues[1]}w720-h720-l90-rj"
    }
    return url
}
