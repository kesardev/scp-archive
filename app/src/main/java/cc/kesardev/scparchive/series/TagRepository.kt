package cc.kesardev.scparchive.series

import android.content.Context
import cc.kesardev.scparchive.catalog.CatalogEntry
import cc.kesardev.scparchive.data.NetworkClient
import cc.kesardev.scparchive.data.RequestRateLimiter
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

data class TagPage(
    val entries: List<CatalogEntry>,
    val source: String
)

class TagRepository(private val context: Context) {

    private val client get() = NetworkClient.get(context)

    private val cache = TagCache(context)


    suspend fun load(tag: String, force: Boolean = false): TagPage = withContext(Dispatchers.IO) {
        val cached = cache.read(tag)
        if (!force && cached != null && cache.isFresh(cached)) {
            return@withContext TagPage(cached.entries, "缓存")
        }

        try {
            val html = fetch(tag)
            val entries = TagParser.parse(html, tag)
            if (entries.isEmpty()) {
                throw IOException("标签页没有条目：" + tag)
            }
            cache.write(tag, entries)
            TagPage(entries, "在线")
        } catch (error: Exception) {
            if (cached != null && cached.entries.isNotEmpty()) {
                TagPage(cached.entries, "离线缓存")
            } else {
                throw error
            }
        }
    }

    private suspend fun fetch(tag: String): String {
        RequestRateLimiter.await()
        val encoded = URLEncoder.encode(tag, "UTF-8").replace("+", "%20")
        val request = Request.Builder()
            .url(ReaderShellBuilder.BASE_URL + "system:page-tags/tag/" + encoded)
            .header("User-Agent", "SCPArchive/2.4 (Android reader)")
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
}