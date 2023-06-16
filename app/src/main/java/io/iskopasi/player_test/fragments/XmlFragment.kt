package io.iskopasi.player_test.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.Utils.e
import io.iskopasi.player_test.databinding.FragmentXmlBinding
import io.iskopasi.player_test.models.MediaState
import io.iskopasi.player_test.models.PlayerXMLViewModel

@AndroidEntryPoint
class XmlFragment : Fragment() {
    private lateinit var binding: FragmentXmlBinding
    private val model: PlayerXMLViewModel by viewModels()

    private val circularProgressDrawable by lazy {
        CircularProgressDrawable(requireContext()).apply {
            setColorSchemeColors(Color.WHITE)
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }
    }

    private val detector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                "${e.x}".e
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                (e1.x - e2.x).let { dx ->
                    when {
                        dx < 0 -> model.prev()
                        dx > 0 -> model.next()
                    }
                }

                return true
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentXmlBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnTouchListener { _, motionEvent ->
            detector.onTouchEvent(motionEvent)
        }

        binding.next.setOnClickListener {
            model.next()
        }

        binding.prev.setOnClickListener {
            model.prev()
        }

        binding.play.setOnClickListener {
            model.play()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser)
                    model.onSeek(value)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        setObservers()

        model.read()
    }

    private fun setObservers() {
        model.isLoading.observe(requireActivity()) {
            if (it) {
                binding.loader.root.visibility = View.VISIBLE
            } else {
                binding.loader.root.visibility = View.GONE
            }
        }

        model.currentTrack.observe(requireActivity()) {
            binding.name.text = it.name
            binding.album.text = it.album
            binding.artist.text = it.artist
            binding.seekBar.max = it.duration.toInt()
        }

        model.image.observe(requireActivity()) {
//            binding.iv1.setImageBitmap(it)

            Glide
                .with(this)
                .load(it)
//                .centerCrop()
                .dontTransform()
                .placeholder(circularProgressDrawable)
                .error(R.drawable.album_placeholder)
                .into(binding.iv1)
        }

        model.mediaState.observe(requireActivity()) {
            if (it == MediaState.PLAYING) {
                binding.play.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding.play.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        }

        model.position.observe(requireActivity()) {
            binding.seekBar.progress = it
        }
    }
}