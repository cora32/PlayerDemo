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
import io.iskopasi.player_test.databinding.FragmentBottomBinding
import io.iskopasi.player_test.databinding.FragmentLeftBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.databinding.FragmentScreenMainBinding
import io.iskopasi.player_test.databinding.FragmentTopBinding
import io.iskopasi.player_test.databinding.LoaderBinding
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.models.RecommendationsModel
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.toPx
import io.iskopasi.player_test.views.SlidingScreenPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val centerBinding by lazy { binding.container.getBinding<FragmentScreenMainBinding>() }
    private val rightBinding by lazy { binding.container.getBinding<FragmentRightBinding>() }
    private val leftBinding by lazy { binding.container.getBinding<FragmentLeftBinding>() }
    private val bottomBinding by lazy { binding.container.getBinding<FragmentBottomBinding>() }
    private val topBinding by lazy { binding.container.getBinding<FragmentTopBinding>() }
    private lateinit var loaderBinding: LoaderBinding
    private val model: PlayerModel by viewModels()
    private val recModel: RecommendationsModel by viewModels()

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

        val topWidth = requireContext().toPx(600).toInt()

        binding.container.apply {
            addScreen(SlidingScreenPosition.LEFT, FragmentLeftBinding::inflate)
            addScreen(SlidingScreenPosition.CENTER, FragmentScreenMainBinding::inflate)
            addScreen(SlidingScreenPosition.RIGHT, FragmentRightBinding::inflate)
            addScreen(
                SlidingScreenPosition.TOP,
                FragmentTopBinding::inflate,
                onVisible = {
                    ui {
                        delay(300L)
                        recModel.show()
                    }
                },
                extendWidth = topWidth,
                xOffset = -topWidth / 2
            )
            addScreen(SlidingScreenPosition.BOTTOM, FragmentBottomBinding::inflate)

            initialize(
                loaderBinding = loaderBinding.apply {
                    // Starting animation in loader screen
                    (loaderIv.drawable as AnimatedVectorDrawable).start()
                },
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.Main) {
            setupRight(model, rightBinding, binding)
            setupLeft(model, leftBinding, binding)
            setupBottom(model, bottomBinding, binding)
            setupCenter(model, centerBinding, binding)
            setupTop(recModel, topBinding, binding)
        }
    }

    fun refreshList() {
        model.refreshData()
    }
}