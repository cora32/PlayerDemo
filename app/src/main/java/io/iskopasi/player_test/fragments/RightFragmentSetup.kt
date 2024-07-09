package io.iskopasi.player_test.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import io.iskopasi.player_test.adapters.MediaAdapter
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.Utils.e

fun MainFragment.setupRight(
    model: PlayerModel,
    binding: FragmentRightBinding,
    rootBinding: FragmentMainBinding
) {
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
    val adapter = MediaAdapter { index ->
        model.setMedia(index)
    }
    binding.recyclerView.adapter = adapter
    model.mediaList.observe(requireActivity()) {
        adapter.data = it
    }
    model.currentActiveIndex.observe(requireActivity()) {
        "--> acrtive: $it".e
        adapter.active = it
    }
}