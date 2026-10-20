package com.vardiya.sludge.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class FilterType(val label: String, private val names: Set<String>) {
    PEAK("Tepe", setOf("peak", "peaking", "bell", "pk", "pea")),
    LOW_SHELF("Alçak raf", setOf("lowshelf", "ls", "lsc", "lowshelving")),
    HIGH_SHELF("Yüksek raf", setOf("highshelf", "hs", "hsc", "highshelving"));

    /** The name written in the saved settings and in the AI's answer. */
    val key: String get() = names.first()

    companion object {
        /** Reads a name the way models write it ("Low Shelf", "low_shelf", "LSC"); null when it is none of them. */
        fun fromName(name: String): FilterType? {
            val squashed = name.lowercase().filter { it.isLetter() }
            return entries.firstOrNull { squashed in it.names }
        }
    }
}

/** One band of a parametric equalizer: a type, a centre or corner frequency, a gain and a Q (the narrowness). */
@Serializable
data class ParametricFilter(
    val type: String = FilterType.PEAK.key,
    @SerialName("freq") val freqHz: Float = DEFAULT_HZ,
    @SerialName("gain") val gainDb: Float = 0f,
    val q: Float = DEFAULT_Q,
) {
    val kind: FilterType get() = FilterType.fromName(type) ?: FilterType.PEAK

    /** The same filter with every value inside what the equalizer and the screen accept. */
    fun clamped(): ParametricFilter = ParametricFilter(
        type = kind.key,
        freqHz = freqHz.coerceIn(MIN_HZ, MAX_HZ),
        gainDb = gainDb.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB),
        q = q.coerceIn(MIN_Q, MAX_Q),
    )

    val isFinite: Boolean get() = freqHz.isFinite() && gainDb.isFinite() && q.isFinite()

    companion object {
        const val MIN_HZ = 20f
        const val MAX_HZ = 20_000f
        const val MIN_Q = 0.3f
        const val MAX_Q = 10f
        const val DEFAULT_HZ = 1_000f
        const val DEFAULT_Q = 1f
    }
}

/** The magnitude response of a biquad filter (the standard "Audio EQ Cookbook" shapes), in dB at a frequency. */
object Biquad {
    private const val SAMPLE_RATE = 48_000.0

    fun responseDb(filter: ParametricFilter, hz: Float): Float {
        val f = filter.clamped()
        val a = 10.0.pow(f.gainDb / 40.0)
        val w0 = 2.0 * PI * f.freqHz / SAMPLE_RATE
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * f.q)
        val sqrtA2Alpha = 2.0 * sqrt(a) * alpha
        val b: DoubleArray
        val c: DoubleArray
        when (f.kind) {
            FilterType.PEAK -> {
                b = doubleArrayOf(1 + alpha * a, -2 * cosW0, 1 - alpha * a)
                c = doubleArrayOf(1 + alpha / a, -2 * cosW0, 1 - alpha / a)
            }
            FilterType.LOW_SHELF -> {
                b = doubleArrayOf(
                    a * ((a + 1) - (a - 1) * cosW0 + sqrtA2Alpha),
                    2 * a * ((a - 1) - (a + 1) * cosW0),
                    a * ((a + 1) - (a - 1) * cosW0 - sqrtA2Alpha),
                )
                c = doubleArrayOf(
                    (a + 1) + (a - 1) * cosW0 + sqrtA2Alpha,
                    -2 * ((a - 1) + (a + 1) * cosW0),
                    (a + 1) + (a - 1) * cosW0 - sqrtA2Alpha,
                )
            }
            FilterType.HIGH_SHELF -> {
                b = doubleArrayOf(
                    a * ((a + 1) + (a - 1) * cosW0 + sqrtA2Alpha),
                    -2 * a * ((a - 1) + (a + 1) * cosW0),
                    a * ((a + 1) + (a - 1) * cosW0 - sqrtA2Alpha),
                )
                c = doubleArrayOf(
                    (a + 1) - (a - 1) * cosW0 + sqrtA2Alpha,
                    2 * ((a - 1) - (a + 1) * cosW0),
                    (a + 1) - (a - 1) * cosW0 - sqrtA2Alpha,
                )
            }
        }
        val w = 2.0 * PI * hz.coerceIn(1f, (SAMPLE_RATE / 2 - 1).toFloat()) / SAMPLE_RATE
        return (10.0 * log10(power(b, w) / power(c, w))).toFloat()
    }

    // |x0 + x1 e^{-jw} + x2 e^{-2jw}|^2
    private fun power(x: DoubleArray, w: Double): Double {
        val re = x[0] + x[1] * cos(w) + x[2] * cos(2 * w)
        val im = -(x[1] * sin(w) + x[2] * sin(2 * w))
        return re * re + im * im
    }
}

