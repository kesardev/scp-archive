package cc.kesardev.scparchive

import cc.kesardev.scparchive.reader.ReaderTheme
import cc.kesardev.scparchive.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun storedValuesRoundTrip() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("unknown"))
    }

    @Test
    fun readerThemeFollowsMode() {
        assertEquals(ReaderTheme.LIGHT, ThemeMode.SYSTEM.readerTheme(false))
        assertEquals(ReaderTheme.DARK, ThemeMode.SYSTEM.readerTheme(true))
        assertEquals(ReaderTheme.LIGHT, ThemeMode.LIGHT.readerTheme(true))
        assertEquals(ReaderTheme.DARK, ThemeMode.DARK.readerTheme(false))
    }
}
