package cc.kesardev.scparchive.data

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ProxyTestResult(
    val ok: Boolean,
    val message: String
)

/**
 * Shared OkHttp client. The proxy is rebuilt when the saved configuration
 * changes, and the same client is used for reader resources so images also go
 * through the selected proxy.
 */
object NetworkClient {

    private val lock = Any()
    private var cachedKey: String = ""
    private var cachedClient: OkHttpClient? = null

    fun get(context: Context): OkHttpClient {
        val config = ProxySettings(context).load()
        val endpoint = if (config.enabled) config.endpoint() else null
        val key = endpoint?.let { "proxy:" + it.kind.name + ":" + it.host + ":" + it.port }
            ?: "direct"
        synchronized(lock) {
            val existing = cachedClient
            if (existing != null && cachedKey == key) {
                return existing
            }
            val client = build(config, forceProxy = endpoint != null)
            cachedClient = client
            cachedKey = key
            return client
        }
    }

    fun reset() {
        synchronized(lock) {
            cachedClient = null
            cachedKey = ""
        }
    }

    fun build(config: ProxyConfig, forceProxy: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
        if (forceProxy || config.enabled) {
            config.endpoint()?.let { builder.proxy(it.toProxy()) }
        }
        return builder.build()
    }

    suspend fun test(
        context: Context,
        config: ProxyConfig,
        targetUrl: String
    ): ProxyTestResult = withContext(Dispatchers.IO) {
        val endpoint = config.endpoint()
            ?: return@withContext ProxyTestResult(false, "代理地址无效，请填写 host:port")
        val started = System.currentTimeMillis()
        return@withContext try {
            val probe = build(config, forceProxy = true)
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "SCPArchive/2.5 (Android reader)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            probe.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - started
                val ok = response.code in 200..399
                ProxyTestResult(
                    ok,
                    (if (ok) "代理可用" else "代理已连接但返回异常") +
                        " · " + endpoint.label() +
                        " · HTTP " + response.code +
                        " · " + elapsed + " ms"
                )
            }
        } catch (error: Exception) {
            ProxyTestResult(
                false,
                "代理测试失败 · " + endpoint.label() + " · " +
                    (error.message ?: error.javaClass.simpleName)
            )
        }
    }

    /**
     * Supplies WebView subresources through the same proxy as the article
     * fetch. Only used when a proxy is enabled, so direct mode keeps WebView's
     * own cache and network stack.
     */
    fun fetchWebResource(context: Context, url: String): WebResourceResponse? {
        val config = ProxySettings(context).load()
        if (!config.enabled || config.endpoint() == null) {
            return null
        }
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return null
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SCPArchive/2.5 (Android reader)")
                .build()
            get(context).newCall(request).execute().use { response ->
                val body = response.body?.bytes() ?: return null
                val contentType = response.header("Content-Type") ?: "application/octet-stream"
                val mime = contentType.substringBefore(';').trim()
                val encoding = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE)
                    .find(contentType)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
                    ?.trim('"')
                WebResourceResponse(
                    mime.ifEmpty { "application/octet-stream" },
                    encoding,
                    response.code,
                    response.message.ifEmpty { "OK" },
                    response.headers.toMultimap().mapValues { it.value.joinToString(", ") },
                    ByteArrayInputStream(body)
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}