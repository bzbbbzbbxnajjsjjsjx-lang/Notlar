package com.vardiya.sludge.domain

const val MAX_GAIN_DB = 12f
const val MAX_EFFECT = 1000
const val MAX_LOUDNESS_MB = 1000

/** Where the current setting came from. */
enum class TuneSource(val label: String) {
    MANUAL("Elle"),
    PRESET("Ön ayar"),
}

/** Everything the equalizer needs to sound the way it does. */
data class EqSettings(
    val enabled: Boolean = true,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessMb: Int = 0,
    val genre: String = "",
    val source: TuneSource = TuneSource.MANUAL,
    /** The headphone whose measured response is corrected toward the Harman target; null = no correction. */
    val headphoneId: String? = Headphones.DEFAULT_ID,
    /** Apply the correction on every output, not only when the headphone is the one connected. */
    val headphoneAlways: Boolean = false,
    /** How much of the correction is applied, in percent: less of it costs less loudness. */
    val headphoneStrength: Int = 100,
    /** The parametric filters the sound is shaped by. */
    val filters: List<ParametricFilter> = emptyList(),
)
