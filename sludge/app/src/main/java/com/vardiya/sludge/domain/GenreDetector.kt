package com.vardiya.sludge.domain

import java.util.Locale

/** Offline genre guess from the track's metadata; the fallback when no AI is configured or reachable. */
object GenreDetector {
    data class Guess(val genre: Genre, val confident: Boolean)

    private val TR = Locale.forLanguageTag("tr-TR")

    // Artists first: the tag a player shows is often empty or just "Rock".
    private val artists: Map<Genre, List<String>> = mapOf(
        Genre.STONER to listOf(
            "kyuss", "queens of the stone age", "fu manchu", "monster magnet", "sleep", "acid king", "orange goblin",
            "truckfighters", "red fang", "clutch", "sasquatch", "bongripper", "elder", "colour haze", "earthless",
        ),
        Genre.DOOM to listOf(
            "electric wizard", "eyehategod", "crowbar", "dopethrone", "windhand", "pallbearer", "black sabbath",
            "saint vitus", "cathedral", "yob", "thou", "weedeater", "bongzilla", "corrupted", "khanate",
        ),
        Genre.PSYCH to listOf(
            "13th floor elevators", "hawkwind", "pink floyd", "tame impala", "king gizzard", "acid mothers temple",
            "ty segall", "can", "amon duul", "gong", "black angels", "causa sui", "pharaoh overlord",
        ),
        Genre.NOISE_ROCK to listOf(
            "melvins", "big black", "jesus lizard", "unsane", "shellac", "swans", "harvey milk", "lightning bolt",
            "boredoms", "sonic youth", "daughters", "locust", "hella", "no age",
        ),
        Genre.HARSH_NOISE to listOf(
            "merzbow", "wolf eyes", "whitehouse", "prurient", "skullflower", "gerogerigegege", "masonna",
            "incapacitants", "pan sonic", "kenji siratori", "hanatarash",
        ),
        Genre.DRONE to listOf(
            "yellow swans", "sunn o)))", "sunn o", "earth", "om", "boris", "stars of the lid", "nadja", "aidan baker",
            "growing", "burning witch", "tim hecker", "angels of light",
        ),
        Genre.SHOEGAZE to listOf(
            "my bloody valentine", "slowdive", "ride", "alcest", "deafheaven", "catherine wheel", "swervedriver",
            "loveless", "cocteau twins",
        ),
    )

    private val keywords: Map<Genre, List<String>> = mapOf(
        Genre.STONER to listOf("stoner", "desert rock", "fuzz"),
        Genre.DOOM to listOf("doom", "sludge", "funeral"),
        Genre.PSYCH to listOf("psychedelic", "psych", "krautrock", "space rock", "acid rock"),
        Genre.NOISE_ROCK to listOf("noise rock", "math rock", "no wave", "post-hardcore"),
        Genre.HARSH_NOISE to listOf("harsh noise", "power electronics", "noise", "japanoise"),
        Genre.DRONE to listOf("drone", "ambient", "dark ambient", "ritual"),
        Genre.SHOEGAZE to listOf("shoegaze", "dream pop", "blackgaze", "post-rock"),
    )

    // Specific wins over generic: "harsh noise" before "noise", "noise rock" before both.
    private val order = listOf(
        Genre.NOISE_ROCK, Genre.HARSH_NOISE, Genre.DOOM, Genre.STONER, Genre.DRONE, Genre.PSYCH, Genre.SHOEGAZE,
    )

    fun detect(track: TrackInfo): Guess {
        val names = artistNames(track.artist)
        for (genre in artists.keys) {
            if (artists.getValue(genre).any { it in names }) return Guess(genre, confident = true)
        }
        val haystack = normalize("${track.genreTag} ${track.album} ${track.title}")
        val tag = normalize(track.genreTag)
        for (genre in order) {
            val words = keywords.getValue(genre)
            if (words.any { matchesWord(tag, it) }) return Guess(genre, confident = true)
        }
        for (genre in order) {
            if (keywords.getValue(genre).any { matchesWord(haystack, it) }) return Guess(genre, confident = false)
        }
        return Guess(Genre.FLAT, confident = false)
    }

    /** Lower case with Turkish rules, dotless ı folded to i so "KYUSS" and "Kyuss" compare equal. */
    internal fun normalize(text: String): String = text.lowercase(TR).replace('ı', 'i').trim()

    // Exact names only: "Earth Crisis" must not count as Earth, so a credit is split into its artists first.
    private val separators = Regex("""\s*(?:,|&|/|;|\bfeat\.?|\bft\.?|\bwith\b|\bx\b)\s*""")

    internal fun artistNames(credit: String): Set<String> {
        val whole = normalize(credit).removePrefix("the ")
        return setOf(whole) + whole.split(separators).map { it.trim().removePrefix("the ") }.filter { it.isNotEmpty() }
    }

    // Whole-word match so "om" does not fire inside "tomorrow".
    private fun matchesWord(haystack: String, needle: String): Boolean {
        var from = 0
        while (true) {
            val at = haystack.indexOf(needle, from)
            if (at < 0) return false
            val before = haystack.getOrNull(at - 1)
            val after = haystack.getOrNull(at + needle.length)
            if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) return true
            from = at + 1
        }
    }
}
