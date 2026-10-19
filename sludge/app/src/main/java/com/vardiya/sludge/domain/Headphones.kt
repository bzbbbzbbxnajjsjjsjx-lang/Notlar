package com.vardiya.sludge.domain

import kotlin.math.min

/**
 * A correction that moves a headphone's measured sound toward a target curve. [correction] is the gain in dB at each
 * of the ten bands ([BAND_HZ]) exactly as the measurement-based tool computed it.
 *
 * Two things keep it from costing more loudness than it is worth. A boost is capped at [maxBoostDb]: the boosts at
 * the very ends (31 Hz, 16 kHz) are where a small headphone cannot deliver and where a boost eats the most headroom.
 * And the headroom (the preamp) is worked out from the boost that is really applied, not taken from the tool.
 */
data class HeadphoneProfile(
    val id: String,
    val name: String,
    /** Where the numbers come from, shown to the person. */
    val source: String,
    val correction: List<Float>,
    /** Other names the Bluetooth device may announce itself with. */
    val aliases: List<String> = emptyList(),
    val maxBoostDb: Float = DEFAULT_MAX_BOOST_DB,
) {
    init {
        require(correction.size == BAND_COUNT) { "expected $BAND_COUNT bands, got ${correction.size}" }
    }

    /** The correction as applied: boosts capped, everything scaled by [strength] (0 = none, 1 = all of it). */
    fun appliedCorrection(strength: Float = 1f): List<Float> = correction.map { min(it, maxBoostDb) * strength.coerceIn(0f, 1f) }

    /** The headroom: as many dB down as the largest boost that is applied, so the boosts cannot clip. */
    fun preampDb(strength: Float = 1f): Float = -maxOf(0f, appliedCorrection(strength).max())

    /** The curve the person chose with the correction and its headroom added on top. */
    fun applyTo(bands: List<Float>, strength: Float = 1f): List<Float> {
        val applied = appliedCorrection(strength)
        val preamp = preampDb(strength)
        return bands.mapIndexed { i, gain -> gain + applied[i] + preamp }
    }

    companion object {
        const val DEFAULT_MAX_BOOST_DB = 3f
    }
}

object Headphones {
    /**
     * JBL Tune 520BT toward the Harman over-ear 2018 target. The ten values are the "Fixed Band EQ" that AutoEq
     * (github.com/jaakkopasanen/AutoEq, MIT) computed from Rtings' B&K 5128 measurement; AutoEq's own preamp for it was
     * -6.1 dB, which came from the two end boosts that are capped here. One unit's measurement: another pair of the same
     * model can differ by a decibel or two.
     */
    val JBL_TUNE_520BT = HeadphoneProfile(
        id = "jbl_tune_520bt",
        name = "JBL Tune 520BT",
        source = "AutoEq, Rtings ölçümü, Harman over-ear 2018 hedefi",
        correction = listOf(6.6f, -2.9f, -0.3f, -0.4f, 0.6f, -1.5f, 2.1f, -1.9f, 2.8f, 5.2f),
        aliases = listOf("JBL T520BT", "JBL 520BT", "520BT"),
    )

    val all: List<HeadphoneProfile> = listOf(JBL_TUNE_520BT)

    /** The profile a fresh install starts with. */
    const val DEFAULT_ID = "jbl_tune_520bt"

    fun byId(id: String?): HeadphoneProfile? = all.firstOrNull { it.id == id }

    /** What was stored: nothing yet means the default, an empty string means the person chose none. */
    fun fromStored(stored: String?): String? = when (stored) {
        null -> DEFAULT_ID
        "" -> null
        else -> stored
    }
}
