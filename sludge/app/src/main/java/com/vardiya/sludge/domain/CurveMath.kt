package com.vardiya.sludge.domain

import kotlin.math.roundToInt

/** Small conversions between the curve in dB and what the Android effects take. */
object CurveMath {
    /** dB to the millibels the Android API wants, clamped to the range the device reports. */
    fun toMillibels(db: Float, minMb: Int, maxMb: Int): Int = (db * 100f).roundToInt().coerceIn(minMb, maxMb)

    /**
     * Loudness gain after taking the headroom into account: every decibel the curve boosts above +6 dB costs half a
     * decibel of loudness gain, so a hot curve plus a loudness boost cannot clip.
     */
    fun safeLoudness(curve: List<Float>, loudnessMb: Int): Int {
        val peak = curve.maxOrNull() ?: 0f
        val over = (peak - 6f).coerceAtLeast(0f)
        return (loudnessMb - (over * 50f).roundToInt()).coerceIn(0, MAX_LOUDNESS_MB)
    }
}
