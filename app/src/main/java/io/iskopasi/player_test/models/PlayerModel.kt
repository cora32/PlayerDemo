package io.iskopasi.player_test.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.iskopasi.player_test.R

data class MediaData(
    val image: Int,
    val name: String,
    val subtitle: String,
)

class PlayerModel(context: Application) : AndroidViewModel(context) {
    val test = arrayOf(
        MediaData(R.drawable.wat, "ШТО??", "Барашек ohiovaet"),
        MediaData(R.drawable.billy, "Реднек", "Беги, няша"),
        MediaData(R.drawable.i2, "Literally me", "Face reveal"),
        MediaData(R.drawable.i3, "OHUET", "anune ohuela"),
        MediaData(R.drawable.i4, "Outer", "As shrimple as that"),
        MediaData(R.drawable.i5, "Жирокот", "Жирный толстый"),
        MediaData(R.drawable.i6, "RAPE TIEM", "Nowhere to hide"),
        MediaData(R.drawable.i7, "Meph time xD", "Best time (and last one)"),
        MediaData(R.drawable.i8, "Daily thoughts", "Being useful every day"),
        MediaData(R.drawable.i9, "Science pepe", "Memetic warfare"),
        MediaData(R.drawable.i10, "Anger and Wraith", "Not even mad"),
    )
    var currentData = MutableLiveData(test.first())
    var currentSeekPosition = MutableLiveData(0)

    fun shuffle() {
        currentData.value = test[(0..10).random()]
    }

    fun setSeekPosition(progress: Int) {
        // Setting position
    }
}