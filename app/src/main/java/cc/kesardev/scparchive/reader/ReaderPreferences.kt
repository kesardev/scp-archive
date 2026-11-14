package cc.kesardev.scparchive.reader

import android.content.Context

class ReaderPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    fun load(): ReaderSettings {
        return ReaderSettings(
            fontSize = prefs.getFloat("font_size", 17f),
            lineHeight = prefs.getFloat("line_height", 1.75f),
            showImages = prefs.getBoolean("show_images", true),
            margin = prefs.getInt("margin", 16)
        )
    }

    fun save(settings: ReaderSettings) {
        prefs.edit()
            .putFloat("font_size", settings.fontSize)
            .putFloat("line_height", settings.lineHeight)
            .putBoolean("show_images", settings.showImages)
            .putInt("margin", settings.margin)
            .apply()
    }

    fun position(slug: String): Float {
        return prefs.getFloat("pos_" + keyOf(slug), 0f)
    }

    fun savePosition(slug: String, ratio: Float) {
        prefs.edit().putFloat("pos_" + keyOf(slug), ratio.coerceIn(0f, 1f)).apply()
    }

    private fun keyOf(slug: String): String {
        return slug.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
    }
}
