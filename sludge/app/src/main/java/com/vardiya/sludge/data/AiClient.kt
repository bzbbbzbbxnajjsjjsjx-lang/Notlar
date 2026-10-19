package com.vardiya.sludge.data

import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AiProtocol
import com.vardiya.sludge.domain.HttpRequestSpec
import com.vardiya.sludge.domain.HttpResult
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.UrlRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Sends an [HttpRequestSpec]. A separate type so the tuner can be tested without a network. */
fun interface Transport {
    suspend fun send(request: HttpRequestSpec): HttpResult
}

class UrlConnectionTransport(private val io: CoroutineDispatcher = Dispatchers.IO) : Transport {
    override suspend fun send(request: HttpRequestSpec): HttpResult = withContext(io) {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_MS
            connection.readTimeout = READ_MS
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in HTTP_OK_RANGE) connection.inputStream else connection.errorStream
            HttpResult(code, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_MS = 10_000
        const val READ_MS = 45_000
        val HTTP_OK_RANGE = 200..299
    }
}

/** Asks the configured model for an equalizer profile. Failures come back as a Turkish message, never a key. */
class AiTuner(private val transport: Transport = UrlConnectionTransport()) {
    suspend fun tune(config: AiConfig, track: TrackInfo): Result<AiProfile> {
        if (!UrlRules.isAcceptable(config.baseUrl)) {
            return Result.failure(IllegalArgumentException("Adres https:// ile başlamalı (yerel ağ hariç)"))
        }
        val response = try {
            transport.send(AiProtocol.buildRequest(config, track))
        } catch (e: IOException) {
            return Result.failure(IOException("Bağlanılamadı: ${e.javaClass.simpleName}"))
        }
        if (response.code !in SUCCESS) {
            return Result.failure(IOException(AiProtocol.describeFailure(response.code, response.body)))
        }
        val text = AiProtocol.extractText(config.provider, response.body)
            ?: return Result.failure(IOException("Yanıt beklenen biçimde değil"))
        return AiProtocol.parseProfile(text).fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IOException(it.message ?: "Yanıt okunamadı")) },
        )
    }

    private companion object {
        val SUCCESS = 200..299
    }
}
