package cc.kesardev.scparchive.reader

import org.jsoup.Jsoup

/**
 * Turns a raw Wikidot HTML-block document into a standalone document for the
 * interactive WebView. The inline page logic is preserved; only the parent
 * iframe resize helper is removed and a mobile viewport is injected.
 */
object HtmlBlockPreparer {

    fun prepare(rawHtml: String): String {
        val doc = Jsoup.parse(rawHtml, ReaderShellBuilder.BASE_URL)
        doc.outputSettings().prettyPrint(false)
        doc.select("script[src*=html-block-iframe.js]").remove()
        doc.select("script[src*=resize-iframe]").remove()
        if (doc.selectFirst("meta[name=viewport]") == null) {
            doc.head().prependElement("meta")
                .attr("name", "viewport")
                .attr("content", "width=device-width, initial-scale=1")
        }
        doc.head().appendElement("style").appendText(
            "html,body{background:#fff;}" +
                "body{margin:12px auto !important;max-width:900px;padding:0 12px;}" +
                "img{max-width:100%;height:auto;}" +
                "table{max-width:100%;}"
        )
        return doc.outerHtml()
    }
}
