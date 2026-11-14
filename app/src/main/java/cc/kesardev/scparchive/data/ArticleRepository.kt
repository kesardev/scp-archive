package cc.kesardev.scparchive.data

import android.content.Context
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class RawPage(
    val html: String,
    val source: String
)

private data class NetworkResult(
    val html: String?,
    val etag: String,
    val lastModified: String,
    val notModified: Boolean
)

/**
 * Source order: fresh cache -> bundled sample -> conditional network request.
 * On network failure a stale cache is returned before the bundled fallback.
 */
class ArticleRepository(private val context: Context) {

    private val client get() = NetworkClient.get(context)

    private val cache = ArticleCache(context)


    suspend fun load(slug: String, forceNetwork: Boolean = false): RawPage = withContext(Dispatchers.IO) {
        val cached = cache.read(slug)

        if (!forceNetwork && cached != null && cache.isFresh(cached)) {
            return@withContext RawPage(cached.html, "缓存")
        }
        if (!forceNetwork && cached == null) {
            loadBundled(slug)?.let { return@withContext it }
        }

        try {
            val result = loadNetwork(slug, cached)
            if (result.notModified && cached != null) {
                return@withContext RawPage(cached.html, "缓存")
            }
            val html = result.html ?: throw IOException("empty response")
            cache.write(slug, html, result.etag, result.lastModified)
            RawPage(html, "在线")
        } catch (error: Exception) {
            cached?.let { return@withContext RawPage(it.html, "离线缓存") }
            loadBundled(slug)?.let { return@withContext it }
            throw error
        }
    }

    private fun loadBundled(slug: String): RawPage? {
        val path = "fixtures/" + slug.lowercase() + ".html"
        return try {
            val html = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
            RawPage(html, "本地样本")
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun loadNetwork(slug: String, cached: CachedArticle?): NetworkResult {
        RequestRateLimiter.await()
        val builder = Request.Builder()
            .url(ReaderShellBuilder.BASE_URL + slug.lowercase())
            .header("User-Agent", "SCPArchive/2.2 (Android reader)")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9")

        if (!cached?.etag.isNullOrEmpty()) {
            builder.header("If-None-Match", cached!!.etag)
        }
        if (!cached?.lastModified.isNullOrEmpty()) {
            builder.header("If-Modified-Since", cached!!.lastModified)
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 304) {
                return NetworkResult(
                    html = null,
                    etag = response.header("ETag") ?: cached?.etag.orEmpty(),
                    lastModified = response.header("Last-Modified") ?: cached?.lastModified.orEmpty(),
                    notModified = true
                )
            }
            if (!response.isSuccessful) {
                throw IOException("HTTP " + response.code)
            }
            val body = response.body?.string() ?: throw IOException("empty response")
            return NetworkResult(
                html = body,
                etag = response.header("ETag").orEmpty(),
                lastModified = response.header("Last-Modified").orEmpty(),
                notModified = false
            )
        }
    }
}
