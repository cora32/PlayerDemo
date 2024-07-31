package io.iskopasi.player_test.activities

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.NavActivityBinding


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: NavActivityBinding

    private val requester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultMap ->
            if (resultMap.values.all { it }) {
                /// TODO: update ui in fragments
                sendIntent
            } else {
                Toast.makeText(this, R.string.perm_request, Toast.LENGTH_SHORT).show()
//            findNavController(R.id.nav_host_fragment).navigate(R.id.to_perm_denied)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = NavActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

        mRequestPermissions()
//        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun mRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requester.launch(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO,
                )
            )
        } else {
            requester.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            )
        }
    }
}