package com.beatdrop.kt.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * Opens the system "Output Switcher" — the same panel Spotify/YouTube Music use
 * to move audio to Bluetooth speakers, headphones, Cast devices, etc.
 *
 * On Android 10+ this is the official Settings media-output panel. On older
 * devices we fall back to Bluetooth settings.
 */
object AudioOutput {
    fun openSwitcher(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent("android.settings.panel.action.MEDIA_OUTPUT")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        // Fallback: Bluetooth settings (older devices / OEMs without the panel)
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        Toast.makeText(context, "No output switcher available", Toast.LENGTH_SHORT).show()
    }

    /** Human-readable label of the current audio route (for the button subtitle). */
    fun currentRouteLabel(context: Context): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            // Prefer the most "external" device if connected.
            val priority = listOf(
                AudioDeviceTypes.BLE_HEADSET, AudioDeviceTypes.BLE_SPEAKER,
                AudioDeviceTypes.BLUETOOTH_A2DP, AudioDeviceTypes.HEARING_AID,
                AudioDeviceTypes.WIRED_HEADPHONES, AudioDeviceTypes.WIRED_HEADSET,
                AudioDeviceTypes.USB_HEADSET,
            )
            for (type in priority) {
                val d = devices.firstOrNull { it.type == type }
                if (d != null) return labelFor(type, d.productName?.toString())
            }
        }
        return "Phone speaker"
    }

    private fun labelFor(type: Int, product: String?): String = when (type) {
        AudioDeviceTypes.BLUETOOTH_A2DP, AudioDeviceTypes.BLE_HEADSET, AudioDeviceTypes.BLE_SPEAKER ->
            product?.takeIf { it.isNotBlank() } ?: "Bluetooth"
        AudioDeviceTypes.WIRED_HEADPHONES, AudioDeviceTypes.WIRED_HEADSET -> "Headphones"
        AudioDeviceTypes.USB_HEADSET -> "USB audio"
        AudioDeviceTypes.HEARING_AID -> "Hearing aid"
        else -> "Phone speaker"
    }
}

/** AudioDeviceInfo type constants referenced safely (avoids API-level import noise). */
private object AudioDeviceTypes {
    const val BLUETOOTH_A2DP = 8
    const val WIRED_HEADSET = 3
    const val WIRED_HEADPHONES = 4
    const val USB_HEADSET = 22
    const val HEARING_AID = 23
    const val BLE_HEADSET = 26
    const val BLE_SPEAKER = 27
}
