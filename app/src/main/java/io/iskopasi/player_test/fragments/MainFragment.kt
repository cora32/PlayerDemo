package io.iskopasi.player_test.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.Utils.ui
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.delay

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val model: PlayerModel by viewModels()
    private val spinner by lazy {
        CircularProgressDrawable(this.requireContext().applicationContext).apply {
            setColorSchemeColors(R.color.bg1, R.color.trans_red)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentData.observe(requireActivity()) {
            loadData(it)
        }

        ui {
            delay(1000L)

            while (true) {
                model.shuffle()
//                loadImages()
                delay(2500L)
            }
        }
    }

    private fun loadData(data: MediaData) {
        Glide
            .with(this)
            .load(data.image)
            .centerCrop()
            .circleCrop()
            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.image)

        Glide
            .with(this)
            .load(data.image)
            .centerCrop()
            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
            .into(binding.bgInclude.imageBg)

        binding.tv.text = data.name
        binding.tv2.text = data.subtitle
    }
}