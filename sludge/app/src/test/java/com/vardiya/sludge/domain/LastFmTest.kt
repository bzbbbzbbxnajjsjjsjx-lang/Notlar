package com.vardiya.sludge.domain

import com.vardiya.sludge.data.LastFmClient
import com.vardiya.sludge.data.Transport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LastFmProtocolTest {
    private val key = "0123456789abcdef0123456789abcdef"
    private val track = TrackInfo(title = "Dopesmoker", artist = "Sunn O)))")

    private fun tagsJson(vararg pairs: Pair<String, Int>) =
        """{"toptags":{"tag":[${pairs.joinToString(",") { """{"count":${it.second},"name":"${it.first}"}""" }}]}}"""

    @Test fun `a track request names the method, encodes the text and asks for json`() {
        val url = LastFmProtocol.trackTagsRequest(key, track).url
        assertTrue(url.startsWith("https://ws.audioscrobbler.com/2.0/?method=track.gettoptags"))
        assertTrue(url.contains("artist=Sunn+O%29%29%29"))
        assertTrue(url.contains("track=Dopesmoker"))
        assertTrue(url.contains("format=json"))
        assertTrue(url.contains("api_key=$key"))
        assertEquals("GET", LastFmProtocol.trackTagsRequest(key, track).method)
    }

    @Test fun `an artist request has no track`() {
        val url = LastFmProtocol.artistTagsRequest(key, track).url
        assertTrue(url.contains("method=artist.gettoptags"))
        assertFalse(url.contains("track="))
    }

    @Test fun `the key never shows in a printed request or config`() {
        assertFalse(LastFmProtocol.trackTagsRequest(key, track).toString().contains(key))
        assertFalse(LastFmConfig(key).toString().contains(key))
    }

    @Test fun `tags keep Last_fm's order and drop what says nothing about the sound`() {
        val body = tagsJson(
            "Stoner Rock" to 100, "seen live" to 80, "doom metal" to 70, "1994" to 60, "90s" to 55, "favorites" to 50,
            "Sleep" to 45, "sludge" to 40, "rarely" to 5,
        )
        val reply = LastFmProtocol.parse(body, artist = "Sleep") as LastFmReply.Tags
        assertEquals(listOf("stoner rock", "doom metal", "sludge"), reply.names)
    }

    @Test fun `at most six tags are kept`() {
        val body = tagsJson(*(1..10).map { "tag$it" to 100 }.toTypedArray())
        assertEquals(6, (LastFmProtocol.parse(body) as LastFmReply.Tags).names.size)
    }

    @Test fun `an empty answer is no tags, an unknown track is not found`() {
        assertEquals(LastFmReply.Tags(emptyList()), LastFmProtocol.parse("""{"toptags":{"tag":[],"@attr":{}}}"""))
        assertEquals(LastFmReply.NotFound, LastFmProtocol.parse("""{"error":6,"message":"Track not found"}"""))
    }

    @Test fun `a bad key or a broken answer is a failure with a message`() {
        val bad = LastFmProtocol.parse("""{"error":10,"message":"Invalid API key - You must be granted a valid key by last.fm"}""")
        assertTrue(bad is LastFmReply.Failure)
        assertTrue(LastFmProtocol.parse("not json") is LastFmReply.Failure)
    }

    @Test fun `a key must be 32 hexadecimal characters`() {
        assertTrue(LastFmConfig(key).isValid)
        assertTrue(LastFmConfig(" $key ").isValid)
        assertFalse(LastFmConfig("short").isValid)
        assertFalse(LastFmConfig("z".repeat(32)).isValid)
        assertFalse(LastFmConfig().isValid)
    }

    @Test fun `found tags are added after the player's own tag`() {
        val t = TrackInfo(title = "x", artist = "y", genreTag = "Rock").withTags(listOf("stoner rock", "doom"))
        assertEquals("Rock, stoner rock, doom", t.genreTag)
        assertEquals(TrackInfo(title = "x"), TrackInfo(title = "x").withTags(emptyList()))
        assertEquals("a, b", TrackInfo(title = "x").withTags(listOf("a", "b")).genreTag)
    }

    @Test fun `remembered tags drop the oldest first`() {
        var cache = emptyMap<String, List<String>>()
        for (i in 1..4) cache = TagMemory.put(cache, "k$i", listOf("t"), max = 2)
        assertEquals(listOf("k3", "k4"), cache.keys.toList())
    }

    @Test fun `the first tag that names a genre decides`() {
        val g = GenreDetector.detect(TrackInfo(artist = "Nobody", genreTag = "rock, sludge metal, stoner rock"))
        assertEquals(Genre.DOOM, g.genre)
        assertTrue(g.confident)
        assertEquals(Genre.STONER, GenreDetector.detect(TrackInfo(artist = "Nobody", genreTag = "seen live, stoner rock, doom")).genre)
        assertEquals(Genre.NOISE_ROCK, GenreDetector.detect(TrackInfo(artist = "Nobody", genreTag = "noise rock, noise")).genre)
        assertEquals(Genre.PSYCH, GenreDetector.detect(TrackInfo(artist = "Nobody", genreTag = "neo-psychedelia, rock")).genre)
    }
}

private class ScriptedTransport(vararg replies: Pair<Int, String>) : Transport {
    private val queue = ArrayDeque(replies.toList())
    val urls = mutableListOf<String>()
    override suspend fun send(request: HttpRequestSpec): HttpResult {
        urls += request.url
        val (code, body) = queue.removeFirst()
        if (code < 0) throw IOException("offline")
        return HttpResult(code, body)
    }
}

class LastFmClientTest {
    private val key = "0123456789abcdef0123456789abcdef"
    private val track = TrackInfo(title = "Dopesmoker", artist = "Sleep")
    private val notFound = 200 to """{"error":6,"message":"Track not found"}"""
    private val doom = 200 to """{"toptags":{"tag":[{"count":100,"name":"doom metal"}]}}"""

    private fun ask(transport: Transport, t: TrackInfo = track) = runBlocking { LastFmClient(transport).tags(key, t) }

    @Test fun `track tags are used when there are some`() {
        val transport = ScriptedTransport(doom)
        assertEquals(LastFmReply.Tags(listOf("doom metal")), ask(transport))
        assertEquals(1, transport.urls.size)
    }

    @Test fun `an unknown track falls back to the artist's tags`() {
        val transport = ScriptedTransport(notFound, doom)
        assertEquals(LastFmReply.Tags(listOf("doom metal")), ask(transport))
        assertTrue(transport.urls[1].contains("artist.gettoptags"))
    }

    @Test fun `a track nobody tagged also falls back to the artist`() {
        val transport = ScriptedTransport(200 to """{"toptags":{"tag":[]}}""", doom)
        assertEquals(LastFmReply.Tags(listOf("doom metal")), ask(transport))
    }

    @Test fun `a failure is not retried against the artist`() {
        val transport = ScriptedTransport(200 to """{"error":29,"message":"Rate limit exceeded"}""")
        assertTrue(ask(transport) is LastFmReply.Failure)
        assertEquals(1, transport.urls.size)
    }

    @Test fun `being offline is a failure, not a crash`() {
        assertTrue(ask(ScriptedTransport(-1 to "")) is LastFmReply.Failure)
        assertTrue(ask(ScriptedTransport(500 to "")) is LastFmReply.Failure)
    }

    @Test fun `without an artist nothing is asked`() {
        val transport = ScriptedTransport()
        assertEquals(LastFmReply.NotFound, ask(transport, TrackInfo(title = "x")))
        assertTrue(transport.urls.isEmpty())
    }
}
