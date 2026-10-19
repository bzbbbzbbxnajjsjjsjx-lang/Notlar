package com.vardiya.sludge.app

import android.util.Log
import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the audio effects in step with the stored settings and with the sessions players announce. Sessions open
 * and close through one queue that a single coroutine drains, so a close can never overtake the open before it.
 */
class EqController(
    scope: CoroutineScope,
    // Effect calls block on the media server: one dedicated background thread keeps them off the main thread and in order.
    audio: CoroutineDispatcher,
    private val engine: EffectEngine,
    private val repo: SettingsRepository,
) {
    private class Command(val session: Int, val packageName: String?, val done: () -> Unit)

    private val commands = Channel<Command>(Channel.UNLIMITED)

    init {
        scope.launch(audio) {
            // Wait for the stored settings before touching the audio, so a restart never flashes the defaults.
            guarded("global mix") { engine.tryGlobalMix(repo.settings.first()) }
            launch { guarded("settings") { repo.settings.distinctUntilChanged().collect { engine.apply(it) } } }
            for (command in commands) {
                try {
                    guarded("audio session ${command.session}") {
                        // A null package name is a close.
                        if (command.packageName == null) {
                            engine.detach(command.session)
                        } else {
                            engine.attach(command.session, command.packageName, repo.settings.first())
                        }
                    }
                } finally {
                    command.done()
                }
            }
        }
    }

    /** Whatever one step throws must not end the queue for every session after it, nor leave a receiver waiting. */
    private suspend fun guarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "$what failed: ${e.javaClass.simpleName}")
        }
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) {
        commands.trySend(Command(session, packageName, done))
    }

    fun detach(session: Int, done: () -> Unit) {
        commands.trySend(Command(session, null, done))
    }

    private companion object {
        const val TAG = "Sludge"
    }
}
