package cc.kesardev.scparchive.reader

import android.content.Context
import cc.kesardev.scparchive.data.RequestRateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class HtmlBlockPage(
    val html: String,
    val source: String
)

/**
 * Raw HTML block cache, separate from the article cache. The block is fetched
 * on demand when the user opens the interactive copy.
 */
class HtmlBlockRepository(private val context: Context) {

    private val directory = File(context.cacheDir, "html-blocks").apply { mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun load(url: String, force: Boolean = false): HtmlBlockPage =
        withContext(Dispatchers.IO) {
            val file = File(directory, keyOf(url) + ".html")
            val cached = if (file.exists()) file.readText(Charsets.UTF_8) else null
            if (!force && cached != null && isFresh(file)) {
                return@withContext HtmlBlockPage(cached, "缓存")
            }
            try {
                val html = fetch(url)
                directory.mkdirs()
                file.writeText(html, Charsets.UTF_8)
                HtmlBlockPage(html, "在线")
            } catch (error: Exception) {
                if (cached != null) {
                    HtmlBlockPage(cached, "离线缓存")
                } else {
                    throw error
                }
            }
        }

    fun prepare(rawHtml: String): String {
        return HtmlBlockPreparer.prepare(rawHtml)
    }

    private suspend fun fetch(url: String): String {
        RequestRateLimiter.await()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SCPArchive/2.2 (Android reader)")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP " + response.code)
            }
            return response.body?.string() ?: throw IOException("empty response")
        }
    }

    private fun isFresh(file: File): Boolean {
        return file.lastModified() > 0L &&
            System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS
    }

    private fun keyOf(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private companion object {
        const val MAX_AGE_MS = 6L * 60L * 60L * 1000L
    }
}
