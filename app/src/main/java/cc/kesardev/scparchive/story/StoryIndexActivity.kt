package cc.kesardev.scparchive.story

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

class StoryIndexActivity : BaseActivity() {

    private val viewModel: StoryIndexViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: LinearProgressIndicator
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: StoryIndexAdapter

    private var path: String = ""
    private var mode: StoryIndexMode = StoryIndexMode.NAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_story_index)
        applySystemBarInsets()

        path = intent.getStringExtra("path") ?: "tales-by-page-name"
        mode = try {
            StoryIndexMode.valueOf(intent.getStringExtra("mode") ?: "NAME")
        } catch (_: Exception) {
            StoryIndexMode.NAME
        }

        toolbar = findViewById(R.id.toolbar)
        progress = findViewById(R.id.story_index_progress)
        status = findViewById(R.id.story_index_status)
        list = findViewById(R.id.story_index_list)

        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra("title") ?: "故事索引"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = StoryIndexAdapter { entry -> openEntry(entry) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        viewModel.state.observe(this) { state -> render(state) }
        viewModel.load(path, mode)
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
        menuInflater.inflate(R.menu.menu_story_index, menu)
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

    private fun render(state: StoryIndexState) {
        when (state) {
            is StoryIndexState.Loading -> {
                progress.visibility = View.VISIBLE
                status.text = "加载故事索引…"
            }

            is StoryIndexState.Ready -> {
                progress.visibility = View.GONE
                val total = state.groups.sumOf { it.entries.size }
                status.text = total.toString() + " 条 · " + state.groups.size +
                    " 个分组 · " + state.source
                adapter.submitGroups(state.groups)
                list.scrollToPosition(0)
            }

            is StoryIndexState.Error -> {
                progress.visibility = View.GONE
                status.text = state.message
            }
        }
    }

    private fun openEntry(entry: StoryEntry) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("slug", entry.slug)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }
}