package cc.kesardev.scparchive

import cc.kesardev.scparchive.home.DailySelector
import cc.kesardev.scparchive.home.HomeArticle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailySelectorTest {

    private val cn = listOf(HomeArticle("scp-cn-1", "CN one", rating = 900))
    private val main = listOf(HomeArticle("scp-1", "SCP one", rating = 1200))

    @Test
    fun refreshAlternatesBetweenSeries() {
        assertEquals("scp-cn-1", DailySelector.pick(cn, main, 0, 20_000L)?.slug)
        assertEquals("scp-1", DailySelector.pick(cn, main, 1, 20_000L)?.slug)
        assertEquals("scp-cn-1", DailySelector.pick(cn, main, 2, 20_000L)?.slug)
    }

    @Test
    fun dayMovesThroughThePool() {
        val many = (1..5).map { HomeArticle("scp-cn-" + it, "CN " + it, rating = 100) }

        assertEquals("scp-cn-1", DailySelector.pick(many, emptyList(), 0, 10L)?.slug)
        assertEquals("scp-cn-2", DailySelector.pick(many, emptyList(), 0, 11L)?.slug)
    }

    @Test
    fun fallsBackToTheOnlyAvailablePool() {
        val picked = DailySelector.pick(emptyList(), main, 0, 20_000L)
        assertEquals("scp-1", picked?.slug)
        assertEquals("评分 +1200 · 基金会本部", picked?.subtitle)
    }

    @Test
    fun returnsNullWhenBothPoolsAreEmpty() {
        assertNull(DailySelector.pick(emptyList(), emptyList(), 0, 20_000L))
    }
}
