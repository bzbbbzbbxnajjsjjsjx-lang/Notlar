// The file holds the bass shelf with the curve maths that goes with it (final curve, limiter gain).
@file:Suppress("MatchingDeclarationName")

package com.vardiya.sludge.domain

/**
 * The bass knob as a low shelf filter. Android's BassBoost effect is optional and on many phones does nothing at all,
 * while the equalizer always works, so the bass knob is built from a filter instead. A shelf's gain is reached well
 * below its corner, so the corner sits above the bass it is meant to lift.
 */
object Bass {
    /** The shelf's gain when the knob is all the way up (dB). */
    const val MAX_SHELF_DB = 4.5f
    const val CORNER_HZ = 150f
    private const val SHELF_Q = 0.7f

    /** The shelf for a knob position (0 up to [MAX_EFFECT]); null when the knob is down. */
    fun filter(strength: Int): ParametricFilter? {
        val amount = strength.coerceIn(0, MAX_EFFECT) / MAX_EFFECT.toFloat() * MAX_SHELF_DB
        return if (amount <= 0f) null else ParametricFilter(FilterType.LOW_SHELF.key, CORNER_HZ, amount, SHELF_Q)
    }
}

/**
 * The curve the sound really gets, read at [hz]: the person's parametric filters plus the bass shelf, plus the headphone
 * correction (with its headroom) while it is active. Never above +12 dB.
 */
fun EqSettings.curveAt(route: OutputRoute, hz: List<Float>): List<Float> {
    val user = Parametric.sample(filters + listOfNotNull(Bass.filter(bassBoost)), hz)
    val profile = Headphones.byId(headphoneId)?.takeIf { correctionActive(route) }
    val strength = headphoneStrength / PERCENT_FULL
    val preamp = profile?.preampDb(strength) ?: 0f
    return hz.indices.map { i ->
        val correction = if (profile == null) 0f else profile.appliedAt(hz[i], strength) + preamp
        (user[i] + correction).coerceAtMost(MAX_GAIN_DB)
    }
}

/** The make-up gain the limiter applies after the bands, in dB, from the loudness setting. */
fun EqSettings.limiterGainDb(): Float = loudnessMb.coerceIn(0, MAX_LOUDNESS_MB) / DB_TO_MB

private const val DB_TO_MB = 100f
