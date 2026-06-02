package com.beatdrop.kt

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.beatdrop.kt.data.DownloadHistory
import com.beatdrop.kt.data.Subscriptions
import com.beatdrop.kt.youtube.InnertubeSearchProvider
import com.beatdrop.kt.youtube.OnlineSearch
import com.beatdrop.kt.youtube.PipedResolver
import com.beatdrop.kt.youtube.YoutubeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BeatDropApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Patch security provider for modern TLS 1.2 / 1.3 support on older devices
        try {
            com.google.android.gms.security.ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Wire online search to the real Innertube backend (no API key required)
        OnlineSearch.provider = InnertubeSearchProvider()
        // Give YoutubeService a context for the download directory
        YoutubeService.init(this)

        // Initialize download history
        DownloadHistory.init(this)

        // Initialize subscriptions
        Subscriptions.init(this)

        // Refresh the public Piped instance list in the background
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { PipedResolver.refreshInstanceList() }
                .onSuccess { DebugLog.i("piped", "instance list refreshed") }
                .onFailure { DebugLog.w("piped", "instance list refresh failed: ${it.message}") }
        }

        DebugLog.i("app", "BeatDrop started — online search wired, YoutubeService ready")
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
}
