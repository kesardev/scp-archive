package cc.kesardev.scparchive.home

import android.content.Context
import cc.kesardev.scparchive.catalog.CatalogDatabase
import cc.kesardev.scparchive.catalog.SeriesParser
import cc.kesardev.scparchive.data.NetworkClient
import cc.kesardev.scparchive.data.RequestRateLimiter
import cc.kesardev.scparchive.library.LibraryDatabase
import cc.kesardev.scparchive.library.LibraryEntry
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class RemoteSnapshot(
    val topRated: List<HomeArticle>,
    val topRatedSource: String,
    val recent: List<HomeArticle>,
    val recentSource: String
)

private data class FeedResult(
    val items: List<HomeArticle>,
    val source: String
)

class HomeRepository(private val context: Context) {

    private val client get() = NetworkClient.get(context)

    private val library = LibraryDatabase(context)
    private val catalog = CatalogDatabase(context)
    private val cache = HomeCache(context)


    suspend fun localSnapshot(): HomeUiState = withContext(Dispatchers.IO) {
        val catalogCount = ensureCatalog()
        val history = library.history()
        val favorites = library.favorites()
        val pick = DailyPicks.forDay(DailyPicks.currentEpochDay())

        HomeUiState(
            daily = HomeArticle(
                slug = pick.slug,
                title = pick.name,
                subtitle = "今日推荐",
                tagline = pick.tagline
            ),
            recommended = recommended(pick),
            continueReading = history
                .filter { it.progress < 0.98f && !it.slug.contains("/html/", ignoreCase = true) }
                .take(6)
                .map { toArticle(it, withProgress = true) },
            catalogCount = catalogCount,
            favoriteCount = favorites.size,
            historyCount = history.size,
            loading = false
        )
    }

    suspend fun remoteSnapshot(force: Boolean = false): RemoteSnapshot = withContext(Dispatchers.IO) {
        val top = loadFeed(
            name = "top-rated",
            path = TOP_RATED_PATH,
            maxAgeMs = TOP_RATED_TTL,
            force = force
        ) { HomeParser.parseTopRated(it) }
        val recent = loadFeed(
            name = "recent",
            path = RECENT_PATH,
            maxAgeMs = RECENT_TTL,
            force = force
        ) { HomeParser.parseRecent(it) }
        RemoteSnapshot(top.items, top.source, recent.items, recent.source)
    }

    suspend fun randomArticle(): HomeArticle? = withContext(Dispatchers.IO) {
        catalog.random()?.let { entry ->
            return@withContext HomeArticle(
                slug = entry.id,
                title = entry.title.ifBlank { entry.id.uppercase() },
                subtitle = "随机条目"
            )
        }
        val pick = DailyPicks.all.random()
        HomeArticle(pick.slug, pick.name, subtitle = "随机条目", tagline = pick.tagline)
    }

    private fun recommended(daily: DailyPick): List<HomeArticle> {
        val all = DailyPicks.all
        val start = all.indexOfFirst { it.slug == daily.slug }.coerceAtLeast(0)
        return (1..minOf(6, all.size - 1)).map { offset ->
            val pick = all[(start + offset) % all.size]
            HomeArticle(
                slug = pick.slug,
                title = pick.name,
                subtitle = "编辑精选",
                tagline = pick.tagline
            )
        }
    }

    private fun ensureCatalog(): Int {
        val existing = catalog.count()
        if (existing > 0) {
            return existing
        }
        for (seriesId in BUNDLED_SERIES) {
            val html = readAsset("catalog/" + seriesId + ".html") ?: continue
            val entries = SeriesParser.parse(html, seriesId)
            catalog.upsert(entries)
        }
        return catalog.count()
    }

    private suspend fun loadFeed(
        name: String,
        path: String,
        maxAgeMs: Long,
        force: Boolean,
        parser: (String) -> List<HomeArticle>
    ): FeedResult {
        val cached = cache.read(name)
        if (!force && cached != null && cache.isFresh(cached, maxAgeMs)) {
            return FeedResult(cached.items, "缓存")
        }
        return try {
            val items = parser(fetch(path))
            if (items.isEmpty()) {
                throw IOException("empty feed")
            }
            runCatching { cache.write(name, items) }
            FeedResult(items, "在线")
        } catch (_: Exception) {
            if (cached != null && cached.items.isNotEmpty()) {
                FeedResult(cached.items, "离线缓存")
            } else {
                FeedResult(emptyList(), "离线")
            }
        }
    }

    private suspend fun fetch(path: String): String {
        RequestRateLimiter.await()
        val request = Request.Builder()
            .url(ReaderShellBuilder.BASE_URL + path)
            .header("User-Agent", "SCPArchive/2.4 (Android reader)")
            .header("Accept", "text/html,application/xhtml+xml,application/xml,text/xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP " + response.code)
            }
            return response.body?.string() ?: throw IOException("empty response")
        }
    }

    private fun readAsset(path: String): String? {
        return try {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: IOException) {
            null
        }
    }

    private fun toArticle(entry: LibraryEntry, withProgress: Boolean): HomeArticle {
        return HomeArticle(
            slug = entry.slug,
            title = entry.title.ifBlank { entry.slug.uppercase() },
            subtitle = if (withProgress) "已读 " + (entry.progress * 100f).toInt() + "%" else "",
            progress = if (withProgress) entry.progress else null,
            timestamp = entry.timestamp
        )
    }

    companion object {
        private const val TOP_RATED_PATH =
            "top-rated-pages/pagescp_limit/1/all_range/-/scp_range/others"
        private const val RECENT_PATH = "feed/site-changes.xml"
        private const val TOP_RATED_TTL = 12L * 60L * 60L * 1000L
        private const val RECENT_TTL = 30L * 60L * 1000L
        private val BUNDLED_SERIES = listOf("scp-series", "scp-series-cn")
    }
}