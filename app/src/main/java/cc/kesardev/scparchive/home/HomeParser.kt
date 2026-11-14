package cc.kesardev.scparchive.home

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Locale

object HomeParser {

    private val SCP_PAGE = Regex("^scp-(?:cn-)?\\d+(?:-[a-z0-9]+)*$", RegexOption.IGNORE_CASE)
    fun parseTopRated(html: String, limit: Int = 10): List<HomeArticle> {
        if (html.isBlank()) {
            return emptyList()
        }
        val document = Jsoup.parse(html)
        val scope = document.selectFirst("div.content-scp")
            ?: document.selectFirst("#page-content")
            ?: document

        val seen = LinkedHashMap<String, HomeArticle>()
        for (row in scope.select("table tr")) {
            val cells = row.children().filter { it.tagName() == "td" }
            if (cells.size < 2) {
                continue
            }
            val link = cells[0].selectFirst("a[href]") ?: continue
            val slug = slugOf(link.attr("href"))
            if (!SCP_PAGE.matches(slug) || seen.containsKey(slug)) {
                continue
            }
            val rating = cells[1].text().replace(",", "").trim().toIntOrNull()
            val title = link.text().trim().ifEmpty { slug.uppercase() }
            seen[slug] = HomeArticle(
                slug = slug,
                title = title,
                subtitle = "评分 +" + (rating ?: 0),
                rating = rating,
                rank = seen.size + 1
            )
            if (seen.size >= limit) {
                break
            }
        }
        return seen.values.toList()
    }

    fun parseRecent(xml: String, limit: Int = 6): List<HomeArticle> {
        if (xml.isBlank()) {
            return emptyList()
        }
        val document = Jsoup.parse(xml, "", Parser.xmlParser())
        val seen = LinkedHashMap<String, HomeArticle>()
        for (item in document.select("item")) {
            val link = item.selectFirst("link")?.text()?.trim().orEmpty()
            val slug = slugOf(link)
            if (!SCP_PAGE.matches(slug) || seen.containsKey(slug)) {
                continue
            }
            val timestamp = parseDate(item.selectFirst("pubDate")?.text().orEmpty())
            seen[slug] = HomeArticle(
                slug = slug,
                title = pageTitle(item).ifEmpty { slug.uppercase() },
                subtitle = slug.uppercase(),
                timestamp = timestamp
            )
            if (seen.size >= limit) {
                break
            }
        }
        return seen.values.toList()
    }

    private fun pageTitle(item: Element): String {
        val description = item.selectFirst("description")?.text().orEmpty()
        if (description.isNotEmpty()) {
            val title = Jsoup.parse(description).selectFirst("a[href]")?.text()?.trim().orEmpty()
            if (title.isNotEmpty()) {
                return title
            }
        }
        val raw = item.selectFirst("title")?.text().orEmpty()
        return raw.substringBefore(" - ").trim().trim('"')
    }

    private fun parseDate(value: String): Long {
        if (value.isEmpty()) {
            return 0L
        }
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                .parse(value)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun slugOf(value: String): String {
        return value
            .substringBefore('#')
            .substringBefore('?')
            .trimEnd('/')
            .substringAfterLast('/')
            .lowercase()
    }
}
