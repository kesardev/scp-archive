package cc.kesardev.scparchive.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Builds the exact document the WebView loads.
 *
 * Fidelity rule: keep #page-content's original HTML and the page's own <style>
 * blocks. We only remove scripts/chrome, absolutize images, and add a small
 * reader interaction layer.
 */
object ReaderShellBuilder {

    const val BASE_URL = "https://scp-wiki-cn.wikidot.com/"

    private val NOISE_SELECTORS = listOf(
        ".footer-wikiwalk-nav",
        "#page-tags",
        ".info-container",
        ".page-options",
        ".page-options-container",
        "script",
        "noscript",
        "link[rel=stylesheet]"
    )

    data class Stats(
        val styles: Int,
        val images: Int,
        val tables: Int,
        val blockquotes: Int,
        val footnotes: Int,
        val headings: Int,
        val tabsets: Int,
        val collapsibles: Int,
        val onclickLeft: Int,
        val javascriptLinksLeft: Int
    )

    data class Built(
        val html: String,
        val title: String,
        val stats: Stats
    )

    fun build(
        rawHtml: String,
        baseUrl: String,
        assets: ReaderAssets,
        theme: ReaderTheme = ReaderTheme.LIGHT
    ): Built {
        val doc = Jsoup.parse(rawHtml, baseUrl)
        val pageContent = doc.selectFirst("#page-content")
            ?: throw IllegalArgumentException("page-content not found")

        val title = doc.selectFirst("#page-title")?.text()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: doc.title().trim().ifEmpty { "SCP" }

        val pageStyles = collectStyles(doc)
        cleanContent(doc)

        val stats = Stats(
            styles = pageStyles.size,
            images = pageContent.select("img").size,
            tables = pageContent.select("table").size,
            blockquotes = pageContent.select("blockquote").size,
            footnotes = pageContent.select(".footnote-footer").size,
            headings = pageContent.select("h1, h2, h3").size,
            tabsets = pageContent.select(".yui-navset").size,
            collapsibles = pageContent.select(".collapsible-block").size,
            onclickLeft = pageContent.select("[onclick]").size,
            javascriptLinksLeft = pageContent.select("a").count { it.attr("href").startsWith("javascript:", ignoreCase = true) }
        )

        val html = shell(
            baseUrl = baseUrl,
            title = title,
            pageStylesHtml = pageStyles.joinToString("\n") { "<style>\n" + it + "\n</style>" },
            readerCss = assets.css,
            readerJs = assets.js,
            contentHtml = pageContent.html(),
            theme = theme
        )
        return Built(html, title, stats)
    }

    private fun collectStyles(doc: Document): List<String> {
        val seen = LinkedHashSet<String>()
        for (style in doc.select("head style, #page-content style")) {
            val css = style.data().trim()
            if (css.isNotEmpty()) {
                seen.add(css)
            }
        }
        return seen.toList()
    }

    private fun cleanContent(doc: Document) {
        for (selector in NOISE_SELECTORS) {
            doc.select(selector).remove()
        }

        for (frame in doc.select("#page-content iframe").toList()) {
            val src = frame.attr("src")
            if (src.contains("interwikiFrame", ignoreCase = true) ||
                src.contains("styleFrame", ignoreCase = true)
            ) {
                frame.remove()
                continue
            }
            val replacement = Element("p").addClass("reader-embed")
            replacement.appendText("嵌入内容：")
            replacement.appendElement("a")
                .attr("href", src.ifBlank { "about:blank" })
                .text("打开")
            frame.replaceWith(replacement)
        }

        for (box in doc.select("#page-content .list-pages-box").toList()) {
            if (box.text().isBlank() && box.selectFirst("iframe") != null) {
                box.remove()
            }
        }

        for (element in doc.select("#page-content *")) {
            for (attribute in element.attributes().asList()) {
                if (attribute.key.startsWith("on", ignoreCase = true)) {
                    element.removeAttr(attribute.key)
                }
            }
            if (element.tagName() == "a") {
                val href = element.attr("href")
                if (href.startsWith("javascript:", ignoreCase = true)) {
                    element.attr("href", "#")
                    element.attr("data-reader-noop", "1")
                }
                element.removeAttr("target")
            }
            if (element.tagName() == "img") {
                val absolute = element.absUrl("src").ifBlank { element.attr("src") }
                if (absolute.isNotEmpty()) {
                    element.attr("src", upgradeToHttps(absolute))
                }
                element.removeAttr("srcset")
                element.removeAttr("sizes")
                element.attr("loading", "lazy")
                element.attr("decoding", "async")
            }
        }

        doc.select("style").remove()
        doc.select("base").remove()
    }

    private fun upgradeToHttps(url: String): String {
        return if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
    }

    private fun shell(
        baseUrl: String,
        title: String,
        pageStylesHtml: String,
        readerCss: String,
        readerJs: String,
        contentHtml: String,
        theme: ReaderTheme
    ): String {
        val template = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<meta name="referrer" content="no-referrer">
<base href="__BASE__">
__PAGE_STYLES__
<style>
__READER_CSS__
</style>
</head>
<body id="html-body" class="__BODY_CLASS__" data-reader-theme="__THEME__">
<div id="skrollr-body">
  <div class="container-wrap-wrap">
    <div class="container-wrap">
      <div class="container">
        <div id="content-wrap">
          <div id="main-content">
            <div id="page-title">__TITLE__</div>
            <div id="page-content">__CONTENT__</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<script>
window.Reader = window.Reader || { postMessage: function (message) { try { console.log(message); } catch (e) {} } };
</script>
<script>
__READER_JS__
</script>
</body>
</html>
"""
        val bodyClass = if (theme.bodyClass == null) "reader-body" else "reader-body " + theme.bodyClass
        return template
            .replace("__BASE__", baseUrl)
            .replace("__BODY_CLASS__", bodyClass)
            .replace("__THEME__", theme.attrValue)
            .replace("__PAGE_STYLES__", pageStylesHtml)
            .replace("__READER_CSS__", readerCss)
            .replace("__TITLE__", escapeHtml(title))
            .replace("__CONTENT__", contentHtml)
            .replace("__READER_JS__", readerJs)
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
