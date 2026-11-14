package cc.kesardev.scparchive.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LibraryAdapter(
    private val onClick: (LibraryEntry) -> Unit,
    private val onRemove: (LibraryEntry) -> Unit
) : ListAdapter<LibraryEntry, LibraryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library, parent, false)
        return ViewHolder(view, onClick, onRemove)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onClick: (LibraryEntry) -> Unit,
        private val onRemove: (LibraryEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.library_title)
        private val meta: TextView = itemView.findViewById(R.id.library_meta)
        private val remove: TextView = itemView.findViewById(R.id.library_remove)
        private val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun bind(entry: LibraryEntry) {
            title.text = entry.title
            val progress = if (entry.progress > 0f) {
                " · " + (entry.progress * 100).toInt() + "%"
            } else {
                ""
            }
            meta.text = entry.slug.uppercase() + " · " + format.format(Date(entry.timestamp)) + progress
            itemView.setOnClickListener { onClick(entry) }
            remove.setOnClickListener { onRemove(entry) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LibraryEntry>() {
            override fun areItemsTheSame(oldItem: LibraryEntry, newItem: LibraryEntry): Boolean {
                return oldItem.slug == newItem.slug
            }

            override fun areContentsTheSame(oldItem: LibraryEntry, newItem: LibraryEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}
