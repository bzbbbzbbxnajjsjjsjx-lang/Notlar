package com.vardiya.sludge.audio

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.vardiya.sludge.domain.TrackInfo

/**
 * Notification access is the only way for an ordinary app to see other apps' media sessions. This service never
 * reads notification text: it only asks the system for the active media controllers to learn what is playing.
 * While access is granted the system keeps this service (and so the process, and the effects) alive.
 */
class SludgeListenerService : NotificationListenerService() {
    private val callbacks = HashMap<MediaController, MediaController.Callback>()
    private var manager: MediaSessionManager? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        watch(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        SludgeRuntime.get(this)
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        this.manager = manager
        val component = ComponentName(this, SludgeListenerService::class.java)
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component)
            watch(manager.getActiveSessions(component))
        }
    }

    override fun onListenerDisconnected() {
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        unwatchAll()
        SludgeRuntime.get(this).publishTrack(null)
    }

    override fun onDestroy() {
        unwatchAll()
        super.onDestroy()
    }

    private fun watch(controllers: List<MediaController>) {
        val keep = controllers.toSet()
        callbacks.keys.filter { it !in keep }.forEach { stop(it) }
        for (controller in controllers) {
            if (controller in callbacks) continue
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() = publish()
            }
            callbacks[controller] = callback
            controller.registerCallback(callback)
        }
        publish()
    }

    private fun stop(controller: MediaController) {
        callbacks.remove(controller)?.let { runCatching { controller.unregisterCallback(it) } }
    }

    private fun unwatchAll() {
        callbacks.keys.toList().forEach { stop(it) }
    }

    /** The playing session wins; otherwise the first one that has a title. */
    private fun publish() {
        val controllers = callbacks.keys.toList()
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.metadata != null }
        SludgeRuntime.get(this).publishTrack(chosen?.let { trackOf(it) })
    }

    private fun trackOf(controller: MediaController): TrackInfo? {
        val meta = controller.metadata ?: return null
        val source = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(controller.packageName, 0)).toString()
        }.getOrDefault(controller.packageName)
        return TrackInfo(
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            genreTag = meta.getString(MediaMetadata.METADATA_KEY_GENRE).orEmpty(),
            source = source,
        ).takeUnless { it.isBlank }
    }
}
