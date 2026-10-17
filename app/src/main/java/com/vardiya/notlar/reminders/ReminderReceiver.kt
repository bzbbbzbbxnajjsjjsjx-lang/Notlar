package com.vardiya.notlar.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.Reminders
import com.vardiya.notlar.widget.NotlarWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Rings a reminder when its alarm goes off, and sets every alarm again after a restart, an app
 * update or a change of the clock, which clear or shift alarms.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_RING -> ReminderScheduler.noteId(intent)?.let { ring(app, it) }
                    in RESCHEDULE_ACTIONS -> ReminderScheduler(app).sync(NoteRepository.get(app).notes.first())
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun ring(context: Context, noteId: Long) {
        val repository = NoteRepository.get(context)
        val now = System.currentTimeMillis()
        val note = repository.notes.first().firstOrNull { it.id == noteId }
        if (note == null || !Reminders.isDue(note, now)) return
        val lock = repository.lock.first()
        val settings = repository.settings.first()
        // With the app lock or "Ekranı gizle" on, the notification does not show what the note says.
        val private = lock.hasPin || lock.unreadable || settings.hideContent
        ReminderNotifier.show(context, note, private)
        val next = Reminders.afterRinging(note, now, ZoneId.systemDefault())
        repository.edit { list -> list.map { if (it.id == noteId) next else it } }
        ReminderScheduler(context).schedule(next, now)
        NotlarWidgets.refresh(context)
    }

    companion object {
        const val ACTION_RING = "com.vardiya.notlar.REMINDER"
        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
