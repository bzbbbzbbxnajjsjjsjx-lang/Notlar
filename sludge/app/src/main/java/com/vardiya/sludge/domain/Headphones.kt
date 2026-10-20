package com.vardiya.sludge.domain

import kotlin.math.min

/**
 * A correction that moves a headphone's measured sound toward a target curve, written as the parametric filters a
 * measurement-based tool computed for it ([filters], exactly as published).
 *
 * The curve's boost is capped at [maxBoostDb]: the biggest boosts sit at the very bottom, where a small headphone
 * cannot deliver and where a boost would only eat headroom. The headroom itself is worked out for the whole curve in
 * `EqSettings.headroomDb`, not taken from the tool.
 */
data class HeadphoneProfile(
    val id: String,
    val name: String,
    /** Where the numbers come from, shown to the person. */
    val source: String,
    val filters: List<ParametricFilter>,
    /** Other names the Bluetooth device may announce itself with. */
    val aliases: List<String> = emptyList(),
    val maxBoostDb: Float = DEFAULT_MAX_BOOST_DB,
) {
    /** The tool's own correction at [hz], before any cap. */
    fun rawAt(hz: Float): Float = Parametric.responseDb(filters, hz)

    /** The correction as applied at [hz]: the boost is capped, cuts are not. */
    fun appliedAt(hz: Float): Float = min(rawAt(hz), maxBoostDb)

    companion object {
        const val DEFAULT_MAX_BOOST_DB = 3f
    }
}

object Headphones {
    /**
     * JBL Tune 520BT toward the Harman over-ear 2018 target. The ten filters are AutoEq's "ParametricEQ" result
     * (github.com/jaakkopasanen/AutoEq, MIT) for Rtings' Bruel & Kjaer 5128 measurement. AutoEq's own preamp for it was
     * -6.4 dB, which came mostly from the sub-bass boost (+6.4 dB at 20 Hz, where this headphone measures -6 dB and
     * cannot play); that boost is capped here, so the headroom is about 3 dB. One unit's measurement: another pair of the
     * same model can differ by a decibel or two.
     */
    val JBL_TUNE_520BT = HeadphoneProfile(
        id = "jbl_tune_520bt",
        name = "JBL Tune 520BT",
        source = "AutoEq, Rtings ölçümü, Harman over-ear 2018 hedefi",
        filters = listOf(
            ParametricFilter(FilterType.LOW_SHELF.key, 105f, 8.0f, 0.70f),
            ParametricFilter(FilterType.PEAK.key, 62f, -8.3f, 0.79f),
            ParametricFilter(FilterType.PEAK.key, 3_532f, -6.8f, 2.66f),
            ParametricFilter(FilterType.PEAK.key, 2_056f, 2.0f, 2.83f),
            ParametricFilter(FilterType.PEAK.key, 4_892f, 3.8f, 0.93f),
            ParametricFilter(FilterType.HIGH_SHELF.key, 10_000f, 3.0f, 0.70f),
            ParametricFilter(FilterType.PEAK.key, 944f, -1.5f, 3.44f),
            ParametricFilter(FilterType.PEAK.key, 173f, 0.9f, 3.12f),
            ParametricFilter(FilterType.PEAK.key, 48f, -1.0f, 2.47f),
            ParametricFilter(FilterType.PEAK.key, 60f, 1.4f, 4.62f),
        ),
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
