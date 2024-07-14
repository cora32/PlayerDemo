package io.iskopasi.player_test.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.FragmentLeftBinding
import io.iskopasi.player_test.databinding.FragmentMainBinding
import io.iskopasi.player_test.databinding.FragmentRightBinding
import io.iskopasi.player_test.databinding.FragmentScreenMainBinding
import io.iskopasi.player_test.databinding.TopScreenBinding
import io.iskopasi.player_test.models.PlayerModel
import io.iskopasi.player_test.views.SlidingScreen
import io.iskopasi.player_test.views.SlidingScreenPosition

@UnstableApi
@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val centerBinding by lazy { binding.container.getBinding<FragmentScreenMainBinding>() }
    private val rightBinding by lazy { binding.container.getBinding<FragmentRightBinding>() }
    private val leftBinding by lazy { binding.container.getBinding<FragmentLeftBinding>() }
    private val model: PlayerModel by viewModels()

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

        setupCenter(model, centerBinding, binding)
        setupRight(model, rightBinding, binding)
        setupLeft(model, leftBinding, binding)
    }
}