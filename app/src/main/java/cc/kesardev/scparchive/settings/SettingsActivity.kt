package cc.kesardev.scparchive.settings

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.BuildConfig
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : BaseActivity() {

    private val preferences by lazy { AppearancePreferences(this) }
    private lateinit var toolbar: MaterialToolbar
    private lateinit var themeGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        themeGroup = findViewById(R.id.theme_group)
        themeGroup.check(checkedIdFor(preferences.mode()))
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = modeFor(checkedId) ?: return@setOnCheckedChangeListener
            if (selected == preferences.mode()) {
                return@setOnCheckedChangeListener
            }
            preferences.setMode(selected)
            AppCompatDelegate.setDefaultNightMode(AppearancePreferences.nightMode(selected))
        }

        findViewById<TextView>(R.id.settings_version).text =
            getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME

        BottomTabs.bind(this, R.id.tab_settings)
    }

    override fun onResume() {
        super.onResume()
        BottomTabs.select(this, R.id.tab_settings)
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun checkedIdFor(mode: ThemeMode): Int {
        return when (mode) {
            ThemeMode.SYSTEM -> R.id.theme_system
            ThemeMode.LIGHT -> R.id.theme_light
            ThemeMode.DARK -> R.id.theme_dark
        }
    }

    private fun modeFor(checkedId: Int): ThemeMode? {
        return when (checkedId) {
            R.id.theme_system -> ThemeMode.SYSTEM
            R.id.theme_light -> ThemeMode.LIGHT
            R.id.theme_dark -> ThemeMode.DARK
            else -> null
        }
    }
}
