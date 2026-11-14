package cc.kesardev.scparchive.series

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.MainActivity
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.catalog.CatalogActivity
import cc.kesardev.scparchive.catalog.SearchActivity
import cc.kesardev.scparchive.home.PageInput
import cc.kesardev.scparchive.hub.HubActivity
import cc.kesardev.scparchive.hub.HubMode
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.roundToInt

class SeriesActivity : BaseActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var scpCategories: LinearLayout
    private lateinit var cnCategories: LinearLayout
    private lateinit var libraryCategories: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_series)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        scpCategories = findViewById(R.id.scp_categories)
        cnCategories = findViewById(R.id.cn_categories)
        libraryCategories = findViewById(R.id.library_categories)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        findViewById<View>(R.id.series_search).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.series_jump).setOnClickListener { showJumpDialog() }

        setupSections()
        BottomTabs.bind(this, R.id.tab_series)
    }

    override fun onResume() {
        super.onResume()
        BottomTabs.select(this, R.id.tab_series)
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

    private fun setupSections() {
        addCategory(
            scpCategories,
            "01",
            "编号条目",
            "SCP-001 – SCP-8999 · 10 个系列"
        ) {
            startActivity(Intent(this, CatalogActivity::class.java))
        }
        addCategory(
            scpCategories,
            "故",
            "故事",
            "系列 I – VIII 的故事集"
        ) {
            openStoryEditions(cn = false)
        }

        addCategory(
            cnCategories,
            "02",
            "编号条目",
            "SCP-CN-001 – CN-4999 · 5 个系列"
        ) {
            startActivity(
                Intent(this, CatalogActivity::class.java)
                    .putExtra("series_id", "scp-series-cn")
            )
        }
        addCategory(
            cnCategories,
            "故",
            "故事",
            "CN 系列 I – IV 的故事集"
        ) {
            openStoryEditions(cn = true)
        }

        addCategory(libraryCategories, "故", "故事", "基金会本部故事中心页") {
            openHub("故事", "foundation-tales", HubMode.STORIES)
        }
        addCategory(libraryCategories, "原", "原创故事", "中文分部原创故事中心页") {
            openHub("原创故事", "foundation-tales-cn", HubMode.STORIES)
        }
        addCategory(libraryCategories, "系", "原创系列", "中文分部原创故事系列列表") {
            openHub("原创系列", "series-archive-cn", HubMode.ARCHIVE)
        }
        addCategory(libraryCategories, "设", "设定", "基金会宇宙的共享设定中心") {
            openHub("设定", "canon-hub", HubMode.CANON)
        }
        addCategory(libraryCategories, "原", "原创设定", "中文分部原创设定中心") {
            openHub("原创设定", "canon-hub-cn", HubMode.CANON)
        }
        addCategory(libraryCategories, "GoI", "GoI 格式", "相关组织文档格式与中心页") {
            openHub("GoI 格式", "goi-formats", HubMode.GOI)
        }
        addCategory(libraryCategories, "际", "SCP 国际翻译中心", "各分部翻译条目入口") {
            openHub("SCP 国际翻译中心", "scp-international", HubMode.GENERIC)
        }
        addCategory(libraryCategories, "书", "被放逐者之图书馆", "姊妹站的故事与索引") {
            openHub("被放逐者之图书馆", "wanderers:start", HubMode.LIBRARY)
        }
        addCategory(libraryCategories, "创", "原创中心", "中文分部原创图书馆中心") {
            openHub("原创中心", "wanderers:the-library-cn", HubMode.LIBRARY)
        }
    }

    private fun addCategory(
        container: LinearLayout,
        badge: String,
        title: String,
        desc: String,
        action: () -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.item_series_category, container, false)
        view.findViewById<TextView>(R.id.category_badge).text = badge
        view.findViewById<TextView>(R.id.category_title).text = title
        view.findViewById<TextView>(R.id.category_desc).text = desc
        view.setOnClickListener { action() }
        container.addView(view)
    }

    private fun openStoryEditions(cn: Boolean) {
        val count = if (cn) 4 else 8
        val prefix = if (cn) "scp-series-cn-" else "scp-series-"
        val links = ArrayList<String>(count)
        val titles = ArrayList<String>(count)
        for (index in 1..count) {
            links.add(prefix + index + "-tales-edition")
            titles.add((if (cn) "CN 系列 " else "系列 ") + roman(index) + " 故事")
        }
        startActivity(
            Intent(this, HubActivity::class.java)
                .putExtra("title", if (cn) "SCP-CN 系列故事" else "SCP 系列故事")
                .putStringArrayListExtra("links", links)
                .putStringArrayListExtra("link_titles", titles)
        )
    }

    private fun openHub(title: String, path: String, mode: HubMode) {
        startActivity(
            Intent(this, HubActivity::class.java)
                .putExtra("title", title)
                .putExtra("path", path)
                .putExtra("mode", mode.name)
        )
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

    private fun roman(value: Int): String {
        return when (value) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            else -> value.toString()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }
}