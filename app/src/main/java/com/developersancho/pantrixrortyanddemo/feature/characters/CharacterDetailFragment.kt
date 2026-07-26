package com.developersancho.pantrixrortyanddemo.feature.characters

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentCharacterDetailBinding
import com.developersancho.pantrixrortyanddemo.feature.shared.Row
import com.developersancho.pantrixrortyanddemo.feature.shared.RowAdapter
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CharacterDetailFragment : Fragment(R.layout.fragment_character_detail) {

    private val viewModel: CharacterDetailViewModel by viewModels()
    private var binding: FragmentCharacterDetailBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCharacterDetailBinding.bind(view).also { this.binding = it }

        val adapter = RowAdapter { row ->
            findNavController().navigate(
                R.id.action_global_episodeDetail,
                Bundle().apply { putInt("episodeId", row.id) }
            )
        }
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    state.character?.let { renderHeader(it) }
                    adapter.submitList(state.episodes.map { Row(it.id, it.name, it.summary) })
                    binding.error.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    binding.error.text = state.error.orEmpty()
                }
            }
        }
    }

    private fun renderHeader(character: RMCharacter) {
        val binding = binding ?: return
        binding.name.text = character.name
        binding.image.load(character.image)
        binding.meta.text = listOf(
            "Status: ${character.status}",
            "Species: ${character.species}",
            "Type: ${character.type.ifBlank { "—" }}",
            "Gender: ${character.gender}",
            "Origin: ${character.origin?.name ?: "—"}",
            "Location: ${character.location?.name ?: "—"}"
        ).joinToString("\n")
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
