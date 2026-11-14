package cc.kesardev.scparchive.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.R

class CatalogAdapter(
    private val onClick: (CatalogEntry) -> Unit
) : ListAdapter<CatalogEntry, CatalogAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalog, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onClick: (CatalogEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.catalog_title)
        private val id: TextView = itemView.findViewById(R.id.catalog_id)

        fun bind(entry: CatalogEntry) {
            title.text = entry.title
            id.text = entry.id.uppercase()
            itemView.setOnClickListener { onClick(entry) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CatalogEntry>() {
            override fun areItemsTheSame(oldItem: CatalogEntry, newItem: CatalogEntry): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CatalogEntry, newItem: CatalogEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}
