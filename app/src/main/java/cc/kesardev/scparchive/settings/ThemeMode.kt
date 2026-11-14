package cc.kesardev.scparchive.settings

import cc.kesardev.scparchive.reader.ReaderTheme

enum class ThemeMode(val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun readerTheme(systemDark: Boolean): ReaderTheme {
        return when (this) {
            SYSTEM -> if (systemDark) ReaderTheme.DARK else ReaderTheme.LIGHT
            LIGHT -> ReaderTheme.LIGHT
            DARK -> ReaderTheme.DARK
        }
    }

    companion object {
        fun fromStored(value: String?): ThemeMode {
            return values().firstOrNull { it.storedValue == value } ?: SYSTEM
        }
    }
}
