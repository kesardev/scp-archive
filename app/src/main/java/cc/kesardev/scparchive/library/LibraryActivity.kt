package cc.kesardev.scparchive.library

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.MainActivity
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout

class LibraryActivity : BaseActivity() {

    private val viewModel: LibraryViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabs: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var pagerAdapter: LibraryPagerAdapter

    private var currentTab = LibraryTab.FAVORITES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_library)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        tabs = findViewById(R.id.library_tabs)
        pager = findViewById(R.id.library_pager)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "书架"

        tabs.addTab(tabs.newTab().setText("收藏"))
        tabs.addTab(tabs.newTab().setText("历史"))

        pagerAdapter = LibraryPagerAdapter(
            onOpen = { entry -> openEntry(entry) },
            onRemove = { tab, entry -> viewModel.remove(tab, entry.slug) }
        )
        pager.adapter = pagerAdapter
        pager.offscreenPageLimit = 1

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTab = if (position == 0) LibraryTab.FAVORITES else LibraryTab.HISTORY
                if (tabs.selectedTabPosition != position) {
                    tabs.selectTab(tabs.getTabAt(position))
                }
                invalidateOptionsMenu()
            }
        })

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (pager.currentItem != tab.position) {
                    pager.setCurrentItem(tab.position, true)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        viewModel.favorites.observe(this) { entries ->
            pagerAdapter.submit(LibraryTab.FAVORITES, entries)
            invalidateOptionsMenu()
        }
        viewModel.history.observe(this) { entries ->
            pagerAdapter.submit(LibraryTab.HISTORY, entries)
            invalidateOptionsMenu()
        }

        viewModel.refresh()
        BottomTabs.bind(this, R.id.tab_library)
    }

    override fun onResume() {
        super.onResume()
        BottomTabs.select(this, R.id.tab_library)
        viewModel.refresh()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_library, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val clear = menu.findItem(R.id.action_clear_history)
        clear?.isVisible = currentTab == LibraryTab.HISTORY
        clear?.isEnabled = currentTab == LibraryTab.HISTORY &&
            !viewModel.history.value.isNullOrEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_history) {
            AlertDialog.Builder(this)
                .setTitle("清空阅读历史")
                .setMessage("确定清空全部阅读历史吗？")
                .setPositiveButton("清空") { _, _ -> viewModel.clearHistory() }
                .setNegativeButton("取消", null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openEntry(entry: LibraryEntry) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", entry.slug)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}