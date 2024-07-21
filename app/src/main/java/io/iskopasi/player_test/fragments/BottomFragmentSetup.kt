package io.iskopasi.player_test.fragments

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import io.iskopasi.player_test.adapters.SingleActiveMediaAdapter
import io.iskopasi.player_test.databinding.FragmentBottomBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.models.PlayerModel

@SuppressLint("UnsafeOptInUsageError")
fun MainFragment.setupBottom(
    model: PlayerModel,
    binding: FragmentBottomBinding,
    rootBinding: FragmentMainBinding
) {
    fun onClick(index: Int, id: Int) {
        if (rootBinding.container.canPressButtons()) {
            model.seekToDefaultPosition(index)
            model.play()
        }
        rootBinding.container.hideMenu()
    }

    fun setMenuActions(index: Int, id: Int) {
        rootBinding.container.menuOnPlay = {
            model.seekToDefaultPosition(index)
            model.play()
        }
        rootBinding.container.menuOnRemove = {
            model.removeFromPlaylist(index, id)
        }
        rootBinding.container.menuOnInfo = {
            model.showInfo(index)
        }
        rootBinding.container.menuOnShare = {
            model.share(requireContext().applicationContext, index)
        }
    }

    fun onLongPress(event: MotionEvent, index: Int, id: Int) {
        rootBinding.container.removeMenuActions()
        setMenuActions(index, id)
        rootBinding.container.showMenu(event, index)
    }

    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
    val adapter = SingleActiveMediaAdapter(
        model.currentActiveIndex.value ?: -1,
        onClick = ::onClick, onLongPress = ::onLongPress
    )
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        if (scrollY != oldScrollY) {
            rootBinding.container.hideMenu()
        }
    }

    model.playlist.observe(requireActivity()) {
        adapter.data = it

        if (it.isEmpty()) {
            binding.noData.visibility = View.VISIBLE
        } else {
            binding.noData.visibility = View.GONE
        }
    }

    model.currentActiveIndex.observe(requireActivity()) {
        adapter.active = it
    }
}
