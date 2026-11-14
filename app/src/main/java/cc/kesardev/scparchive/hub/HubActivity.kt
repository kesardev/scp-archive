package cc.kesardev.scparchive.hub

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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator

class HubActivity : BaseActivity() {

    private val viewModel: HubViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: LinearProgressIndicator
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: HubAdapter

    private var path: String = ""
    private var mode: HubMode = HubMode.GENERIC
    private var isStatic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_hub)
        applySystemBarInsets()

        path = intent.getStringExtra("path") ?: ""
        mode = HubMode.from(intent.getStringExtra("mode"))
        val links = intent.getStringArrayListExtra("links")
        val linkTitles = intent.getStringArrayListExtra("link_titles")

        toolbar = findViewById(R.id.toolbar)
        progress = findViewById(R.id.hub_progress)
        status = findViewById(R.id.hub_status)
        list = findViewById(R.id.hub_list)

        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra("title") ?: "中心页"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = HubAdapter(
            onEntry = { entry -> openEntry(entry, null) },
            onChild = { child -> openEntry(HubEntry(child.slug, child.label), child.anchor) }
        )
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        if (!links.isNullOrEmpty()) {
            isStatic = true
            val entries = ArrayList<HubEntry>(links.size)
            for ((index, link) in links.withIndex()) {
                val title = linkTitles?.getOrNull(index) ?: link
                entries.add(HubEntry(link, title))
            }
            progress.visibility = View.GONE
            status.text = entries.size.toString() + " 个入口"
            adapter.submitList(entries)
        } else {
            viewModel.state.observe(this) { state -> render(state) }
            viewModel.load(path, mode)
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
        if (!isStatic) {
            menuInflater.inflate(R.menu.menu_hub, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.action_refresh -> {
                viewModel.load(path, mode, force = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun render(state: HubState) {
        when (state) {
            is HubState.Loading -> {
                progress.visibility = View.VISIBLE
                status.text = "加载中心页…"
            }

            is HubState.Ready -> {
                progress.visibility = View.GONE
                status.text = state.entries.size.toString() + " 条 · " + state.source
                adapter.submitList(state.entries)
                list.scrollToPosition(0)
            }

            is HubState.Error -> {
                progress.visibility = View.GONE
                status.text = state.message
            }
        }
    }

    private fun openEntry(entry: HubEntry, anchor: String?) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", entry.slug)
            .putExtra("anchor", anchor)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}