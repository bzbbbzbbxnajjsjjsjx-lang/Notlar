package com.vardiya.notlar.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** When reminders ring and when a repeating one comes back; pure, so the times are unit-tested. */
object Reminders {

    /** A ready-made time in the reminder sheet. */
    data class Option(val label: String, val at: LocalDateTime)

    private val Evening: LocalTime = LocalTime.of(18, 0)
    private val Morning: LocalTime = LocalTime.of(9, 0)

    /** A reminder this close to its time counts as due; alarms may ring a little early. */
    private const val DUE_SLACK_MILLIS = 60_000L

    /** "Bugün 18:00" while the evening is still ahead, "Yarın 09:00" and next Monday morning. */
    fun quickOptions(now: LocalDateTime): List<Option> = buildList {
        val today = now.toLocalDate()
        if (now.toLocalTime() < Evening) add(Option("Bugün 18:00", today.atTime(Evening)))
        add(Option("Yarın 09:00", today.plusDays(1).atTime(Morning)))
        add(Option("Gelecek hafta", today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(Morning)))
    }

    /**
     * The first time after [after] that a reminder set for [at] rings again, or null if it does
     * not repeat. Months are counted from [at], so the 31st stays the last day of shorter months.
     */
    fun next(at: LocalDateTime, repeat: ReminderRepeat, after: LocalDateTime): LocalDateTime? {
        val step: (Long) -> LocalDateTime = when (repeat) {
            ReminderRepeat.NONE -> return null
            ReminderRepeat.DAILY -> at::plusDays
            ReminderRepeat.WEEKLY -> at::plusWeeks
            ReminderRepeat.MONTHLY -> at::plusMonths
        }
        return generateSequence(1L) { it + 1 }.map(step).first { it > after }
    }

    /** Notes whose reminder should be scheduled: every note with a reminder that is not in the trash. */
    fun scheduled(notes: List<Note>): List<Note> = notes.filter { it.reminderAt != null && !it.isTrashed }

    /** True if [note] should ring now: its reminder is set, not in the trash, and its time has come. */
    fun isDue(note: Note?, nowMillis: Long): Boolean {
        val at = note?.reminderAt ?: return false
        return !note.isTrashed && at <= nowMillis + DUE_SLACK_MILLIS
    }

    /** The note after its reminder rang: a repeating one moves to its next time, a single one is cleared. */
    fun afterRinging(note: Note, nowMillis: Long, zone: ZoneId): Note {
        val at = note.reminderAt ?: return note
        val next = next(at.toLocal(zone), note.reminderRepeat, nowMillis.toLocal(zone))
        return if (next == null) {
            note.copy(reminderAt = null, reminderRepeat = ReminderRepeat.NONE)
        } else {
            note.copy(reminderAt = next.atZone(zone).toInstant().toEpochMilli())
        }
    }

    private fun Long.toLocal(zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)
}
