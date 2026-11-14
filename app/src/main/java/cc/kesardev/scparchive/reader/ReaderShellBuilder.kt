package cc.kesardev.scparchive.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

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

    private val CHROME_IFRAME_MARKERS = listOf(
        "interwikiframe",
        "styleframe",
        "credit%3abackmodule",
        "credit:backmodule",
        "resize-iframe",
        "wdfiles.com/local--code/credit"
    )

    private val CHROME_CLASSES = setOf(
        "page-rate-widget-box",
        "creditRate",
        "credit",
        "rate-box-with-credit-button"
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
        val javascriptLinksLeft: Int,
        val htmlBlocks: Int = 0,
        val externalEmbeds: Int = 0,
        val interactiveControls: Int = 0
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
        theme: ReaderTheme = ReaderTheme.LIGHT,
        sourceUrl: String = baseUrl
    ): Built {
        val doc = Jsoup.parse(rawHtml, baseUrl)
        val pageContent = doc.selectFirst("#page-content")
            ?: throw IllegalArgumentException("page-content not found")

        val title = doc.selectFirst("#page-title")?.text()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: doc.title().trim().ifEmpty { "SCP" }

        val pageStyles = collectStyles(doc)
        val htmlBlocks = pageContent.select("iframe").count { isHtmlBlock(it) }
        val externalEmbeds = pageContent.select("iframe").count { isExternalEmbed(it) }
        val interactiveControls = countInteractiveControls(pageContent)
        cleanContent(doc)

        if (interactiveControls > 0) {
            pageContent.prependChild(interactiveNotice(sourceUrl))
        }

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
            javascriptLinksLeft = pageContent.select("a").count { it.attr("href").startsWith("javascript:", ignoreCase = true) },
            htmlBlocks = htmlBlocks,
            externalEmbeds = externalEmbeds,
            interactiveControls = interactiveControls
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
            if (isChromeFrame(frame)) {
                frame.remove()
                continue
            }
            val src = frame.absUrl("src").ifBlank { frame.attr("src") }
            if (src.isBlank() || src.startsWith("about:", ignoreCase = true)) {
                frame.remove()
                continue
            }
            if (isHtmlBlock(frame)) {
                frame.replaceWith(
                    interactiveCard(
                        titleText = "互动式内容",
                        description = "此条目包含互动式 HTML 内容。",
                        primaryUrl = "scparchive://interactive?url=" + encode(src),
                        primaryText = "进入互动版",
                        fallbackUrl = src,
                        fallbackText = "在浏览器打开"
                    )
                )
            } else if (isExternalEmbed(frame)) {
                frame.replaceWith(
                    interactiveCard(
                        titleText = "外部互动内容",
                        description = "此条目包含外部互动页面。",
                        primaryUrl = src,
                        primaryText = "在浏览器打开",
                        fallbackUrl = null,
                        fallbackText = null
                    )
                )
            } else {
                frame.remove()
            }
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

    private fun isChromeFrame(frame: Element): Boolean {
        val value = (frame.attr("src") + " " + frame.absUrl("src")).lowercase()
        return CHROME_IFRAME_MARKERS.any { value.contains(it) }
    }

    private fun isHtmlBlock(frame: Element): Boolean {
        if (frame.hasClass("html-block-iframe")) {
            return true
        }
        val src = frame.absUrl("src")
        if (src.isBlank()) {
            return false
        }
        val uri = runCatching { java.net.URI(src) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return (host == "scp-wiki-cn.wikidot.com" || host == "scp-wiki.wikidot.com") &&
            uri.path?.contains("/html/") == true
    }

    private fun isExternalEmbed(frame: Element): Boolean {
        if (isChromeFrame(frame) || isHtmlBlock(frame)) {
            return false
        }
        val src = frame.absUrl("src").ifBlank { frame.attr("src") }
        if (src.isBlank() || src.startsWith("about:", ignoreCase = true)) {
            return false
        }
        val uri = runCatching { java.net.URI(src) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        return host != "scp-wiki-cn.wikidot.com" && host != "scp-wiki.wikidot.com"
    }

    private fun countInteractiveControls(pageContent: Element): Int {
        return pageContent
            .select("[onclick], form, input, button, select, textarea")
            .count { !isChromeElement(it) }
    }

    private fun isChromeElement(element: Element): Boolean {
        if (CHROME_CLASSES.any { element.hasClass(it) }) {
            return true
        }
        return element.parents().any { parent ->
            CHROME_CLASSES.any { parent.hasClass(it) }
        }
    }

    private fun interactiveCard(
        titleText: String,
        description: String,
        primaryUrl: String?,
        primaryText: String?,
        fallbackUrl: String?,
        fallbackText: String?
    ): Element {
        val card = Element("span").addClass("reader-interactive-block")
        card.appendElement("span").addClass("reader-interactive-title").text(titleText)
        card.appendElement("span").addClass("reader-interactive-desc").text(description)
        val actions = card.appendElement("span").addClass("reader-interactive-actions")
        if (primaryUrl != null && primaryText != null) {
            actions.appendElement("a")
                .addClass("reader-interactive-action")
                .attr("href", primaryUrl)
                .text(primaryText)
        }
        if (fallbackUrl != null && fallbackText != null) {
            if (primaryUrl != null) {
                actions.appendText(" · ")
            }
            actions.appendElement("a")
                .addClass("reader-interactive-action")
                .attr("href", fallbackUrl)
                .attr("data-reader-external", "1")
                .text(fallbackText)
        }
        return card
    }

    private fun interactiveNotice(sourceUrl: String): Element {
        val notice = Element("span").addClass("reader-interactive-notice")
        notice.appendText("此条目包含页面级互动元素，阅读器仅显示静态内容。")
        if (sourceUrl.isNotBlank()) {
            notice.appendText(" ")
            notice.appendElement("a")
                .attr("href", sourceUrl)
                .attr("data-reader-external", "1")
                .text("在浏览器打开原页")
        }
        return notice
    }

    private fun encode(value: String): String {
        return runCatching { URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)
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
