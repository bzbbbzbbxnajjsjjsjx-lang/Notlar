package com.vardiya.sludge.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/** The Last.fm key. [toString] never prints it. */
data class LastFmConfig(val apiKey: String = "") {
    /** A Last.fm key is 32 hexadecimal characters. */
    val isValid: Boolean get() = KEY_FORMAT.matches(apiKey.trim())

    override fun toString() = "LastFmConfig(apiKey=<hidden>)"

    private companion object {
        val KEY_FORMAT = Regex("[0-9a-fA-F]{32}")
    }
}

/** Where a track's genre tags come from. Returns nothing (never throws) when disabled, offline or unknown. */
fun interface TagSource {
    suspend fun tagsFor(track: TrackInfo): List<String>
}

/** What Last.fm answered. */
sealed interface LastFmReply {
    data class Tags(val names: List<String>) : LastFmReply

    /** Error 6 and friends: Last.fm does not know this track or artist. */
    data object NotFound : LastFmReply

    /** Bad key, rate limit, malformed answer: nothing to cache. */
    data class Failure(val message: String) : LastFmReply
}

/** Requests and answers of the Last.fm web API, as plain data so it is tested without a network. */
object LastFmProtocol {
    private const val BASE = "https://ws.audioscrobbler.com/2.0/"
    private const val MIN_COUNT = 15
    private const val MAX_TAGS = 6
    private const val MAX_TAG_LENGTH = 32
    private const val NOT_FOUND_CODE = 6
    private const val ERROR_TEXT = 120

    // Tags people put on everything: they say nothing about the sound.
    private val noise = setOf(
        "seen live", "favorites", "favourites", "favorite", "favourite", "albums i own", "awesome", "love", "loved",
        "love at first listen", "male vocalists", "female vocalists", "spotify", "check out", "owned", "good", "best",
        "cool", "amazing", "my music", "all", "under 2000 listeners", "usa", "uk", "american", "british", "swedish",
        "german", "finnish", "japanese", "canadian", "australian", "english",
    )
    private val yearOrDecade = Regex("""\d{2,4}s?""")

    // A tag is a short name, not a sentence: letters, digits and a few joiners only, so it cannot carry instructions.
    private val plainTag = Regex("""[\p{L}\p{N} &'+./-]{1,$MAX_TAG_LENGTH}""")

    fun trackTagsRequest(apiKey: String, track: TrackInfo): HttpRequestSpec =
        request("track.gettoptags", apiKey, "artist" to track.artist, "track" to track.title)

    fun artistTagsRequest(apiKey: String, track: TrackInfo): HttpRequestSpec =
        request("artist.gettoptags", apiKey, "artist" to track.artist)

    private fun request(method: String, apiKey: String, vararg params: Pair<String, String>): HttpRequestSpec {
        val query = buildList {
            add("method" to method)
            addAll(params)
            add("autocorrect" to "1")
            add("api_key" to apiKey.trim())
            add("format" to "json")
        }.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return HttpRequestSpec(url = "$BASE?$query", headers = mapOf("accept" to "application/json"), body = "", method = "GET")
    }

    /** Reads either answer ("toptags" with "tag" entries). Noise tags are dropped, the rest keep Last.fm's order. */
    fun parse(body: String, artist: String = ""): LastFmReply = try {
        val root = AiProtocol.json.parseToJsonElement(body).jsonObject
        val error = root["error"]?.jsonPrimitive?.intOrNull
        if (error != null) {
            if (error == NOT_FOUND_CODE) {
                LastFmReply.NotFound
            } else {
                LastFmReply.Failure((root["message"]?.jsonPrimitive?.contentOrNull ?: "hata $error").take(ERROR_TEXT))
            }
        } else {
            val entries = (root["toptags"] as? JsonObject)?.get("tag") as? JsonArray ?: JsonArray(emptyList())
            LastFmReply.Tags(
                entries.mapNotNull { it as? JsonObject }
                    .filter { (it["count"]?.jsonPrimitive?.intOrNull ?: MIN_COUNT) >= MIN_COUNT }
                    .mapNotNull { it["name"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() }
                    .filter { plainTag.matches(it) && !isNoise(it, artist) }
                    .distinct()
                    .take(MAX_TAGS),
            )
        }
    } catch (_: IllegalArgumentException) {
        LastFmReply.Failure("Yanıt okunamadı")
    }

    private fun isNoise(tag: String, artist: String) =
        tag in noise || yearOrDecade.matches(tag) || tag == artist.trim().lowercase()
}

/** Tags found for a track, added after the ones the player gave. */
fun TrackInfo.withTags(extra: List<String>): TrackInfo =
    if (extra.isEmpty()) this else copy(genreTag = (listOf(genreTag).filter { it.isNotBlank() } + extra).joinToString(", "))

/** Remembered tags, oldest dropped first, like [TuneCache]. */
object TagMemory {
    const val MAX_ENTRIES = 300

    fun put(cache: Map<String, List<String>>, key: String, tags: List<String>, max: Int = MAX_ENTRIES): Map<String, List<String>> {
        val next = LinkedHashMap(cache)
        next.remove(key)
        next[key] = tags
        while (next.size > max) next.remove(next.keys.first())
        return next
    }
}
