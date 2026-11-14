package cc.kesardev.scparchive

import cc.kesardev.scparchive.series.TagParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagParserTest {

    @Test
    fun parsesOnlyEntryListAndIgnoresTagCloud() {
        val html = """
            <html><body><div id="page-content">
              <p>欢迎来到标签页面！</p>
              <div class="tag-list">
                <div class="list-pages-box">
                  <ol>
                    <li><a href="/scp-cn-1000">SCP-CN-1000</a></li>
                    <li><a href="/scp-5000?from=tag#top">SCP-5000</a></li>
                    <li><a href="/old:scp-5500">SCP-5500（旧）</a></li>
                    <li><a href="/scp-cn-1000">重复链接</a></li>
                  </ol>
                </div>
              </div>
              <div class="tag-cloud">
                <a href="/system:page-tags/tag/scp">scp</a>
                <a href="/system:page-tags/tag/%E6%95%85%E4%BA%8B">故事</a>
                <a href="/tag-guide">标签指导</a>
              </div>
            </div></body></html>
        """.trimIndent()

        val entries = TagParser.parse(html, "故事")

        assertEquals(3, entries.size)
        assertEquals("scp-cn-1000", entries[0].id)
        assertEquals("SCP-CN-1000", entries[0].title)
        assertEquals("tag:故事", entries[0].seriesId)
        assertEquals("scp-5000", entries[1].id)
        assertEquals("old:scp-5500", entries[2].id)
        assertTrue(entries.none { it.id.startsWith("system:") })
    }

    @Test
    fun decodesPercentEncodedPaths() {
        val html = """
            <div class="tag-list"><div class="list-pages-box"><ol>
              <li><a href="/%73cp-cn-2000">SCP-CN-2000</a></li>
            </ol></div></div>
        """.trimIndent()

        val entries = TagParser.parse(html, "设定")

        assertEquals(1, entries.size)
        assertEquals("scp-cn-2000", entries[0].id)
    }
}