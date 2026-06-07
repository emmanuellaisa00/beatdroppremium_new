package com.beatdrop.kt.util

/**
 * Manifest for sideload updates.
 *
 * Host as JSON next to APK releases. The updater must verify [sha256] before
 * handing the APK to Android's package installer.
 */
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val required: Boolean = false,
    val minAndroidSdk: Int = 24,
    val publishedAtEpochMs: Long = 0L,
    val changelog: List<String> = emptyList(),
)
