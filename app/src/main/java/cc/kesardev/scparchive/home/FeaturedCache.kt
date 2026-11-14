package cc.kesardev.scparchive.home

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Caches the featured panels parsed from the wiki home page. */
class FeaturedCache(context: Context) {

    private val dir = File(context.cacheDir, "home").apply { mkdirs() }

    fun read(name: String): Pair<List<FeaturedItem>, Long>? {
        val file = File(dir, name + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val array = json.optJSONArray("items") ?: JSONArray()
            val items = ArrayList<FeaturedItem>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val slug = item.optString("slug")
                if (slug.isEmpty()) {
                    continue
                }
                items.add(
                    FeaturedItem(
                        slug = slug,
                        title = item.optString("title", slug.uppercase()),
                        tagline = item.optString("tagline", ""),
                        author = item.optString("author", ""),
                        group = item.optString("group", "")
                    )
                )
            }
            items to json.optLong("fetchedAt", 0L)
        } catch (_: Exception) {
            null
        }
    }

    fun write(name: String, items: List<FeaturedItem>) {
        dir.mkdirs()
        val array = JSONArray()
        for (item in items) {
            array.put(
                JSONObject()
                    .put("slug", item.slug)
                    .put("title", item.title)
                    .put("tagline", item.tagline)
                    .put("author", item.author)
                    .put("group", item.group)
            )
        }
        JSONObject()
            .put("fetchedAt", System.currentTimeMillis())
            .put("items", array)
            .toString()
            .let { File(dir, name + ".json").writeText(it, Charsets.UTF_8) }
    }

    fun isFresh(fetchedAt: Long, maxAgeMs: Long): Boolean {
        return fetchedAt > 0L && System.currentTimeMillis() - fetchedAt < maxAgeMs
    }
}
