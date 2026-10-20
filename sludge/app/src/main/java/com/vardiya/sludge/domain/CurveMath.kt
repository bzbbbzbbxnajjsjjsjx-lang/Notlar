package com.vardiya.sludge.domain

import kotlin.math.roundToInt

/** Small conversions between the curve in dB and what the Android effects take. */
object CurveMath {
    /** dB to the millibels the Android API wants, clamped to the range the device reports. */
    fun toMillibels(db: Float, minMb: Int, maxMb: Int): Int = (db * 100f).roundToInt().coerceIn(minMb, maxMb)
}
