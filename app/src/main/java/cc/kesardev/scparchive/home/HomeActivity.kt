package cc.kesardev.scparchive.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.MainActivity
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.catalog.CatalogActivity
import cc.kesardev.scparchive.catalog.SearchActivity
import cc.kesardev.scparchive.catalog.SeriesCatalog
import cc.kesardev.scparchive.library.LibraryActivity
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale
import kotlin.math.roundToInt

class HomeActivity : BaseActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var loading: LinearProgressIndicator
    private lateinit var heroCard: View
    private lateinit var heroTitle: TextView
    private lateinit var heroTagline: TextView
    private lateinit var heroMeta: TextView
    private lateinit var heroShuffle: TextView
    private lateinit var sectionContinue: View
    private lateinit var continueRow: LinearLayout
    private lateinit var sectionFavorites: View
    private lateinit var favoritesRow: LinearLayout
    private lateinit var quickRow: LinearLayout
    private lateinit var sectionTopRated: View
    private lateinit var topRatedRow: LinearLayout
    private lateinit var topRatedSource: TextView
    private lateinit var sectionRecent: View
    private lateinit var recentList: LinearLayout
    private lateinit var recentSource: TextView
    private lateinit var seriesRow: LinearLayout
    private lateinit var statCatalog: TextView
    private lateinit var statFavorites: TextView
    private lateinit var statHistory: TextView

    private var state = HomeUiState()
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_home)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        loading = findViewById(R.id.home_loading)
        heroCard = findViewById(R.id.hero_card)
        heroTitle = findViewById(R.id.hero_title)
        heroTagline = findViewById(R.id.hero_tagline)
        heroMeta = findViewById(R.id.hero_meta)
        heroShuffle = findViewById(R.id.hero_shuffle)
        sectionContinue = findViewById(R.id.section_continue)
        continueRow = findViewById(R.id.continue_row)
        sectionFavorites = findViewById(R.id.section_favorites)
        favoritesRow = findViewById(R.id.favorites_row)
        quickRow = findViewById(R.id.quick_row)
        sectionTopRated = findViewById(R.id.section_top_rated)
        topRatedRow = findViewById(R.id.top_rated_row)
        topRatedSource = findViewById(R.id.top_rated_source)
        sectionRecent = findViewById(R.id.section_recent)
        recentList = findViewById(R.id.recent_list)
        recentSource = findViewById(R.id.recent_source)
        seriesRow = findViewById(R.id.series_row)
        statCatalog = findViewById(R.id.stat_catalog)
        statFavorites = findViewById(R.id.stat_favorites)
        statHistory = findViewById(R.id.stat_history)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        heroCard.setOnClickListener { state.daily?.let { openArticle(it.slug) } }
        heroShuffle.setOnClickListener { shuffleDaily() }
        findViewById<View>(R.id.home_catalog_button).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }

        setupQuickActions()
        setupSeriesChips()
        BottomTabs.bind(this, R.id.tab_home)

        viewModel.state.observe(this) { value ->
            state = value
            render(value)
        }
        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        BottomTabs.select(this, R.id.tab_home)
        if (firstResume) {
            firstResume = false
        } else {
            viewModel.refreshLocal()
        }
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
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }

            R.id.action_refresh -> {
                viewModel.load(force = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun render(value: HomeUiState) {
        loading.visibility = if (value.loading || value.remoteLoading) View.VISIBLE else View.GONE
        renderHero(value.daily)

        sectionContinue.visibility = if (value.continueReading.isEmpty()) View.GONE else View.VISIBLE
        continueRow.removeAllViews()
        for (article in value.continueReading) {
            addArticleCard(continueRow, article, "继续阅读", showProgress = true)
        }

        sectionFavorites.visibility = if (value.favorites.isEmpty()) View.GONE else View.VISIBLE
        favoritesRow.removeAllViews()
        for (article in value.favorites) {
            addArticleCard(favoritesRow, article, "收藏", showProgress = false)
        }

        sectionTopRated.visibility = if (value.topRated.isEmpty()) View.GONE else View.VISIBLE
        topRatedSource.text = value.topRatedSource
        topRatedRow.removeAllViews()
        value.topRated.forEachIndexed { index, article ->
            val rank = if (article.rank > 0) article.rank else index + 1
            addArticleCard(
                topRatedRow,
                article,
                String.format(Locale.CHINA, "TOP %02d", rank),
                showProgress = false,
                forcedMeta = article.subtitle
            )
        }

        sectionRecent.visibility = if (value.recent.isEmpty()) View.GONE else View.VISIBLE
        recentSource.text = value.recentSource
        recentList.removeAllViews()
        value.recent.forEachIndexed { index, article ->
            addRecentRow(index, article)
        }

        statCatalog.text = "索引 " + String.format(Locale.CHINA, "%,d", value.catalogCount) + " 篇"
        statFavorites.text = "收藏 " + value.favoriteCount
        statHistory.text = "历史 " + value.historyCount
    }

    private fun renderHero(article: HomeArticle?) {
        if (article == null) {
            heroTitle.text = "SCP 档案库"
            heroTagline.text = "正在整理今日条目…"
            heroMeta.text = ""
            return
        }
        heroTitle.text = article.title
        heroTagline.text = article.tagline
        heroMeta.text = "每日轮换 · 离线可用"
    }

    private fun shuffleDaily() {
        val current = state.daily?.slug
        val index = DailyPicks.all.indexOfFirst { it.slug == current }.coerceAtLeast(0)
        val next = DailyPicks.all[((index + 1) % DailyPicks.all.size + DailyPicks.all.size) % DailyPicks.all.size]
        val article = HomeArticle(next.slug, next.name, "今日推荐", next.tagline)
        state = state.copy(daily = article)
        renderHero(article)
    }

    private fun addArticleCard(
        container: LinearLayout,
        article: HomeArticle,
        badge: String,
        showProgress: Boolean,
        forcedMeta: String = ""
    ) {
        val view = layoutInflater.inflate(R.layout.item_home_article, container, false)
        view.findViewById<TextView>(R.id.card_badge).text = badge
        view.findViewById<TextView>(R.id.card_title).text = article.title

        val meta = when {
            forcedMeta.isNotBlank() -> forcedMeta
            showProgress -> relativeTime(article.timestamp).ifBlank { "阅读进度" }
            article.subtitle.isNotBlank() -> article.subtitle
            else -> relativeTime(article.timestamp)
        }
        view.findViewById<TextView>(R.id.card_meta).text = meta

        val progressBox = view.findViewById<View>(R.id.card_progress_box)
        if (showProgress) {
            progressBox.visibility = View.VISIBLE
            val percent = ((article.progress ?: 0f) * 100f).roundToInt().coerceIn(0, 100)
            view.findViewById<android.widget.ProgressBar>(R.id.card_progress).progress = percent
            view.findViewById<TextView>(R.id.card_progress_label).text = "已读 " + percent + "%"
        } else {
            progressBox.visibility = View.GONE
        }

        view.setOnClickListener { openArticle(article.slug) }
        container.addView(view)
    }

    private fun addRecentRow(index: Int, article: HomeArticle) {
        val row = layoutInflater.inflate(R.layout.item_home_recent, recentList, false)
        row.findViewById<TextView>(R.id.recent_index)
            .text = String.format(Locale.CHINA, "%02d", index + 1)
        row.findViewById<TextView>(R.id.recent_title).text = article.title
        row.findViewById<TextView>(R.id.recent_meta).text = listOf(
            relativeTime(article.timestamp),
            article.subtitle
        ).filter { it.isNotBlank() }.joinToString(" · ")
        row.setOnClickListener { openArticle(article.slug) }
        recentList.addView(row)
    }

    private fun setupQuickActions() {
        quickRow.removeAllViews()
        val actions = listOf(
            Triple(R.drawable.ic_shuffle, "随机") { openRandom() },
            Triple(R.drawable.ic_hash, "编号") { showJumpDialog() },
            Triple(R.drawable.ic_list, "目录") {
                startActivity(Intent(this, CatalogActivity::class.java))
            },
            Triple(R.drawable.ic_bookmark, "书架") {
                startActivity(Intent(this, LibraryActivity::class.java))
            }
        )
        actions.forEachIndexed { index, (icon, label, action) ->
            val view = layoutInflater.inflate(R.layout.item_home_quick, quickRow, false)
            view.findViewById<ImageView>(R.id.quick_icon).setImageResource(icon)
            view.findViewById<TextView>(R.id.quick_label).text = label
            val params = view.layoutParams as LinearLayout.LayoutParams
            params.marginEnd = if (index == actions.lastIndex) 0 else dp(8)
            view.layoutParams = params
            view.setOnClickListener { action() }
            quickRow.addView(view)
        }
    }

    private fun setupSeriesChips() {
        seriesRow.removeAllViews()
        for (series in SeriesCatalog.all) {
            val chip = Chip(this).apply {
                text = series.name.replace("系列 ", "")
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@HomeActivity, CatalogActivity::class.java)
                            .putExtra("series_id", series.id)
                    )
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = dp(8)
            chip.layoutParams = params
            seriesRow.addView(chip)
        }
    }

    private fun openRandom() {
        viewModel.random { article ->
            if (article == null) {
                Toast.makeText(this, "目录为空，先打开系列目录", Toast.LENGTH_SHORT).show()
            } else {
                openArticle(article.slug)
            }
        }
    }

    private fun showJumpDialog() {
        val input = EditText(this).apply {
            hint = "例如 173、CN-1000、scp-5000"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("按编号打开")
            .setView(container)
            .setPositiveButton("打开", null)
            .setNegativeButton("取消", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val slug = PageInput.normalize(input.text.toString())
                if (slug == null) {
                    input.error = "请输入 SCP 编号，例如 173 或 CN-1000"
                } else {
                    dialog.dismiss()
                    openArticle(slug)
                }
            }
        }
        dialog.show()
        input.requestFocus()
    }

    private fun openArticle(slug: String) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", slug)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

}
