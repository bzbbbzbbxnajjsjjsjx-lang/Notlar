package com.vardiya.sludge.domain

/**
 * A correction that moves a headphone's measured sound toward a target curve. [correction] is the gain in dB at each of
 * the ten bands ([BAND_HZ]); [preampDb] is the headroom the correction asks for, so its boosts cannot clip.
 */
data class HeadphoneProfile(
    val id: String,
    val name: String,
    /** Where the numbers come from, shown to the person. */
    val source: String,
    val preampDb: Float,
    val correction: List<Float>,
    /** Other names the Bluetooth device may announce itself with. */
    val aliases: List<String> = emptyList(),
) {
    init {
        require(correction.size == BAND_COUNT) { "expected $BAND_COUNT bands, got ${correction.size}" }
    }

    /** The curve the person chose with the headphone's correction and preamp added on top. */
    fun applyTo(bands: List<Float>): List<Float> = bands.mapIndexed { i, gain -> gain + correction[i] + preampDb }
}

object Headphones {
    /**
     * JBL Tune 520BT toward the Harman over-ear 2018 target. The ten values are the "Fixed Band EQ" that AutoEq
     * (github.com/jaakkopasanen/AutoEq, MIT) computed from Rtings' B&K 5128 measurement: preamp -6.1 dB.
     * One unit's measurement: another pair of the same model can differ by a decibel or two.
     */
    val JBL_TUNE_520BT = HeadphoneProfile(
        id = "jbl_tune_520bt",
        name = "JBL Tune 520BT",
        source = "AutoEq, Rtings ölçümü, Harman over-ear 2018 hedefi",
        preampDb = -6.1f,
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
