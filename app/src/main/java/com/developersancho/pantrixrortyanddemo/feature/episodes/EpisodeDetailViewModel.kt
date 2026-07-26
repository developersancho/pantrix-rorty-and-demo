package com.developersancho.pantrixrortyanddemo.feature.episodes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersancho.pantrixrortyanddemo.feature.shared.RickMortyRepository
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.developersancho.pantrixrortyanddemo.network.model.RMEpisode
import com.pantrix.api.Pantrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpisodeDetailUiState(
    val item: RMEpisode? = null,
    val characters: List<RMCharacter> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class EpisodeDetailViewModel @Inject constructor(
    private val repository: RickMortyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Int = savedStateHandle["episodeId"] ?: 0

    private val _state = MutableStateFlow(EpisodeDetailUiState())
    val state: StateFlow<EpisodeDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val item = repository.episode(id)
                // A second request on purpose: it exercises the batch endpoint AND gives the HTTP
                // tracking something with a different shape to record.
                item to repository.charactersByUrls(item.characters)
            }
                .onSuccess { (item, characters) ->
                    _state.value = EpisodeDetailUiState(item = item, characters = characters)
                }
                .onFailure {
                    Pantrix.trackException(it, mapOf("screen" to "EpisodeDetail", "id" to id))
                    _state.value = EpisodeDetailUiState(error = it.message ?: "Could not load")
                }
        }
    }
}
