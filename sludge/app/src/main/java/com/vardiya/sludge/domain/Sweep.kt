package com.vardiya.sludge.domain

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** A slow sine sweep the app plays itself, through its own equalizer, so a headphone can be checked by ear. */
enum class SweepKind(val label: String, val fromHz: Float, val toHz: Float, val seconds: Float) {
    BASS("Alt bas taraması (20–200 Hz)", 20f, 200f, 30f),
    FULL("Tüm aralık (20 Hz–20 kHz)", 20f, 20_000f, 40f),
}

/** What the tone player is doing: nothing, or which sweep and the frequency it is at. */
data class ToneState(val kind: SweepKind? = null, val hz: Float = 0f)

object Sweep {
    const val SAMPLE_RATE = 48_000

    /** About -24 dBFS: loud enough to hear a struggling driver, far too quiet to hurt. */
    const val AMPLITUDE = 0.06f
    private const val FADE_SECONDS = 0.5f
    private const val PCM_MAX = 32_767f

    fun totalSamples(kind: SweepKind): Int = (kind.seconds * SAMPLE_RATE).roundToInt()

    /** The frequency at [seconds] into the sweep: exponential, so every octave takes the same time. */
    fun frequencyAt(kind: SweepKind, seconds: Float): Float {
        val t = (seconds / kind.seconds).coerceIn(0f, 1f)
        return kind.fromHz * (kind.toHz / kind.fromHz).pow(t)
    }

    /**
     * Fills [out] with the samples from [startSample] on (the rest of the sweep stays silent past its end) and returns the
     * phase to hand to the next call, so consecutive chunks join without a click.
     */
    fun fill(kind: SweepKind, startSample: Int, phase: Double, out: ShortArray): Double {
        var p = phase
        val total = totalSamples(kind)
        for (i in out.indices) {
            val n = startSample + i
            if (n >= total) {
                out[i] = 0
                continue
            }
            val t = n.toFloat() / SAMPLE_RATE
            p += 2.0 * PI * frequencyAt(kind, t) / SAMPLE_RATE
            val fade = minOf(1f, t / FADE_SECONDS, (kind.seconds - t) / FADE_SECONDS).coerceAtLeast(0f)
            out[i] = (sin(p) * AMPLITUDE * fade * PCM_MAX).toInt().toShort()
        }
        return p % (2.0 * PI)
    }
}

/** The general bass and treble trim: a person's own correction for how their pair differs, on top of any preset. */
object Trim {
    const val MAX_DB = 3f
    const val SLIDER_MAX = 120
    private const val STEPS_PER_DB = 20f
    const val BASS_CORNER_HZ = 150f
    const val TREBLE_CORNER_HZ = 8_000f
    private const val SHELF_Q = 0.7f

    fun clamp(db: Float): Float = db.coerceIn(-MAX_DB, MAX_DB)

    /** Slider position (0..[SLIDER_MAX], half-decibel steps) for a trim in dB. */
    fun toSlider(db: Float): Int = ((clamp(db) + MAX_DB) * STEPS_PER_DB).roundToInt()

    fun fromSlider(position: Int): Float = clamp(position / STEPS_PER_DB - MAX_DB)

    /** The two shelves for a trim, leaving out one that is zero. */
    fun filters(bassDb: Float, trebleDb: Float): List<ParametricFilter> = listOfNotNull(
        bassDb.takeIf { it != 0f }?.let { ParametricFilter(FilterType.LOW_SHELF.key, BASS_CORNER_HZ, clamp(it), SHELF_Q) },
        trebleDb.takeIf { it != 0f }?.let { ParametricFilter(FilterType.HIGH_SHELF.key, TREBLE_CORNER_HZ, clamp(it), SHELF_Q) },
    )
}
