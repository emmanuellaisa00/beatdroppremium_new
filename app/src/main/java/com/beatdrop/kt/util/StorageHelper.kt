package com.beatdrop.kt.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * Storage management — SD card detection, space calculation, directory management.
 */
object StorageHelper {

    data class StorageInfo(
        val path: String,
        val label: String,
        val totalBytes: Long,
        val freeBytes: Long,
        val isRemovable: Boolean,
        val isInternal: Boolean,
    ) {
        val usedBytes: Long get() = totalBytes - freeBytes
        val usedPercent: Int get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
        val freeGB: Float get() = freeBytes / (1024f * 1024f * 1024f)
        val totalGB: Float get() = totalBytes / (1024f * 1024f * 1024f)
    }

    /** All available storage locations (internal + SD card). */
    fun getStorageLocations(context: Context): List<StorageInfo> {
        val out = mutableListOf<StorageInfo>()

        // Internal app-specific storage
        val internal = context.getExternalFilesDir(null)
        if (internal != null) {
            val stat = StatFs(internal.absolutePath)
            out.add(StorageInfo(
                path = internal.absolutePath,
                label = "Internal Storage",
                totalBytes = stat.totalBytes,
                freeBytes = stat.availableBytes,
                isRemovable = false,
                isInternal = true,
            ))
        }

        // SD card / external storage
        val externalDirs = context.getExternalFilesDirs(null)
        for (dir in externalDirs) {
            if (dir == null) continue
            val isRemovable = Environment.isExternalStorageRemovable(dir)
            if (isRemovable || dir != internal) {
                val stat = StatFs(dir.absolutePath)
                out.add(StorageInfo(
                    path = dir.absolutePath,
                    label = if (isRemovable) "SD Card" else "External Storage",
                    totalBytes = stat.totalBytes,
                    freeBytes = stat.availableBytes,
                    isRemovable = isRemovable,
                    isInternal = false,
                ))
            }
        }

        // Public Downloads directory
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads.exists() || publicDownloads.mkdirs()) {
            val stat = StatFs(publicDownloads.absolutePath)
            out.add(StorageInfo(
                path = publicDownloads.absolutePath,
                label = "Public Downloads",
                totalBytes = stat.totalBytes,
                freeBytes = stat.availableBytes,
                isRemovable = false,
                isInternal = true,
            ))
        }

        return out
    }

    /** Get or create the download directory for a given storage path. */
    fun getDownloadDir(storagePath: String): File {
        val dir = File(storagePath, "BeatDrop/Downloads")
        dir.mkdirs()
        return dir
    }

    /** Calculate total size of BeatDrop downloads across all storage locations. */
    fun getDownloadsSize(downloadDir: File): Long {
        if (!downloadDir.exists()) return 0L
        return downloadDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Delete all downloads in a directory. */
    fun clearDownloads(downloadDir: File): Int {
        if (!downloadDir.exists()) return 0
        var count = 0
        downloadDir.walkTopDown().filter { it.isFile }.forEach {
            if (it.delete()) count++
        }
        return count
    }

    /** Format bytes to human-readable string. */
    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
