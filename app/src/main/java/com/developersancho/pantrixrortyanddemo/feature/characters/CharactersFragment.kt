package com.developersancho.pantrixrortyanddemo.feature.characters

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentCharactersBinding
import com.pantrix.api.Pantrix
import com.pantrix.core.processors.event.data.interaction.InteractionType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CharactersFragment : Fragment(R.layout.fragment_characters) {

    private val viewModel: CharactersViewModel by viewModels()
    private var binding: FragmentCharactersBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCharactersBinding.bind(view).also { this.binding = it }

        val adapter = CharactersAdapter { character ->
            // A first-class interaction event — separate from the screen view the SDK tracks itself.
            Pantrix.trackInteraction(InteractionType.CLICK, mapOf("target" to "character_row"))
            findNavController().navigate(
                R.id.action_characters_to_detail,
                Bundle().apply { putInt("characterId", character.id) }
            )
        }
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val manager = recyclerView.layoutManager as LinearLayoutManager
                // Prefetch a page before the user hits the bottom, so scrolling never stalls.
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
                    adapter.submitList(state.characters)
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    val message = state.error ?: getString(R.string.characters_empty).takeIf { state.isEmpty }
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
