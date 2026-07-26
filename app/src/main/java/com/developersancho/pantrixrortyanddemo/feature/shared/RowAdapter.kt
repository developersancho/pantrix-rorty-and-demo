package com.developersancho.pantrixrortyanddemo.feature.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.developersancho.pantrixrortyanddemo.databinding.ItemRowBinding

/** What a list row shows, independent of which endpoint produced it. */
data class Row(val id: Int, val title: String, val subtitle: String, val imageUrl: String? = null)

/**
 * One adapter for all three tabs. Characters, Episodes and Locations render the same row — an
 * optional image, a title and a subtitle — so they share this and differ only in the mapping to [Row].
 */
class RowAdapter(
    private val onClick: (Row) -> Unit
) : ListAdapter<Row, RowAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: Row) = with(binding) {
            name.text = row.title
            subtitle.text = row.subtitle
            if (row.imageUrl.isNullOrBlank()) {
                avatar.visibility = View.GONE
            } else {
                avatar.visibility = View.VISIBLE
                avatar.load(row.imageUrl)
            }
            root.setOnClickListener { onClick(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(oldItem: Row, newItem: Row) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
    }
}
