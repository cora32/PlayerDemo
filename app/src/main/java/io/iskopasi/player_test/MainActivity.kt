package io.iskopasi.player_test

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.iskopasi.player_test.databinding.StartActivityBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: StartActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StartActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.buttonXml.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlayerXMLActivity::class.java))
        }
        binding.buttonJetpack.setOnClickListener {
            Snackbar.make(binding.root, "Not implemented", Snackbar.LENGTH_LONG).show()
        }
    }
}