package com.beatdrop.kt.util

/**
 * Sideload production remote configuration model.
 *
 * This is intentionally a data model only for now. Next step is wiring it to a
 * signed JSON endpoint (GitHub Releases, Cloudflare, etc.) so resolver strategy
 * order and risky extraction features can be changed without shipping a new APK.
 */
data class SideloadRemoteConfig(
    val extraction: ExtractionConfig = ExtractionConfig(),
    val downloads: DownloadConfig = DownloadConfig(),
    val ui: UiConfig = UiConfig(),
    val minSupportedVersionCode: Int = 1,
    val messageOfTheDay: String? = null,
) {
    data class ExtractionConfig(
        val enabled: Boolean = true,
        val enableInnertube: Boolean = true,
        val enableNewPipe: Boolean = true,
        val enableWebViewExtractor: Boolean = true,
        val enablePiped: Boolean = true,
        val enableInvidious: Boolean = true,
        val preferredResolverOrder: List<String> = listOf("newpipe", "innertube", "webview", "piped", "invidious"),
        val blockedPipedInstances: List<String> = emptyList(),
        val blockedInvidiousInstances: List<String> = emptyList(),
    )

    data class DownloadConfig(
        val enabled: Boolean = true,
        val maxConcurrentDownloads: Int = 3,
        val maxChunkCount: Int = 4,
        val requireWifiByDefault: Boolean = false,
    )

    data class UiConfig(
        val enableHeavyGlass: Boolean = true,
        val enableTiltHighlights: Boolean = true,
        val reduceEffectsDefault: Boolean = false,
    )
}
