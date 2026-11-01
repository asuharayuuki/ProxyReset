package org.eu.suisei.proxyreset

import java.net.IDN
import java.util.Locale

data class ProxyEndpoint(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"

    companion object {
        private val hostLabel = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")

        fun parse(value: String): ProxyEndpoint? {
            val parts = value.trim().split(':')
            if (parts.size != 2) return null
            val port = parts[1].takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
                ?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            val host = try {
                IDN.toASCII(parts[0], IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
            } catch (_: IllegalArgumentException) {
                return null
            }
            if (host.isEmpty() || host.length > 253 || host.split('.').any { !hostLabel.matches(it) }) {
                return null
            }
            // HTTP_PROXY uses one colon separator; IPv6 needs a different system API.
            if (host.all { it.isDigit() || it == '.' }) {
                val octets = host.split('.')
                if (octets.size != 4 || octets.any { it.toIntOrNull() !in 0..255 }) return null
            }
            return ProxyEndpoint(host, port)
        }
    }
}
