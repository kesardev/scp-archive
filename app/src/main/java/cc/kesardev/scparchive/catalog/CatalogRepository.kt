package cc.kesardev.scparchive.catalog

import android.content.Context
import cc.kesardev.scparchive.data.RequestRateLimiter
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CatalogPage(
    val entries: List<CatalogEntry>,
    val source: String
)

class CatalogRepository(private val context: Context) {

    private val cache = SeriesCache(context)
    private val index = CatalogDatabase(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun load(series: SeriesInfo, force: Boolean = false): CatalogPage = withContext(Dispatchers.IO) {
        val cached = cache.read(series.id)

        if (!force && cached != null && cache.isFresh(cached)) {
            index.upsert(cached.entries)
            return@withContext CatalogPage(cached.entries, "缓存")
        }
        if (!force && cached == null) {
            loadBundled(series.id)?.let { html ->
                val entries = parseOrThrow(html, series.id)
                cache.write(series.id, entries)
                return@withContext CatalogPage(entries, "本地样本")
            }
        }

        try {
            val html = loadNetwork(series.path)
            val entries = parseOrThrow(html, series.id)
            cache.write(series.id, entries)
            CatalogPage(entries, "在线")
        } catch (error: Exception) {
            cached?.let {
                index.upsert(it.entries)
                return@withContext CatalogPage(it.entries, "离线缓存")
            }
            loadBundled(series.id)?.let { html ->
                return@withContext CatalogPage(parseOrThrow(html, series.id), "本地样本")
            }
            throw error
        }
    }

    suspend fun syncAll(onProgress: (Int, Int, String) -> Unit): Int = withContext(Dispatchers.IO) {
        var indexed = 0
        val seriesList = SeriesCatalog.all
        for ((position, series) in seriesList.withIndex()) {
            onProgress(position + 1, seriesList.size, series.name)
            try {
                val page = load(series, force = true)
                index.upsert(page.entries)
                indexed += page.entries.size
            } catch (_: Exception) {
                // Keep going: one broken series should not stop the whole sync.
            }
        }
        indexed
    }

    private fun parseOrThrow(html: String, seriesId: String): List<CatalogEntry> {
        val entries = SeriesParser.parse(html, seriesId)
        if (entries.isEmpty()) {
            throw IOException("no catalog entries in " + seriesId)
        }
        index.upsert(entries)
        return entries
    }

    private fun loadBundled(seriesId: String): String? {
        return try {
            context.assets.open("catalog/" + seriesId + ".html")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun loadNetwork(path: String): String {
        RequestRateLimiter.await()
        val request = Request.Builder()
            .url(ReaderShellBuilder.BASE_URL + path)
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
}
