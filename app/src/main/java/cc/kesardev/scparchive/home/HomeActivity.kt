package cc.kesardev.scparchive.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
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
    private lateinit var sectionRecommended: View
    private lateinit var recommendedRow: LinearLayout
    private lateinit var recommendModes: MaterialButtonToggleGroup
    private lateinit var recommendShuffle: TextView
    private lateinit var randomCard: View
    private lateinit var randomTitle: TextView
    private lateinit var randomMeta: TextView
    private lateinit var randomShuffle: TextView
    private lateinit var continueRow: LinearLayout
    private lateinit var sectionFeatured: View
    private lateinit var featuredList: LinearLayout
    private lateinit var featuredSource: TextView
    private lateinit var sectionRecent: View
    private lateinit var recentList: LinearLayout
    private lateinit var recentSource: TextView
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
        sectionRecommended = findViewById(R.id.section_recommended)
        recommendedRow = findViewById(R.id.recommended_row)
        recommendModes = findViewById(R.id.recommend_modes)
        recommendShuffle = findViewById(R.id.recommend_shuffle)
        randomCard = findViewById(R.id.random_card)
        randomTitle = findViewById(R.id.random_title)
        randomMeta = findViewById(R.id.random_meta)
        randomShuffle = findViewById(R.id.random_shuffle)
        continueRow = findViewById(R.id.continue_row)
        sectionFeatured = findViewById(R.id.section_featured)
        featuredList = findViewById(R.id.featured_list)
        featuredSource = findViewById(R.id.featured_source)
        sectionRecent = findViewById(R.id.section_recent)
        recentList = findViewById(R.id.recent_list)
        recentSource = findViewById(R.id.recent_source)
        statCatalog = findViewById(R.id.stat_catalog)
        statFavorites = findViewById(R.id.stat_favorites)
        statHistory = findViewById(R.id.stat_history)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        heroCard.setOnClickListener { state.daily?.let { openArticle(it.slug) } }
        heroShuffle.setOnClickListener { viewModel.shuffleDaily() }
        recommendModes.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            viewModel.setRecommendMode(
                when (checkedId) {
                    R.id.recommend_mode_rating -> RecommendMode.RATING
                    R.id.recommend_mode_heat -> RecommendMode.HEAT
                    else -> RecommendMode.MIXED
                }
            )
        }
        recommendShuffle.setOnClickListener { viewModel.shuffleRecommended() }

        randomCard.setOnClickListener { state.random?.let { openArticle(it.slug) } }
        randomShuffle.setOnClickListener {
            viewModel.random { article ->
                if (article == null) {
                    Toast.makeText(this, "索引为空，先去系列页同步目录", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
        renderRandom(value.random)

        sectionRecommended.visibility =
            if (value.recommended.isEmpty()) View.GONE else View.VISIBLE
        val modeButton = when (value.recommendMode) {
            RecommendMode.RATING -> R.id.recommend_mode_rating
            RecommendMode.HEAT -> R.id.recommend_mode_heat
            RecommendMode.MIXED -> R.id.recommend_mode_mixed
        }
        if (recommendModes.checkedButtonId != modeButton) {
            recommendModes.check(modeButton)
        }
        recommendedRow.removeAllViews()
        for (article in value.recommended) {
            addArticleCard(
                recommendedRow,
                article,
                value.recommendMode.badge,
                showProgress = false,
                forcedMeta = article.subtitle
            )
        }

        continueRow.removeAllViews()
        for (article in value.continueReading) {
            addArticleCard(continueRow, article, "继续阅读", showProgress = true)
        }

        renderFeatured(value.featured, value.featuredSource)

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
        heroMeta.text = article.subtitle.ifBlank { "每日轮换 · 离线可用" }
    }

    private fun renderRandom(article: HomeArticle?) {
        if (article == null) {
            randomTitle.text = "交给随机数"
            randomMeta.text = "从索引里抽一篇没读过的档案"
        } else {
            randomTitle.text = article.title
            randomMeta.text = if (article.subtitle.isNotBlank()) {
                article.subtitle
            } else {
                "点开看看，或再换一篇"
            }
        }
    }

    private fun renderFeatured(items: List<FeaturedItem>, source: String) {
        sectionFeatured.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        featuredSource.text = source
        featuredList.removeAllViews()
        var lastGroup = ""
        for (item in items) {
            if (item.group.isNotEmpty() && item.group != lastGroup) {
                lastGroup = item.group
                val heading = layoutInflater.inflate(
                    R.layout.item_featured_group,
                    featuredList,
                    false
                ) as TextView
                heading.text = item.group
                featuredList.addView(heading)
            }
            val card = layoutInflater.inflate(R.layout.item_featured, featuredList, false)
            card.findViewById<TextView>(R.id.featured_title).text = item.title
            val tagline = card.findViewById<TextView>(R.id.featured_tagline)
            if (item.tagline.isBlank()) {
                tagline.visibility = View.GONE
            } else {
                tagline.text = item.tagline
            }
            val author = card.findViewById<TextView>(R.id.featured_author)
            if (item.author.isBlank()) {
                author.visibility = View.GONE
            } else {
                author.text = "by " + item.author
            }
            card.setOnClickListener { openArticle(item.slug) }
            featuredList.addView(card)
        }
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

    private fun openArticle(slug: String) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", slug)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}