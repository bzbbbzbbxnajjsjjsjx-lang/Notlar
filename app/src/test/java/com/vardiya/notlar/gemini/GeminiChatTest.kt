package com.vardiya.notlar.gemini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiChatTest {

    @Test
    fun acceptsShareLinks() {
        assertNotNull(GeminiChat.validLink("https://gemini.google.com/share/abc123def456"))
        assertNotNull(GeminiChat.validLink("  https://gemini.google.com/share/abc123def456  "))
        assertNotNull(GeminiChat.validLink("https://g.co/gemini/share/abc123def456"))
    }

    @Test
    fun rejectsEverythingElse() {
        listOf(
            "http://gemini.google.com/share/abc123",
            "https://gemini.google.com.evil.com/share/abc123",
            "https://evil.com/?u=https://gemini.google.com/share/abc123",
            "https://user@gemini.google.com/share/abc123",
            "https://gemini.google.com:8443/share/abc123",
            "https://gemini.google.com/app",
            "https://gemini.google.com/share/",
            "https://g.co/share/abc123",
            "file:///sdcard/x",
            "javascript:alert(1)",
            "not a link",
        ).forEach { assertNull(it, GeminiChat.validLink(it)) }
    }

    @Test
    fun findsLinkInSharedText() {
        val text = "Gemini ile sohbetim: https://gemini.google.com/share/abc123def456."
        assertEquals("https://gemini.google.com/share/abc123def456", GeminiChat.linkIn(text))
        assertNull(GeminiChat.linkIn("https://example.com/share/abc123"))
    }

    @Test
    fun navigationStaysOnGoogleHosts() {
        assertEquals(true, GeminiChat.isAllowedNavigation("https://gemini.google.com/share/x1234"))
        assertEquals(false, GeminiChat.isAllowedNavigation("https://evil.com"))
        assertEquals(false, GeminiChat.isAllowedNavigation("intent://x#Intent;end"))
    }

    @Test
    fun parsesScriptAnswer() {
        val raw = "\"[{\\\"role\\\":\\\"User\\\",\\\"text\\\":\\\" Merhaba \\\"}," +
            "{\\\"role\\\":\\\"Model\\\",\\\"text\\\":\\\"Selam\\\"},{\\\"role\\\":\\\"Model\\\",\\\"text\\\":\\\"  \\\"}]\""
        val turns = GeminiChat.parseTurns(raw)
        assertEquals(listOf(ChatTurn(ChatRole.User, "Merhaba"), ChatTurn(ChatRole.Model, "Selam")), turns)
    }

    @Test
    fun unusableAnswersAreNull() {
        assertNull(GeminiChat.parseTurns(null))
        assertNull(GeminiChat.parseTurns("null"))
        assertNull(GeminiChat.parseTurns("\"oops\""))
        assertEquals(emptyList<ChatTurn>(), GeminiChat.parseTurns("\"[]\""))
    }

    @Test
    fun exportsMarkdownAndJson() {
        val turns = listOf(ChatTurn(ChatRole.User, "Soru"), ChatTurn(ChatRole.Model, "Cevap"))
        val md = GeminiChat.toMarkdown(turns, "https://gemini.google.com/share/abc123")
        assertEquals(
            "# Gemini Sohbeti\n\nKaynak: https://gemini.google.com/share/abc123\n\n## Sen\n\nSoru\n\n## Gemini\n\nCevap\n",
            md,
        )
        val json = GeminiChat.toJson(turns, "https://gemini.google.com/share/abc123")
        assertEquals(true, json.contains("\"role\": \"User\"") && json.contains("\"text\": \"Cevap\""))
        assertEquals(1, GeminiChat.questionCount(turns))
    }
}
