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

        fun updateProgress(context: Context, percent: Int, title: String) {
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification(context, "Downloading: $title", percent, ongoing = true))
        }

        fun notifyComplete(context: Context, title: String) {
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_DONE_ID, buildNotification(context, "Saved to library: $title", -1, ongoing = false))
        }

        private fun buildNotification(
            context: Context,
            text: String,
            percent: Int,
            ongoing: Boolean,
        ): Notification {
            ensureChannel(context)
            val tapIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("BeatDrop")
                .setContentText(text)
                .setContentIntent(tapIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .apply {
                    when {
                        percent in 0..99 -> setProgress(100, percent, false)
                        percent == 0     -> setProgress(100, 0, true)  // indeterminate
                    }
                }
                .build()
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
        val notif = buildNotification(this, "Downloading music…", 0, ongoing = true)
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
