package com.beatdrop.kt.playback

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Lightweight per-track loudness probe — the "ReplayGain in 80 lines" piece.
 *
 * Why this exists:
 *   Auto-Mix crossfades feel jarring when adjacent tracks have very different
 *   loudness. A quiet ambient track followed by a loud pop song dips audibly
 *   at the midpoint of the equal-power blend; you reach for the volume knob.
 *
 * What it does:
 *   Decodes the first ~5 seconds of PCM (cheap — single MediaCodec pass),
 *   computes the RMS amplitude (root-mean-square, the perceptual loudness
 *   proxy), then maps it to a volume scalar that targets a common reference.
 *
 *   target_volume = clamp( reference_rms / measured_rms , 0.5, 1.0 )
 *
 *   Capped at 1.0 so we never AMPLIFY (which would clip on loud tracks);
 *   floored at 0.5 so a near-silent intro doesn't make the next track blast
 *   the user (the analyzer probes the first 5 s which on some tracks is
 *   intro silence).
 *
 * Used by: PlayerViewModel.triggerAutoMix() to balance Deck B's target volume
 * during the crossfade and to set the main player's volume after handoff.
 * The default (when the analyzer hasn't run yet) is 1.0 — no change.
 */
object LoudnessAnalyzer {

    /** Target RMS amplitude — empirically chosen. ~−18 dBFS feels neutral. */
    private const val REFERENCE_RMS = 0.125f

    /** Probe duration. Long enough to skip an intro tick, short enough to
     *  finish in <100 ms on most files. */
    private const val PROBE_SECS = 5

    /** Compute a volume multiplier ∈ [0.5, 1.0] to play this track at the
     *  target reference loudness. Returns null on decode failure. */
    fun analyze(filePath: String): Float? {
        val rms = runCatching { decodeAndComputeRms(filePath) }.getOrNull() ?: return null
        if (rms.isNaN() || rms <= 0f) return null
        val gain = REFERENCE_RMS / rms
        return gain.coerceIn(0.5f, 1.0f)
    }

    /** Same idea but expressed in dB — handy for the Debug Log. */
    fun rmsDb(filePath: String): Float? {
        val rms = runCatching { decodeAndComputeRms(filePath) }.getOrNull() ?: return null
        if (rms.isNaN() || rms <= 0f) return null
        return (20.0 * (ln(rms.toDouble()) / ln(10.0))).toFloat()
    }

    private fun decodeAndComputeRms(path: String): Float {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        var audioTrack = -1
        var srcFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { audioTrack = i; srcFormat = f; break }
        }
        if (audioTrack < 0 || srcFormat == null) { extractor.release(); return 0f }
        extractor.selectTrack(audioTrack)
        val sampleRate = srcFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = srcFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = srcFormat.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release(); return 0f
        }
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(srcFormat, null, null, 0)
        codec.start()

        val maxSamples = PROBE_SECS * sampleRate * channels
        var sumSq = 0.0
        var sampleCount = 0L
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone && sampleCount < maxSamples) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx) ?: continue
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(idx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val oIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (oIdx >= 0) {
                    val buf: ByteBuffer = codec.getOutputBuffer(oIdx) ?: continue
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    val shortBuf = buf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val n = shortBuf.remaining()
                    var i = 0
                    while (i < n && sampleCount < maxSamples) {
                        val s = shortBuf.get(i).toFloat() / 32768f
                        sumSq += (s * s).toDouble()
                        sampleCount++
                        i++
                    }
                    codec.releaseOutputBuffer(oIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { extractor.release() }
        }
        if (sampleCount == 0L) return 0f
        return sqrt(sumSq / sampleCount).toFloat()
    }
}
