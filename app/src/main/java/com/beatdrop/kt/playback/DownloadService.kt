package com.beatdrop.kt.playback

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.beatdrop.kt.MainActivity

/**
 * Foreground service that keeps the process alive for the duration of downloads.
 * SnapTube runs downloads in a persistent foreground service so Android cannot
 * kill mid-transfer when the user navigates away. This replaces the old
 * viewModelScope.launch approach that died on app backgrounding.
 *
 * DownloadManager owns the actual coroutines; this service just holds the
 * foreground slot and notification. Start it before launching a download,
 * stop it once all queued downloads are complete.
 */
class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID   = "beatdrop_downloads"
        private const val CHANNEL_NAME = "Music Downloads"
        const val  NOTIF_ID            = 7001
        private const val NOTIF_DONE_ID = 7002

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, DownloadService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }

        /** Progress update during an active download. */
        fun updateProgress(context: Context, percent: Int, title: String) {
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(
                NOTIF_ID,
                buildProgressNotification(context, title, percent, ongoing = true),
            )
        }

        /**
         * Download finished. Posts a SEPARATE notification (different id
         * per videoId, so multiple downloads complete independently)
         * with:
         *   • The green-check ic_notification_done small icon
         *   • Title 'Saved to BeatDrop' + the track name as the text
         *   • setOngoing = false so the user can swipe it away
         *   • A tap intent that carries the videoId so MainActivity can
         *     open Now Playing on that track
         */
        fun notifyComplete(context: Context, title: String, videoId: String?) {
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // One id per videoId so completed notifications coexist
            // instead of overwriting each other. Hash to a stable int.
            val id = videoId?.hashCode() ?: NOTIF_DONE_ID
            nm.notify(id, buildDoneNotification(context, title, videoId))
        }

        private fun buildProgressNotification(
            context: Context,
            title: String,
            percent: Int,
            ongoing: Boolean,
        ): Notification {
            ensureChannel(context)
            return NotificationCompat.Builder(context, CHANNEL_ID)
                // Branded teardrop silhouette in the status bar.
                .setSmallIcon(com.beatdrop.kt.R.drawable.ic_notification)
                .setContentTitle("Downloading")
                .setContentText(title)
                // Larger expanded body so the title doesn't truncate.
                .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                .setContentIntent(buildOpenAppIntent(context))
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .apply {
                    when {
                        percent in 1..99 -> setProgress(100, percent, false)
                        percent == 0     -> setProgress(100, 0, true)  // indeterminate
                        else             -> setProgress(0, 0, false)   // clear
                    }
                }
                .build()
        }

        private fun buildDoneNotification(
            context: Context,
            title: String,
            videoId: String?,
        ): Notification {
            ensureChannel(context)
            return NotificationCompat.Builder(context, CHANNEL_ID)
                // Green-check 'done' icon instead of the teardrop so the
                // status bar visually signals success at a glance.
                .setSmallIcon(com.beatdrop.kt.R.drawable.ic_notification_done)
                .setContentTitle("Saved to BeatDrop")
                .setContentText(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                .setContentIntent(buildPlayTrackIntent(context, videoId))
                .setAutoCancel(true)        // tap dismisses
                .setOngoing(false)           // user can swipe away
                .build()
        }

        /** Generic 'open app' intent (Downloading notification). */
        private fun buildOpenAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /**
         * Tap-intent for a completed download — opens the app and asks
         * MainActivity to play the just-downloaded track. The videoId
         * rides in the Intent extra `play_video_id` which MainActivity
         * checks in handleIntent().
         */
        private fun buildPlayTrackIntent(context: Context, videoId: String?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                action = "com.beatdrop.kt.PLAY_DOWNLOADED"
                if (videoId != null) putExtra("play_video_id", videoId)
            }
            // Per-videoId requestCode so PendingIntents for different
            // completed downloads don't share the same extras (Android
            // re-uses pending intents that match by everything except
            // extras, otherwise).
            val rc = videoId?.hashCode() ?: 0
            return PendingIntent.getActivity(
                context, rc, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL_ID) != null) return
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Shows music download progress"
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildProgressNotification(this, "Downloading music…", 0, ongoing = true)
        // Android 14 (API 34) throws MissingForegroundServiceTypeException if the manifest
        // declares android:foregroundServiceType but startForeground() is called without the
        // type parameter.  Pass the type on API 29+ (when the 3-param overload was added).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        @Suppress("DEPRECATION")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
