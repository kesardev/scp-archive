package cc.kesardev.scparchive

import cc.kesardev.scparchive.home.DailyPicks
import cc.kesardev.scparchive.home.PageInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSelectionTest {

    @Test
    fun dailyPickIsStableForTheSameDay() {
        assertEquals(DailyPicks.forDay(20_000L), DailyPicks.forDay(20_000L))
        assertEquals(DailyPicks.forDay(0L).slug, DailyPicks.all[0].slug)
        assertEquals(DailyPicks.all.size, DailyPicks.all.map { it.slug }.distinct().size)
        assertTrue(DailyPicks.forDay(12L).slug.isNotEmpty())
    }

    @Test
    fun normalizesNumberAndCnInput() {
        assertEquals("scp-173", PageInput.normalize("173"))
        assertEquals("scp-173", PageInput.normalize("SCP-173"))
        assertEquals("scp-cn-1000", PageInput.normalize("CN-1000"))
        assertEquals("scp-cn-1000", PageInput.normalize("scp-cn-1000"))
        assertEquals(
            "scp-5000",
            PageInput.normalize("https://scp-wiki-cn.wikidot.com/scp-5000")
        )
        assertEquals("scp-173-j", PageInput.normalize("173-j"))
        assertEquals("scp-cn-1000", PageInput.normalize("ＣＮ－１０００"))
    }

    @Test
    fun rejectsNonScpInput() {
        assertNull(PageInput.normalize(""))
        assertNull(PageInput.normalize("tale"))
        assertNull(PageInput.normalize("scp-series"))
        assertNull(PageInput.normalize("scp-"))
    }
}
