package io.iskopasi.player_test.fragments

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import io.iskopasi.player_test.databinding.FragmentLeftBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.models.PlayerModel


@OptIn(UnstableApi::class)
fun MainFragment.setupLeft(
    model: PlayerModel,
    binding: FragmentLeftBinding,
    rootBinding: FragmentMainBinding
) {
    model.fftChartData.observe(requireActivity()) {
        binding.fftView.data = it
    }
    model.spectrumChartData.observe(requireActivity()) {
        binding.spectroView.set(it)
    }
}