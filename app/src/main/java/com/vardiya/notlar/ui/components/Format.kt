package com.vardiya.notlar.ui.components

import com.vardiya.notlar.domain.ReminderRepeat
import com.vardiya.notlar.domain.TextSize
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)
private val DayFormatter = DateTimeFormatter.ofPattern("d MMMM", TurkishLocale)
private val DayYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TurkishLocale)
private val WeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", TurkishLocale)

/** "Bugün 14:32", "Dün 09:10", "3 Ekim 14:32" or "3 Ekim 2025". */
fun formatEdited(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val date = time.toLocalDate()
    return when {
        date == today -> "Bugün " + time.format(TimeFormatter)
        date == today.minusDays(1) -> "Dün " + time.format(TimeFormatter)
        date.year == today.year -> time.format(DayFormatter) + " " + time.format(TimeFormatter)
        else -> time.format(DayYearFormatter)
    }
}

/** "Bugün 18:00", "Yarın 09:00", "Pazartesi 09:00" within the week, then "3 Kasım 09:00". */
fun formatReminder(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val date = time.toLocalDate()
    val clock = time.format(TimeFormatter)
    return when {
        date == today -> "Bugün $clock"
        date == today.plusDays(1) -> "Yarın $clock"
        date > today && date < today.plusDays(WEEK_DAYS) -> time.format(WeekdayFormatter).replaceFirstChar { it.titlecase(TurkishLocale) } + " $clock"
        date.year == today.year -> time.format(DayFormatter) + " " + clock
        else -> time.format(DayYearFormatter) + " " + clock
    }
}

private const val WEEK_DAYS = 7L

val ReminderRepeat.label: String
    get() = when (this) {
        ReminderRepeat.NONE -> "Tekrar yok"
        ReminderRepeat.DAILY -> "Her gün"
        ReminderRepeat.WEEKLY -> "Her hafta"
        ReminderRepeat.MONTHLY -> "Her ay"
    }

val TextSize.label: String
    get() = when (this) {
        TextSize.SMALL -> "Küçük"
        TextSize.NORMAL -> "Normal"
        TextSize.LARGE -> "Büyük"
        TextSize.HUGE -> "Dev"
    }
