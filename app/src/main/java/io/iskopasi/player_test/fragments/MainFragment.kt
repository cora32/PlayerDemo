package io.iskopasi.player_test.fragments

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentBottomBinding
import io.iskopasi.player_test.databinding.FragmentLeftBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.databinding.FragmentScreenMainBinding
import io.iskopasi.player_test.databinding.LoaderBinding
import io.iskopasi.player_test.databinding.TopScreenBinding
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.views.SlidingScreen
import io.iskopasi.player_test.views.SlidingScreenPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val centerBinding by lazy { binding.container.getBinding<FragmentScreenMainBinding>() }
    private val rightBinding by lazy { binding.container.getBinding<FragmentRightBinding>() }
    private val leftBinding by lazy { binding.container.getBinding<FragmentLeftBinding>() }
    private val bottomBinding by lazy { binding.container.getBinding<FragmentBottomBinding>() }
    private lateinit var loaderBinding: LoaderBinding
    private val model: PlayerModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback {
            if (!binding.container.isIsCenter()) {
                binding.container.goToCenter()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        loaderBinding = LoaderBinding.inflate(inflater, binding.root, false)

        // Starting animation in loader screen
        (loaderBinding.loaderIv.drawable as AnimatedVectorDrawable).start()

        binding.container.initialize(
            loaderBinding = loaderBinding,
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
                SlidingScreen(
                    R.layout.fragment_bottom,
                    SlidingScreenPosition.BOTTOM,
                    FragmentBottomBinding::inflate
                ),
            )
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.Main) {
            setupRight(model, rightBinding, binding)
            setupLeft(model, leftBinding, binding)
            setupBottom(model, bottomBinding, binding)
            setupCenter(model, centerBinding, binding)
        }
    }

    fun refreshList() {
        model.refreshData()
    }
}