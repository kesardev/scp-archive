package cc.kesardev.scparchive.hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.R
import com.google.android.material.chip.Chip
import kotlin.math.roundToInt

class HubAdapter(
    private val onEntry: (HubEntry) -> Unit,
    private val onChild: (HubChild) -> Unit
) : ListAdapter<HubEntry, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).children.isEmpty()) TYPE_ENTRY else TYPE_SECTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SECTION) {
            SectionViewHolder(
                inflater.inflate(R.layout.item_hub_section, parent, false),
                onEntry,
                onChild
            )
        } else {
            EntryViewHolder(
                inflater.inflate(R.layout.item_hub, parent, false),
                onEntry
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is SectionViewHolder) {
            holder.bind(item)
        } else if (holder is EntryViewHolder) {
            holder.bind(item)
        }
    }

    class EntryViewHolder(
        itemView: View,
        private val onEntry: (HubEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.hub_title)
        private val slug: TextView = itemView.findViewById(R.id.hub_slug)

        fun bind(entry: HubEntry) {
            title.text = entry.title
            slug.text = entry.slug
            itemView.setOnClickListener { onEntry(entry) }
        }
    }

    class SectionViewHolder(
        itemView: View,
        private val onEntry: (HubEntry) -> Unit,
        private val onChild: (HubChild) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.hub_section_title)
        private val children: LinearLayout = itemView.findViewById(R.id.hub_section_children)

        fun bind(entry: HubEntry) {
            title.text = entry.title
            children.removeAllViews()
            val margin = (8 * itemView.resources.displayMetrics.density).roundToInt()
            for (child in entry.children) {
                val chip = Chip(itemView.context).apply {
                    text = child.label
                    isCheckable = false
                    isClickable = true
                    textSize = 13f
                    setOnClickListener { onChild(child) }
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = margin
                chip.layoutParams = params
                children.addView(chip)
            }
            itemView.setOnClickListener { onEntry(entry) }
        }
    }

    companion object {
        private const val TYPE_ENTRY = 0
        private const val TYPE_SECTION = 1

        private val DIFF = object : DiffUtil.ItemCallback<HubEntry>() {
            override fun areItemsTheSame(oldItem: HubEntry, newItem: HubEntry): Boolean {
                return oldItem.slug == newItem.slug
            }

            override fun areContentsTheSame(oldItem: HubEntry, newItem: HubEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}