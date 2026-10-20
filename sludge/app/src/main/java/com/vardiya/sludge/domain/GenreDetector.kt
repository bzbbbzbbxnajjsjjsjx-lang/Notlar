package com.vardiya.sludge.domain

import java.util.Locale

/** Genre guess from the track's own metadata (the player's genre tag, the artist, the album and title); fully offline. */
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

    // Phrases that name a genre. Bare "noise" and "ambient" are too loose to match inside a longer tag, see [exactTags].
    private val keywords: Map<Genre, List<String>> = mapOf(
        Genre.STONER to listOf("stoner", "desert rock"),
        Genre.DOOM to listOf("doom", "sludge", "funeral doom"),
        Genre.PSYCH to listOf("psychedelic", "psychedelia", "psych", "krautrock", "space rock", "acid rock"),
        Genre.NOISE_ROCK to listOf("noise rock", "no wave"),
        Genre.HARSH_NOISE to listOf("harsh noise", "power electronics", "japanoise", "noise wall", "wall noise"),
        Genre.DRONE to listOf("drone", "dark ambient"),
        Genre.SHOEGAZE to listOf("shoegaze", "blackgaze", "post-rock"),
    )

    // A whole tag that means one genre, though the same word inside a longer tag ("noise pop") does not.
    private val exactTags: Map<String, Genre> = mapOf("noise" to Genre.HARSH_NOISE, "ambient" to Genre.DRONE)

    // A tag that also names one of these is another scene's music ("psychedelic pop", "ambient house").
    private val otherScenes = listOf("pop", "house", "techno", "hip hop", "rap", "disco", "dance", "country", "jazz")

    // Specific wins over generic: "noise rock" before "harsh noise", both before the rest.
    private val order = listOf(
        Genre.NOISE_ROCK, Genre.HARSH_NOISE, Genre.DOOM, Genre.STONER, Genre.DRONE, Genre.PSYCH, Genre.SHOEGAZE,
    )

    /**
     * The evidence, best first: the player's genre tags (the first tag that names a genre decides),
     * then the artist list, then words in the album and title.
     */
    fun detect(track: TrackInfo): Guess {
        for (tag in track.genreTag.split(tagSeparators).map(::normalize).filter { it.isNotEmpty() }) {
            genreOfTag(tag)?.let { return Guess(it, confident = true) }
        }
        val names = artistNames(track.artist)
        for (genre in artists.keys) {
            if (artists.getValue(genre).any { it in names }) return Guess(genre, confident = true)
        }
        val words = normalize("${track.album} ${track.title}")
        for (genre in order) {
            if (keywords.getValue(genre).any { matchesWord(words, it) }) return Guess(genre, confident = false)
        }
        return Guess(Genre.FLAT, confident = false)
    }

    private fun genreOfTag(tag: String): Genre? {
        exactTags[tag]?.let { return it }
        if (otherScenes.any { matchesWord(tag, it) }) return null
        return order.firstOrNull { genre -> keywords.getValue(genre).any { matchesWord(tag, it) } }
    }

    /** Lower case with Turkish rules, dotless ı folded to i so "KYUSS" and "Kyuss" compare equal. */
    internal fun normalize(text: String): String = text.lowercase(TR).replace('ı', 'i').trim()

    // Exact names only: "Earth Crisis" must not count as Earth, so a credit is split into its artists first.
    private val tagSeparators = Regex("[,;/]")
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
