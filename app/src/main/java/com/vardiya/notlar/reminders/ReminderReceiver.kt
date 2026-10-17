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

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Runs [work] off the main thread while the system keeps the receiver alive. */
private fun BroadcastReceiver.runAsync(work: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            work()
        } finally {
            pending.finish()
        }
    }
}

/** Rings a reminder when its alarm goes off; only the app's own alarms reach it (not exported). */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = ReminderScheduler.noteId(intent) ?: return
        val app = context.applicationContext
        runAsync { ring(app, noteId) }
    }

    private suspend fun ring(context: Context, noteId: Long) {
        val repository = NoteRepository.get(context)
        val now = System.currentTimeMillis()
        val due = repository.notes.first().firstOrNull { it.id == noteId }?.takeIf { Reminders.isDue(it, now) } ?: return
        val lock = repository.lock.first()
        val settings = repository.settings.first()
        // With the app lock or "Ekranı gizle" on, the notification does not show what the note says.
        val private = lock.hasPin || lock.unreadable || settings.hideContent
        ReminderNotifier.show(context, due, private)
        // Cleared or moved inside the transaction, so an edit made meanwhile is kept.
        repository.edit { list ->
            list.map { if (it.id == noteId && Reminders.isDue(it, now)) Reminders.afterRinging(it, now, ZoneId.systemDefault()) else it }
        }
        repository.notes.first().firstOrNull { it.id == noteId }?.let { ReminderScheduler(context).schedule(it) }
        NotlarWidgets.refresh(context)
    }

    companion object {
        const val ACTION_RING = "com.vardiya.notlar.REMINDER"
    }
}

/** Sets every alarm again after a restart, an app update or a change of the clock, which clear or shift them. */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val app = context.applicationContext
        runAsync { ReminderScheduler(app).sync(NoteRepository.get(app).notes.first()) }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
