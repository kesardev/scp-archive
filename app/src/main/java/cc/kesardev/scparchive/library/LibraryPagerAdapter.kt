package cc.kesardev.scparchive.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.R

class LibraryPagerAdapter(
    private val onOpen: (LibraryEntry) -> Unit,
    private val onRemove: (LibraryTab, LibraryEntry) -> Unit
) : RecyclerView.Adapter<LibraryPagerAdapter.PageHolder>() {

    private var favorites: List<LibraryEntry> = emptyList()
    private var history: List<LibraryEntry> = emptyList()

    fun submit(tab: LibraryTab, items: List<LibraryEntry>) {
        if (tab == LibraryTab.FAVORITES) {
            favorites = items
        } else {
            history = items
        }
        notifyItemChanged(tab.ordinal)
    }

    override fun getItemCount(): Int = 2

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_page, parent, false)
        val tab = if (viewType == 0) LibraryTab.FAVORITES else LibraryTab.HISTORY
        return PageHolder(view, tab, onOpen, onRemove)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(if (position == 0) favorites else history)
    }

    class PageHolder(
        itemView: View,
        private val tab: LibraryTab,
        onOpen: (LibraryEntry) -> Unit,
        onRemove: (LibraryTab, LibraryEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val status: TextView = itemView.findViewById(R.id.library_page_status)
        private val list: RecyclerView = itemView.findViewById(R.id.library_page_list)
        private val adapter = LibraryAdapter(
            onClick = onOpen,
            onRemove = { entry -> onRemove(tab, entry) }
        )

        init {
            list.layoutManager = LinearLayoutManager(itemView.context)
            list.adapter = adapter
        }

        fun bind(items: List<LibraryEntry>) {
            adapter.submitList(items)
            status.text = when {
                items.isEmpty() && tab == LibraryTab.FAVORITES -> "暂无收藏"
                items.isEmpty() -> "暂无阅读历史"
                tab == LibraryTab.FAVORITES -> items.size.toString() + " 条收藏"
                else -> items.size.toString() + " 条历史"
            }
        }
    }
}