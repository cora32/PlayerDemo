package io.iskopasi.player_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.databinding.NavActivityBinding


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: NavActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = NavActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

//        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }
}