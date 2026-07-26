package com.developersancho.pantrixrortyanddemo.feature.locations

import com.developersancho.pantrixrortyanddemo.feature.shared.Page
import com.developersancho.pantrixrortyanddemo.feature.shared.PagedListViewModel
import com.developersancho.pantrixrortyanddemo.feature.shared.RickMortyRepository
import com.developersancho.pantrixrortyanddemo.network.model.RMLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: RickMortyRepository
) : PagedListViewModel<RMLocation>() {

    override val screenName = "Locations"

    init {
        start()
    }

    override suspend fun fetch(page: Int, query: String?): Page<RMLocation> =
        repository.locations(page, query)
}
