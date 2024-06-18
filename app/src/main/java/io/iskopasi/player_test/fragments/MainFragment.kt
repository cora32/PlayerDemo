package io.iskopasi.player_test.fragments

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.res.ResourcesCompat
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

        binding.controls.seekBar.setColor(
            ResourcesCompat.getColor(
                resources,
                R.color.text_color_1,
                null
            )
        )
        model.currentData.observe(requireActivity()) {
            loadData(it)
        }

        model.currentProgress.observe(requireActivity()) {
            binding.controls.seekBar.progress = it
            binding.controls.timerStart.text = DateUtils.formatElapsedTime(it.toLong())
        }

        binding.controls.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    model.setSeekPosition(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.controls.b3.setOnClickListener {
            if (model.isPlaying.value!!) {
                model.pause()

                binding.controls.b3.setImageResource(R.drawable.pause_start_avd)
            } else {
                model.start()

                binding.controls.b3.setImageResource(R.drawable.start_pause_avd)
            }

            (binding.controls.b3.drawable as AnimatedVectorDrawable).start()
        }


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
        binding.controls.seekBar.max = data.duration
        binding.controls.timerEnd.text = DateUtils.formatElapsedTime(data.duration.toLong())

        data.image.getAccent(requireContext().applicationContext) {
            it?.let {
                binding.imageF.setBorderColor(it)
                binding.bgInclude.gradientFrame.setColor(it)
//                binding.controls.seekBar.setColor(it)
            }
        }
    }
}
