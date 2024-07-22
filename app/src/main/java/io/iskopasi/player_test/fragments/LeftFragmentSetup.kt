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
    model.metadata.observe(requireActivity()) {
        binding.tvMimeVal.text = it.mime
        binding.tvSampleRateVal.text = it.sampleRateHz.toString()
        binding.tvBitrateVal.text = it.bitrate.toString()
        binding.tvMaxBitrateVal.text = it.maxBitrate.toString()
        binding.tvChannelsVal.text = it.channelCount.toString()
        binding.tvEncodingVal.text = it.encoding
    }
}