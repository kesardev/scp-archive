package cc.kesardev.scparchive.home

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedFeed(
    val items: List<HomeArticle>,
    val fetchedAt: Long
)

class HomeCache(context: Context) {

    private val dir = File(context.cacheDir, "home").apply { mkdirs() }

    fun read(name: String): CachedFeed? {
        val file = File(dir, name + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val array = json.optJSONArray("items") ?: JSONArray()
            val items = ArrayList<HomeArticle>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val slug = item.optString("slug")
                if (slug.isEmpty()) {
                    continue
                }
                items.add(
                    HomeArticle(
                        slug = slug,
                        title = item.optString("title", slug.uppercase()),
                        subtitle = item.optString("subtitle", ""),
                        rating = if (item.has("rating")) item.optInt("rating") else null,
                        rank = item.optInt("rank", 0),
                        timestamp = item.optLong("timestamp", 0L)
                    )
                )
            }
            CachedFeed(items, json.optLong("fetchedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    fun write(name: String, items: List<HomeArticle>) {
        dir.mkdirs()
        val array = JSONArray()
        for (item in items) {
            array.put(
                JSONObject()
                    .put("slug", item.slug)
                    .put("title", item.title)
                    .put("subtitle", item.subtitle)
                    .put("rating", item.rating ?: 0)
                    .put("rank", item.rank)
                    .put("timestamp", item.timestamp)
            )
        }
        JSONObject()
            .put("fetchedAt", System.currentTimeMillis())
            .put("items", array)
            .toString()
            .let { File(dir, name + ".json").writeText(it, Charsets.UTF_8) }
    }

    fun isFresh(cached: CachedFeed, maxAgeMs: Long): Boolean {
        return cached.fetchedAt > 0L && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }
}
