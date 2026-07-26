package com.developersancho.pantrixrortyanddemo.feature.characters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.pantrix.api.Pantrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterDetailUiState(
    val character: RMCharacter? = null,
    val error: String? = null
)

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    private val repository: CharactersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: Int = savedStateHandle["characterId"] ?: 0

    private val _state = MutableStateFlow(CharacterDetailUiState())
    val state: StateFlow<CharacterDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.character(characterId) }
                .onSuccess { _state.value = CharacterDetailUiState(character = it) }
                .onFailure {
                    Pantrix.trackException(it, mapOf("screen" to "CharacterDetail", "id" to characterId))
                    _state.value = CharacterDetailUiState(error = it.message ?: "Could not load")
                }
        }
    }
}
