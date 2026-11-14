package cc.kesardev.scparchive.series

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.MainActivity
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.catalog.CatalogAdapter
import cc.kesardev.scparchive.catalog.CatalogEntry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator

class TagActivity : BaseActivity() {

    private val viewModel: TagViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: LinearProgressIndicator
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: CatalogAdapter

    private var tag: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_tag)
        applySystemBarInsets()

        tag = intent.getStringExtra("tag") ?: "故事"
        val title = intent.getStringExtra("title") ?: tag

        toolbar = findViewById(R.id.toolbar)
        progress = findViewById(R.id.tag_progress)
        status = findViewById(R.id.tag_status)
        list = findViewById(R.id.tag_list)

        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = CatalogAdapter { entry -> openEntry(entry) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        viewModel.state.observe(this) { state -> render(state) }
        viewModel.load(tag)
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
        menuInflater.inflate(R.menu.menu_tag, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.action_refresh -> {
                viewModel.load(tag, force = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun render(state: TagState) {
        when (state) {
            is TagState.Loading -> {
                progress.visibility = View.VISIBLE
                status.text = "加载标签页…"
            }

            is TagState.Ready -> {
                progress.visibility = View.GONE
                status.text = state.entries.size.toString() + " 条 · " + state.source
                adapter.submitList(state.entries)
                list.scrollToPosition(0)
            }

            is TagState.Error -> {
                progress.visibility = View.GONE
                status.text = state.message
            }
        }
    }

    private fun openEntry(entry: CatalogEntry) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", entry.id)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}