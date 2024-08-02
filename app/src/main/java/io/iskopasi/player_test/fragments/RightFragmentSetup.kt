package io.iskopasi.player_test.fragments

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
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
    fun onClick(index: Int, id: Int) {
        if (rootBinding.container.canPressButtons()) {
            model.addToPlaylist(index, id)
        }
        rootBinding.container.hideMenu()
    }

    fun setMenuActions(index: Int, id: Int) {
        rootBinding.container.menuOnPlay = {
            model.setAsPlaylist(index, id)
        }
        rootBinding.container.menuOnAdd = {
            model.addToPlaylist(index, id)
        }
        rootBinding.container.menuOnInfo = {
            model.showInfo(this, id)
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
    val adapter = MediaAdapter(
        model.allMediaActiveMapData.value!!,
        onClick = ::onClick, onLongPress = ::onLongPress
    )
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        if (scrollY != oldScrollY) {
            rootBinding.container.hideMenu()
        }
    }

    model.mediaList.observe(requireActivity()) {
        adapter.data = it

        if (it.isEmpty()) {
            binding.noData.visibility = View.VISIBLE
        } else {
            binding.noData.visibility = View.GONE
        }
    }
    model.allMediaActiveMapData.observe(requireActivity()) {
        val addedIndexes = it.keys - adapter.activeMap.keys
        val removedIndexes = adapter.activeMap.keys - it.keys

        for (index in addedIndexes) {
            adapter.addActive(index)
        }

        for (index in removedIndexes) {
            adapter.removeActive(index)
        }
    }
    model.currentActiveMediaIndex.observe(requireActivity()) {
        adapter.active = it
    }

    model.currentActiveState.observe(requireActivity()) {
        adapter.state = it
    }
}
