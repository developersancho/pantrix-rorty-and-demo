package com.developersancho.pantrixrortyanddemo.feature.locations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentRefDetailBinding
import com.developersancho.pantrixrortyanddemo.feature.shared.Row
import com.developersancho.pantrixrortyanddemo.feature.shared.RowAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationDetailFragment : Fragment(R.layout.fragment_ref_detail) {

    private val viewModel: LocationDetailViewModel by viewModels()
    private var binding: FragmentRefDetailBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentRefDetailBinding.bind(view).also { this.binding = it }

        val adapter = RowAdapter { row ->
            findNavController().navigate(
                R.id.action_global_characterDetail,
                Bundle().apply { putInt("characterId", row.id) }
            )
        }
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.title.text = state.item?.name.orEmpty()
                    binding.subtitle.text = state.item?.summary.orEmpty()
                    adapter.submitList(
                        state.characters.map { Row(it.id, it.name, it.summary, it.image) }
                    )
                    binding.error.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    binding.error.text = state.error.orEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
