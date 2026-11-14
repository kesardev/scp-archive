package cc.kesardev.scparchive

import cc.kesardev.scparchive.reader.ReaderAssets
import cc.kesardev.scparchive.reader.ReaderTheme
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderShellBuilderTest {

    private fun build(name: String): ReaderShellBuilder.Built {
        val stream = javaClass.getResourceAsStream("/pages/" + name + ".html")
            ?: error("missing fixture: " + name)
        val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return ReaderShellBuilder.build(
            rawHtml = raw,
            baseUrl = ReaderShellBuilder.BASE_URL,
            assets = ReaderAssets.forTest(css = "/* reader css */", js = "/* reader js */")
        )
    }

    @Test
    fun scp173KeepsImageAndStripsScripts() {
        val built = build("scp-173")
        val doc = Jsoup.parse(built.html)

        assertEquals("SCP-173", built.title)
        assertEquals(1, doc.select("#page-content .scp-image-block").size)
        assertEquals(1, doc.select("#page-content img").size)
        assertTrue(
            "image should be upgraded to https",
            doc.selectFirst("#page-content img")!!.attr("src").startsWith("https://")
        )
        assertEquals(0, doc.select("#page-content [onclick]").size)
        assertEquals(0, doc.select("#page-content a").count { it.attr("href").startsWith("javascript:") })
        assertEquals(0, built.stats.footnotes)
        assertTrue("page styles should be kept", built.stats.styles >= 3)
        assertTrue(built.html.contains("id=\"page-content\""))
        assertTrue(built.html.contains("id=\"main-content\""))
    }

    @Test
    fun scp5000KeepsQuotesTablesAndFootnotes() {
        val built = build("scp-5000")
        val doc = Jsoup.parse(built.html)

        assertEquals("SCP-5000", built.title)
        assertEquals(25, doc.select("#page-content blockquote").size)
        assertEquals(6, doc.select("#page-content table").size)
        assertEquals(5, doc.select("#page-content .footnote-footer").size)
        assertEquals(1, doc.select("#page-content h1").size)
        assertTrue(doc.select("#page-content img").size >= 2)
        assertEquals(0, doc.select("#page-content [onclick]").size)
        assertEquals(0, doc.select("#page-content a").count { it.attr("href").startsWith("javascript:") })
        assertEquals(0, built.stats.onclickLeft)
        assertEquals(0, built.stats.javascriptLinksLeft)
        assertTrue("page styles should be kept", built.stats.styles >= 4)
    }

    @Test
    fun scpCn1000KeepsTabsRatingAndPermissionText() {
        val built = build("scp-cn-1000")
        val doc = Jsoup.parse(built.html)

        assertEquals("SCP-CN-1000", built.title)
        assertEquals(1, built.stats.tabsets)
        assertEquals(3, doc.select("#page-content .yui-navset ul.yui-nav li").size)
        assertEquals(3, doc.select("#page-content .yui-content > div").size)
        assertEquals(1, doc.select("#page-content .creditRate").size)
        assertTrue(doc.selectFirst("#page-content .creditRate")!!.text().contains("评分"))
        assertTrue(doc.text().contains("以下内容需要5/CN-1000级权限"))
        assertTrue(doc.text().contains("好吧，你看到这了"))
        assertEquals(0, built.stats.onclickLeft)
        assertEquals(0, built.stats.javascriptLinksLeft)
        assertTrue(built.html.contains("data-reader-theme=\"light\""))
    }

    @Test
    fun darkThemeSetsBodyClassAndAttribute() {
        val stream = javaClass.getResourceAsStream("/pages/scp-173.html")
            ?: error("missing fixture: scp-173")
        val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val built = ReaderShellBuilder.build(
            rawHtml = raw,
            baseUrl = ReaderShellBuilder.BASE_URL,
            assets = ReaderAssets.forTest(),
            theme = ReaderTheme.DARK
        )
        assertTrue(built.html.contains("class=\"reader-body reader-dark\""))
        assertTrue(built.html.contains("data-reader-theme=\"dark\""))
    }

    @Test
    fun scp3029KeepsCollapsibleContent() {
        val built = build("scp-3029")
        val doc = Jsoup.parse(built.html)

        assertEquals("SCP-3029", built.title)
        assertEquals(1, built.stats.collapsibles)
        assertEquals(1, doc.select("#page-content .collapsible-block").size)
        assertEquals(1, doc.select("#page-content .collapsible-block-content").size)
        assertTrue(doc.selectFirst("#page-content .collapsible-block-content")!!.text().contains("项目编号"))
        assertEquals(3, built.stats.footnotes)
        assertEquals(1, built.stats.tables)
        assertTrue(doc.selectFirst("#page-content .collapsible-block-unfolded")!!.attr("style").contains("display:none"))
    }
}
