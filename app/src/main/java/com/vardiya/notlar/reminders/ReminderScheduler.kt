package com.vardiya.notlar.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.Reminders
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Sets one alarm per note with a reminder. Each alarm's intent carries the note id in its data, so
 * setting it again replaces it and cancelling needs only the id. When the alarm rings,
 * [ReminderReceiver] checks the stored note again, so a stale alarm for a changed or deleted note
 * rings nothing.
 */
class ReminderScheduler(private val context: Context) {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Sets the alarm of [note], or cancels it if the note has no reminder left to ring. */
    fun schedule(note: Note, nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()) {
        val at = ringTime(note, nowMillis, zone)
        if (at == null) {
            cancel(note.id)
            return
        }
        val intent = pendingIntent(note.id)
        // Exact alarms need the user's permission from Android 12 on; without it the alarm may ring a few minutes late.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun cancel(noteId: Long) {
        alarms.cancel(pendingIntent(noteId))
    }

    /** Sets the alarms of [notes] and cancels those of [goneIds], notes whose reminder went away. */
    fun sync(notes: List<Note>, goneIds: Set<Long> = emptySet()) {
        goneIds.forEach(::cancel)
        Reminders.scheduled(notes).forEach { schedule(it) }
    }

    /**
     * When the alarm should ring: the reminder time, or for a repeating reminder whose time has
     * passed, its next time. A single reminder whose time has passed is not rung late.
     */
    private fun ringTime(note: Note, nowMillis: Long, zone: ZoneId): Long? {
        val at = note.reminderAt?.takeIf { !note.isTrashed } ?: return null
        val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone)
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        return if (at > nowMillis) at else Reminders.next(local, note.reminderRepeat, now)?.atZone(zone)?.toInstant()?.toEpochMilli()
    }

    private fun pendingIntent(noteId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_RING)
            .setData(Uri.parse("notlar://reminder/$noteId")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** The note id an alarm intent was made for. */
        fun noteId(intent: Intent): Long? = intent.data?.lastPathSegment?.toLongOrNull()
    }
}
