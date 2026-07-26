package com.developersancho.pantrixrortyanddemo.feature.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.pantrix.api.Pantrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharactersUiState(
    val characters: List<RMCharacter> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val query: String = ""
) {
    val isEmpty: Boolean get() = !isLoading && error == null && characters.isEmpty()
}

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val repository: CharactersRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CharactersUiState())
    val state: StateFlow<CharactersUiState> = _state.asStateFlow()

    private var page = 1
    private var hasMore = true
    private var searchJob: Job? = null

    init {
        load(reset = true)
    }

    /** Debounced so a fast typist produces one request, not one per keystroke. */
    fun search(query: String) {
        if (query == _state.value.query) return
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!hasMore || current.isLoading || current.isLoadingMore) return
        load(reset = false)
    }

    fun retry() = load(reset = true)

    private fun load(reset: Boolean) {
        if (reset) {
            page = 1
            hasMore = true
        }
        _state.update {
            if (reset) it.copy(isLoading = true, error = null) else it.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            runCatching { repository.characters(page, _state.value.query) }
                .onSuccess { result ->
                    hasMore = result.hasMore
                    page += 1
                    _state.update { s ->
                        s.copy(
                            characters = if (reset) result.characters else s.characters + result.characters,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    // A handled failure: the app recovers, but Pantrix should still see it — this is
                    // exactly the case `trackException` exists for.
                    Pantrix.trackException(throwable, mapOf("screen" to "Characters", "page" to page))
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = throwable.message ?: "Something went wrong"
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
