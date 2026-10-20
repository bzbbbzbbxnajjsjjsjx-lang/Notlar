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

/** The curve before any headroom: the person's filters, the bass shelf and the headphone correction, read at [hz]. */
private fun EqSettings.shapeAt(route: OutputRoute, hz: List<Float>): List<Float> {
    val user = Parametric.sample(filters + listOfNotNull(Bass.filter(bassBoost)), hz)
    val profile = Headphones.byId(headphoneId)?.takeIf { correctionActive(route) }
    return hz.indices.map { i -> user[i] + (profile?.appliedAt(hz[i]) ?: 0f) }
}

/** Below this the headroom does not look: the bottom octave carries little of the music and a small headphone cannot play it. */
const val HEADROOM_FROM_HZ = 40f

/** What the limiter is trusted with: music is rarely at full scale across a whole boosted band, so the last dB are not given up. */
const val HEADROOM_SLACK_DB = 3f

/**
 * How many dB the whole sound is turned down so that what the curve boosts does not clip: its highest point from
 * [HEADROOM_FROM_HZ] up, less [HEADROOM_SLACK_DB] (never below zero). It follows the filters, the bass knob and the
 * headphone correction together, so the level stays comparable whichever preset is chosen.
 */
fun EqSettings.headroomDb(route: OutputRoute = OutputRoute.UNKNOWN): Float {
    // A narrow peak can fall between two grid points, so every filter's own centre is looked at too.
    val centres = filters.map { it.freqHz } + Bass.CORNER_HZ
    val at = (Parametric.GRID_HZ + centres).filter { it >= HEADROOM_FROM_HZ }
    return maxOf(0f, shapeAt(route, at).max() - HEADROOM_SLACK_DB)
}

/**
 * The curve the sound really gets, read at [hz]: the shape minus the headroom. From [HEADROOM_FROM_HZ] up its highest
 * point is at most [HEADROOM_SLACK_DB]; the loudness setting (the limiter's make-up gain) brings the level back up.
 */
fun EqSettings.curveAt(route: OutputRoute, hz: List<Float>): List<Float> {
    val headroom = headroomDb(route)
    return shapeAt(route, hz).map { it - headroom }
}

/** The make-up gain the limiter applies after the bands, in dB, from the loudness setting. */
fun EqSettings.limiterGainDb(): Float = loudnessMb.coerceIn(0, MAX_LOUDNESS_MB) / DB_TO_MB

private const val DB_TO_MB = 100f
