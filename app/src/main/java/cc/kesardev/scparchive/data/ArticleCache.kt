package cc.kesardev.scparchive.data

import android.content.Context
import org.json.JSONObject
import java.io.File

data class CachedArticle(
    val html: String,
    val etag: String,
    val lastModified: String,
    val fetchedAt: Long
)

/**
 * Raw HTML cache. Parsed documents are rebuilt from this cache, so changing
 * reader CSS/JS does not require invalidating stored articles.
 */
class ArticleCache(context: Context) {

    private val dir = File(context.cacheDir, "articles").apply { mkdirs() }

    fun read(slug: String): CachedArticle? {
        val key = keyOf(slug)
        val htmlFile = File(dir, key + ".html")
        val metaFile = File(dir, key + ".json")
        if (!htmlFile.exists() || !metaFile.exists()) {
            return null
        }
        return try {
            val meta = JSONObject(metaFile.readText(Charsets.UTF_8))
            CachedArticle(
                html = htmlFile.readText(Charsets.UTF_8),
                etag = meta.optString("etag", ""),
                lastModified = meta.optString("lastModified", ""),
                fetchedAt = meta.optLong("fetchedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun write(slug: String, html: String, etag: String, lastModified: String) {
        val key = keyOf(slug)
        File(dir, key + ".html").writeText(html, Charsets.UTF_8)
        File(dir, key + ".json").writeText(
            JSONObject()
                .put("etag", etag)
                .put("lastModified", lastModified)
                .put("fetchedAt", System.currentTimeMillis())
                .toString(),
            Charsets.UTF_8
        )
    }

    fun isFresh(cached: CachedArticle, maxAgeMs: Long = 6L * 60L * 60L * 1000L): Boolean {
        return cached.fetchedAt > 0 && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }

    private fun keyOf(slug: String): String {
        return slug.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
    }
}
