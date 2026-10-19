package com.vardiya.sludge.data

import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.LastFmProtocol
import com.vardiya.sludge.domain.LastFmReply
import com.vardiya.sludge.domain.TagMemory
import com.vardiya.sludge.domain.TagSource
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException

/** Asks Last.fm for a track's tags, and for the artist's when the track itself is unknown. */
class LastFmClient(private val transport: Transport = UrlConnectionTransport()) {
    suspend fun tags(apiKey: String, track: TrackInfo): LastFmReply {
        if (track.artist.isBlank()) return LastFmReply.NotFound
        val byTrack = if (track.title.isNotBlank()) ask(LastFmProtocol.trackTagsRequest(apiKey, track), track.artist) else LastFmReply.NotFound
        // An unknown track or one nobody tagged: the artist's tags still say what it sounds like.
        val usable = byTrack is LastFmReply.Tags && byTrack.names.isNotEmpty()
        return if (usable || byTrack is LastFmReply.Failure) byTrack else ask(LastFmProtocol.artistTagsRequest(apiKey, track), track.artist)
    }

    private suspend fun ask(request: com.vardiya.sludge.domain.HttpRequestSpec, artist: String): LastFmReply = try {
        val response = transport.send(request)
        if (response.code in OK) LastFmProtocol.parse(response.body, artist) else LastFmReply.Failure("Last.fm yanıtı: ${response.code}")
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        LastFmReply.Failure("Last.fm'e bağlanılamadı")
    } catch (_: IllegalArgumentException) {
        LastFmReply.Failure("Last.fm anahtarı geçersiz karakter içeriyor")
    }

    private companion object {
        val OK = 200..299
    }
}

/** The tag source the app uses: the saved key, a cache on disk, and nothing at all when there is no key. */
class LastFmTagSource(private val client: LastFmClient, private val repo: SettingsRepository) : TagSource {
    override suspend fun tagsFor(track: TrackInfo): List<String> {
        val config = repo.lastFm.first()
        if (!config.isValid) return emptyList()
        val key = TuneCache.key(track)
        repo.tagCache.first()[key]?.let { return it }
        return when (val reply = client.tags(config.apiKey.trim(), track)) {
            is LastFmReply.Tags -> remember(key, reply.names)
            // Unknown track and artist: remember the empty answer so it is not asked again for every play.
            LastFmReply.NotFound -> remember(key, emptyList())
            // Offline or a bad key: nothing is remembered, the next play tries again.
            is LastFmReply.Failure -> emptyList()
        }
    }

    private suspend fun remember(key: String, names: List<String>): List<String> {
        repo.updateTagCache { TagMemory.put(it, key, names) }
        return names
    }
}

/** For the "try it" button: the key as typed, without saving it. */
suspend fun LastFmClient.test(config: LastFmConfig, track: TrackInfo): Result<List<String>> {
    if (!config.isValid) return Result.failure(IllegalArgumentException("Anahtar 32 karakterlik onaltılık bir değer olmalı"))
    return when (val reply = tags(config.apiKey.trim(), track)) {
        is LastFmReply.Tags -> Result.success(reply.names)
        LastFmReply.NotFound -> Result.success(emptyList())
        is LastFmReply.Failure -> Result.failure(IOException(reply.message))
    }
}
