package io.iskopasi.player_test.fragments

import android.annotation.SuppressLint
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import io.iskopasi.player_test.R
import io.iskopasi.player_test.adapters.CardsAdapter
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentTopBinding
import io.iskopasi.player_test.models.RecommendationsModel


@SuppressLint("UnsafeOptInUsageError")
fun MainFragment.setupTop(
    model: RecommendationsModel,
    binding: FragmentTopBinding,
    rootBinding: FragmentMainBinding
) {
    fun onClick(index: Int, id: Int) {
    }

    val dividerItemDecoration = DividerItemDecoration(
        context,
        LinearLayout.HORIZONTAL
    )

    dividerItemDecoration.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.divider)!!)

    val adapter = CardsAdapter(
        onClick = ::onClick
    )
    binding.recyclerView1.adapter = adapter
    binding.recyclerView1.addItemDecoration(dividerItemDecoration)
    binding.recyclerView1.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        if (scrollY != oldScrollY) {
            rootBinding.container.hideMenu()
        }
    }

    val adapter2 = CardsAdapter(
        onClick = ::onClick
    )
    binding.recyclerView2.adapter = adapter2
    binding.recyclerView2.addItemDecoration(dividerItemDecoration)
    binding.recyclerView2.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        if (scrollY != oldScrollY) {
            rootBinding.container.hideMenu()
        }
    }

    model.recommendTracks.observe(requireActivity()) {
        adapter.data = it
    }
    model.recommendAlbums.observe(requireActivity()) {
        adapter2.data = it
    }
}
