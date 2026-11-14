package cc.kesardev.scparchive.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.kesardev.scparchive.R

sealed interface StoryRow {
    val key: String

    data class Header(
        val label: String,
        val count: Int
    ) : StoryRow {
        override val key: String = "header:" + label
    }

    data class Entry(val entry: StoryEntry) : StoryRow {
        override val key: String = "entry:" + entry.slug
    }
}

class StoryIndexAdapter(
    private val onEntry: (StoryEntry) -> Unit
) : ListAdapter<StoryRow, RecyclerView.ViewHolder>(DIFF) {

    fun submitGroups(groups: List<StoryGroup>) {
        val rows = ArrayList<StoryRow>()
        for (group in groups) {
            rows.add(StoryRow.Header(StoryIndexParser.displayLabel(group.label), group.entries.size))
            for (entry in group.entries) {
                rows.add(StoryRow.Entry(entry))
            }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is StoryRow.Header) TYPE_HEADER else TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_story_group, parent, false))
        } else {
            EntryHolder(inflater.inflate(R.layout.item_hub, parent, false), onEntry)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is StoryRow.Header -> (holder as HeaderHolder).bind(row)
            is StoryRow.Entry -> (holder as EntryHolder).bind(row.entry)
        }
    }

    private class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.story_group_label)
        private val count: TextView = itemView.findViewById(R.id.story_group_count)

        fun bind(row: StoryRow.Header) {
            label.text = row.label
            count.text = row.count.toString() + " 条"
        }
    }

    private class EntryHolder(
        itemView: View,
        private val onEntry: (StoryEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.hub_title)
        private val slug: TextView = itemView.findViewById(R.id.hub_slug)

        fun bind(entry: StoryEntry) {
            title.text = entry.title
            slug.text = entry.slug
            itemView.setOnClickListener { onEntry(entry) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1

        private val DIFF = object : DiffUtil.ItemCallback<StoryRow>() {
            override fun areItemsTheSame(oldItem: StoryRow, newItem: StoryRow): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: StoryRow, newItem: StoryRow): Boolean {
                return oldItem == newItem
            }
        }
    }
}