package cc.kesardev.scparchive

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import cc.kesardev.scparchive.settings.AppearancePreferences

class ScpArchiveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val preferences = AppearancePreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            AppearancePreferences.nightMode(preferences.mode())
        )
    }
}
