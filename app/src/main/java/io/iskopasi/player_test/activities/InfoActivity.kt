package io.iskopasi.player_test.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentInfoBinding
import io.iskopasi.player_test.models.InfoViewModel
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.utils.getAccent
import io.iskopasi.player_test.utils.toBitmap
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@Keep
//class InfoActivityArgs : Parcelable {
//
//}

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class InfoActivity : AppCompatActivity() {
    private lateinit var binding: FragmentInfoBinding
    private val model: InfoViewModel by viewModels()
//    val args: InfoActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = FragmentInfoBinding.inflate(layoutInflater)

        val mediaId = intent.getIntExtra("mediaId", -1)

        "--> Recevied index: $mediaId".e

        setup(mediaId)

        setContentView(binding.root)
    }

    private fun setBitmap(bitmap: Bitmap) {
        Glide
            .with(applicationContext)
            .load(bitmap)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(binding.image)

        // BG
        Glide
            .with(applicationContext)
            .load(bitmap)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(200, 200)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(15, 1)))
            .into(binding.imageBg)
    }

    private fun setMediaImage(data: MediaData) {
        Glide
            .with(applicationContext)
            .load(data.imageId)
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .into(binding.image)

        // BG
        Glide
            .with(applicationContext)
            .load(data.imageId)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(200, 200)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(15, 1)))
            .into(binding.imageBg)
    }

    private fun setup(mediaId: Int) {
        val data = model.getItemById(mediaId)

        binding.tv.text = data.title
        binding.tv2.text = data.subtitle
        binding.genreTv.text = data.genre

        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = data.path.toBitmap

            // Has to be run on UI
            lifecycleScope.launch {
                if (bitmap != null) setBitmap(bitmap) else setMediaImage(data)
            }
        }

        data.imageId.getAccent(applicationContext, data.path) {
            it?.let {
                binding.imageF.setBorderColor(it.darkVibrant)
//                rootBinding.bgInclude.gradientFrame.setColor(it.vibrant)
//                binding.controls.seekBar.setColor(it)
            }
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.share.setOnClickListener {
            model.share(applicationContext, data.id)
        }

        model.lyrics.observe(this) {
            binding.text.text = it
        }

        model.error.observe(this) {
            binding.error.text = it
        }

        TransitionManager.beginDelayedTransition(binding.constr)
        (binding.loaderIv.drawable as AnimatedVectorDrawable).start()
        model.isLoading.observe(this) {
            binding.loaderIv.visibility = if (it) View.VISIBLE else View.GONE
        }
    }
}