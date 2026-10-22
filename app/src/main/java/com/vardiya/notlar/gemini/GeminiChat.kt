package com.vardiya.notlar.gemini

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI

/** Who wrote a turn; the names are what the page script and the JSON export call them. */
@Serializable
enum class ChatRole { User, Model }

@Serializable
data class ChatTurn(val role: ChatRole, val text: String)

/** Pure rules for share links and exports; no Android classes, so unit tests run on the JVM. */
object GeminiChat {

    /** The most turns and characters a page may yield; a hostile page cannot fill the memory. */
    const val MAX_TURNS = 2_000
    const val MAX_CHARS = 5_000_000

    private val allowedHosts = setOf("gemini.google.com", "g.co")
    private val sharePath = Regex("^/(gemini/)?share/[A-Za-z0-9_-]{4,128}/?$")
    private val urlInText = Regex("https://[^\\s<>\"']+")

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    private val lenientJson = Json { ignoreUnknownKeys = true }

    /**
     * The link when it is a plain https Gemini share link: exact host, no user info, no port,
     * no fragment tricks. Everything else is null, so nothing else is ever loaded.
     */
    fun validLink(raw: String): String? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()
        val ok = uri.scheme == "https" && host in allowedHosts && uri.rawUserInfo == null && uri.port == -1 &&
            sharePath.matches(uri.rawPath.orEmpty()) && (host != "g.co" || uri.rawPath.orEmpty().startsWith("/gemini/"))
        return if (ok) uri.toString() else null
    }

    /** The first valid share link in text shared from another app, which may add a title around it. */
    fun linkIn(text: String): String? =
        urlInText.findAll(text).firstNotNullOfOrNull { validLink(it.value.trimEnd('.', ',', ')', ';')) }

    /** True for the hosts a page may navigate to while it loads. */
    fun isAllowedNavigation(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host?.lowercase() in allowedHosts
    }

    /**
     * Turns the script's answer into turns. [raw] is what `evaluateJavascript` returns: a JSON string
     * literal that holds the JSON array. Blank turns are dropped; null means the answer is unusable.
     */
    fun parseTurns(raw: String?): List<ChatTurn>? {
        if (raw.isNullOrBlank() || raw == "null" || raw.length > MAX_CHARS * 2) return null
        val array = runCatching {
            val element = lenientJson.parseToJsonElement(raw)
            val inner = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: raw
            lenientJson.decodeFromString<List<RawTurn>>(inner)
        }.getOrNull()
        val turns = array.orEmpty().asSequence().mapNotNull { it.toTurn() }.take(MAX_TURNS).toList()
        return turns.takeIf { array != null && it.sumOf { turn -> turn.text.length } <= MAX_CHARS }
    }

    @Serializable
    private data class RawTurn(val role: String = "", val text: String = "") {
        fun toTurn(): ChatTurn? {
            val parsedRole = when (role) {
                "User" -> ChatRole.User
                "Model" -> ChatRole.Model
                else -> return null
            }
            val trimmed = text.trim()
            return if (trimmed.isEmpty()) null else ChatTurn(parsedRole, trimmed)
        }
    }

    @Serializable
    private data class Export(val source: String, val turns: List<ChatTurn>)

    fun toMarkdown(turns: List<ChatTurn>, source: String): String = buildString {
        append("# Gemini Sohbeti\n\nKaynak: ").append(source).append("\n")
        turns.forEach { turn ->
            append("\n## ").append(if (turn.role == ChatRole.User) "Sen" else "Gemini").append("\n\n")
            append(turn.text).append("\n")
        }
    }

    fun toJson(turns: List<ChatTurn>, source: String): String = json.encodeToString(Export(source, turns))

    fun questionCount(turns: List<ChatTurn>) = turns.count { it.role == ChatRole.User }
}
