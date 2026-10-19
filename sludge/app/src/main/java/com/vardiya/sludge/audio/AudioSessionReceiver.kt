package com.vardiya.sludge.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

/**
 * Players that support external equalizers (Spotify, Poweramp, VLC, YouTube Music on many builds and others)
 * announce each audio session with this broadcast. It needs no permission, which makes it the reliable path.
 */
class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR_BAD_VALUE)
        if (session == AudioEffect.ERROR_BAD_VALUE || session <= 0) return
        val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME).orEmpty()
        val runtime = SludgeRuntime.get(context)
        val pending = goAsync()
        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> runtime.attach(session, pkg) { pending.finish() }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> runtime.detach(session) { pending.finish() }
            else -> pending.finish()
        }
    }
}
