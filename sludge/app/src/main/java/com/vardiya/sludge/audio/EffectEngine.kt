// Virtualizer is deprecated on the newest API levels but still the only widening effect every device has.
@file:Suppress("DEPRECATION")

package com.vardiya.sludge.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.EngineState
import com.vardiya.sludge.domain.EqSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds an Equalizer, BassBoost, Virtualizer and LoudnessEnhancer on every audio session a player announces, and
 * keeps them in step with the settings. Every effect is optional: devices differ, and a missing one must not
 * stop the others. Calls are serialised by the caller (see [SludgeRuntime]).
 */
class EffectEngine {
    private class Chain(val session: Int, val packageName: String) {
        var eq: Equalizer? = null
        var bass: BassBoost? = null
        var virt: Virtualizer? = null
        var loud: LoudnessEnhancer? = null

        val failed get() = eq == null && bass == null && virt == null && loud == null

        fun release() {
            listOf<AudioEffect?>(eq, bass, virt, loud).forEach { runCatching { it?.release() } }
            eq = null; bass = null; virt = null; loud = null
        }
    }

    private val chains = LinkedHashMap<Int, Chain>()
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    @Synchronized
    fun attach(session: Int, packageName: String, settings: EqSettings) {
        chains.remove(session)?.release()
        val chain = Chain(session, packageName)
        // Priority 0 is the normal level; the effects are created disabled and switched on by apply().
        chain.eq = create { Equalizer(0, session) }
        chain.bass = create { BassBoost(0, session) }
        chain.virt = create { Virtualizer(0, session) }
        chain.loud = create { LoudnessEnhancer(session) }
        chains[session] = chain
        // A player that crashes never sends CLOSE; drop the oldest sessions so their native effects are freed.
        while (chains.size > MAX_SESSIONS) chains.remove(chains.keys.first())?.release()
        apply(chain, settings)
        publish()
    }

    /** Tries the global mix (session 0). Most Android 10+ builds ignore or refuse it, so the result is reported. */
    @Synchronized
    fun tryGlobalMix(settings: EqSettings) {
        attach(GLOBAL_SESSION, "global", settings)
        _state.value = _state.value.copy(globalMixWorks = chains[GLOBAL_SESSION]?.failed == false)
    }

    @Synchronized
    fun detach(session: Int) {
        chains.remove(session)?.release()
        publish()
    }

    @Synchronized
    fun apply(settings: EqSettings) {
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
        chain.eq?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                val centers = (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / MILLI_HZ }
                val gains = CurveMath.mapToDevice(s.bands, centers)
                gains.forEachIndexed { band, db ->
                    eq.setBandLevel(band.toShort(), CurveMath.toMillibels(db, range[0].toInt(), range[1].toInt()).toShort())
                }
                eq.enabled = on
            }
        }
        chain.bass?.let { bass ->
            runCatching {
                if (bass.strengthSupported) bass.setStrength(s.bassBoost.toShort())
                bass.enabled = on && s.bassBoost > 0
            }
        }
        chain.virt?.let { virt ->
            runCatching {
                if (virt.strengthSupported) virt.setStrength(s.virtualizer.toShort())
                virt.enabled = on && s.virtualizer > 0
            }
        }
        chain.loud?.let { loud ->
            runCatching {
                val gain = CurveMath.safeLoudness(s.bands, s.loudnessMb)
                loud.setTargetGain(gain)
                loud.enabled = on && gain > 0
            }
        }
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
                .filter { it.session != GLOBAL_SESSION }
                .map { AttachedSession(it.session, it.packageName, it.failed) },
        )
    }

    private companion object {
        const val GLOBAL_SESSION = 0
        const val MAX_SESSIONS = 8
        const val MILLI_HZ = 1000f
    }
}
