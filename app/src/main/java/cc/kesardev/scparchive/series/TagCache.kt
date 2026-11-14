package cc.kesardev.scparchive.series

import android.content.Context
import cc.kesardev.scparchive.catalog.CatalogEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedTag(
    val entries: List<CatalogEntry>,
    val fetchedAt: Long
)

class TagCache(context: Context) {

    private val dir = File(context.cacheDir, "tags").apply { mkdirs() }

    fun read(tag: String): CachedTag? {
        val file = File(dir, keyOf(tag) + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val array = root.optJSONArray("entries") ?: JSONArray()
            val entries = ArrayList<CatalogEntry>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isEmpty()) {
                    continue
                }
                entries.add(
                    CatalogEntry(
                        id = id,
                        title = item.optString("title", id.uppercase()),
                        seriesId = "tag:" + tag
                    )
                )
            }
            CachedTag(entries, root.optLong("fetchedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    fun write(tag: String, entries: List<CatalogEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
            )
        }
        File(dir, keyOf(tag) + ".json").writeText(
            JSONObject()
                .put("fetchedAt", System.currentTimeMillis())
                .put("entries", array)
                .toString(),
            Charsets.UTF_8
        )
    }

    fun isFresh(cached: CachedTag, maxAgeMs: Long = DEFAULT_TTL): Boolean {
        return cached.fetchedAt > 0L && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }

    private fun keyOf(tag: String): String {
        val bytes = tag.lowercase().toByteArray(Charsets.UTF_8)
        return "tag-" + bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    companion object {
        private const val DEFAULT_TTL = 24L * 60L * 60L * 1000L
    }
}