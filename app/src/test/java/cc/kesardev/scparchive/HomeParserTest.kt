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

    @Test
    fun parsesEnglishTopRatedList() {
        val html = """
            <div id="page-content">
              <p>
                <a href="/scp-173">SCP-173</a> (rating: 11175, comments: 2083)<br />
                <a href="/scp-5000">SCP-5000</a> (rating: 4390, comments: 465)<br />
                <a href="/scp-cn-2000">SCP-CN-2000</a> (rating: 5744, comments: 515)<br />
                <a href="/taboo">REDACTED</a> (rating: 3469, comments: 446)<br />
                <a href="/scp-173">SCP-173</a> (rating: 11175, comments: 2083)
              </p>
            </div>
        """.trimIndent()

        val result = HomeParser.parseEnglishTopRated(html)

        assertEquals(listOf("scp-173", "scp-5000"), result.map { it.slug })
        assertEquals(11175, result[0].rating)
        assertEquals("评分 +11175", result[0].subtitle)
    }
    @Test
    fun parsesFeaturedPanelsFromTheHomePage() {
        val html = """
            <div class="feature-block">
              <div class="content-panel left-column">
                <div class="panel-heading"><p>精品原创SCP</p></div>
                <div class="panel-body">
                  <div class="feature-title"><p><a href="/scp-cn-4005">命题#4005：形影博弈</a></p></div>
                  <div class="feature-subtitle"><p>by <span class="printuser">CaroyalKKaia</span></p></div>
                  <p><em>“相信这不是一场单纯的赌注。”</em></p>
                </div>
              </div>
              <div class="content-panel right-column">
                <div class="panel-heading"><p>主题精品：盗墓</p></div>
                <div class="panel-body">
                  <div class="feature-title"><p><a href="/scp-cn-3926">SCP-CN-3926：基金会盗墓故事</a></p></div>
                  <div class="feature-subtitle"><p>by <span class="printuser">Someone</span></p></div>
                  <p><em>“墓里有什么。”</em></p>
                </div>
              </div>
            </div>
        """.trimIndent()

        val items = HomeParser.parseFeatured(html)

        assertEquals(2, items.size)
        assertEquals("scp-cn-4005", items[0].slug)
        assertEquals("精品原创SCP", items[0].group)
        assertEquals("CaroyalKKaia", items[0].author)
        assertEquals("相信这不是一场单纯的赌注。", items[0].tagline)
        assertEquals("主题精品：盗墓", items[1].group)
    }

    @Test
    fun summarySkipsArticleHeaders() {
        val html = """
            <div id="page-content">
              <p>项目编号：SCP-CN-1000</p>
              <p>项目等级：Keter</p>
              <p>这是一段足够长的摘要文本，用来测试解析器是否跳过抬头信息并提取正文首段。</p>
            </div>
        """.trimIndent()

        val summary = HomeParser.parseSummary(html)

        assertTrue(summary.startsWith("这是一段足够长的摘要文本"))
    }
    @Test
    fun summaryPrefersTheAuthorPreview() {
        val html = """
            <div id="page-content">
              <span class="myPreview" style="display: none;">玄不救非，氪不改命啊！</span>
              <p>这是一段足够长的正文段落，不应该被选为导语，因为作者已经写了 preview。</p>
            </div>
        """.trimIndent()

        assertEquals("玄不救非，氪不改命啊！", HomeParser.parseSummary(html))
    }}
