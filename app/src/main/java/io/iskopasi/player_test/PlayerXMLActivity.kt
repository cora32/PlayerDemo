package io.iskopasi.player_test

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import io.iskopasi.player_test.databinding.ActivityXmlPlayerBinding

class PlayerXMLActivity : AppCompatActivity() {
    private lateinit var binding: ActivityXmlPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        binding = ActivityXmlPlayerBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }
}