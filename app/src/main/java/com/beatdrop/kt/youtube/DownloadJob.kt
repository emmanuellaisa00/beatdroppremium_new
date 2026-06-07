package com.beatdrop.kt.youtube

import com.beatdrop.kt.data.Track

enum class DownloadStatus { IDLE, QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadJob(
    val videoId: String,
    val title: String,
    val status: DownloadStatus,
    val progress: Int = 0,           // 0–100
    val localTrack: Track? = null,   // set when COMPLETED
    val error: String? = null,       // set when FAILED
)
