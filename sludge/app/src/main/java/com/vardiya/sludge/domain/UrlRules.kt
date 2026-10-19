package com.vardiya.sludge.domain

import java.net.URI

/** The API key only travels over HTTPS, or plain HTTP to a machine on the local network (a self-hosted model). */
object UrlRules {
    fun isAcceptable(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        val host = uri.host ?: return false
        return when (uri.scheme?.lowercase()) {
            "https" -> true
            "http" -> isLocalHost(host)
            else -> false
        }
    }

    private fun isLocalHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local")) return true
        // Exactly four decimal octets: "127.0.0.1.evil.com" is a public name, not an address.
        val parts = host.split('.').map { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it == null || it !in 0..255 }) return false
        val octets = parts.filterNotNull()
        return octets[0] == 127 || octets[0] == 10 ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 172 && octets[1] in 16..31)
    }
}
