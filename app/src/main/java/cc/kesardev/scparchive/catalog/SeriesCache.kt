package cc.kesardev.scparchive.catalog

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedSeries(
    val entries: List<CatalogEntry>,
    val fetchedAt: Long
)

class SeriesCache(context: Context) {

    private val dir = File(context.cacheDir, "catalog").apply { mkdirs() }

    fun read(seriesId: String): CachedSeries? {
        val file = File(dir, keyOf(seriesId) + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val array = root.optJSONArray("entries") ?: JSONArray()
            val entries = ArrayList<CatalogEntry>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                entries.add(
                    CatalogEntry(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        seriesId = item.optString("seriesId", seriesId)
                    )
                )
            }
            CachedSeries(entries, root.optLong("fetchedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    fun write(seriesId: String, entries: List<CatalogEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("seriesId", entry.seriesId)
            )
        }
        File(dir, keyOf(seriesId) + ".json").writeText(
            JSONObject()
                .put("fetchedAt", System.currentTimeMillis())
                .put("entries", array)
                .toString(),
            Charsets.UTF_8
        )
    }

    fun isFresh(cached: CachedSeries, maxAgeMs: Long = 24L * 60L * 60L * 1000L): Boolean {
        return cached.fetchedAt > 0 && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }

    private fun keyOf(seriesId: String): String {
        return seriesId.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
    }
}
