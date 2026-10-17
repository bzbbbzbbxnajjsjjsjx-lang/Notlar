package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class RemindersTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private fun millis(at: LocalDateTime) = at.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun quickOptionsOfferTonightOnlyBeforeSix() {
        // Friday 16 October 2026; the next Monday is the 19th.
        val afternoon = LocalDateTime.of(2026, 10, 16, 14, 30)
        assertEquals(
            listOf(
                Reminders.Option("Bugün 18:00", LocalDateTime.of(2026, 10, 16, 18, 0)),
                Reminders.Option("Yarın 09:00", LocalDateTime.of(2026, 10, 17, 9, 0)),
                Reminders.Option("Gelecek hafta", LocalDateTime.of(2026, 10, 19, 9, 0)),
            ),
            Reminders.quickOptions(afternoon),
        )
        val evening = LocalDateTime.of(2026, 10, 16, 18, 0)
        assertEquals(listOf("Yarın 09:00", "Gelecek hafta"), Reminders.quickOptions(evening).map { it.label })
    }

    @Test
    fun nextStepsPastNowAndKeepsMonthEnds() {
        val at = LocalDateTime.of(2026, 1, 31, 9, 0)
        assertNull(Reminders.next(at, ReminderRepeat.NONE, at))
        assertEquals(LocalDateTime.of(2026, 2, 3, 9, 0), Reminders.next(at, ReminderRepeat.DAILY, LocalDateTime.of(2026, 2, 2, 10, 0)))
        assertEquals(LocalDateTime.of(2026, 2, 7, 9, 0), Reminders.next(at, ReminderRepeat.WEEKLY, at))
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), Reminders.next(at, ReminderRepeat.MONTHLY, at))
        // Counted from the 31st, not from the shortened 28th.
        assertEquals(LocalDateTime.of(2026, 3, 31, 9, 0), Reminders.next(at, ReminderRepeat.MONTHLY, LocalDateTime.of(2026, 3, 1, 0, 0)))
    }

    @Test
    fun ringingClearsASingleReminderAndMovesARepeatingOne() {
        val at = LocalDateTime.of(2026, 10, 16, 9, 0)
        val now = millis(at.plusMinutes(1))
        val single = Note(1, reminderAt = millis(at))
        assertNull(Reminders.afterRinging(single, now, zone).reminderAt)
        val weekly = single.copy(reminderRepeat = ReminderRepeat.WEEKLY)
        assertEquals(millis(at.plusWeeks(1)), Reminders.afterRinging(weekly, now, zone).reminderAt)
        assertEquals(ReminderRepeat.WEEKLY, Reminders.afterRinging(weekly, now, zone).reminderRepeat)
    }

    @Test
    fun onlyStoredDueRemindersRing() {
        val now = millis(LocalDateTime.of(2026, 10, 16, 9, 0))
        assertTrue(Reminders.isDue(Note(1, reminderAt = now), now))
        assertTrue(Reminders.isDue(Note(1, reminderAt = now + 30_000), now))
        assertFalse(Reminders.isDue(Note(1, reminderAt = now + 3_600_000), now))
        assertFalse(Reminders.isDue(Note(1, reminderAt = now, trashedAt = now), now))
        assertFalse(Reminders.isDue(Note(1), now))
        assertFalse(Reminders.isDue(null, now))
    }

    @Test
    fun trashedNotesAreNotScheduled() {
        val notes = listOf(Note(1, reminderAt = 5), Note(2, reminderAt = 5, trashedAt = 1), Note(3))
        assertEquals(listOf(1L), Reminders.scheduled(notes).map { it.id })
    }

    @Test
    fun duplicateDoesNotCopyTheReminder() {
        val copy = NoteEdits.duplicate(Note(1, title = "Kira", reminderAt = 5, reminderRepeat = ReminderRepeat.MONTHLY, labels = listOf("Ev")), 2, 10)
        assertNull(copy.reminderAt)
        assertEquals(ReminderRepeat.NONE, copy.reminderRepeat)
        assertEquals(listOf("Ev"), copy.labels)
    }
}
