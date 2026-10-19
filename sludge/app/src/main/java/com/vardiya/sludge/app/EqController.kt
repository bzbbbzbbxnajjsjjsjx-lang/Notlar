package com.vardiya.sludge.app

import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.data.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Keeps the audio effects in step with the stored settings and with the sessions players announce. */
class EqController(
    private val scope: CoroutineScope,
    // Effect calls block on the media server: one background thread keeps them off the main thread and in order.
    private val audio: CoroutineDispatcher,
    private val engine: EffectEngine,
    private val repo: SettingsRepository,
) {
    init {
        scope.launch(audio) {
            // Wait for the stored settings before touching the audio, so a restart never flashes the defaults.
            engine.tryGlobalMix(repo.settings.first())
            repo.settings.distinctUntilChanged().collect { engine.apply(it) }
        }
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) = run(done) {
        engine.attach(session, packageName, repo.settings.first())
    }

    fun detach(session: Int, done: () -> Unit) = run(done) { engine.detach(session) }

    private fun run(done: () -> Unit, block: suspend () -> Unit) {
        scope.launch(audio) {
            try {
                block()
            } finally {
                done()
            }
        }
    }
}