object Parametric {
    const val MAX_FILTERS = 10

    /** The curve of all filters together: their responses add in dB. */
    fun responseDb(filters: List<ParametricFilter>, hz: Float): Float = filters.sumOf { Biquad.responseDb(it, hz).toDouble() }.toFloat()

    fun sample(filters: List<ParametricFilter>, hz: List<Float>): List<Float> = hz.map { responseDb(filters, it) }

    /** The ten graphic bands written as ten peaking filters, so a curve can be taken over into the parametric list. */
    fun fromBands(bands: List<Float>): List<ParametricFilter> = bands.mapIndexedNotNull { i, gain ->
        if (abs(gain) < MIN_AUDIBLE_DB) null else ParametricFilter(FilterType.PEAK.key, BAND_HZ[i], gain, BAND_Q).clamped()
    }

    /** Where the engine samples the curve: the 31 third-octave centres from 20 Hz to 20 kHz. */
    val ENGINE_HZ: List<Float> = listOf(
        20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f, 800f, 1_000f, 1_250f,
        1_600f, 2_000f, 2_500f, 3_150f, 4_000f, 5_000f, 6_300f, 8_000f, 10_000f, 12_500f, 16_000f, 20_000f,
    )

    /** The upper edge of each engine band: halfway between centres on a log axis, the last one just under Nyquist. */
    val ENGINE_EDGES_HZ: List<Float> = ENGINE_HZ.mapIndexed { i, hz ->
        if (i == ENGINE_HZ.lastIndex) TOP_EDGE_HZ else sqrt(hz * ENGINE_HZ[i + 1])
    }

    private const val MIN_AUDIBLE_DB = 0.25f
    private const val BAND_Q = 1.4f
    private const val TOP_EDGE_HZ = 21_000f
}

/** How the filter sliders map to values: frequency and Q on a log scale (equal steps are equal ratios), gain linear. */
object FilterScale {
    const val MAX = 1_000
    const val GAIN_MAX = 480
    const val KILO = 1_000f
    const val HUNDRED = 100f
    const val TEN = 10f
    private const val GAIN_UNITS_PER_DB = 20f

    fun freqToSlider(hz: Float): Int = logToSlider(hz, ParametricFilter.MIN_HZ, ParametricFilter.MAX_HZ)

    fun sliderToFreq(v: Int): Float = sliderToLog(v, ParametricFilter.MIN_HZ, ParametricFilter.MAX_HZ).roundToInt().toFloat()

    fun qToSlider(q: Float): Int = logToSlider(q, ParametricFilter.MIN_Q, ParametricFilter.MAX_Q)

    /** One decimal, as shown. */
    fun sliderToQ(v: Int): Float = (sliderToLog(v, ParametricFilter.MIN_Q, ParametricFilter.MAX_Q) * TEN).roundToInt() / TEN

    fun gainToSlider(db: Float): Int = ((db + MAX_GAIN_DB) * GAIN_UNITS_PER_DB).roundToInt().coerceIn(0, GAIN_MAX)

    fun sliderToGain(v: Int): Float = v / GAIN_UNITS_PER_DB - MAX_GAIN_DB

    private fun logToSlider(x: Float, lo: Float, hi: Float): Int =
        (MAX * (ln(x / lo) / ln(hi / lo))).roundToInt().coerceIn(0, MAX)

    private fun sliderToLog(v: Int, lo: Float, hi: Float): Float = lo * (hi / lo).pow(v.toFloat() / MAX)
}
