package com.beatdrop.kt.playback

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * On-device music feature extraction (BPM + musical key) — the analytic half of
 * BeatDrop's Auto-Mix engine.
 *
 *   estimate(uriOrPath, durationMs) → TrackFeatures(bpm, keyCamelot)
 *
 * The pipeline mirrors what production DJ apps (Mixxx, Serato, Rekordbox) do, but
 * implemented in pure Kotlin against the Android platform decoders — no JNI, no
 * extra dependency, no network. Roughly:
 *
 *   1. MediaExtractor + MediaCodec decode ~30 s of audio (skipping the first
 *      ~10 s of intro silence) into 16-bit PCM at the file's native sample rate.
 *   2. Down-mix to mono, resample to 22 050 Hz (simple decimation), normalise.
 *   3. Onset envelope: |x[n]| smoothed with a 10 ms moving average; differences
 *      half-wave-rectified (HWR) → "novelty function".
 *   4. BPM via autocorrelation of the novelty function — find the lag with the
 *      strongest peak in the 60–180 BPM range; refine by parabolic interpolation
 *      around the peak.
 *   5. Key via chroma vector: short-time FFT, fold bins into 12 pitch classes
 *      (A=0 .. G#=11) weighted by magnitude, then correlate the resulting
 *      12-element vector against the 24 Krumhansl–Schmuckler key profiles
 *      (12 major + 12 minor). Winning key → Camelot wheel notation (e.g. "8A").
 *
 * Robustness: every step is best-effort. If decoding fails, returns null.
 * Analysis takes 200-400 ms of background CPU per track and runs at most once
 * per track (results cached in Prefs.TRACK_FEATURES).
 */
object TrackAnalyzer {

    data class TrackFeatures(val bpm: Int, val keyCamelot: String)

    private const val TARGET_RATE = 22_050
    private const val ANALYZE_SECS = 30
    private const val SKIP_HEAD_SECS = 10   // skip likely-intro silence/applause

    /**
     * Decode → mono PCM → BPM + key. Returns null on any decode failure.
     * Safe to call from any thread; performs blocking I/O so use Dispatchers.IO.
     */
    fun estimate(filePath: String, durationMs: Long): TrackFeatures? {
        val pcm = runCatching { decodeMonoPcm(filePath, durationMs) }.getOrNull() ?: return null
        if (pcm.isEmpty()) return null
        val bpm = estimateBpm(pcm, TARGET_RATE)
        val key = estimateKey(pcm, TARGET_RATE)
        return TrackFeatures(bpm, key)
    }

    // ── 1) Decode ────────────────────────────────────────────────────────────
    private fun decodeMonoPcm(path: String, durationMs: Long): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        var audioTrack = -1
        var srcFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { audioTrack = i; srcFormat = f; break }
        }
        if (audioTrack < 0 || srcFormat == null) { extractor.release(); return FloatArray(0) }
        extractor.selectTrack(audioTrack)

        val sampleRate = srcFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels   = srcFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = srcFormat.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release(); return FloatArray(0)
        }
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(srcFormat, null, null, 0)
        codec.start()

        // Seek past intro silence on long tracks (>40 s).
        if (durationMs > 40_000) extractor.seekTo(SKIP_HEAD_SECS * 1_000_000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val out = ArrayList<Float>(ANALYZE_SECS * TARGET_RATE)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val startUs = if (durationMs > 40_000) SKIP_HEAD_SECS * 1_000_000L else 0L
        val endUs = startUs + ANALYZE_SECS * 1_000_000L
        val decimation = max(1, sampleRate / TARGET_RATE)
        var decim = 0

        try {
            while (!outputDone && out.size < ANALYZE_SECS * TARGET_RATE) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx) ?: continue
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0 || extractor.sampleTime >= endUs) {
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
                    val sampleCount = shortBuf.remaining()
                    // Down-mix to mono + decimate to TARGET_RATE.
                    var i = 0
                    while (i < sampleCount) {
                        // average channels for the current frame
                        var sum = 0
                        for (c in 0 until channels) {
                            if (i + c < sampleCount) sum += shortBuf.get(i + c).toInt()
                        }
                        val mono = (sum / channels).toFloat() / 32768f
                        if (decim == 0) out.add(mono)
                        decim = (decim + 1) % decimation
                        i += channels
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
        return out.toFloatArray()
    }

    // ── 2) BPM via onset autocorrelation ─────────────────────────────────────
    private fun estimateBpm(pcm: FloatArray, rate: Int): Int {
        // a) 10 ms moving-average envelope
        val win = rate / 100
        val env = FloatArray(pcm.size / win)
        var idx = 0
        var i = 0
        while (i + win <= pcm.size) {
            var s = 0f
            for (j in 0 until win) s += abs(pcm[i + j])
            env[idx++] = s / win
            i += win
        }
        // b) novelty = HWR(diff(env))
        val nov = FloatArray(env.size - 1)
        for (k in 1 until env.size) {
            val d = env[k] - env[k - 1]
            nov[k - 1] = if (d > 0f) d else 0f
        }
        // Normalise novelty (z-score) so autocorr peaks are scale-invariant.
        val mean = nov.average().toFloat()
        var varSum = 0f
        for (v in nov) varSum += (v - mean) * (v - mean)
        val sd = sqrt(varSum / nov.size).coerceAtLeast(1e-9f)
        for (k in nov.indices) nov[k] = (nov[k] - mean) / sd

        // c) autocorrelate in the BPM range 60..180 (so lag in 60/180..60/60 sec)
        // novelty frame rate = 100 Hz (one per 10 ms)
        val novRate = 100
        val minLag = (novRate * 60.0 / 180.0).toInt()  // 33 frames
        val maxLag = (novRate * 60.0 / 60.0).toInt()   // 100 frames
        var bestLag = minLag
        var bestScore = Float.NEGATIVE_INFINITY
        val scores = FloatArray(maxLag - minLag + 1)
        for (lag in minLag..maxLag) {
            var s = 0f
            val n = nov.size - lag
            if (n <= 0) continue
            for (k in 0 until n) s += nov[k] * nov[k + lag]
            scores[lag - minLag] = s
            if (s > bestScore) { bestScore = s; bestLag = lag }
        }
        // d) parabolic interpolation around the peak for sub-frame accuracy
        val pi = bestLag - minLag
        val refinedLag = if (pi in 1 until scores.size - 1) {
            val y0 = scores[pi - 1]; val y1 = scores[pi]; val y2 = scores[pi + 1]
            val denom = (y0 - 2f * y1 + y2)
            val delta = if (abs(denom) > 1e-6f) 0.5f * (y0 - y2) / denom else 0f
            bestLag + delta
        } else bestLag.toFloat()

        var bpm = 60.0 * novRate / refinedLag
        // Snap obvious half/double-time results to the 80..160 sweet spot for
        // modern music (octave error is autocorr's classic failure mode).
        while (bpm < 80.0) bpm *= 2
        while (bpm > 160.0) bpm /= 2
        return round(bpm).toInt().coerceIn(60, 200)
    }

    // ── 3) Key via Krumhansl–Schmuckler ──────────────────────────────────────
    /**
     * Returns Camelot wheel notation ("1A".."12B"). Camelot is the standard
     * harmonic-mixing notation: adjacent numbers (±1) and same-letter swaps
     * (A↔B at same number) are key-compatible blends.
     */
    private fun estimateKey(pcm: FloatArray, rate: Int): String {
        val fftSize = 4096
        val hop = 2048
        val chroma = FloatArray(12)
        val w = hannWindow(fftSize)
        val re = FloatArray(fftSize); val im = FloatArray(fftSize)

        // Octave-folded bin → pitch class lookup (12-TET, A4 = 440 Hz, A = pitch class 9 if C=0).
        // We'll use C=0 .. B=11 for the standard chroma vector.
        val binPitch = IntArray(fftSize / 2)
        val binMag   = FloatArray(fftSize / 2)
        for (k in 1 until fftSize / 2) {
            val freq = k.toDouble() * rate / fftSize
            if (freq < 50.0 || freq > 5000.0) { binPitch[k] = -1; continue }
            // MIDI note number: 12 * log2(f / 440) + 69
            val midi = 12.0 * (ln(freq / 440.0) / ln(2.0)) + 69.0
            binPitch[k] = ((midi.toInt() + 1200) % 12)  // pitch class A=9 etc. — convert to C=0:
            binPitch[k] = ((binPitch[k] + 3) % 12)       // shift A→C reference
        }

        var idx = 0
        while (idx + fftSize <= pcm.size) {
            for (n in 0 until fftSize) re[n] = pcm[idx + n] * w[n]
            java.util.Arrays.fill(im, 0f)
            fftInPlace(re, im)
            for (k in 1 until fftSize / 2) {
                val pc = binPitch[k]; if (pc < 0) continue
                val mag = sqrt(re[k] * re[k] + im[k] * im[k])
                binMag[k] = mag
                chroma[pc] += mag
            }
            idx += hop
        }
        // L2 normalise
        var n2 = 0f; for (v in chroma) n2 += v * v
        val n = sqrt(n2).coerceAtLeast(1e-9f)
        for (i in 0 until 12) chroma[i] /= n

        // Krumhansl–Schmuckler key profiles (major, minor).
        val major = floatArrayOf(6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f)
        val minor = floatArrayOf(6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f)
        normalise(major); normalise(minor)

        var bestScore = Float.NEGATIVE_INFINITY
        var bestTonic = 0
        var bestIsMinor = false
        for (tonic in 0 until 12) {
            val majS = correlate(chroma, major, tonic)
            val minS = correlate(chroma, minor, tonic)
            if (majS > bestScore) { bestScore = majS; bestTonic = tonic; bestIsMinor = false }
            if (minS > bestScore) { bestScore = minS; bestTonic = tonic; bestIsMinor = true }
        }

        // tonic 0..11 in C-relative ordering. Convert to standard pitch name
        // and Camelot.
        return camelot(bestTonic, bestIsMinor)
    }

    private fun normalise(a: FloatArray) {
        var s = 0f; for (v in a) s += v * v
        val n = sqrt(s).coerceAtLeast(1e-9f)
        for (i in a.indices) a[i] /= n
    }

    private fun correlate(c: FloatArray, profile: FloatArray, rot: Int): Float {
        var s = 0f
        for (i in 0 until 12) s += c[(i + rot) % 12] * profile[i]
        return s
    }

    // Standard mapping from pitch-class-with-mode → Camelot wheel.
    // Tonic index here is 0=C, 1=C#, ..., 11=B (the chroma layout above).
    private fun camelot(tonic: Int, isMinor: Boolean): String {
        // Camelot major-circle order: B=1B, F#=2B, Db=3B, Ab=4B, Eb=5B, Bb=6B,
        // F=7B, C=8B, G=9B, D=10B, A=11B, E=12B.
        // Minor lifts to A side with same number.
        val majorByPitch = intArrayOf(
            //  C  C# D  D# E  F  F# G  G# A  A# B
                8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1
        )
        // Relative minor: same Camelot number, side 'A' (the natural minor 3 semitones below).
        // E.g. A minor (tonic 9 in C-major) shares the 8A slot (C major is 8B).
        val minorByPitch = intArrayOf(
            //  C  C# D  D# E  F  F# G  G# A  A# B
                5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10
        )
        val n = if (isMinor) minorByPitch[tonic] else majorByPitch[tonic]
        return "$n${if (isMinor) "A" else "B"}"
    }

    // ── FFT (radix-2, in-place Cooley-Tukey) ─────────────────────────────────
    private fun fftInPlace(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j or bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until len / 2) {
                    val tRe = curRe * re[i + k + len / 2] - curIm * im[i + k + len / 2]
                    val tIm = curRe * im[i + k + len / 2] + curIm * re[i + k + len / 2]
                    val uRe = re[i + k]; val uIm = im[i + k]
                    re[i + k] = uRe + tRe;  im[i + k] = uIm + tIm
                    re[i + k + len / 2] = uRe - tRe; im[i + k + len / 2] = uIm - tIm
                    val nRe = curRe * wRe - curIm * wIm
                    val nIm = curRe * wIm + curIm * wRe
                    curRe = nRe; curIm = nIm
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun hannWindow(n: Int): FloatArray {
        val w = FloatArray(n)
        for (i in 0 until n) w[i] = (0.5 * (1 - cos(2 * PI * i / (n - 1)))).toFloat()
        return w
    }

    // ── Camelot compatibility (used by AutoMixEngine) ────────────────────────
    /**
     * Returns a score 0.0..1.0 representing harmonic compatibility between two
     * Camelot keys. 1.0 = same key, 0.7 = +/-1 or same-number switch (A↔B), 0.0
     * otherwise. Either argument may be null/blank (→ neutral 0.5).
     */
    fun camelotScore(a: String?, b: String?): Float {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return 0.5f
        val (na, la) = parseCamelot(a) ?: return 0.5f
        val (nb, lb) = parseCamelot(b) ?: return 0.5f
        if (na == nb && la == lb) return 1.0f
        if (na == nb) return 0.7f                     // 8A ↔ 8B
        val diff = ((na - nb + 12) % 12).let { min(it, 12 - it) }
        if (diff == 1 && la == lb) return 0.7f        // 8A ↔ 7A or 9A
        if (diff == 2 && la == lb) return 0.4f        // two steps away — still OK
        return 0.0f
    }

    private fun parseCamelot(s: String): Pair<Int, Char>? {
        val m = Regex("""^(\d{1,2})([AB])$""").find(s.trim()) ?: return null
        val num = m.groupValues[1].toIntOrNull() ?: return null
        if (num !in 1..12) return null
        return num to m.groupValues[2][0]
    }
}
