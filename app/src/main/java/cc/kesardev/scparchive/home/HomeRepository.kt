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
import java.io.File

data class RemoteSnapshot(
    val topRated: List<HomeArticle>,
    val topRatedSource: String,
    val recent: List<HomeArticle>,
    val recentSource: String,
    val scpTopRated: List<HomeArticle> = emptyList(),
    val featured: List<FeaturedItem> = emptyList(),
    val featuredSource: String = ""
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
    private val featuredCache = FeaturedCache(context)


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
            heat = buildHeat(history, favorites),
            continueReading = history
                .filter { it.progress < 1f && !it.slug.contains("/html/", ignoreCase = true) }
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
            name = "top-rated-40",
            path = TOP_RATED_PATH,
            maxAgeMs = TOP_RATED_TTL,
            force = force
        ) { HomeParser.parseTopRated(it, 40) }
        val recent = loadFeed(
            name = "recent",
            path = RECENT_PATH,
            maxAgeMs = RECENT_TTL,
            force = force
        ) { HomeParser.parseRecent(it) }
        val scp = loadFeed(
            name = "top-rated-en",
            path = EN_TOP_RATED_URL,
            maxAgeMs = TOP_RATED_TTL,
            force = force
        ) { HomeParser.parseEnglishTopRated(it, 24) }
        val featured = loadFeatured(force)
        RemoteSnapshot(
            topRated = top.items,
            topRatedSource = top.source,
            recent = recent.items,
            recentSource = recent.source,
            scpTopRated = scp.items,
            featured = featured.first,
            featuredSource = featured.second
        )
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


    suspend fun dailySummary(slug: String, title: String): String = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "home/summary2-" + slug + ".txt")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < SUMMARY_TTL) {
            return@withContext file.readText(Charsets.UTF_8)
        }
        val html = try {
            fetch(slug)
        } catch (_: Exception) {
            return@withContext ""
        }
        val summary = HomeParser.parseSummary(html, title)
        if (summary.isNotEmpty()) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(summary, Charsets.UTF_8)
            }
        }
        summary
    }

    private suspend fun loadFeatured(force: Boolean): Pair<List<FeaturedItem>, String> {
        val cached = featuredCache.read("featured")
        if (!force && cached != null && featuredCache.isFresh(cached.second, FEATURED_TTL)) {
            return cached.first to "缓存"
        }
        return try {
            val items = HomeParser.parseFeatured(fetch(""))
            if (items.isEmpty()) {
                throw IOException("empty featured")
            }
            runCatching { featuredCache.write("featured", items) }
            items to "在线"
        } catch (_: Exception) {
            if (cached != null && cached.first.isNotEmpty()) {
                cached.first to "离线缓存"
            } else {
                emptyList<FeaturedItem>() to "离线"
            }
        }
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

    private fun buildHeat(
        history: List<LibraryEntry>,
        favorites: List<LibraryEntry>
    ): List<HomeHeat> {
        val favoriteSlugs = favorites.map { it.slug }.toHashSet()
        val items = LinkedHashMap<String, HomeHeat>()
        for (entry in history) {
            items[entry.slug] = HomeHeat(
                slug = entry.slug,
                title = entry.title.ifBlank { entry.slug.uppercase() },
                readCount = entry.readCount,
                lastReadAt = entry.timestamp,
                progress = entry.progress,
                favorite = entry.slug in favoriteSlugs
            )
        }
        for (entry in favorites) {
            if (items.containsKey(entry.slug)) {
                continue
            }
            items[entry.slug] = HomeHeat(
                slug = entry.slug,
                title = entry.title.ifBlank { entry.slug.uppercase() },
                lastReadAt = entry.timestamp,
                favorite = true
            )
        }
        return items.values.toList()
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
            .url(if (path.startsWith("http")) path else ReaderShellBuilder.BASE_URL + path)
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
        private const val EN_TOP_RATED_URL = "https://scp-wiki.wikidot.com/top-rated-pages"
        private const val TOP_RATED_TTL = 12L * 60L * 60L * 1000L
        private const val RECENT_TTL = 30L * 60L * 1000L
        private const val FEATURED_TTL = 6L * 60L * 60L * 1000L
        private const val SUMMARY_TTL = 7L * 24L * 60L * 60L * 1000L
        private val BUNDLED_SERIES = listOf("scp-series", "scp-series-cn")
    }
}