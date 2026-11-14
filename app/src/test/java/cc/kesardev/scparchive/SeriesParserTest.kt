package cc.kesardev.scparchive

import cc.kesardev.scparchive.catalog.SeriesParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesParserTest {

    private fun html(name: String): String {
        val stream = javaClass.getResourceAsStream("/pages/" + name + ".html")
            ?: error("missing fixture: " + name)
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun parsesMainSeriesEntries() {
        val entries = SeriesParser.parse(html("scp-series"), "scp-series")

        assertTrue("expected a full series page, got " + entries.size, entries.size >= 900)
        assertEquals("scp-001", entries.first().id)
        assertEquals("scp-999", entries.last().id)
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertTrue(entries.all { it.title.isNotBlank() })
    }

    @Test
    fun parsesCnSeriesEntries() {
        val entries = SeriesParser.parse(html("scp-series-cn"), "scp-series-cn")

        assertTrue("expected CN entries, got " + entries.size, entries.size >= 900)
        assertEquals("scp-cn-001", entries.first().id)
        assertTrue(entries.any { it.id == "scp-cn-100" })
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
    }
}
