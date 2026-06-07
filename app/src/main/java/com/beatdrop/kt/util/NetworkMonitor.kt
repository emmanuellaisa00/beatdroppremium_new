package com.beatdrop.kt.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network state — used for WiFi-only download guard and
 * connectivity checks before streaming/downloading.
 */
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var contextRef: Context? = null

    fun init(context: Context) {
        contextRef = context.applicationContext
        _isOnline.value = isConnected(context)
    }

    fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo ?: return false
        return info.type == ConnectivityManager.TYPE_WIFI
    }

    fun isOnMobileData(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo ?: return false
        return info.type == ConnectivityManager.TYPE_MOBILE
    }

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo ?: return false
        return info.isConnected
    }

    fun getConnectionType(context: Context): String {
        return when {
            isOnWifi(context) -> "WiFi"
            isOnMobileData(context) -> "Mobile"
            else -> "None"
        }
    }
}