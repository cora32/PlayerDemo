package io.iskopasi.player_test.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.adapters.MediaListAdapter
import io.iskopasi.player_test.databinding.FragmentXmlBinding
import io.iskopasi.player_test.models.MediaState
import io.iskopasi.player_test.models.PlayerXMLViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class XmlFragment : Fragment() {
    private lateinit var binding: FragmentXmlBinding
    private val model: PlayerXMLViewModel by viewModels()
    private val adapter = MediaListAdapter(this::onItemClicked)

    private val circularProgressDrawable by lazy {
        CircularProgressDrawable(requireContext()).apply {
            setColorSchemeColors(Color.WHITE)
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }
    }

//    private val detector: GestureDetectorCompat by lazy {
//        GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
//            override fun onDown(e: MotionEvent): Boolean {
//                "${e.x}".e
//                return false
//            }
//
//            override fun onFling(
//                e1: MotionEvent,
//                e2: MotionEvent,
//                velocityX: Float,
//                velocityY: Float
//            ): Boolean {
//                (e1.x - e2.x).let { dx ->
//                    when {
//                        dx < 0 -> model.prev()
//                        dx > 0 -> model.next()
//                        else -> {}
//                    }
//                }
//
//                return false
//            }
//        })
//    }

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

//        binding.root.setOnTouchListener { _, motionEvent ->
//            detector.onTouchEvent(motionEvent)
//        }

        binding.content.next.setOnClickListener {
            model.next()?.let {
                binding.recyclerView.scrollToPosition(it.id)
            }
        }

        binding.content.prev.setOnClickListener {
            model.prev()?.let {
                binding.recyclerView.scrollToPosition(it.id)
            }
        }

        binding.content.play.setOnClickListener {
            model.play()
        }

        binding.content.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser)
                    model.onSeek(value)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        setObservers()

        model.read()
    }

    private fun onItemClicked(id: Int) {
        model.play(id)
    }

    private fun setObservers() {
        model.isLoading.observe(requireActivity()) {
            if (it) {
                binding.content.loader.root.visibility = View.VISIBLE
            } else {
                binding.content.loader.root.visibility = View.GONE
            }
        }

        model.currentTrack.observe(requireActivity()) {
            if (it.id == -1) return@observe

            binding.content.name.text = it.name
            binding.content.album.text = it.album
            binding.content.artist.text = it.artist
            binding.content.seekBar.max = it.duration.toInt()

            adapter.active = it.id

            CoroutineScope(Dispatchers.Main).launch {
                binding.content.blur.blur(binding.content.root)
            }

            CoroutineScope(Dispatchers.Main).launch {
                delay(2000L)
                binding.content.blur.blur(binding.content.root)
            }
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
                .into(binding.content.iv1)
        }

        model.mediaState.observe(requireActivity()) {
            if (it == MediaState.PLAYING) {
                binding.content.play.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding.content.play.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        }

        model.position.observe(requireActivity()) {
            binding.content.seekBar.progress = it
        }

        model.list.observe(requireActivity()) {
            adapter.data = it
        }
    }
}