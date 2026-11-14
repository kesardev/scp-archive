package cc.kesardev.scparchive.story

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedStoryIndex(
    val groups: List<StoryGroup>,
    val fetchedAt: Long
)

class StoryIndexCache(context: Context) {

    private val dir = File(context.cacheDir, "story-index").apply { mkdirs() }

    fun read(path: String, mode: StoryIndexMode): CachedStoryIndex? {
        val file = File(dir, keyOf(path, mode) + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val groups = ArrayList<StoryGroup>()
            val array = root.optJSONArray("groups") ?: JSONArray()
            for (i in 0 until array.length()) {
                val group = array.optJSONObject(i) ?: continue
                val label = group.optString("label")
                if (label.isEmpty()) {
                    continue
                }
                val entries = ArrayList<StoryEntry>()
                val items = group.optJSONArray("entries") ?: JSONArray()
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val slug = item.optString("slug")
                    if (slug.isEmpty()) {
                        continue
                    }
                    entries.add(StoryEntry(slug, item.optString("title", slug)))
                }
                if (entries.isNotEmpty()) {
                    groups.add(StoryGroup(label, entries))
                }
            }
            CachedStoryIndex(groups, root.optLong("fetchedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    fun write(path: String, mode: StoryIndexMode, groups: List<StoryGroup>) {
        val array = JSONArray()
        for (group in groups) {
            val items = JSONArray()
            for (entry in group.entries) {
                items.put(
                    JSONObject()
                        .put("slug", entry.slug)
                        .put("title", entry.title)
                )
            }
            array.put(
                JSONObject()
                    .put("label", group.label)
                    .put("entries", items)
            )
        }
        File(dir, keyOf(path, mode) + ".json").writeText(
            JSONObject()
                .put("fetchedAt", System.currentTimeMillis())
                .put("groups", array)
                .toString(),
            Charsets.UTF_8
        )
    }

    fun isFresh(cached: CachedStoryIndex, maxAgeMs: Long = DEFAULT_TTL): Boolean {
        return cached.fetchedAt > 0L && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }

    private fun keyOf(path: String, mode: StoryIndexMode): String {
        val bytes = (mode.name + ":" + path).toByteArray(Charsets.UTF_8)
        return "story-" + bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    companion object {
        private const val DEFAULT_TTL = 24L * 60L * 60L * 1000L
    }
}