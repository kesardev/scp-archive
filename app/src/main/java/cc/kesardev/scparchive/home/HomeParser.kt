package cc.kesardev.scparchive.home

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Locale

object HomeParser {

    private val SCP_PAGE = Regex("^scp-(?:cn-)?\\d+(?:-[a-z0-9]+)*$", RegexOption.IGNORE_CASE)
    private val MAIN_SERIES = Regex("^scp-\\d+(?:-[a-z0-9]+)*$", RegexOption.IGNORE_CASE)
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

    /**
     * Parses the English wiki top rated list, whose entries look like
     * <a href="/scp-173">SCP-173</a> (rating: 11175, comments: 2083).
     * Only main series SCP pages are kept; translated copies live on the CN
     * wiki under the same slug.
     */
    fun parseEnglishTopRated(html: String, limit: Int = 24): List<HomeArticle> {
        if (html.isBlank()) {
            return emptyList()
        }
        val document = Jsoup.parse(html)
        val scope = document.selectFirst("#page-content") ?: document
        val ratingPattern = Regex("rating:\\s*(-?\\d+)")
        val seen = LinkedHashMap<String, HomeArticle>()
        for (link in scope.select("a[href]")) {
            val slug = slugOf(link.attr("href"))
            if (!MAIN_SERIES.matches(slug) || seen.containsKey(slug)) {
                continue
            }
            val rating = ratingPattern.find(link.nextSibling()?.toString().orEmpty())
                ?.groupValues?.get(1)?.toIntOrNull() ?: continue
            seen[slug] = HomeArticle(
                slug = slug,
                title = slug.uppercase(),
                subtitle = "评分 +" + rating,
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


    /**
     * Reads the four featured panels on the CN wiki home page: 精品原创SCP,
     * 精品原创故事, 精品原创GOI格式 and 主题精品. Each panel contributes one
     * title, one hand written tagline and the credited author.
     */
    fun parseFeatured(html: String): List<FeaturedItem> {
        if (html.isBlank()) {
            return emptyList()
        }
        val document = Jsoup.parse(html)
        val scope = document.selectFirst("#page-content") ?: document
        val items = ArrayList<FeaturedItem>()
        val seen = HashSet<String>()
        for (panel in scope.select("div.content-panel")) {
            val group = panel.selectFirst(".panel-heading")?.text()?.trim().orEmpty()
            val body = panel.selectFirst(".panel-body") ?: continue
            val link = body.selectFirst(".feature-title a[href]") ?: continue
            val slug = slugOf(link.attr("href"))
            if (slug.isEmpty() || !seen.add(slug)) {
                continue
            }
            val author = body.selectFirst(".feature-subtitle")?.text()
                ?.replace(Regex("^by\\s*"), "")
                ?.trim()
                .orEmpty()
            val tagline = body.selectFirst("em")?.text()
                ?.trim()
                ?.trim('“', '”', '"', ' ')
                .orEmpty()
            items.add(
                FeaturedItem(
                    slug = slug,
                    title = link.text().trim().ifEmpty { slug.uppercase() },
                    tagline = tagline,
                    author = author,
                    group = group
                )
            )
        }
        return items
    }

    /** Picks the first readable paragraph of an article as its intro line. */
    fun parseSummary(html: String, title: String = ""): String {
        if (html.isBlank()) {
            return ""
        }
        val document = Jsoup.parse(html)
        val content = document.selectFirst("#page-content") ?: return ""
        val preview = content.selectFirst(".myPreview")?.text()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
        if (preview.length >= 4) {
            return preview
        }
        val skipPrefixes = listOf(
            "项目编号", "项目等级", "特殊收容措施", "负责站点", "许可等级",
            "著作信息", "评分", "本页", "作者", "译者"
        )
        for (paragraph in content.select("p")) {
            val text = paragraph.text().replace(Regex("\\s+"), " ").trim()
            if (text.length < 24) {
                continue
            }
            if (title.isNotEmpty() && (text == title || text.startsWith(title))) {
                continue
            }
            if (skipPrefixes.any { text.startsWith(it) }) {
                continue
            }
            return if (text.length > 90) text.substring(0, 90).trimEnd() + "…" else text
        }
        return ""
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
