package com.beatdrop.kt.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * File sharing — one-tap share downloaded files to WhatsApp, Bluetooth, etc.
 */
object ShareHelper {

    /** Share a downloaded audio/video file via Android share sheet. */
    fun shareFile(context: Context, file: File, title: String) {
        if (!file.exists()) return
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            // Fallback for older Android — use file:// URI
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        val mimeType = when {
            file.name.endsWith(".mp3", true) -> "audio/mpeg"
            file.name.endsWith(".m4a", true) -> "audio/mp4"
            file.name.endsWith(".opus", true) -> "audio/opus"
            file.name.endsWith(".mp4", true) -> "video/mp4"
            file.name.endsWith(".webm", true) -> "video/webm"
            else -> "audio/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Shared from BeatDrop — $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share \"$title\""))
    }

    /** Share a YouTube/video link. */
    fun shareLink(context: Context, videoId: String, title: String) {
        val url = "https://youtube.com/watch?v=$videoId"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    /** Share multiple downloaded files at once. */
    fun shareMultipleFiles(context: Context, files: List<Pair<File, String>>) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFile(context, files[0].first, files[0].second)
            return
        }
        val uris = files.mapNotNull { (file, _) ->
            if (!file.exists()) return@mapNotNull null
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) { null }
        }
        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${files.size} songs"))
    }
}
