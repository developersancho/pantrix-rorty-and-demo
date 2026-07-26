package com.developersancho.pantrixrortyanddemo.feature.characters

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.feature.shared.PagedListFragment
import com.developersancho.pantrixrortyanddemo.feature.shared.Row
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CharactersFragment : PagedListFragment<RMCharacter>() {

    override val viewModel: CharactersViewModel by viewModels()
    override val searchHint get() = getString(R.string.characters_search_hint)

    override fun toRow(item: RMCharacter) =
        Row(id = item.id, title = item.name, subtitle = item.summary, imageUrl = item.image)

    override fun onRowClick(row: Row) = findNavController().navigate(
        R.id.action_characters_to_detail,
        Bundle().apply { putInt("characterId", row.id) }
    )
}
