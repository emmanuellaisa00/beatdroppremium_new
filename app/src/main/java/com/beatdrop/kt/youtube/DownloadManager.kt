package com.beatdrop.kt.youtube

import android.content.Context
import com.beatdrop.kt.data.Track
import com.beatdrop.kt.playback.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton download manager — owns a CoroutineScope that is NOT tied to any
 * ViewModel or Activity lifecycle, so downloads survive configuration changes
 * and app backgrounding (the foreground DownloadService keeps the process alive).
 *
 * Replaces the old viewModelScope.launch pattern that was cancelled whenever
 * Android reclaimed the ViewModel.
 */
object DownloadManager {

    // Outlives every ViewModel — cancelled only when the process dies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Active coroutine jobs keyed by videoId — used for cancellation
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Observable state for the UI
    private val _jobs = MutableStateFlow<Map<String, DownloadJob>>(emptyMap())
    val jobs: StateFlow<Map<String, DownloadJob>> = _jobs.asStateFlow()

    // SharedFlow emits each Track the moment its download completes
    // ViewModel collects this to add the track to the library without polling
    private val _trackReady = MutableSharedFlow<Track>(extraBufferCapacity = 32)
    val trackReady: SharedFlow<Track> = _trackReady.asSharedFlow()

    // ─── Public API ───────────────────────────────────────────────────────────

    fun enqueue(result: OnlineResult, context: Context) {
        val vid = result.videoId
        val existing = _jobs.value[vid]
        if (existing?.status == DownloadStatus.QUEUED ||
            existing?.status == DownloadStatus.DOWNLOADING) return

        // Start the foreground service before launching so Android doesn't kill us
        DownloadService.start(context)
        updateJob(DownloadJob(vid, result.title, DownloadStatus.QUEUED))

        val job = scope.launch {
            updateJob(DownloadJob(vid, result.title, DownloadStatus.DOWNLOADING))

            runCatching {
                downloadYoutubeTrack(result) { progress ->
                    // Propagate progress to UI and notification
                    updateJob(DownloadJob(vid, result.title, DownloadStatus.DOWNLOADING, progress.percent))
                    DownloadService.updateProgress(context, progress.percent, result.title)
                }
            }.onSuccess { track ->
                updateJob(DownloadJob(vid, result.title, DownloadStatus.COMPLETED, 100, track))
                _trackReady.tryEmit(track)
                DownloadService.notifyComplete(context, result.title, result.videoId)
            }.onFailure { err ->
                if (err !is CancellationException) {
                    updateJob(DownloadJob(vid, result.title, DownloadStatus.FAILED, 0, null, err.message))
                }
            }

            activeJobs.remove(vid)
            stopServiceIfIdle(context)
        }

        activeJobs[vid] = job
    }

    fun cancel(videoId: String, context: Context) {
        activeJobs.remove(videoId)?.cancel()
        _jobs.value = _jobs.value - videoId
        stopServiceIfIdle(context)
    }

    fun retry(result: OnlineResult, context: Context) {
        _jobs.value = _jobs.value - result.videoId
        enqueue(result, context)
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun updateJob(job: DownloadJob) {
        _jobs.value = _jobs.value + (job.videoId to job)
    }

    private fun stopServiceIfIdle(context: Context) {
        val busy = _jobs.value.values.any {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        if (!busy) DownloadService.stop(context)
    }
}
