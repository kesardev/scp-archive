package cc.kesardev.scparchive

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cc.kesardev.scparchive.catalog.CatalogActivity
import cc.kesardev.scparchive.catalog.SearchActivity
import cc.kesardev.scparchive.library.LibraryActivity
import cc.kesardev.scparchive.library.LibraryDatabase
import cc.kesardev.scparchive.reader.ReaderPreferences
import cc.kesardev.scparchive.reader.ReaderSettings
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import cc.kesardev.scparchive.reader.ReaderTheme
import cc.kesardev.scparchive.reader.ReaderWebViewClient
import cc.kesardev.scparchive.settings.AppearancePreferences
import cc.kesardev.scparchive.settings.SettingsActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

class MainActivity : BaseActivity() {

    private val viewModel: ReaderViewModel by viewModels()
    private val preferences by lazy { ReaderPreferences(this) }
    private val libraryDb by lazy { LibraryDatabase(this) }
    private val appearance by lazy { AppearancePreferences(this) }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    private var currentSlug: String = "scp-173"
    private var currentTitle: String = "SCP-173"
    private var isFavorite: Boolean = false
    private var lastHistoryWrite: Long = 0L
    private var baseStatus: String = ""
    private var currentTheme: ReaderTheme = ReaderTheme.LIGHT
    private val tocItems = mutableListOf<TocItem>()

