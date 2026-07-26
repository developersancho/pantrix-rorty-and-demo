package com.developersancho.pantrixrortyanddemo.feature.locations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersancho.pantrixrortyanddemo.feature.shared.RickMortyRepository
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.developersancho.pantrixrortyanddemo.network.model.RMLocation
import com.pantrix.api.Pantrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationDetailUiState(
    val item: RMLocation? = null,
    val characters: List<RMCharacter> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    private val repository: RickMortyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Int = savedStateHandle["locationId"] ?: 0

    private val _state = MutableStateFlow(LocationDetailUiState())
    val state: StateFlow<LocationDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val item = repository.location(id)
                // A second request on purpose: it exercises the batch endpoint AND gives the HTTP
                // tracking something with a different shape to record.
                item to repository.charactersByUrls(item.residents)
            }
                .onSuccess { (item, characters) ->
                    _state.value = LocationDetailUiState(item = item, characters = characters)
                }
                .onFailure {
                    Pantrix.trackException(it, mapOf("screen" to "LocationDetail", "id" to id))
                    _state.value = LocationDetailUiState(error = it.message ?: "Could not load")
                }
        }
    }
}
