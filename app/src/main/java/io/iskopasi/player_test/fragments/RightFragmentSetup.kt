package io.iskopasi.player_test.fragments

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import io.iskopasi.player_test.adapters.MediaAdapter
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.models.PlayerModel

@SuppressLint("UnsafeOptInUsageError")
fun MainFragment.setupRight(
    model: PlayerModel,
    binding: FragmentRightBinding,
    rootBinding: FragmentMainBinding
) {
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
    val adapter = MediaAdapter(
        onClick = { index ->
            model.setMedia(index)
            rootBinding.container.hideMenu()
        }, onLongPress = { event, index ->
            rootBinding.container.menuOnPlay = {
                model.setMedia(index)
            }
            rootBinding.container.menuOnInfo = {
                model.showInfo(index)
            }
            rootBinding.container.menuOnShare = {
                model.share(requireContext().applicationContext, index)
            }
            rootBinding.container.showMenu(event, index)
        }
    )
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//        "---> scrollY: $scrollY $oldScrollY".e
        if (scrollY != oldScrollY) {
            rootBinding.container.hideMenu()
        }
    }

    model.mediaList.observe(requireActivity()) {
        adapter.data = it
    }
    model.currentActiveIndex.observe(requireActivity()) {
        adapter.active = it
    }
}
