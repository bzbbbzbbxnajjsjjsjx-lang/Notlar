package com.vardiya.notlar.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)
private val DayFormatter = DateTimeFormatter.ofPattern("d MMMM", TurkishLocale)
private val DayYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TurkishLocale)

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
