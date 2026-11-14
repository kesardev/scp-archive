package cc.kesardev.scparchive

import cc.kesardev.scparchive.reader.HtmlBlockPreparer
import cc.kesardev.scparchive.reader.ReaderAssets
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveContentTest {

    private fun page(name: String): String {
        val stream = javaClass.getResourceAsStream("/pages/" + name + ".html")
            ?: error("missing fixture: " + name)
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun build(
        raw: String,
        sourceUrl: String = ReaderShellBuilder.BASE_URL
    ): ReaderShellBuilder.Built {
        return ReaderShellBuilder.build(
            rawHtml = raw,
            baseUrl = ReaderShellBuilder.BASE_URL,
            assets = ReaderAssets.forTest(css = "/* reader css */", js = "/* reader js */"),
            sourceUrl = sourceUrl
        )
    }

    @Test
    fun scpCn963JExposesHtmlBlockInsteadOfArticleParser() {
        val built = build(page("scp-cn-963-j"))

        assertEquals(1, built.stats.htmlBlocks)
        assertTrue(built.html.contains("scparchive://interactive?url="))
        assertTrue(built.html.contains("进入互动版"))
        assertTrue(built.html.contains("data-reader-external=\"1\""))
        assertFalse(built.html.contains("<iframe"))
    }

    @Test
    fun chromeIframesAreRemoved() {
        val raw = """
            <html><body><div id="page-content">
              <iframe src="https://scp-wiki-cn.wdfiles.com/local--code/credit%3Abackmodule/1"></iframe>
              <iframe src="//interwiki.scpwikicn.com/styleFrame.html?x"></iframe>
              <p>Hello</p>
            </div></body></html>
        """.trimIndent()

        val built = build(raw)

        assertFalse(built.html.contains("<iframe"))
        assertTrue(built.html.contains("Hello"))
        assertEquals(0, built.stats.htmlBlocks)
        assertEquals(0, built.stats.externalEmbeds)
    }

    @Test
    fun externalEmbedBecomesBrowserAction() {
        val raw = """
            <html><body><div id="page-content">
              <iframe src="https://example.com/game"></iframe>
            </div></body></html>
        """.trimIndent()

        val built = build(raw)

        assertEquals(1, built.stats.externalEmbeds)
        assertTrue(built.html.contains("外部互动内容"))
        assertTrue(built.html.contains("https://example.com/game"))
        assertTrue(built.html.contains("在浏览器打开"))
    }

    @Test
    fun pageLevelControlsGetBrowserNotice() {
        val raw = """
            <html><body><div id="page-content">
              <p><a href="#" onclick="run()">click</a></p>
            </div></body></html>
        """.trimIndent()

        val built = build(raw, "https://scp-wiki-cn.wikidot.com/test")

        assertTrue(built.stats.interactiveControls > 0)
        assertTrue(built.html.contains("页面级互动元素"))
        assertTrue(built.html.contains("在浏览器打开原页"))
        assertTrue(built.html.contains("data-reader-external=\"1\""))
    }

    @Test
    fun htmlBlockPreparerKeepsPageLogicAndDropsResizeHelper() {
        val prepared = HtmlBlockPreparer.prepare(page("scp-cn-963-j-iframe"))

        assertTrue(prepared.contains("name=\"viewport\""))
        assertFalse(prepared.contains("html-block-iframe.js"))
        assertTrue(prepared.contains("id=\"realcontent\""))
        assertTrue(prepared.contains("timecounting1"))
    }
}
