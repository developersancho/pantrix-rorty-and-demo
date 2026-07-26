package com.developersancho.pantrixrortyanddemo.feature.episodes

import com.developersancho.pantrixrortyanddemo.feature.shared.Page
import com.developersancho.pantrixrortyanddemo.feature.shared.PagedListViewModel
import com.developersancho.pantrixrortyanddemo.feature.shared.RickMortyRepository
import com.developersancho.pantrixrortyanddemo.network.model.RMEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpisodesViewModel @Inject constructor(
    private val repository: RickMortyRepository
) : PagedListViewModel<RMEpisode>() {

    override val screenName = "Episodes"

    init {
        start()
    }

    override suspend fun fetch(page: Int, query: String?): Page<RMEpisode> =
        repository.episodes(page, query)
}