    private data class TocItem(val id: String, val text: String, val level: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        currentTheme = appearance.readerTheme(this)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.web_view)
        status = findViewById(R.id.status)
        progress = findViewById(R.id.progress)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        configureWebView()
        currentSlug = intent.getStringExtra("slug") ?: "scp-173"
        currentTitle = currentSlug.uppercase()
        viewModel.state.observe(this) { state -> render(state) }
        viewModel.load(currentSlug, currentTheme)
    }

    override fun onResume() {
        super.onResume()
        val theme = appearance.readerTheme(this)
        if (theme != currentTheme) {
            currentTheme = theme
            webView.setBackgroundColor(readerBackground())
            viewModel.load(currentSlug, theme)
        }
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                maxOf(bars.bottom, ime)
            )
            insets
        }
    }

    private fun configureWebView() {
        webView.setBackgroundColor(readerBackground())
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = false
            loadWithOverviewMode = false
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(false)
            setAcceptThirdPartyCookies(webView, false)
        }

        webView.addJavascriptInterface(Bridge(), "Reader")
        webView.webViewClient = ReaderWebViewClient(
            onInternal = { path -> loadInternal(path) },
            onExternal = { url -> openExternal(url) },
            pageFinished = { view, url ->
                if (url != "about:blank") {
                    applySettings(preferences.load())
                    val ratio = preferences.position(currentSlug)
                    if (ratio > 0f) {
                        view.evaluateJavascript("window.__readerRestorePosition(" + ratio + ")", null)
                    }
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_toc)?.isEnabled = tocItems.isNotEmpty()
        menu.findItem(R.id.action_favorite)?.apply {
            setIcon(if (isFavorite) R.drawable.ic_star_on else R.drawable.ic_star_off)
            setTitle(if (isFavorite) "已收藏" else "收藏")
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_catalog -> {
                startActivity(Intent(this, CatalogActivity::class.java))
                true
            }

            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }

            R.id.action_library -> {
                startActivity(Intent(this, LibraryActivity::class.java))
                true
            }

            R.id.action_favorite -> {
                toggleFavorite()
                true
            }

            R.id.page_173 -> {
                openPage("scp-173")
                true
            }

            R.id.page_5000 -> {
                openPage("scp-5000")
                true
            }

            R.id.page_cn1000 -> {
                openPage("scp-cn-1000")
                true
            }

            R.id.page_3029 -> {
                openPage("scp-3029")
                true
            }

            R.id.action_toc -> {
                showTocDialog()
                true
            }

            R.id.action_settings -> {
                showSettingsDialog()
                true
            }

            R.id.action_find -> {
                showFindDialog()
                true
            }

            R.id.action_appearance -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_reload -> {
                viewModel.load(currentSlug, currentTheme, forceNetwork = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("slug")?.let { slug -> openPage(slug) }
    }

    private fun openPage(slug: String) {
        currentSlug = slug
        currentTitle = slug.uppercase()
        isFavorite = false
        lastHistoryWrite = 0L
        tocItems.clear()
        invalidateOptionsMenu()
        viewModel.load(slug, currentTheme)
    }

    private fun render(state: ReaderState) {
        when (state) {
            is ReaderState.Loading -> {
                progress.visibility = View.VISIBLE
                status.text = "加载中…"
            }

            is ReaderState.Ready -> {
                progress.visibility = View.GONE
                currentTheme = state.theme
                webView.setBackgroundColor(readerBackground())
                supportActionBar?.title = state.title
                currentTitle = state.title
                recordHistory(preferences.position(currentSlug))
                refreshFavoriteState()
                baseStatus = buildString {
                    append(state.title)
                    append(" · 图片 ")
                    append(state.stats.images)
                    append(" · 表格 ")
                    append(state.stats.tables)
                    append(" · 脚注 ")
                    append(state.stats.footnotes)
                    append(" · ")
                    append(state.source)
                }
                status.text = baseStatus
                webView.loadDataWithBaseURL(
                    ReaderShellBuilder.BASE_URL,
                    state.html,
                    "text/html",
                    "utf-8",
                    null
                )
            }

            is ReaderState.Error -> {
                progress.visibility = View.GONE
                status.text = state.message
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun recordHistory(progress: Float) {
        val now = System.currentTimeMillis()
        if (now - lastHistoryWrite < 2500L) {
            return
        }
        lastHistoryWrite = now
        val slug = currentSlug
        val title = currentTitle.ifBlank { slug.uppercase() }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                libraryDb.recordHistory(slug, title, progress)
            }
        }
    }

    private fun refreshFavoriteState() {
        val slug = currentSlug
        lifecycleScope.launch {
            val favorite = withContext(Dispatchers.IO) { libraryDb.isFavorite(slug) }
            if (slug == currentSlug) {
                isFavorite = favorite
                invalidateOptionsMenu()
            }
        }
    }

    private fun toggleFavorite() {
        val slug = currentSlug
        val title = currentTitle.ifBlank { slug.uppercase() }
        lifecycleScope.launch {
            val added = withContext(Dispatchers.IO) {
                libraryDb.toggleFavorite(slug, title)
            }
            if (slug == currentSlug) {
                isFavorite = added
                invalidateOptionsMenu()
                Toast.makeText(
                    this@MainActivity,
                    if (added) "已加入收藏" else "已取消收藏",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applySettings(settings: ReaderSettings) {
        webView.evaluateJavascript(
            "window.__readerApplySettings(" + settings.toJson() + ")",
            null
        )
    }

    private fun readerBackground(): Int {
        return if (currentTheme == ReaderTheme.DARK) Color.rgb(18, 18, 18) else Color.WHITE
    }

    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_reader_settings, null)
        val labelFont = view.findViewById<TextView>(R.id.label_font)
        val labelLine = view.findViewById<TextView>(R.id.label_line)
        val labelMargin = view.findViewById<TextView>(R.id.label_margin)
        val seekFont = view.findViewById<SeekBar>(R.id.seek_font)
        val seekLine = view.findViewById<SeekBar>(R.id.seek_line)
        val seekMargin = view.findViewById<SeekBar>(R.id.seek_margin)
        val switchImages = view.findViewById<MaterialSwitch>(R.id.switch_images)

        val current = preferences.load()
        seekFont.progress = (current.fontSize - 14f).roundToInt().coerceIn(0, 10)
        seekLine.progress = ((current.lineHeight - 1.3f) / 0.05f).roundToInt().coerceIn(0, 20)
        seekMargin.progress = (current.margin - 8).coerceIn(0, 24)
        switchImages.isChecked = current.showImages

        fun updateLabels() {
            labelFont.text = "字号 " + (14 + seekFont.progress) + "px"
            labelLine.text = "行距 " + String.format("%.2f", 1.3f + seekLine.progress * 0.05f)
            labelMargin.text = "页边距 " + (8 + seekMargin.progress) + "dp"
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }
        seekFont.setOnSeekBarChangeListener(listener)
        seekLine.setOnSeekBarChangeListener(listener)
        seekMargin.setOnSeekBarChangeListener(listener)
        updateLabels()

        AlertDialog.Builder(this)
            .setTitle("阅读设置")
            .setView(view)
            .setPositiveButton("应用") { _, _ ->
                val settings = ReaderSettings(
                    fontSize = (14 + seekFont.progress).toFloat(),
                    lineHeight = 1.3f + seekLine.progress * 0.05f,
                    showImages = switchImages.isChecked,
                    margin = 8 + seekMargin.progress
                )
                preferences.save(settings)
                applySettings(settings)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTocDialog() {
        if (tocItems.isEmpty()) {
            Toast.makeText(this, "当前页面没有目录", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = tocItems.map { item ->
            "  ".repeat((item.level - 1).coerceAtLeast(0)) + item.text
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("目录")
            .setItems(labels) { _, which ->
                val id = tocItems[which].id
                webView.evaluateJavascript(
                    "window.__readerTocGo(" + JSONObject.quote(id) + ")",
                    null
                )
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showFindDialog() {
        val input = EditText(this).apply {
            hint = "输入要查找的文字"
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("页内查找")
            .setView(input)
            .setPositiveButton("查找") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    webView.findAllAsync(query)
                    webView.findNext(true)
                }
            }
            .setNeutralButton("下一个") { _, _ -> webView.findNext(true) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun loadInternal(path: String) {
        val slug = path
            .substringBefore('#')
            .substringBefore('?')
            .trim('/')
        if (slug.isBlank()) {
            return
        }
        openPage(slug)
    }

    private fun openExternal(url: String) {
        if (url.isBlank()) {
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "没有可打开该链接的应用", Toast.LENGTH_SHORT).show()
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            runOnUiThread {
                val json = runCatching { JSONObject(message) }.getOrNull() ?: return@runOnUiThread
                when (json.optString("type")) {
                    "progress" -> {
                        if (baseStatus.isNotEmpty()) {
                            status.text = baseStatus + " · " + json.optInt("value") + "%"
                        }
                        if (json.has("ratio")) {
                            val ratio = json.optDouble("ratio", 0.0).toFloat()
                            preferences.savePosition(currentSlug, ratio)
                            recordHistory(ratio)
                        }
                    }

                    "position" -> {
                        if (json.has("ratio")) {
                            val ratio = json.optDouble("ratio", 0.0).toFloat()
                            preferences.savePosition(currentSlug, ratio)
                            recordHistory(ratio)
                        }
                    }

                    "toc" -> {
                        tocItems.clear()
                        val items = json.optJSONArray("items")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue
                                val id = item.optString("id")
                                val text = item.optString("text")
                                if (id.isNotEmpty() && text.isNotEmpty()) {
                                    tocItems.add(TocItem(id, text, item.optInt("level", 1)))
                                }
                            }
                        }
                        invalidateOptionsMenu()
                    }

                    "openInternal" -> {
                        val path = json.optString("path")
                        if (path.isNotBlank()) {
                            loadInternal(path)
                        }
                    }

                    "openExternal" -> openExternal(json.optString("url"))
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("Reader")
        webView.destroy()
        super.onDestroy()
    }
}
