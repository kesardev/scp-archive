package cc.kesardev.scparchive.hub

import org.jsoup.nodes.Element
import java.net.URLDecoder

data class HubChild(
    val label: String,
    val slug: String,
    val anchor: String
)

data class HubEntry(
    val slug: String,
    val title: String,
    val children: List<HubChild> = emptyList()
)

enum class HubMode {
    CANON,
    STORIES,
    ARCHIVE,
    GOI,
    LIBRARY,
    GENERIC;

    companion object {
        fun from(value: String?): HubMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERIC
        }
    }
}

/**
 * Extracts the entry list from a Wikidot hub page.
 *
 * Story hubs mix real entries with "按…排列" navigation groups. Those groups
 * are kept as HubEntry.children so the UI can show them as sub-items instead of
 * treating the alphabet/year index as an article.
 */
object HubParser {

    fun parse(html: String, mode: HubMode): List<HubEntry> {
        val document = org.jsoup.Jsoup.parse(html)
        val content = document.selectFirst("#page-content") ?: document

        if (mode == HubMode.STORIES) {
            return parseStories(content)
        }

        val links = when (mode) {
            HubMode.CANON -> {
                val canon = content.select(".canon-block h1 a[href]")
                if (canon.isNotEmpty()) canon else content.select("h1 a[href]")
            }

            HubMode.ARCHIVE -> {
                val rows = content.select(".content-type-description a[href]")
                if (rows.isNotEmpty()) rows else content.select(".content-panel a[href]")
            }

            HubMode.GOI -> {
                val organizations = content.select(".content-panel h2 a[href]")
                if (organizations.isNotEmpty()) organizations else content.select("h2 a[href]")
            }

            HubMode.LIBRARY, HubMode.GENERIC -> content.select("a[href]")
            HubMode.STORIES -> emptyList()
        }

        val seen = LinkedHashMap<String, HubEntry>()
        for (link in links) {
            val slug = normalizeSlug(link.attr("href")) ?: continue
            if (mode == HubMode.LIBRARY && !slug.startsWith("wanderers:")) {
                continue
            }
            val title = normalizeText(link.text())
            if (title.isEmpty()) {
                continue
            }
            if (!seen.containsKey(slug)) {
                seen[slug] = HubEntry(slug, title)
            }
        }
        return seen.values.toList()
    }

    private fun parseStories(content: Element): List<HubEntry> {
        val panel = content.selectFirst(".content-panel.series") ?: return emptyList()
        val items = ArrayList<HubEntry>()
        for (child in panel.children()) {
            when (child.tagName()) {
                "ul" -> {
                    for (li in child.select("li")) {
                        val link = li.selectFirst("strong a[href]")
                            ?: li.selectFirst("a[href]")
                            ?: continue
                        val slug = normalizeSlug(link.attr("href")) ?: continue
                        val title = normalizeText(link.text())
                        if (title.isNotEmpty()) {
                            items.add(HubEntry(slug, title))
                        }
                    }
                }

                "div" -> {
                    val last = items.lastOrNull() ?: continue
                    val children = child.select("a[href]").mapNotNull { link ->
                        val slug = normalizeSlug(link.attr("href")) ?: return@mapNotNull null
                        if (slug != last.slug) {
                            return@mapNotNull null
                        }
                        val anchor = link.attr("href").substringAfter('#', "")
                        if (anchor.isEmpty()) {
                            return@mapNotNull null
                        }
                        val label = normalizeText(link.text())
                        if (label.isEmpty()) null else HubChild(label, slug, anchor)
                    }
                    if (children.isNotEmpty()) {
                        items[items.lastIndex] = last.copy(children = children)
                    }
                }
            }
        }
        return items
    }

    private fun normalizeSlug(href: String): String? {
        val raw = href.substringBefore('#').substringBefore('?').trim()
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
        val slug = decoded.trimStart('/').lowercase()
        if (slug.isEmpty()) {
            return null
        }
        if (slug.startsWith("system:") || slug.startsWith("nav:") ||
            slug.startsWith("forum:") || slug.startsWith("user:")
        ) {
            return null
        }
        return slug
    }

    private fun normalizeText(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }
}