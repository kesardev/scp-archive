package cc.kesardev.scparchive.story

import java.net.URLDecoder

/**
 * Parses story index pages such as tales-by-page-name and
 * tales-cn-by-create-time. Each h3 is an alphabet or year group, followed by
 * one or more list-pages boxes with the actual entries.
 */
enum class StoryIndexMode {
    NAME,
    SERIES
}

object StoryIndexParser {

    private val SCP_PAGE = Regex("^scp-(?:cn-)?\\d+$")

    fun parse(html: String, mode: StoryIndexMode = StoryIndexMode.NAME): List<StoryGroup> {
        return if (mode == StoryIndexMode.SERIES) parseSeries(html) else parseNameIndex(html)
    }

    private fun parseNameIndex(html: String): List<StoryGroup> {
        val document = org.jsoup.Jsoup.parse(html)
        val content = document.selectFirst("#page-content") ?: document
        val groups = ArrayList<StoryGroup>()
        val seen = HashSet<String>()

        for (heading in content.select("h3")) {
            val label = normalizeText(heading.text())
            if (label.isEmpty()) {
                continue
            }
            val entries = LinkedHashMap<String, StoryEntry>()
            var sibling = heading.nextElementSibling()
            while (sibling != null && sibling.tagName() != "h3") {
                for (link in sibling.select("a[href]")) {
                    val slug = normalizeSlug(link.attr("href")) ?: continue
                    if (!seen.add(slug)) {
                        continue
                    }
                    val title = normalizeText(link.text())
                    if (title.isEmpty()) {
                        continue
                    }
                    entries[slug] = StoryEntry(slug, title)
                }
                sibling = sibling.nextElementSibling()
            }
            if (entries.isNotEmpty()) {
                groups.add(StoryGroup(label, entries.values.toList()))
            }
        }
        return groups
    }

    private fun parseSeries(html: String): List<StoryGroup> {
        val document = org.jsoup.Jsoup.parse(html)
        val content = document.selectFirst("#page-content") ?: document
        val paged = collectSeries(content, requireListItem = true)
        return if (paged.isNotEmpty()) paged else collectSeries(content, requireListItem = false)
    }

    private fun collectSeries(
        content: org.jsoup.nodes.Element,
        requireListItem: Boolean
    ): List<StoryGroup> {
        val groups = LinkedHashMap<String, LinkedHashMap<String, StoryEntry>>()
        val seen = HashSet<String>()
        var currentLabel = "条目"

        for (element in content.getAllElements()) {
            when (element.tagName()) {
                "h1", "h2", "h3", "h4" -> {
                    val text = normalizeText(element.text())
                    if (text.isNotEmpty() && text.length <= 80) {
                        currentLabel = text
                    }
                }

                "a" -> {
                    if (requireListItem && element.parents().none { it.tagName() == "li" }) {
                        continue
                    }
                    val slug = normalizeSlug(element.attr("href")) ?: continue
                    if (SCP_PAGE.matches(slug) || slug.endsWith("-hub") ||
                        slug.startsWith("series-archive")
                    ) {
                        continue
                    }
                    if (!seen.add(slug)) {
                        continue
                    }
                    val title = normalizeText(element.text())
                    if (title.isEmpty() || title.contains("系列中心") ||
                        title.contains("中心页")
                    ) {
                        continue
                    }
                    if (!requireListItem &&
                        (title.length > 80 || title.contains("著作信息"))
                    ) {
                        continue
                    }
                    groups.getOrPut(currentLabel) { LinkedHashMap() }[slug] =
                        StoryEntry(slug, title)
                }
            }
        }

        return groups.mapNotNull { (label, entries) ->
            if (entries.isEmpty()) null else StoryGroup(label, entries.values.toList())
        }
    }

    fun displayLabel(label: String): String {
        return if (Regex("^\\d{6}$").matches(label)) {
            label.substring(0, 4) + "-" + label.substring(4)
        } else {
            label
        }
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
            slug.startsWith("forum:") || slug.startsWith("user:") ||
            slug.startsWith("tag:")
        ) {
            return null
        }
        if (slug == "tag-guide" || slug == "tag-search" || slug == "search") {
            return null
        }
        return slug
    }

    private fun normalizeText(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }
}