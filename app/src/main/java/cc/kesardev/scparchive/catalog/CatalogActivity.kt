package cc.kesardev.scparchive.catalog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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

class CatalogActivity : BaseActivity() {

    private val viewModel: CatalogViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spinner: Spinner
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: CatalogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_catalog)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        spinner = findViewById(R.id.series_spinner)
        progress = findViewById(R.id.catalog_progress)
        status = findViewById(R.id.catalog_status)
        list = findViewById(R.id.catalog_list)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "系列目录"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            SeriesCatalog.all.map { it.name }
        )
        val startIndex = SeriesCatalog.all
            .indexOfFirst { it.id == intent.getStringExtra("series_id") }
            .coerceAtLeast(0)
        spinner.setSelection(startIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.load(SeriesCatalog.all[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        adapter = CatalogAdapter { entry -> openEntry(entry) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        viewModel.state.observe(this) { state -> render(state) }
        viewModel.syncing.observe(this) { syncing ->
            progress.visibility = if (syncing) View.VISIBLE else View.GONE
        }
        viewModel.syncMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                status.text = message
            }
        }
        viewModel.load(SeriesCatalog.all[startIndex])
        if (intent.getBooleanExtra("sync", false)) {
            viewModel.syncAll()
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

    private fun render(state: CatalogState) {
        when (state) {
            is CatalogState.Loading -> {
                progress.visibility = View.VISIBLE
                status.text = "加载目录…"
            }

            is CatalogState.Ready -> {
                progress.visibility = View.GONE
                status.text = state.entries.size.toString() + " 条 · " + state.source
                adapter.submitList(state.entries)
                list.scrollToPosition(0)
            }

            is CatalogState.Error -> {
                progress.visibility = View.GONE
                status.text = state.message
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
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