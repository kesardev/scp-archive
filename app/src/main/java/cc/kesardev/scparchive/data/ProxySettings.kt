package cc.kesardev.scparchive.data

import android.content.Context
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

enum class ProxyKind {
    HTTP,
    SOCKS;

    fun toProxyType(): Proxy.Type {
        return if (this == SOCKS) Proxy.Type.SOCKS else Proxy.Type.HTTP
    }
}

data class ProxyEndpoint(
    val host: String,
    val port: Int,
    val kind: ProxyKind
) {
    fun toProxy(): Proxy {
        return Proxy(kind.toProxyType(), InetSocketAddress(host, port))
    }

    fun label(): String {
        return kind.name + " · " + host + ":" + port
    }

    companion object {
        fun parse(raw: String, preferred: ProxyKind): ProxyEndpoint? {
            val text = raw.trim()
            if (text.isEmpty()) {
                return null
            }
            val withScheme = if (text.contains("://")) {
                text
            } else if (preferred == ProxyKind.SOCKS) {
                "socks5://" + text
            } else {
                "http://" + text
            }
            val uri = try {
                URI(withScheme)
            } catch (_: Exception) {
                return null
            }
            val scheme = uri.scheme?.lowercase() ?: return null
            val kind = when {
                scheme.startsWith("socks") -> ProxyKind.SOCKS
                scheme == "http" || scheme == "https" -> ProxyKind.HTTP
                else -> preferred
            }
            val host = uri.host?.trim()?.trim('[', ']')
            if (host.isNullOrEmpty()) {
                return null
            }
            val port = if (uri.port in 1..65535) {
                uri.port
            } else if (kind == ProxyKind.SOCKS) {
                1080
            } else {
                8080
            }
            return ProxyEndpoint(host, port, kind)
        }
    }
}

data class ProxyConfig(
    val enabled: Boolean = false,
    val kind: ProxyKind = ProxyKind.HTTP,
    val address: String = ProxySettings.DEFAULT_ADDRESS
) {
    fun endpoint(): ProxyEndpoint? {
        return ProxyEndpoint.parse(address, kind)
    }
}

class ProxySettings(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): ProxyConfig {
        val kind = try {
            ProxyKind.valueOf(
                preferences.getString(KEY_KIND, ProxyKind.HTTP.name) ?: ProxyKind.HTTP.name
            )
        } catch (_: Exception) {
            ProxyKind.HTTP
        }
        return ProxyConfig(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            kind = kind,
            address = preferences.getString(KEY_ADDRESS, DEFAULT_ADDRESS) ?: DEFAULT_ADDRESS
        )
    }

    fun save(config: ProxyConfig) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_KIND, config.kind.name)
            .putString(KEY_ADDRESS, config.address)
            .apply()
    }

    companion object {
        const val DEFAULT_ADDRESS = ""
        private const val PREFERENCES = "network_settings"
        private const val KEY_ENABLED = "proxy_enabled"
        private const val KEY_KIND = "proxy_kind"
        private const val KEY_ADDRESS = "proxy_address"
    }
}