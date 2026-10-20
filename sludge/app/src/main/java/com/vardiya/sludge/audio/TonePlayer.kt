package com.vardiya.sludge.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.vardiya.sludge.domain.Sweep
import com.vardiya.sludge.domain.SweepKind
import com.vardiya.sludge.domain.ToneState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Plays a [SweepKind] on an audio session of its own and has the equalizer attached to that session first, so the tone is
 * heard through the same filters as music. [attach] must call its callback once the effects are on the session.
 */
class TonePlayer(
    context: Context,
    private val scope: CoroutineScope,
    private val attach: (session: Int, done: () -> Unit) -> Unit,
    private val detach: (session: Int) -> Unit,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _state = MutableStateFlow(ToneState())
    val state: StateFlow<ToneState> = _state.asStateFlow()
    private var job: Job? = null
    private var generation = 0

    /** Starts a sweep, replacing one that is playing. */
    @Synchronized
    fun start(kind: SweepKind) {
        job?.cancel()
        val mine = ++generation
        job = scope.launch(Dispatchers.IO) { play(kind, mine) }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
    }

    private suspend fun play(kind: SweepKind, mine: Int) {
        val session = audio.generateAudioSessionId()
        val ready = CompletableDeferred<Unit>()
        attach(session) { ready.complete(Unit) }
        val track = runCatching { buildTrack(session) }.getOrNull()
        var finished = false
        try {
            // A tone that would play without the equalizer would say one thing and do another, so it does not play.
            if (track == null || withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } == null) return
            track.play()
            finished = stream(kind, track)
            // The last buffered samples hold the fade-out; let them play before the track is released.
            if (finished) delay(DRAIN_MS)
        } finally {
            withContext(NonCancellable) {
                runCatching {
                    if (!finished) {
                        track?.pause()
                        track?.flush()
                    }
                    track?.release()
                }.onFailure { Log.w(TAG, "tone track release failed: ${it.javaClass.simpleName}") }
                detach(session)
                // A newer sweep owns the state now.
                synchronized(this@TonePlayer) { if (generation == mine) _state.value = ToneState() }
            }
        }
    }

    /** Writes the sweep; false when stopped or when the track stops accepting samples. */
    private suspend fun stream(kind: SweepKind, track: AudioTrack): Boolean {
        val chunk = ShortArray(CHUNK_SAMPLES)
        val total = Sweep.totalSamples(kind)
        var written = 0
        var phase = 0.0
        while (written < total) {
            currentCoroutineContext().ensureActive()
            phase = Sweep.fill(kind, written, phase, chunk)
            // A negative result is an error (a dead track): stop instead of racing through the rest of the sweep.
            if (track.write(chunk, 0, chunk.size) < 0) return false
            written += chunk.size
            _state.value = ToneState(kind, Sweep.frequencyAt(kind, written.toFloat() / Sweep.SAMPLE_RATE))
        }
        return true
    }

    private fun buildTrack(session: Int): AudioTrack {
        val format = AudioFormat.Builder()
            .setSampleRate(Sweep.SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minimum = AudioTrack.getMinBufferSize(Sweep.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            )
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minimum, CHUNK_SAMPLES * BYTES_PER_SAMPLE * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setSessionId(session)
            .build()
    }

    private companion object {
        const val TAG = "Sludge"
        const val CHUNK_SAMPLES = 4_800
        const val BYTES_PER_SAMPLE = 2
        const val READY_TIMEOUT_MS = 2_000L
        const val DRAIN_MS = 300L
    }
}
