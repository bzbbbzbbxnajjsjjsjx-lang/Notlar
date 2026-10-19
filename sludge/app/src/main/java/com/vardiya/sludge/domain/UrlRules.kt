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
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return parts[0] == 127 || parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31)
    }
}
