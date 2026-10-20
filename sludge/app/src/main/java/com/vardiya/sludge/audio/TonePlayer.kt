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

    /** Starts a sweep, replacing one that is playing. */
    fun start(kind: SweepKind) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) { play(kind) }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun play(kind: SweepKind) {
        val session = audio.generateAudioSessionId()
        val ready = CompletableDeferred<Unit>()
        attach(session) { ready.complete(Unit) }
        val track = runCatching { buildTrack(session) }.getOrNull()
        try {
            if (track == null) return
            withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() }
            track.play()
            stream(kind, track)
        } finally {
            withContext(NonCancellable) {
                runCatching {
                    track?.pause()
                    track?.flush()
                    track?.release()
                }.onFailure { Log.w(TAG, "tone track release failed: ${it.javaClass.simpleName}") }
                detach(session)
                _state.value = ToneState()
            }
        }
    }

    private suspend fun stream(kind: SweepKind, track: AudioTrack) {
        val chunk = ShortArray(CHUNK_SAMPLES)
        val total = Sweep.totalSamples(kind)
        var written = 0
        var phase = 0.0
        while (written < total) {
            currentCoroutineContext().ensureActive()
            phase = Sweep.fill(kind, written, phase, chunk)
            track.write(chunk, 0, chunk.size)
            written += chunk.size
            _state.value = ToneState(kind, Sweep.frequencyAt(kind, written.toFloat() / Sweep.SAMPLE_RATE))
        }
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
    }
}
