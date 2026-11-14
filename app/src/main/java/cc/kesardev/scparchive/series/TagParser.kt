package cc.kesardev.scparchive.series

import cc.kesardev.scparchive.catalog.CatalogEntry
import java.net.URLDecoder

/**
 * Parses the page list from a Wikidot tag page.
 *
 * The tag page also contains a large tag cloud in #page-content. Only links
 * inside div.tag-list are real entries, so selecting all page links would mix
 * hundreds of tag links into the result.
 */
object TagParser {

    fun parse(html: String, tag: String): List<CatalogEntry> {
        val document = org.jsoup.Jsoup.parse(html)
        val links = document.select("div.tag-list div.list-pages-box a[href]")
            .ifEmpty { document.select("div.tag-list a[href]") }

        val seen = LinkedHashMap<String, CatalogEntry>()
        for (link in links) {
            val id = normalizeId(link.attr("href")) ?: continue
            val title = link.text().trim()
            if (title.isEmpty()) {
                continue
            }
            if (!seen.containsKey(id)) {
                seen[id] = CatalogEntry(id, title, "tag:" + tag)
            }
        }
        return seen.values.toList()
    }

    private fun normalizeId(href: String): String? {
        val raw = href
            .substringBefore('#')
            .substringBefore('?')
            .trim()
        if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("javascript:", true)) {
            return null
        }
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
            return null
        }
        val decoded = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
        val id = decoded.trimStart('/').lowercase()
        if (id.isEmpty() || id.startsWith("system:") || id == "tag-guide" || id == "tag-search") {
            return null
        }
        return id
    }
}