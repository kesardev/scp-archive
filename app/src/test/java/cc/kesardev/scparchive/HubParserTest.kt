package cc.kesardev.scparchive

import cc.kesardev.scparchive.hub.HubMode
import cc.kesardev.scparchive.hub.HubParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HubParserTest {

    @Test
    fun canonUsesCanonBlocksInsteadOfToc() {
        val html = """
            <div id="page-content">
              <div class="canon-block"><h1 id="toc0"><span><a href="/aces-and-eights">死者手牌</a></span></h1></div>
              <div class="canon-block"><h1 id="toc1"><span><a href="/ad-astra-per-aspera-hub">循此苦旅，直抵群星</a></span></h1></div>
            </div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.CANON)

        assertEquals(2, entries.size)
        assertEquals("aces-and-eights", entries[0].slug)
        assertEquals("循此苦旅，直抵群星", entries[1].title)
    }

    @Test
    fun canonFallsBackToContentHeadings() {
        val html = """
            <div id="page-content">
              <h1 id="toc0"><span><a href="/alive">生</a></span></h1>
              <h1 id="toc1"><span><a href="/nirvana-hub">Nirvana</a></span></h1>
            </div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.CANON)

        assertEquals(2, entries.size)
        assertEquals("alive", entries[0].slug)
    }

    @Test
    fun storiesReadListItemsButIgnoreAlphabetAnchors() {
        val html = """
            <div id="page-content">
              <div class="content-panel standalone series">
                <ul><li><a href="/tales-by-page-name">按页面名称排列</a></li></ul>
                <ul><li><a href="/canon-hub">设定中心</a></li></ul>
                <div><a href="/tales#A">A</a><a href="/tales#B">B</a></div>
              </div>
            </div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.STORIES)

        assertEquals(2, entries.size)
        assertEquals("tales-by-page-name", entries[0].slug)
        assertEquals("canon-hub", entries[1].slug)
    }

    @Test
    fun storyIndexesBecomeGroupedChildren() {
        val html = """
            <div id="page-content"><div class="content-panel standalone series">
              <div><h1>导引一览</h1></div>
              <ul><li><strong><a href="/tales-cn-by-page-name">按故事的页面名称排列</a></strong></li></ul>
              <div><a href="/tales-cn-by-page-name#A">A</a>
                   <a href="/tales-cn-by-page-name#B">B</a>
                   <a href="/tales-cn-by-page-name#num">0-9</a></div>
              <ul><li><strong><a href="/tales-cn-by-create-time">按故事的发布时间排列</a></strong></li></ul>
              <div><a href="/tales-cn-by-create-time#202601">2026</a>
                   <a href="/tales-cn-by-create-time#202501">2025</a></div>
              <ul><li><strong><a href="/canon-hub-cn">中文分部设定中心</a></strong></li></ul>
            </div></div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.STORIES)

        assertEquals(3, entries.size)
        assertEquals("按故事的页面名称排列", entries[0].title)
        assertEquals(3, entries[0].children.size)
        assertEquals("A", entries[0].children[0].label)
        assertEquals("A", entries[0].children[0].anchor)
        assertEquals("按故事的发布时间排列", entries[1].title)
        assertEquals(2, entries[1].children.size)
        assertEquals("202601", entries[1].children[0].anchor)
        assertEquals(0, entries[2].children.size)
    }
    @Test
    fun goiUsesOrganizationHeadingsOnly() {
        val html = """
            <div id="page-content"><div class="content-panel">
              <h2 id="toc2"><a href="/alexylva-university-hub">Alexylva大学</a></h2>
              <ul><li><a href="/alexylva-document">Alexylva文件</a></li></ul>
              <h2 id="toc4"><a href="/ambrose-restaurant-hub">安布罗斯餐厅</a></h2>
            </div></div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.GOI)

        assertEquals(2, entries.size)
        assertEquals("alexylva-university-hub", entries[0].slug)
        assertEquals("安布罗斯餐厅", entries[1].title)
    }

    @Test
    fun archiveReadsSeriesTable() {
        val html = """
            <div id="page-content"><div class="content-panel content-row">
              <div class="content-type-description">
                <table><tr><td><a href="/1111event">1111事件中心页</a></td></tr></table>
              </div>
            </div></div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.ARCHIVE)

        assertEquals(1, entries.size)
        assertEquals("1111event", entries[0].slug)
    }

    @Test
    fun libraryKeepsOnlyWanderersLinks() {
        val html = """
            <div id="page-content">
              <a href="/wanderers:start">被放逐者之图书馆</a>
              <a href="/wanderers:the-index">记载索引</a>
              <a href="/scp-173">SCP-173</a>
              <a href="#top">回到顶部</a>
            </div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.LIBRARY)

        assertEquals(2, entries.size)
        assertTrue(entries.all { it.slug.startsWith("wanderers:") })
    }

    @Test
    fun genericSkipsSystemAndDuplicateLinks() {
        val html = """
            <div id="page-content">
              <a href="/scp-1000-ru">SCP-1000-RU</a>
              <a href="/scp-1000-ru#x">SCP-1000-RU</a>
              <a href="/system:page-tags/tag/scp">SCP</a>
              <a href="javascript:;">菜单</a>
            </div>
        """.trimIndent()

        val entries = HubParser.parse(html, HubMode.GENERIC)

        assertEquals(1, entries.size)
        assertEquals("scp-1000-ru", entries[0].slug)
    }
}