package io.iskopasi.player_test.fragments

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.getAccent
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

        model.currentSeekPosition.observe(requireActivity()) {
            binding.controls.seekBar.progress = it
        }

        binding.controls.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                model.setSeekPosition(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        ui {
            delay(1000L)

            while (true) {
                model.shuffle()
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
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
            .into(binding.bgInclude.imageBg)

        binding.tv.text = data.name
        binding.tv2.text = data.subtitle

        data.image.getAccent(requireContext().applicationContext) {
            it?.let {
                binding.imageF.setBorderColor(it)
                binding.bgInclude.gradientFrame.setColor(it)
                binding.controls.seekBar.progressDrawable.apply {
                    mutate()

                    colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN)
                }
                binding.controls.seekBar.thumb.apply {
                    mutate()

                    colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
    }
}
