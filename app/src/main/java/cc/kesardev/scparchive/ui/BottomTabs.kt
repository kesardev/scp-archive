package cc.kesardev.scparchive.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.catalog.CatalogActivity
import cc.kesardev.scparchive.home.HomeActivity
import cc.kesardev.scparchive.library.LibraryActivity
import cc.kesardev.scparchive.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomTabs {

    fun bind(activity: AppCompatActivity, selected: Int) {
        val navigation = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        // The screen root already pads for the navigation bar. Stop the
        // navigation view from adding a second bottom inset gap.
        ViewCompat.setOnApplyWindowInsetsListener(navigation) { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
            insets
        }
        navigation.selectedItemId = selected
        navigation.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.tab_home -> HomeActivity::class.java
                R.id.tab_catalog -> CatalogActivity::class.java
                R.id.tab_library -> LibraryActivity::class.java
                R.id.tab_settings -> SettingsActivity::class.java
                else -> null
            }
            if (target != null && activity.javaClass != target) {
                val flags = if (target == HomeActivity::class.java) {
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                } else {
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(
                    Intent(activity, target).addFlags(flags)
                )
            }
            true
        }
    }

    fun select(activity: AppCompatActivity, selected: Int) {
        activity.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = selected
    }
}
