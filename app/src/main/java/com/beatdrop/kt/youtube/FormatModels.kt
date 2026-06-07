package com.beatdrop.kt.youtube

/**
 * Represents a single downloadable format/quality option for a video.
 * Used by the FormatPickerDialog to let users choose before downloading.
 */
data class FormatOption(
    val id: String,             // unique ID (e.g., "audio_251" or "video_22")
    val label: String,          // display label, e.g., "Audio — 160 kbps (Opus)"
    val kind: FormatKind,       // audio, video, or muxed
    val mimeType: String,       // "audio/webm", "audio/mp4", "video/mp4"
    val itag: Int,              // YouTube format itag
    val bitrate: Long,          // bits per second
    val width: Int = 0,         // video width (0 for audio-only)
    val height: Int = 0,        // video height (0 for audio-only)
    val fps: Int = 0,           // video FPS
    val estimatedSizeMB: Long,  // rough file size estimate
    val isDrmFree: Boolean = true,
)

enum class FormatKind { AUDIO, VIDEO, MUXED }

/**
 * Parsed format options from a YouTube /player response.
 * Contains all available audio and video formats.
 */
data class AvailableFormats(
    val videoId: String,
    val title: String,
    val durationSecs: Int,
    val audioFormats: List<FormatOption>,
    val videoFormats: List<FormatOption>,
    val muxedFormats: List<FormatOption>,
) {
    val allFormats: List<FormatOption> get() = audioFormats + videoFormats + muxedFormats

    /** Default recommended format: best audio-only (M4A preferred). */
    val recommended: FormatOption?
        get() = audioFormats.sortedByDescending {
            it.bitrate + (if (it.mimeType.contains("mp4")) 50_000L else 0L)
        }.firstOrNull()
}

// ── Known YouTube itags with their properties ──────────────────────────────
object ItagTable {
    data class ItagInfo(val itag: Int, val kind: FormatKind, val label: String, val container: String)

    private val table = mapOf(
        // Audio-only
        140 to ItagInfo(140, FormatKind.AUDIO, "128 kbps AAC", "m4a"),
        141 to ItagInfo(141, FormatKind.AUDIO, "256 kbps AAC", "m4a"),
        251 to ItagInfo(251, FormatKind.AUDIO, "160 kbps Opus", "webm"),
        250 to ItagInfo(250, FormatKind.AUDIO, "70 kbps Opus", "webm"),
        249 to ItagInfo(249, FormatKind.AUDIO, "50 kbps Opus", "webm"),
        139 to ItagInfo(139, FormatKind.AUDIO, "48 kbps AAC", "m4a"),
        171 to ItagInfo(171, FormatKind.AUDIO, "128 kbps Vorbis", "webm"),
        258 to ItagInfo(258, FormatKind.AUDIO, "320 kbps Opus", "webm"),
        327 to ItagInfo(327, FormatKind.AUDIO, "256 kbps AAC", "m4a"),
        599 to ItagInfo(599, FormatKind.AUDIO, "128 kbps Opus", "webm"),
        600 to ItagInfo(600, FormatKind.AUDIO, "64 kbps Opus", "webm"),
        // Muxed (video + audio)
        18  to ItagInfo(18,  FormatKind.MUXED, "360p MP4", "mp4"),
        22  to ItagInfo(22,  FormatKind.MUXED, "720p MP4", "mp4"),
        59  to ItagInfo(59,  FormatKind.MUXED, "480p MP4", "mp4"),
        37  to ItagInfo(37,  FormatKind.MUXED, "1080p MP4", "mp4"),
        43  to ItagInfo(43,  FormatKind.MUXED, "360p WebM", "webm"),
        // Video-only
        133 to ItagInfo(133, FormatKind.VIDEO, "240p", "mp4"),
        134 to ItagInfo(134, FormatKind.VIDEO, "360p", "mp4"),
        135 to ItagInfo(135, FormatKind.VIDEO, "480p", "mp4"),
        136 to ItagInfo(136, FormatKind.VIDEO, "720p", "mp4"),
        137 to ItagInfo(137, FormatKind.VIDEO, "1080p", "mp4"),
        248 to ItagInfo(248, FormatKind.VIDEO, "1080p WebM", "webm"),
        247 to ItagInfo(247, FormatKind.VIDEO, "720p WebM", "webm"),
        244 to ItagInfo(244, FormatKind.VIDEO, "480p WebM", "webm"),
        243 to ItagInfo(243, FormatKind.VIDEO, "360p WebM", "webm"),
        271 to ItagInfo(271, FormatKind.VIDEO, "1440p WebM", "webm"),
        313 to ItagInfo(313, FormatKind.VIDEO, "2160p WebM", "webm"),
        401 to ItagInfo(401, FormatKind.VIDEO, "2160p AV1", "mp4"),
        266 to ItagInfo(266, FormatKind.VIDEO, "2160p", "mp4"),
    )

    fun getInfo(itag: Int): ItagInfo? = table[itag]

    fun labelFor(itag: Int): String = table[itag]?.label ?: "itag $itag"
}
