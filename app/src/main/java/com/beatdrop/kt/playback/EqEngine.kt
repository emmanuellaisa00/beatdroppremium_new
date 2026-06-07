package com.beatdrop.kt.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Real native DSP via android.media.audiofx.Equalizer — a genuine upgrade over
 * the RN app's stubbed EQ (which had no DSP module at all). Bound to the
 * ExoPlayer audio session id by PlaybackService.
 */
object EqEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _bands = MutableStateFlow<List<Band>>(emptyList())
    val bands: StateFlow<List<Band>> = _bands

    private val _bassStrength = MutableStateFlow(0) // 0..1000
    val bassStrength: StateFlow<Int> = _bassStrength

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets

    data class Band(val index: Short, val centerFreqHz: Int, val minMb: Short, val maxMb: Short, val levelMb: Short)

    /** Called by PlaybackService once the player's audio session id is known. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        runCatching {
            release()
            val eq = Equalizer(0, audioSessionId)
            val bb = BassBoost(0, audioSessionId)
            equalizer = eq
            bassBoost = bb
            refreshBands()
            _presets.value = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
        }
    }

    private fun refreshBands() {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange // [min, max] in millibels
        _bands.value = (0 until eq.numberOfBands).map { i ->
            val idx = i.toShort()
            Band(
                index = idx,
                centerFreqHz = eq.getCenterFreq(idx) / 1000,
                minMb = range[0], maxMb = range[1],
                levelMb = eq.getBandLevel(idx),
            )
        }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        runCatching { equalizer?.enabled = on; bassBoost?.enabled = on }
    }

    fun setBandLevel(index: Short, levelMb: Short) {
        runCatching { equalizer?.setBandLevel(index, levelMb) }
        refreshBands()
    }

    fun applyPreset(presetIndex: Short) {
        runCatching { equalizer?.usePreset(presetIndex) }
        refreshBands()
    }

    fun setBassStrength(strength0to1000: Int) {
        val s = strength0to1000.coerceIn(0, 1000)
        _bassStrength.value = s
        runCatching { bassBoost?.setStrength(s.toShort()) }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }
}
