package cc.kesardev.scparchive

import cc.kesardev.scparchive.reader.ReaderSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderSettingsTest {

    @Test
    fun settingsJsonMatchesTheJavaScriptContract() {
        val settings = ReaderSettings(fontSize = 20f, lineHeight = 1.9f, showImages = false, margin = 24)
        val json = JSONObject(settings.toJson())

        assertEquals(20.0, json.getDouble("fontSize"), 0.001)
        assertEquals(1.9, json.getDouble("lineHeight"), 0.001)
        assertFalse(json.getBoolean("showImages"))
        assertEquals(24, json.getInt("margin"))
    }
}
