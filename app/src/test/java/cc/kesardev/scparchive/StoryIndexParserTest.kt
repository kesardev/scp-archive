package cc.kesardev.scparchive

import cc.kesardev.scparchive.story.StoryIndexMode
import cc.kesardev.scparchive.story.StoryIndexParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryIndexParserTest {

    @Test
    fun parsesHeadingGroupsAndEntries() {
        val html = """
            <div id="page-content">
              <h1>故事列表一览</h1>
              <h3 id="toc0"><span>A<a name="A"></a></span></h3>
              <div class="list-pages-box">
                <a href="/tale-a">Tale A</a>
                <a href="/tale-a-2">Tale A2</a>
                <a href="/system:page-tags/tag/scp">noise</a>
              </div>
              <h3 id="toc1"><span>B<a name="B"></a></span></h3>
              <div class="list-pages-box">
                <a href="/tale-b">Tale B</a>
              </div>
            </div>
        """.trimIndent()

        val groups = StoryIndexParser.parse(html)

        assertEquals(2, groups.size)
        assertEquals("A", groups[0].label)
        assertEquals(2, groups[0].entries.size)
        assertEquals("tale-a", groups[0].entries[0].slug)
        assertEquals("B", groups[1].label)
        assertEquals(1, groups[1].entries.size)
        assertTrue(groups.none { group -> group.entries.any { it.slug.startsWith("system:") } })
    }

    @Test
    fun deduplicatesRepeatedEntries() {
        val html = """
            <div id="page-content">
              <h3>A</h3>
              <div class="list-pages-box"><a href="/tale-a">Tale A</a></div>
              <h3>B</h3>
              <div class="list-pages-box"><a href="/tale-a">Tale A again</a><a href="/tale-b">Tale B</a></div>
            </div>
        """.trimIndent()

        val groups = StoryIndexParser.parse(html)

        assertEquals(2, groups.size)
        assertEquals(1, groups[0].entries.size)
        assertEquals(1, groups[1].entries.size)
        assertEquals("tale-b", groups[1].entries[0].slug)
    }

    @Test
    fun parsesStorySeriesEntriesByHeadings() {
        val html = """
            <div id="page-content">
              <h2><span>序幕</span></h2>
              <h4><span>第一篇</span></h4>
              <ul>
                <li><a href="/tale-one">I</a></li>
                <li><a href="/tale-two">II</a></li>
                <li><a href="/scp-100">SCP-100</a></li>
                <li><a href="/some-hub">某系列中心</a></li>
              </ul>
              <h2><span>正传</span></h2>
              <ul>
                <li><a href="/tale-three">III</a></li>
              </ul>
            </div>
        """.trimIndent()

        val groups = StoryIndexParser.parse(html, StoryIndexMode.SERIES)

        assertEquals(2, groups.size)
        assertEquals("第一篇", groups[0].label)
        assertEquals(2, groups[0].entries.size)
        assertEquals("tale-one", groups[0].entries[0].slug)
        assertEquals("正传", groups[1].label)
        assertEquals("tale-three", groups[1].entries[0].slug)
    }
    @Test
    fun fallsBackToHeadingGroupsWhenSeriesPageHasNoListItems() {
        val html = """
            <div id="page-content">
              <h2><span>合作故事</span></h2>
              <table>
                <tr><td><a href="/collab-one">合作一</a></td></tr>
                <tr><td><a href="/scp-2000">SCP-2000</a></td></tr>
              </table>
              <h2><span>已解明</span></h2>
              <p><a href="/explained-one">解明一</a></p>
            </div>
        """.trimIndent()

        val groups = StoryIndexParser.parse(html, StoryIndexMode.SERIES)

        assertEquals(2, groups.size)
        assertEquals("合作故事", groups[0].label)
        assertEquals(1, groups[0].entries.size)
        assertEquals("collab-one", groups[0].entries[0].slug)
        assertEquals("已解明", groups[1].label)
        assertEquals("explained-one", groups[1].entries[0].slug)
    }
    @Test
    fun formatsMonthLabels() {
        assertEquals("2026-01", StoryIndexParser.displayLabel("202601"))
        assertEquals("A", StoryIndexParser.displayLabel("A"))
    }
}