package cc.kesardev.scparchive.hub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedHub(
    val entries: List<HubEntry>,
    val fetchedAt: Long
)

class HubCache(context: Context) {

    private val dir = File(context.cacheDir, "hubs").apply { mkdirs() }

    fun read(path: String, mode: HubMode): CachedHub? {
        val file = File(dir, keyOf(path, mode) + ".json")
        if (!file.exists()) {
            return null
        }
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val array = root.optJSONArray("entries") ?: JSONArray()
            val entries = ArrayList<HubEntry>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val slug = item.optString("slug")
                if (slug.isEmpty()) {
                    continue
                }
                entries.add(
                    HubEntry(
                        slug = slug,
                        title = item.optString("title", slug),
                        children = readChildren(item.optJSONArray("children"))
                    )
                )
            }
            CachedHub(entries, root.optLong("fetchedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    fun write(path: String, mode: HubMode, entries: List<HubEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val children = JSONArray()
            for (child in entry.children) {
                children.put(
                    JSONObject()
                        .put("label", child.label)
                        .put("slug", child.slug)
                        .put("anchor", child.anchor)
                )
            }
            array.put(
                JSONObject()
                    .put("slug", entry.slug)
                    .put("title", entry.title)
                    .put("children", children)
            )
        }
        File(dir, keyOf(path, mode) + ".json").writeText(
            JSONObject()
                .put("fetchedAt", System.currentTimeMillis())
                .put("entries", array)
                .toString(),
            Charsets.UTF_8
        )
    }

    fun isFresh(cached: CachedHub, maxAgeMs: Long = DEFAULT_TTL): Boolean {
        return cached.fetchedAt > 0L && System.currentTimeMillis() - cached.fetchedAt < maxAgeMs
    }

    private fun readChildren(array: JSONArray?): List<HubChild> {
        if (array == null) {
            return emptyList()
        }
        val children = ArrayList<HubChild>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val slug = item.optString("slug")
            val anchor = item.optString("anchor")
            if (slug.isEmpty() || anchor.isEmpty()) {
                continue
            }
            children.add(HubChild(item.optString("label", anchor), slug, anchor))
        }
        return children
    }

    private fun keyOf(path: String, mode: HubMode): String {
        val bytes = (mode.name + ":" + path).toByteArray(Charsets.UTF_8)
        return "hub-" + bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    companion object {
        private const val DEFAULT_TTL = 24L * 60L * 60L * 1000L
    }
}