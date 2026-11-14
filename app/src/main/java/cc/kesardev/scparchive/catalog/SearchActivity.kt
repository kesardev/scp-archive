package cc.kesardev.scparchive.catalog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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

class SearchActivity : BaseActivity() {

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var input: EditText
    private lateinit var searchButton: Button
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: CatalogAdapter

    private var lastStatus: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_search)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        input = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)
        status = findViewById(R.id.search_status)
        list = findViewById(R.id.search_list)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "搜索"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = CatalogAdapter { entry -> openEntry(entry) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        searchButton.setOnClickListener { doSearch() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else {
                false
            }
        }

        viewModel.results.observe(this) { results ->
            adapter.submitList(results)
            status.text = if (input.text.isBlank()) {
                "索引 " + (viewModel.indexCount.value ?: 0) + " 条"
            } else {
                "找到 " + results.size + " 条 · 索引 " + (viewModel.indexCount.value ?: 0) + " 条"
            }
        }
        viewModel.indexCount.observe(this) { count ->
            lastStatus = "索引 " + count + " 条"
            if (input.text.isBlank()) {
                status.text = if (count == 0) "索引为空，请先在系列目录同步" else lastStatus
            }
        }

        viewModel.refreshCount()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime))
            insets
        }
    }

    private fun doSearch() {
        val query = input.text.toString().trim()
        if (query.isEmpty()) {
            status.text = "请输入编号或标题"
            return
        }
        status.text = "搜索中…"
        viewModel.search(query)
    }

    private fun openEntry(entry: CatalogEntry) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("slug", entry.id)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}
