// The file holds the bass shelf with the band maths that goes with it (edges, final bands, limiter gain).
@file:Suppress("MatchingDeclarationName")

package com.vardiya.sludge.domain

import kotlin.math.sqrt

/**
 * The bass knob as a low shelf on the equalizer's own bands. Android's BassBoost effect is optional and on many phones
 * does nothing at all, while the equalizer always works, so the bass knob is built from the bands instead.
 */
object Bass {
    /** The shelf's gain at the lowest bands when the knob is all the way up (dB). */
    const val MAX_SHELF_DB = 4f

    // How much of the shelf each band gets: full at 31 and 62 Hz, tapering out by 500 Hz.
    private val weights = listOf(1f, 1f, 0.7f, 0.35f, 0.1f, 0f, 0f, 0f, 0f, 0f)

    /** The shelf for a knob position (0 up to [MAX_EFFECT]), one gain per band. */
    fun shelf(strength: Int): List<Float> {
        val amount = strength.coerceIn(0, MAX_EFFECT) / MAX_EFFECT.toFloat() * MAX_SHELF_DB
        return weights.map { it * amount }
    }
}

/** The upper edge of each of the ten bands (Hz): halfway between neighbours on a log axis, the last one at 20 kHz. */
val BAND_EDGES_HZ: List<Float> = BAND_HZ.mapIndexed { i, hz ->
    if (i == BAND_HZ.lastIndex) TOP_HZ else sqrt(hz * BAND_HZ[i + 1])
}

private const val TOP_HZ = 20_000f

/** The gain on every band the sound really gets: the headphone-corrected curve plus the bass shelf, never above +12 dB. */
fun EqSettings.finalBands(route: OutputRoute = OutputRoute.UNKNOWN): List<Float> {
    val shelf = Bass.shelf(bassBoost)
    return effectiveBands(route).mapIndexed { i, gain -> (gain + shelf[i]).coerceAtMost(MAX_GAIN_DB) }
}

/** The make-up gain the limiter applies after the bands, in dB, from the loudness setting. */
fun EqSettings.limiterGainDb(): Float = loudnessMb.coerceIn(0, MAX_LOUDNESS_MB) / DB_TO_MB

private const val DB_TO_MB = 100f
