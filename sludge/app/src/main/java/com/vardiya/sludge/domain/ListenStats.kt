package com.vardiya.sludge.domain

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One track the player started, with the genre bucket it was filed under. */
@Serializable
data class ListenEntry(val at: Long, val artist: String, val title: String, val genre: String)

data class GenreShare(val genre: Genre, val count: Int, val fraction: Float)

data class DayCount(val date: LocalDate, val count: Int)

data class ListenSummary(
    val total: Int = 0,
    val genres: List<GenreShare> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
    /** The last seven days, oldest first, today last. */
    val days: List<DayCount> = emptyList(),
)

/** What the stats tab shows, computed from the listening log. Pure, so the maths is tested. */
object ListenStats {
    const val WINDOW_DAYS = 30
    const val WEEK = 7
    const val TOP_ARTISTS = 5
    const val MAX_ENTRIES = 1_000

    fun summarize(entries: List<ListenEntry>, now: Long, zone: ZoneId, windowDays: Int = WINDOW_DAYS): ListenSummary {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val since = today.minusDays(windowDays - 1L)
        val inWindow = entries.filter { dateOf(it, zone) >= since }
        val byGenre = inWindow.groupingBy { genreOf(it) }.eachCount()
        val shares = byGenre.entries
            .sortedWith(compareByDescending<Map.Entry<Genre, Int>> { it.value }.thenBy { it.key.ordinal })
            .map { GenreShare(it.key, it.value, it.value.toFloat() / inWindow.size) }
        val artists = inWindow.filter { it.artist.isNotBlank() }
            .groupingBy { it.artist.trim() }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(TOP_ARTISTS).map { it.key to it.value }
        val perDay = inWindow.groupingBy { dateOf(it, zone) }.eachCount()
        val days = (WEEK - 1 downTo 0).map { back -> today.minusDays(back.toLong()).let { DayCount(it, perDay[it] ?: 0) } }
        return ListenSummary(inWindow.size, shares, artists, days)
    }

    /** Appends [entry], dropping the oldest ones beyond [max]. */
    fun append(log: List<ListenEntry>, entry: ListenEntry, max: Int = MAX_ENTRIES): List<ListenEntry> = (log + entry).takeLast(max)

    private fun dateOf(entry: ListenEntry, zone: ZoneId) = Instant.ofEpochMilli(entry.at).atZone(zone).toLocalDate()

    private fun genreOf(entry: ListenEntry): Genre = runCatching { Genre.valueOf(entry.genre) }.getOrDefault(Genre.FLAT)
}

/** The chart label: the preset's name, and "Diğer" for what no preset describes. */
val Genre.chartLabel: String get() = if (this == Genre.FLAT) "Diğer" else label
