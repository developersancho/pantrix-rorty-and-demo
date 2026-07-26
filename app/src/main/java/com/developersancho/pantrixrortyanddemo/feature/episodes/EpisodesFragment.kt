package com.developersancho.pantrixrortyanddemo.feature.episodes

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.feature.shared.PagedListFragment
import com.developersancho.pantrixrortyanddemo.feature.shared.Row
import com.developersancho.pantrixrortyanddemo.network.model.RMEpisode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EpisodesFragment : PagedListFragment<RMEpisode>() {

    override val viewModel: EpisodesViewModel by viewModels()
    override val searchHint get() = getString(R.string.episodes_search_hint)

    // No image on this endpoint — RowAdapter hides the ImageView when the url is blank.
    override fun toRow(item: RMEpisode) = Row(id = item.id, title = item.name, subtitle = item.summary)

    override fun onRowClick(row: Row) = findNavController().navigate(
        R.id.action_episodes_to_detail,
        Bundle().apply { putInt("episodeId", row.id) }
    )
}
