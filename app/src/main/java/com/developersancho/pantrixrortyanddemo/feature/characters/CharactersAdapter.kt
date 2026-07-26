package com.developersancho.pantrixrortyanddemo.feature.characters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.developersancho.pantrixrortyanddemo.databinding.ItemCharacterBinding
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter

class CharactersAdapter(
    private val onClick: (RMCharacter) -> Unit
) : ListAdapter<RMCharacter, CharactersAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(
        private val binding: ItemCharacterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(character: RMCharacter) = with(binding) {
            name.text = character.name
            subtitle.text = character.summary
            avatar.load(character.image)
            root.setOnClickListener { onClick(character) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RMCharacter>() {
        override fun areItemsTheSame(oldItem: RMCharacter, newItem: RMCharacter) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RMCharacter, newItem: RMCharacter) = oldItem == newItem
    }
}
