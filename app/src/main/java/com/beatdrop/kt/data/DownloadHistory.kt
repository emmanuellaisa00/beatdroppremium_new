package com.beatdrop.kt.data

import android.content.Context
import com.beatdrop.kt.util.StorageHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent download history — tracks all downloads (completed, failed, deleted)
 * so users can recover/re-download and see their download history.
 *
 * Uses a simple JSON file instead of Room to avoid annotation processing overhead.
 * Thread-safe via ConcurrentHashMap.
 */
object DownloadHistory {

    data class DownloadRecord(
        val videoId: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val durationSecs: Int,
        val filePath: String?,
        val fileSize: Long,
        val format: String,         // "m4a", "opus", "mp4", "mp3"
        val quality: String,        // "high", "medium", "low", "4K", "1080p", etc.
        val downloadedAt: Long,
        val status: String,         // "completed", "failed", "deleted", "cancelled"
        val error: String? = null,
        val sourcePlatform: String = "YouTube",
        val isVideo: Boolean = false,
    )

    private val gson = Gson()
    private val records = ConcurrentHashMap<String, DownloadRecord>()
    private var historyFile: File? = null

    fun init(context: Context) {
        historyFile = File(context.filesDir, "download_history.json")
        load()
    }

    private fun load() {
        val file = historyFile ?: return
        if (!file.exists()) return
        try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, DownloadRecord>>() {}.type
            val map: Map<String, DownloadRecord> = gson.fromJson(json, type)
            records.putAll(map)
        } catch (_: Exception) { }
    }

    private fun persist() {
        val file = historyFile ?: return
        try {
            val json = gson.toJson(records.toMap())
            file.writeText(json)
        } catch (_: Exception) { }
    }

    fun record(record: DownloadRecord) {
        records[record.videoId] = record
        persist()
    }

    fun markDeleted(videoId: String) {
        records[videoId]?.let {
            records[videoId] = it.copy(status = "deleted")
            persist()
        }
    }

    fun markFailed(videoId: String, error: String?) {
        records[videoId]?.let {
            records[videoId] = it.copy(status = "failed", error = error)
            persist()
        }
    }

    fun get(videoId: String): DownloadRecord? = records[videoId]

    fun getAll(): List<DownloadRecord> = records.values.sortedByDescending { it.downloadedAt }

    fun getCompleted(): List<DownloadRecord> =
        records.values.filter { it.status == "completed" }.sortedByDescending { it.downloadedAt }

    fun getFailed(): List<DownloadRecord> =
        records.values.filter { it.status == "failed" }.sortedByDescending { it.downloadedAt }

    fun getDeleted(): List<DownloadRecord> =
        records.values.filter { it.status == "deleted" }.sortedByDescending { it.downloadedAt }

    /** Check if a file still exists on disk. */
    fun isFilePresent(record: DownloadRecord): Boolean {
        val path = record.filePath ?: return false
        return File(path).exists()
    }

    /** Get total size of all completed downloads that still exist. */
    fun totalDownloadSize(): Long {
        return records.values
            .filter { it.status == "completed" && it.filePath != null && File(it.filePath).exists() }
            .sumOf { File(it.filePath).length() }
    }

    /** Clear all history. */
    fun clear() {
        records.clear()
        persist()
    }

    /** Remove a single record from history. */
    fun remove(videoId: String) {
        records.remove(videoId)
        persist()
    }

    /** Count of downloads by status. */
    fun countByStatus(status: String): Int =
        records.values.count { it.status == status }

    /**
     * All download records whose status is 'completed' AND whose file
     * still exists on disk. Used by PlayerViewModel.loadLibrary() to
     * make sure downloaded tracks survive a restart even when the
     * device's MediaStore doesn't index them (e.g. the user picked an
     * app-private download folder, or the file is in a hidden
     * directory MediaStore skips).
     */
    fun completedRecords(): List<DownloadRecord> =
        records.values
            .filter { it.status == "completed" && it.filePath != null && File(it.filePath).exists() }
            .sortedByDescending { it.downloadedAt }
}
