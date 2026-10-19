package com.vardiya.sludge.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/** One model a provider offers. Prices are dollars per million tokens; null when the provider does not say. */
data class ModelOption(
    val id: String,
    val name: String,
    val promptPerMillion: Double?,
    val completionPerMillion: Double?,
    val contextLength: Int?,
) {
    val isFree: Boolean get() = (promptPerMillion == 0.0 && completionPerMillion == 0.0) || id.endsWith(":free")
}

/** How a model's price reads on the list. */
fun ModelOption.priceLabel(): String = when {
    isFree -> "ücretsiz"
    promptPerMillion == null -> "değişken"
    else -> "$" + String.format(Locale.forLanguageTag("tr-TR"), "%.2f", promptPerMillion) + " / 1M"
}

/** The model list of an OpenRouter-style API (`GET {base}/models`), which is public and needs no key. */
object ModelCatalogProtocol {
    private const val PER_MILLION = 1_000_000.0

    /** "Cheap" on the list: at most this many dollars per million input tokens. */
    const val CHEAP_PER_MILLION = 1.0

    fun request(baseUrl: String): HttpRequestSpec =
        HttpRequestSpec(url = baseUrl.trim().trimEnd('/') + "/models", headers = mapOf("accept" to "application/json"), body = "", method = "GET")

    /** Text-in, text-out models in the provider's own order (newest first on OpenRouter). Bad entries are skipped. */
    fun parse(body: String): Result<List<ModelOption>> = runCatching {
        val data = AiProtocol.json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: JsonArray(emptyList())
        data.mapNotNull { (it as? JsonObject)?.let(::option) }
    }

    private fun option(entry: JsonObject): ModelOption? {
        val id = entry["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val outputs = (entry["architecture"] as? JsonObject)?.get("output_modalities") as? JsonArray
        if (outputs != null && outputs.none { it.jsonPrimitive.contentOrNull == "text" }) return null
        val pricing = entry["pricing"] as? JsonObject
        return ModelOption(
            id = id,
            name = entry["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: id,
            promptPerMillion = perMillion(pricing, "prompt"),
            completionPerMillion = perMillion(pricing, "completion"),
            contextLength = entry["context_length"]?.jsonPrimitive?.intOrNull,
        )
    }

    // OpenRouter prices are dollars per token, as strings; "-1" marks a router whose price depends on the pick.
    private fun perMillion(pricing: JsonObject?, field: String): Double? =
        pricing?.get(field)?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { it * PER_MILLION }

    /** The list narrowed by a search text (name or id), and optionally to free or cheap models. */
    fun filter(models: List<ModelOption>, query: String, filter: ModelFilter = ModelFilter.ALL): List<ModelOption> {
        val words = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return models.filter { m ->
            val text = (m.name + " " + m.id).lowercase(Locale.ROOT)
            words.all { it in text } && when (filter) {
                ModelFilter.ALL -> true
                ModelFilter.FREE -> m.isFree
                ModelFilter.CHEAP -> m.isFree || (m.promptPerMillion != null && m.promptPerMillion <= CHEAP_PER_MILLION)
            }
        }
    }
}

enum class ModelFilter(val label: String) { ALL("Tümü"), FREE("Ücretsiz"), CHEAP("Ucuz") }
