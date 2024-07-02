package io.iskopasi.player_test.fragments

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentLeftBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.databinding.FragmentScreenMainBinding
import io.iskopasi.player_test.databinding.TopScreenBinding
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.getAccent
import io.iskopasi.player_test.views.SlidingScreen
import io.iskopasi.player_test.views.SlidingScreenPosition
import jp.wasabeef.glide.transformations.BlurTransformation


class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val centerBinding by lazy { binding.container.getBinding<FragmentScreenMainBinding>() }
    private val model: PlayerModel by viewModels()
//    private val spinner by lazy {
//        CircularProgressDrawable(this.requireContext().applicationContext).apply {
//            setColorSchemeColors(R.color.bg1, R.color.trans_red)
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding.container.initialize(
            listOf(
                SlidingScreen(
                    R.layout.fragment_left,
                    SlidingScreenPosition.LEFT,
                    FragmentLeftBinding::inflate
                ),
                SlidingScreen(
                    R.layout.fragment_screen_main,
                    SlidingScreenPosition.CENTER,
                    FragmentScreenMainBinding::inflate
                ),
                SlidingScreen(
                    R.layout.fragment_right,
                    SlidingScreenPosition.RIGHT,
                    FragmentRightBinding::inflate
                ),
                SlidingScreen(
                    R.layout.fragment_right,
                    SlidingScreenPosition.TOP,
                    TopScreenBinding::inflate
                ),
            )
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        centerBinding.controls.seekBar.setColor(
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
            centerBinding.controls.seekBar.progress = it
            centerBinding.controls.timerStart.text = DateUtils.formatElapsedTime(it.toLong())
        }

        centerBinding.controls.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
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
                centerBinding.controls.b3.setImageResource(R.drawable.start_pause_avd)
            } else {
                centerBinding.controls.b3.setImageResource(R.drawable.pause_start_avd)
            }
            (centerBinding.controls.b3.drawable as AnimatedVectorDrawable).start()
        }

        model.isShuffling.observe(requireActivity()) {
            if (it!!) {
                centerBinding.controls.b5.setImageResource(R.drawable.shuffleoff_shuffleon_avd)
            } else {
                centerBinding.controls.b5.setImageResource(R.drawable.shuffleon_shuffleoff_avd)
            }
            (centerBinding.controls.b5.drawable as AnimatedVectorDrawable).start()
        }

        model.isRepeating.observe(requireActivity()) {
            if (it!!) {
                centerBinding.controls.b1.setImageResource(R.drawable.roff_ron_avd)
            } else {
                centerBinding.controls.b1.setImageResource(R.drawable.ron_roff_avd)
            }
            (centerBinding.controls.b1.drawable as AnimatedVectorDrawable).start()
        }

        model.isFavorite.observe(requireActivity()) {
            setFavoriteResource(model.currentData.value!!.isFavorite)
        }

        centerBinding.controls.b1.setOnClickListener {
            model.repeat()
        }

        centerBinding.controls.b2.setOnClickListener {
            model.prev()
        }

        centerBinding.controls.b3.setOnClickListener {
            if (model.isPlaying.value!!) {
                model.pause()
            } else {
                model.start()
            }
        }

        centerBinding.controls.b4.setOnClickListener {
            model.next()
        }

        centerBinding.controls.b5.setOnClickListener {
            model.shuffle()
        }

        centerBinding.btnLike.setOnClickListener {
            model.favorite()
        }

        centerBinding.btnShare.setOnClickListener {
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
        Glide.with(requireContext().applicationContext).clear(centerBinding.image)
        Glide.with(requireContext().applicationContext).clear(binding.bgInclude.imageBg)

        Glide
            .with(requireContext().applicationContext)
            .load(data.image)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(centerBinding.image)

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
            .apply(RequestOptions.bitmapTransform(BlurTransformation(15, 1)))
            .into(binding.bgInclude.imageBg)

        centerBinding.tv.text = data.name
        centerBinding.tv2.text = data.subtitle
        centerBinding.controls.seekBar.max = data.duration
        centerBinding.controls.timerEnd.text = DateUtils.formatElapsedTime(data.duration.toLong())

        data.image.getAccent(requireContext().applicationContext) {
            it?.let {
                centerBinding.imageF.setBorderColor(it.darkVibrant)
                binding.bgInclude.gradientFrame.setColor(it.vibrant)
//                centerBinding.controls.seekBar.setColor(it)
            }
        }

        setFavoriteResource(data.isFavorite)
    }

    private fun setFavoriteResource(isFavorite: Boolean) {
        if (isFavorite) {
            if (R.drawable.favb_fav_avd != centerBinding.btnLike.tag) {
                centerBinding.btnLike.apply {
                    tag = R.drawable.favb_fav_avd
                    setImageResource(R.drawable.favb_fav_avd)
                    (drawable as AnimatedVectorDrawable).start()
                }
            }
        } else {
            if (R.drawable.fav_favb_avd != centerBinding.btnLike.tag) {
                centerBinding.btnLike.apply {
                    tag = R.drawable.fav_favb_avd
                    setImageResource(R.drawable.fav_favb_avd)
                    (drawable as AnimatedVectorDrawable).start()
                }
            }
        }
    }
}
