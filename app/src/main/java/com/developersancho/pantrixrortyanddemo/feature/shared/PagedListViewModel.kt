package com.developersancho.pantrixrortyanddemo.feature.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pantrix.api.Pantrix
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One page plus whether the API has more — the shape every Rick & Morty list endpoint returns. */
data class Page<T>(val items: List<T>, val hasMore: Boolean)

data class PagedListUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val query: String = ""
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()
}

/**
 * Search + infinite scroll + error handling, written once. Characters, Episodes and Locations differ
 * only in which endpoint they call, so they differ only in [fetch] — three copies of this logic would
 * have drifted, and the debounce/guard details are exactly the kind that drift silently.
 */
abstract class PagedListViewModel<T> : ViewModel() {

    private val _state = MutableStateFlow(PagedListUiState<T>())
    val state: StateFlow<PagedListUiState<T>> = _state.asStateFlow()

    private var page = 1
    private var hasMore = true
    private var searchJob: Job? = null

    /** The screen name reported with a failure, so the dashboard says which list broke. */
    protected abstract val screenName: String

    protected abstract suspend fun fetch(page: Int, query: String?): Page<T>

    init {
        // Subclasses have finished construction by the time this runs; the first load is kicked off
        // from `start()` rather than here so an overridden `fetch` is safely initialized.
    }

    /** Call once from the subclass's init block. */
    protected fun start() = load(reset = true)

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
            runCatching { fetch(page, _state.value.query.takeIf { it.isNotBlank() }) }
                .onSuccess { result ->
                    hasMore = result.hasMore
                    page += 1
                    _state.update { s ->
                        s.copy(
                            items = if (reset) result.items else s.items + result.items,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    // A handled failure: the app recovers, but Pantrix should still see it.
                    Pantrix.trackException(throwable, mapOf("screen" to screenName, "page" to page))
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
