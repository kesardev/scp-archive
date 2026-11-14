package cc.kesardev.scparchive.catalog

/**
 * Extracts entry links from a series page.
 *
 * It scans links inside the series panel instead of relying on the position of
 * <ul> elements. The current CN site has archive lists and a TOC in the same
 * page, so index-based selection is not reliable.
 */
object SeriesParser {

    private val ENTRY_ID = Regex("^scp-(?:cn-)?\\d+$")

    fun parse(html: String, seriesId: String): List<CatalogEntry> {
        val document = org.jsoup.Jsoup.parse(html)
        val panel = document.selectFirst("div.content-panel.series")
            ?: document.selectFirst("#page-content")
            ?: return emptyList()

        val seen = LinkedHashMap<String, CatalogEntry>()
        for (link in panel.select("a[href]")) {
            val id = normalizeId(link.attr("href"))
            if (!ENTRY_ID.matches(id)) {
                continue
            }
            val title = link.text().trim()
            if (title.isEmpty()) {
                continue
            }
            if (!seen.containsKey(id)) {
                seen[id] = CatalogEntry(id, title, seriesId)
            }
        }

        return seen.values.sortedBy { entry ->
            entry.id.substringAfterLast('-').toIntOrNull() ?: Int.MAX_VALUE
        }
    }

    private fun normalizeId(href: String): String {
        return href
            .substringBefore('#')
            .substringBefore('?')
            .trim()
            .trimStart('/')
            .lowercase()
    }
}
