// Virtualizer is deprecated on the newest API levels but still the only widening effect every device has.
@file:Suppress("DEPRECATION")

package com.vardiya.sludge.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.RequiresApi
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.Parametric
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.EngineState
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.curveAt
import com.vardiya.sludge.domain.engineCurve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Puts the sound of every audio session a player announces through the best effect the phone has, and keeps it in
 * step with the settings.
 *
 * First choice is DynamicsProcessing (Android 9+): 31 real bands (the parametric filters sampled on them, with the
 * loudness setting added to their gain) and a limiter after them that catches the peaks. If a phone refuses it, the
 * classic Equalizer and LoudnessEnhancer are used. The bass knob is a low shelf filter in both cases. The Virtualizer is
 * separate and optional. Calls are serialised by the caller.
 */
class EffectEngine {
    private class Chain(val session: Int, val packageName: String) {
        var dp: DynamicsProcessing? = null
        var eq: Equalizer? = null
        var virt: Virtualizer? = null
        var loud: LoudnessEnhancer? = null

        val failed get() = dp == null && eq == null && virt == null && loud == null

        val path get() = when {
            dp != null -> "DynamicsProcessing"
            eq != null -> "Ekolayzır"
            else -> ""
        }

        fun effects(): List<String> = buildList {
            if (dp != null) add("${Parametric.ENGINE_HZ.size} bantlı ekolayzır")
            if (dp != null) add("sınırlayıcı (ses yükseltici)")
            if (eq != null) add("${eq?.numberOfBands} bantlı ekolayzır")
            if (loud != null) add("ses yükseltici")
            if (virt != null) add("sanallaştırıcı")
        }

        fun release() {
            listOf<AudioEffect?>(dp, eq, virt, loud).forEach { runCatching { it?.release() } }
            dp = null; eq = null; virt = null; loud = null
        }
    }

    private val chains = LinkedHashMap<Int, Chain>()
    private var route = OutputRoute.UNKNOWN
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    @Synchronized
    fun attach(session: Int, packageName: String, settings: EqSettings) {
        chains.remove(session)?.release()
        val chain = Chain(session, packageName)
        // Priority 0 is the normal level; the effects are created disabled and switched on by apply().
        chain.dp = createDynamics(session)
        if (chain.dp == null) createClassic(chain)
        chain.virt = create { Virtualizer(0, session) }
        chains[session] = chain
        // A player that crashes never sends CLOSE; drop the oldest sessions so their native effects are freed.
        while (chains.size > MAX_SESSIONS) chains.remove(chains.keys.first())?.release()
        apply(chain, settings)
        publish()
    }

    @Synchronized
    fun detach(session: Int) {
        chains.remove(session)?.release()
        publish()
    }

    @Synchronized
    fun apply(settings: EqSettings, route: OutputRoute) {
        this.route = route
        chains.values.forEach { apply(it, settings) }
        publish()
    }

    @Synchronized
    fun releaseAll() {
        chains.values.forEach { it.release() }
        chains.clear()
        publish()
    }

    private fun apply(chain: Chain, s: EqSettings) {
        val on = s.enabled
        // The curve is read at the engine's 31 bands; the classic equalizer reads it again at its own centres.
        val bands = s.engineCurve(route, Parametric.ENGINE_HZ)
        // A phone that accepts DynamicsProcessing but then refuses its settings falls back to the classic effects.
        val dp = chain.dp
        if (dp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !applyDynamics(dp, s, bands)) {
            runCatching { dp.release() }
            chain.dp = null
            createClassic(chain)
        }
        chain.eq?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                val centers = (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / MILLI_HZ }
                s.curveAt(route, centers).forEachIndexed { band, db ->
                    eq.setBandLevel(band.toShort(), CurveMath.toMillibels(db, range[0].toInt(), range[1].toInt()).toShort())
                }
                eq.enabled = on
            }
        }
        chain.loud?.let { loud ->
            runCatching {
                val gain = s.loudnessMb.coerceIn(0, MAX_LOUDNESS_MB)
                loud.setTargetGain(gain)
                loud.enabled = on && gain > 0
            }
        }
        chain.virt?.let { virt ->
            runCatching {
                if (virt.strengthSupported) virt.setStrength(s.virtualizer.toShort())
                virt.enabled = on && s.virtualizer > 0
            }
        }
    }

    /**
     * 31 bands (with the loudness already in their gain) before the limiter, which then catches the peaks. False when
     * the phone refuses it.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyDynamics(dp: DynamicsProcessing, s: EqSettings, bands: List<Float>): Boolean = try {
        bands.forEachIndexed { i, db ->
            dp.setPreEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, Parametric.ENGINE_EDGES_HZ[i], db))
        }
        dp.setLimiterAllChannelsTo(
            DynamicsProcessing.Limiter(true, true, LIMITER_LINK_GROUP, ATTACK_MS, RELEASE_MS, RATIO, THRESHOLD_DB, 0f),
        )
        dp.enabled = s.enabled
        true
    } catch (_: RuntimeException) {
        false
    }

    private fun createDynamics(session: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return create {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, CHANNELS,
                true, Parametric.ENGINE_HZ.size, // pre-EQ
                false, 0, // multiband compressor
                false, 0, // post-EQ
                true, // limiter
            ).build()
            DynamicsProcessing(0, session, config)
        }
    }

    private fun createClassic(chain: Chain) {
        chain.eq = create { Equalizer(0, chain.session) }
        chain.loud = create { LoudnessEnhancer(chain.session) }
    }

    private fun <T : AudioEffect> create(factory: () -> T): T? = try {
        factory()
    } catch (_: RuntimeException) {
        // UnsupportedOperationException, IllegalArgumentException and effect-framework failures all land here.
        null
    }

    private fun publish() {
        _state.value = _state.value.copy(
            sessions = chains.values
                .map { AttachedSession(it.session, it.packageName, it.failed, it.path, it.effects()) },
        )
    }

    private companion object {
        const val MAX_SESSIONS = 8
        const val MILLI_HZ = 1000f
        const val CHANNELS = 2
        const val LIMITER_LINK_GROUP = 0
        const val ATTACK_MS = 1f
        const val RELEASE_MS = 60f
        const val RATIO = 10f
        const val THRESHOLD_DB = -1f
    }
}
