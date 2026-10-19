package com.vardiya.sludge.domain

import com.vardiya.sludge.data.ModelCatalog
import com.vardiya.sludge.data.Transport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private val SAMPLE = """
{"data":[
 {"id":"anthropic/claude-sonnet","name":"Anthropic: Claude Sonnet","context_length":200000,
  "architecture":{"input_modalities":["text","image"],"output_modalities":["text"]},
  "pricing":{"prompt":"0.000003","completion":"0.000015"}},
 {"id":"openai/gpt-4o-mini","name":"OpenAI: GPT-4o mini","context_length":128000,
  "architecture":{"output_modalities":["text"]},"pricing":{"prompt":"0.00000015","completion":"0.0000006"}},
 {"id":"meta/llama-free:free","name":"Meta: Llama (free)","pricing":{"prompt":"0","completion":"0"}},
 {"id":"openrouter/auto","name":"Auto Router","pricing":{"prompt":"-1","completion":"-1"}},
 {"id":"image/painter","name":"Painter","architecture":{"output_modalities":["image"]},"pricing":{"prompt":"0.00001","completion":"0"}},
 {"name":"No id"},
 "not an object"
]}
""".trimIndent()

class ModelCatalogProtocolTest {
    private val models = ModelCatalogProtocol.parse(SAMPLE).getOrThrow()

    @Test fun `only text models with an id are kept, in the provider's order`() {
        assertEquals(listOf("anthropic/claude-sonnet", "openai/gpt-4o-mini", "meta/llama-free:free", "openrouter/auto"), models.map { it.id })
    }

    @Test fun `prices become dollars per million tokens`() {
        assertEquals(3.0, models[0].promptPerMillion!!, 0.0001)
        assertEquals(15.0, models[0].completionPerMillion!!, 0.0001)
        assertEquals(0.15, models[1].promptPerMillion!!, 0.0001)
        assertEquals(200_000, models[0].contextLength)
    }

    @Test fun `a router with a negative price has no price`() {
        assertNull(models[3].promptPerMillion)
        assertEquals("değişken", models[3].priceLabel())
    }

    @Test fun `free models are recognised by price or by the free suffix`() {
        assertTrue(models[2].isFree)
        assertTrue(ModelOption("x/y:free", "y", 1.0, 1.0, null).isFree)
        assertFalse(models[0].isFree)
        assertEquals("ücretsiz", models[2].priceLabel())
        assertEquals("$3,00 / 1M", models[0].priceLabel())
    }

    @Test fun `a missing name falls back to the id`() {
        assertEquals("openrouter/auto", ModelCatalogProtocol.parse("""{"data":[{"id":"openrouter/auto"}]}""").getOrThrow().single().name)
    }

    @Test fun `search matches every word in the name or the id`() {
        assertEquals(listOf("openai/gpt-4o-mini"), ModelCatalogProtocol.filter(models, "gpt mini").map { it.id })
        assertEquals(listOf("anthropic/claude-sonnet"), ModelCatalogProtocol.filter(models, "ANTHROPIC").map { it.id })
        assertEquals(models, ModelCatalogProtocol.filter(models, "  "))
        assertTrue(ModelCatalogProtocol.filter(models, "nothing like this").isEmpty())
    }

    @Test fun `the free and cheap filters narrow the list`() {
        assertEquals(listOf("meta/llama-free:free"), ModelCatalogProtocol.filter(models, "", ModelFilter.FREE).map { it.id })
        assertEquals(
            listOf("openai/gpt-4o-mini", "meta/llama-free:free"),
            ModelCatalogProtocol.filter(models, "", ModelFilter.CHEAP).map { it.id },
        )
    }

    @Test fun `the request is a keyless GET on the models path`() {
        val r = ModelCatalogProtocol.request("https://openrouter.ai/api/v1/")
        assertEquals("https://openrouter.ai/api/v1/models", r.url)
        assertEquals("GET", r.method)
        assertTrue(r.headers.keys.none { it.equals("authorization", true) })
    }

    @Test fun `a broken body is a failure`() {
        assertTrue(ModelCatalogProtocol.parse("<html>").isFailure)
    }
}

private class CountingTransport(private val reply: () -> HttpResult) : Transport {
    var calls = 0
    override suspend fun send(request: HttpRequestSpec): HttpResult {
        calls++
        return reply()
    }
}

class ModelCatalogLoadTest {
    private val base = "https://openrouter.ai/api/v1"

    @Test fun `the list is fetched once and then remembered`() {
        val transport = CountingTransport { HttpResult(200, SAMPLE) }
        val catalog = ModelCatalog(transport)
        runBlocking {
            assertEquals(4, catalog.load(base).getOrThrow().size)
            assertEquals(4, catalog.load(base).getOrThrow().size)
        }
        assertEquals(1, transport.calls)
    }

    @Test fun `an insecure address is refused before any request`() {
        val transport = CountingTransport { HttpResult(200, SAMPLE) }
        assertTrue(runBlocking { ModelCatalog(transport).load("http://example.com/api") }.isFailure)
        assertEquals(0, transport.calls)
    }

    @Test fun `a failure is not remembered`() {
        var offline = true
        val transport = CountingTransport { if (offline) throw IOException("offline") else HttpResult(200, SAMPLE) }
        val catalog = ModelCatalog(transport)
        assertTrue(runBlocking { catalog.load(base) }.isFailure)
        offline = false
        assertTrue(runBlocking { catalog.load(base) }.isSuccess)
    }

    @Test fun `an empty or broken list is a failure`() {
        assertTrue(runBlocking { ModelCatalog(CountingTransport { HttpResult(200, """{"data":[]}""") }).load(base) }.isFailure)
        assertTrue(runBlocking { ModelCatalog(CountingTransport { HttpResult(200, "nope") }).load(base) }.isFailure)
        assertTrue(runBlocking { ModelCatalog(CountingTransport { HttpResult(503, "") }).load(base) }.isFailure)
    }
}
