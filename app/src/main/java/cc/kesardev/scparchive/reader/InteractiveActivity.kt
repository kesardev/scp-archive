package cc.kesardev.scparchive.reader

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class InteractiveActivity : BaseActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var webView: WebView

    private val repository by lazy { HtmlBlockRepository(this) }
    private var blockUrl: String = ""
    private var articleTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interactive)
        applySystemBarInsets()

        blockUrl = intent.getStringExtra("url").orEmpty()
        articleTitle = intent.getStringExtra("title").orEmpty()

        toolbar = findViewById(R.id.toolbar)
        progress = findViewById(R.id.interactive_progress)
        status = findViewById(R.id.interactive_status)
        webView = findViewById(R.id.interactive_web_view)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "互动版"
        supportActionBar?.subtitle = articleTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        configureWebView()
        load(force = false)
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

    private fun configureWebView() {
        webView.setBackgroundColor(Color.WHITE)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(false)
            setAcceptThirdPartyCookies(webView, false)
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return handle(request.url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handle(Uri.parse(url))
            }
        }
    }

    private fun handle(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "javascript" || scheme == "about" || scheme == "data") {
            return false
        }
        if (scheme != "http" && scheme != "https") {
            return true
        }
        val host = uri.host?.lowercase()
        if (host == "scp-wiki-cn.wikidot.com" ||
            host == "scp-wiki.wikidot.com" ||
            host == "scp-wiki-cn.wdfiles.com" ||
            host == "scp-wikicn.com" ||
            host == "scpwiki.com"
        ) {
            return false
        }
        openExternal(uri.toString())
        return true
    }

    private fun load(force: Boolean) {
        if (blockUrl.isBlank()) {
            status.text = "缺少互动内容地址"
            return
        }
        progress.visibility = View.VISIBLE
        status.text = "加载互动内容…"
        lifecycleScope.launch {
            try {
                val page = repository.load(blockUrl, force)
                webView.loadDataWithBaseURL(
                    ReaderShellBuilder.BASE_URL,
                    repository.prepare(page.html),
                    "text/html",
                    "utf-8",
                    null
                )
                status.text = "互动内容 · " + page.source
            } catch (error: Exception) {
                status.text = "加载失败：" + (error.message ?: "未知错误")
                Toast.makeText(
                    this@InteractiveActivity,
                    "互动内容加载失败，可用菜单在浏览器打开",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_interactive, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_interactive_browser -> {
                openExternal(blockUrl)
                true
            }

            R.id.action_interactive_refresh -> {
                load(force = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
