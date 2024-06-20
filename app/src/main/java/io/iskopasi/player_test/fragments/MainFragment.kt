package io.iskopasi.player_test.fragments

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.getAccent
import jp.wasabeef.glide.transformations.BlurTransformation


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

//        binding.controls.seekBar.setColor(
//            ResourcesCompat.getColor(
//                resources,
//                R.color.text_color_1,
//                null
//            )
//        )
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

        model.isPlaying.observe(requireActivity()) {
            if (it!!) {
                binding.controls.b3.setImageResource(R.drawable.start_pause_avd)
            } else {
                binding.controls.b3.setImageResource(R.drawable.pause_start_avd)
            }
            (binding.controls.b3.drawable as AnimatedVectorDrawable).start()
        }

        model.isShuffling.observe(requireActivity()) {
            if (it!!) {
                binding.controls.b5.setImageResource(R.drawable.shuffleoff_shuffleon_avd)
            } else {
                binding.controls.b5.setImageResource(R.drawable.shuffleon_shuffleoff_avd)
            }
            (binding.controls.b5.drawable as AnimatedVectorDrawable).start()
        }

        model.isRepeating.observe(requireActivity()) {
            if (it!!) {
                binding.controls.b1.setImageResource(R.drawable.roff_ron_avd)
            } else {
                binding.controls.b1.setImageResource(R.drawable.ron_roff_avd)
            }
            (binding.controls.b1.drawable as AnimatedVectorDrawable).start()
        }

        model.isFavorite.observe(requireActivity()) {
            setFavoriteResource(model.currentData.value!!.isFavorite)
        }

        binding.controls.b1.setOnClickListener {
            model.repeat()
        }

        binding.controls.b2.setOnClickListener {
            model.prev()
        }

        binding.controls.b3.setOnClickListener {
            if (model.isPlaying.value!!) {
                model.pause()
            } else {
                model.start()
            }
        }

        binding.controls.b4.setOnClickListener {
            model.next()
        }

        binding.controls.b5.setOnClickListener {
            model.shuffle()
        }

        binding.btnLike.setOnClickListener {
            model.favorite()
        }

        binding.btnShare.setOnClickListener {
            model.share(requireContext().applicationContext)
        }

//        ui {
//            delay(1000L)
//
//            while (true) {
//                model.next()
//                delay(2500L)
//            }
//        }
    }

    private fun loadData(data: MediaData) {
        Glide.with(requireContext().applicationContext).clear(binding.image)
        Glide.with(requireContext().applicationContext).clear(binding.bgInclude.imageBg)

        Glide
            .with(requireContext().applicationContext)
            .load(data.image)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(binding.image)

        Glide
            .with(requireContext().applicationContext)
            .load(data.image)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(200, 200)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
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

        setFavoriteResource(data.isFavorite)
    }

    private fun setFavoriteResource(isFavorite: Boolean) {
        if (isFavorite) {
            if (R.drawable.favb_fav_avd != binding.btnLike.tag) {
                binding.btnLike.apply {
                    tag = R.drawable.favb_fav_avd
                    setImageResource(R.drawable.favb_fav_avd)
                    (drawable as AnimatedVectorDrawable).start()
                }
            }
        } else {
            if (R.drawable.fav_favb_avd != binding.btnLike.tag) {
                binding.btnLike.apply {
                    tag = R.drawable.fav_favb_avd
                    setImageResource(R.drawable.fav_favb_avd)
                    (drawable as AnimatedVectorDrawable).start()
                }
            }
        }
    }
}
