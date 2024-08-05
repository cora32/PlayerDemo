package io.iskopasi.player_test.fragments

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.text.format.DateUtils
import android.view.View.OnClickListener
import android.widget.SeekBar
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentScreenMainBinding
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.utils.getAccent
import io.iskopasi.player_test.utils.toBitmap
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(UnstableApi::class)
fun MainFragment.setupCenter(
    model: PlayerModel,
    binding: FragmentScreenMainBinding,
    rootBinding: FragmentMainBinding
) {
    var isEmpty = true
    val goToMedia = OnClickListener {
        rootBinding.container.goToRight()
    }

    fun disableControls() {
        binding.controls.root.isClickable = true
        binding.controls.b1.isClickable = false
        binding.controls.b2.isClickable = false
        binding.controls.b3.isClickable = false
        binding.controls.b4.isClickable = false
        binding.controls.b5.isClickable = false
        binding.controls.seekBar.isClickable = false

//        val color = ContextCompat.getColor(requireActivity(), R.color.trans)
//
//        binding.btnLike.setColorFilter(color)
//        binding.btnShare.setColorFilter(color)
//        binding.controls.b1.setColorFilter(color)
//        binding.controls.b2.setColorFilter(color)
//        binding.controls.b3.setColorFilter(color)
//        binding.controls.b4.setColorFilter(color)
//        binding.controls.b5.setColorFilter(color)

        binding.controls.root.setOnClickListener(goToMedia)
        binding.tv.setOnClickListener(goToMedia)
        binding.tv2.setOnClickListener(goToMedia)
    }

    fun enableControls() {
        binding.controls.root.isClickable = false
        binding.controls.b1.isClickable = true
        binding.controls.b2.isClickable = true
        binding.controls.b3.isClickable = true
        binding.controls.b4.isClickable = true
        binding.controls.b5.isClickable = true
        binding.controls.seekBar.isClickable = true

//        val color = ContextCompat.getColor(requireActivity(), R.color.white)
//
//        binding.btnLike.setColorFilter(color)
//        binding.btnShare.setColorFilter(color)
//        binding.controls.b1.setColorFilter(color)
//        binding.controls.b2.setColorFilter(color)
//        binding.controls.b3.setColorFilter(color)
//        binding.controls.b4.setColorFilter(color)
//        binding.controls.b5.setColorFilter(color)

        binding.controls.root.setOnClickListener(null)
        binding.tv.setOnClickListener(null)
        binding.tv2.setOnClickListener(null)
    }

    fun setFavoriteResource(isFavorite: Boolean) {
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

    fun setMediaImage(imageId: Int) {
        Glide
            .with(requireContext().applicationContext)
            .load(imageId)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(binding.image)

        Glide
            .with(requireContext().applicationContext)
            .load(imageId)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(200, 200)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(15, 1)))
            .into(rootBinding.bgInclude.imageBg)
    }

    fun setBitmap(bitmap: Bitmap) {
        Glide
            .with(requireContext().applicationContext)
            .load(bitmap)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(binding.image)

        Glide
            .with(requireContext().applicationContext)
            .load(bitmap)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(200, 200)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(15, 1)))
            .into(rootBinding.bgInclude.imageBg)
    }

    fun loadData(data: MediaData) {
        isEmpty = data.path.isEmpty()

        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = data.path.toBitmap

            // Has to be run on UI
            lifecycleScope.launch {
                if (bitmap != null) setBitmap(bitmap) else setMediaImage(data.imageId)
            }
        }

        setFavoriteResource(data.isFavorite)

        if (isEmpty) {
            binding.tv.text = ContextCompat.getString(requireActivity(), R.string.empty_playlist)
            binding.tv2.text =
                ContextCompat.getString(requireActivity(), R.string.empty_playlist_directive)

            disableControls()
        } else {
            binding.tv.text = data.title
            binding.tv2.text = data.subtitle

            enableControls()
        }

        binding.controls.seekBar.max = data.duration
        binding.controls.timerEnd.text =
            DateUtils.formatElapsedTime(data.duration.toLong() / 1000L)

        data.imageId.getAccent(requireContext().applicationContext, data.path) {
            it?.let {
                binding.imageF.setBorderColor(it.darkVibrant)
                rootBinding.bgInclude.gradientFrame.setColor(it.vibrant)
//                binding.controls.seekBar.setColor(it)
            }
        }
    }

//        binding.controls.seekBar.setColor(
//            ResourcesCompat.getColor(
//                resources,
//                R.color.text_color_1,
//                null
//            )
//        )
    binding.image.setOnClickListener {
        if (isEmpty) rootBinding.container.goToRight() else {
            if (rootBinding.container.canPressButtons())
                model.showInfo(this, model.currentData.value!!.id)
        }
    }

    binding.volumeView.interceptTouches = { intercept ->
        rootBinding.container.interceptTouches(intercept)
    }
    binding.controls.seekBar.interceptTouches = { intercept ->
        rootBinding.container.interceptTouches(intercept)
    }

    model.currentData.observe(requireActivity()) {
        loadData(it!!)

        lifecycleScope.launch {
            delay(timeMillis = 200)
            rootBinding.container.hideLoader()
        }
    }

    model.currentProgress.observe(requireActivity()) {
        binding.controls.seekBar.progress = it.toInt()
        binding.controls.timerStart.text = DateUtils.formatElapsedTime(it.toLong() / 1000L)
    }

    binding.controls.seekBar.setOnSeekBarChangeListener(object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                model.setSeekPosition(progress.toLong())
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
            model.onPause()
        } else {
            model.play()
        }
    }

    binding.controls.b4.setOnClickListener {
        model.next()
    }

    binding.controls.b5.setOnClickListener {
        model.shuffle()
    }

    binding.btnLike.setOnClickListener {
        "isEmpty: $isEmpty".e
        if (isEmpty) rootBinding.container.goToRight() else
            model.favorite()
    }

    binding.btnShare.setOnClickListener {
        "isEmpty: $isEmpty".e
        if (isEmpty) rootBinding.container.goToRight() else
            model.share(requireContext().applicationContext, model.currentData.value!!.id)
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
