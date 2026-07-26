package com.developersancho.pantrixrortyanddemo.feature.characters

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentCharacterDetailBinding
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CharacterDetailFragment : Fragment(R.layout.fragment_character_detail) {

    private val viewModel: CharacterDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCharacterDetailBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    state.character?.let { render(binding, it) }
                    binding.error.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    binding.error.text = state.error.orEmpty()
                }
            }
        }
    }

    private fun render(binding: FragmentCharacterDetailBinding, character: RMCharacter) {
        binding.name.text = character.name
        binding.image.load(character.image)
        binding.rows.removeAllViews()
        listOf(
            "Status" to character.status,
            "Species" to character.species,
            "Type" to character.type.ifBlank { "—" },
            "Gender" to character.gender,
            "Origin" to (character.origin?.name ?: "—"),
            "Location" to (character.location?.name ?: "—"),
            "Episodes" to character.episode.size.toString()
        ).forEach { (label, value) ->
            binding.rows.addView(TextView(requireContext()).apply {
                text = "$label: $value"
                setPadding(0, 6, 0, 6)
            })
        }
    }
}
