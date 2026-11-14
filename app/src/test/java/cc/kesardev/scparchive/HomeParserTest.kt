package cc.kesardev.scparchive

import cc.kesardev.scparchive.home.HomeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeParserTest {

    @Test
    fun parsesTopRatedScpRows() {
        val html = """
            <html><body><div id="page-content"><div class="content-scp">
            <table>
              <tr><td>页面</td><td>评分</td><td>评论</td></tr>
              <tr><td><div><a href="/scp-cn-2000">SCP-CN-2000</a></div></td><td>5,743</td><td>515</td></tr>
              <tr><td><a href="/scp-cn-963-j">SCP-CN-963-J</a></td><td>4572</td><td>297</td></tr>
              <tr><td><a href="/scp-cn-2000">duplicate</a></td><td>1</td><td>1</td></tr>
              <tr><td><a href="/tale:example">A tale</a></td><td>99</td><td>1</td></tr>
            </table>
            </div></div></body></html>
        """.trimIndent()

        val items = HomeParser.parseTopRated(html)

        assertEquals(2, items.size)
        assertEquals("scp-cn-2000", items[0].slug)
        assertEquals("SCP-CN-2000", items[0].title)
        assertEquals(5743, items[0].rating)
        assertEquals(1, items[0].rank)
        assertEquals("scp-cn-963-j", items[1].slug)
        assertEquals(2, items[1].rank)
    }

    @Test
    fun parsesRecentFeedAndSkipsNonScpPages() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <item>
                <link>https://scp-wiki-cn.wikidot.com/scp-7866</link>
                <title>"某条目" - 源代码变更</title>
                <description>页面: &lt;a href="https://scp-wiki-cn.wikidot.com/scp-7866"&gt;SCP-7866&lt;/a&gt; (scp-7866)</description>
                <pubDate>Sat, 14 Nov 2026 08:27:11 +0000</pubDate>
              </item>
              <item>
                <link>https://scp-wiki-cn.wikidot.com/scp-series-8</link>
                <title>系列 VIII</title>
                <description>页面: 系列 VIII (scp-series-8)</description>
                <pubDate>Sat, 14 Nov 2026 08:25:44 +0000</pubDate>
              </item>
              <item>
                <link>https://scp-wiki-cn.wikidot.com/scp-cn-4534</link>
                <title>CN-4534</title>
                <description>页面: &lt;a href="x"&gt;SCP-CN-4534&lt;/a&gt; (scp-cn-4534)</description>
                <pubDate>Sat, 14 Nov 2026 08:20:00 +0000</pubDate>
              </item>
              <item>
                <link>https://scp-wiki-cn.wikidot.com/scp-7866</link>
                <title>SCP-7866 again</title>
                <description>页面: &lt;a href="x"&gt;SCP-7866&lt;/a&gt; (scp-7866)</description>
                <pubDate>Sat, 14 Nov 2026 08:10:00 +0000</pubDate>
              </item>
            </channel></rss>
        """.trimIndent()

        val items = HomeParser.parseRecent(xml)

        assertEquals(2, items.size)
        assertEquals("scp-7866", items[0].slug)
        assertEquals("SCP-7866", items[0].title)
        assertTrue(items[0].timestamp > 0L)
        assertEquals("scp-cn-4534", items[1].slug)
    }

    @Test
    fun blankInputReturnsEmptyLists() {
        assertTrue(HomeParser.parseTopRated("").isEmpty())
        assertTrue(HomeParser.parseRecent("").isEmpty())
    }
}
