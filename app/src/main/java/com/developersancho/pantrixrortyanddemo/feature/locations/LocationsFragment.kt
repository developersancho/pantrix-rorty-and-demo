package com.developersancho.pantrixrortyanddemo.feature.locations

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.feature.shared.PagedListFragment
import com.developersancho.pantrixrortyanddemo.feature.shared.Row
import com.developersancho.pantrixrortyanddemo.network.model.RMLocation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationsFragment : PagedListFragment<RMLocation>() {

    override val viewModel: LocationsViewModel by viewModels()
    override val searchHint get() = getString(R.string.locations_search_hint)

    // No image on this endpoint — RowAdapter hides the ImageView when the url is blank.
    override fun toRow(item: RMLocation) = Row(id = item.id, title = item.name, subtitle = item.summary)

    override fun onRowClick(row: Row) = findNavController().navigate(
        R.id.action_locations_to_detail,
        Bundle().apply { putInt("locationId", row.id) }
    )
}
