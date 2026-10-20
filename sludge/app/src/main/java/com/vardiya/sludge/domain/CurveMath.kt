package com.vardiya.sludge.domain

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/** Pure maths that moves the five canonical bands onto whatever bands the device equalizer has. */
object CurveMath {
    /**
     * Gain in dB at each of [deviceHz], read off the canonical [bands] by linear interpolation on a log frequency
     * axis. Outside the canonical range the end values hold.
     */
    fun mapToDevice(bands: List<Float>, deviceHz: List<Float>): List<Float> {
        require(bands.size == BAND_COUNT) { "expected $BAND_COUNT bands, got ${bands.size}" }
        return resample(bands, BAND_HZ, deviceHz)
    }

    /** The same log-frequency reading for any pair of frequency lists. */
    fun resample(values: List<Float>, valuesHz: List<Float>, targetHz: List<Float>): List<Float> =
        targetHz.map { hz -> interpolate(values, valuesHz, hz) }

    private fun interpolate(values: List<Float>, valuesHz: List<Float>, hz: Float): Float {
        if (hz <= valuesHz.first()) return values.first()
        if (hz >= valuesHz.last()) return values.last()
        val upper = valuesHz.indexOfFirst { it >= hz }
        val lower = upper - 1
        val t = (ln(hz) - ln(valuesHz[lower])) / (ln(valuesHz[upper]) - ln(valuesHz[lower]))
        return values[lower] + (values[upper] - values[lower]) * t
    }

    /** dB to the millibels the Android API wants, clamped to the range the device reports. */
    fun toMillibels(db: Float, minMb: Int, maxMb: Int): Int = (db * 100f).roundToInt().coerceIn(minMb, maxMb)

    /** Snaps to half decibels, which is as fine as the on-screen columns go. */
    fun snap(db: Float): Float = (db * 2f).roundToInt() / 2f

    /**
     * Loudness gain after taking the headroom into account: every dB the curve boosts above +6 dB costs half a
     * decibel of loudness gain, so a hot curve plus a loudness boost cannot clip.
     */
    fun safeLoudness(bands: List<Float>, loudnessMb: Int): Int {
        val peak = bands.maxOrNull() ?: 0f
        val over = (peak - 6f).coerceAtLeast(0f)
        return (loudnessMb - (over * 50f).roundToInt()).coerceIn(0, MAX_LOUDNESS_MB)
    }

    /** True when [a] and [b] differ by less than [tolerance] dB on every band. */
    fun sameCurve(a: List<Float>, b: List<Float>, tolerance: Float = 0.25f): Boolean =
        a.size == b.size && a.indices.all { abs(a[it] - b[it]) < tolerance }
}
