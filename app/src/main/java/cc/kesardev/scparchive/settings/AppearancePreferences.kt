package cc.kesardev.scparchive.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import cc.kesardev.scparchive.reader.ReaderTheme

class AppearancePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("appearance_prefs", Context.MODE_PRIVATE)

    fun mode(): ThemeMode {
        return ThemeMode.fromStored(prefs.getString("theme_mode", null))
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.storedValue).apply()
    }

    fun readerTheme(context: Context): ReaderTheme {
        val systemDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return mode().readerTheme(systemDark)
    }

    companion object {
        fun nightMode(mode: ThemeMode): Int {
            return when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        }
    }
}
