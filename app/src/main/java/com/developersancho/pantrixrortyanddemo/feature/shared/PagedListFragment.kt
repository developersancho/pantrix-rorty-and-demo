package com.developersancho.pantrixrortyanddemo.feature.shared

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentPagedListBinding
import com.pantrix.api.Pantrix
import com.pantrix.core.processors.event.data.interaction.InteractionType
import kotlinx.coroutines.launch

/**
 * The list half of every tab: search box, infinite scroll, empty/error states. Subclasses supply the
 * ViewModel, the search hint, and what a tap does.
 */
abstract class PagedListFragment<T> : Fragment(R.layout.fragment_paged_list) {

    private var binding: FragmentPagedListBinding? = null

    protected abstract val viewModel: PagedListViewModel<T>
    protected abstract val searchHint: String
    protected abstract fun toRow(item: T): Row
    protected abstract fun onRowClick(row: Row)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPagedListBinding.bind(view).also { this.binding = it }

        val adapter = RowAdapter { row ->
            Pantrix.trackInteraction(InteractionType.CLICK, mapOf("target" to "list_row"))
            onRowClick(row)
        }
        binding.searchLayout.hint = searchHint
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val manager = recyclerView.layoutManager as LinearLayoutManager
                // Prefetch a page before the user reaches the bottom, so scrolling never stalls.
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - PREFETCH_DISTANCE) {
                    viewModel.loadMore()
                }
            }
        })
        binding.searchInput.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.retryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items.map(::toRow))
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    val message = state.error ?: getString(R.string.list_empty).takeIf { state.isEmpty }
                    binding.emptyState.visibility = if (message != null) View.VISIBLE else View.GONE
                    binding.emptyText.text = message.orEmpty()
                    binding.retryButton.visibility = if (state.error != null) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        // The adapter outlives the view otherwise and leaks the RecyclerView.
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val PREFETCH_DISTANCE = 4
    }
}
