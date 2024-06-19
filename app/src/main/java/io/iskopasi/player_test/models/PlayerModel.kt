package io.iskopasi.player_test.models

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.iskopasi.player_test.LoopIterator
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.share
import java.io.File

data class MediaData(
    val image: Int,
    val name: String,
    val subtitle: String,
    val duration: Int,
    var isFavorite: Boolean
)

class PlayerModel(context: Application) : AndroidViewModel(context) {
    private val iter by lazy {
        LoopIterator<MediaData>(test.shuffled())
    }
    val test = listOf(
        MediaData(R.drawable.wat, "ШТО??", "Барашек ohiovaet", 12124214, false),
        MediaData(R.drawable.billy, "Реднек", "Беги, няша", 122143, true),
        MediaData(R.drawable.i2, "Literally me", "Face reveal", 3904, true),
        MediaData(R.drawable.i3, "OHUET", "anune ohuela", 109284, false),
        MediaData(R.drawable.i4, "Outer", "As shrimple as that", 2434325, true),
        MediaData(R.drawable.i5, "Жирокот", "Жирный толстый", 56757, true),
        MediaData(R.drawable.i6, "RAPE TIEM", "Nowhere to hide", 112131243, false),
        MediaData(R.drawable.i7, "Meph time xD", "Best time (and last one)", 53446, true),
        MediaData(R.drawable.i8, "Daily thoughts", "Being useful every day", 5098567, false),
        MediaData(R.drawable.i9, "Science pepe", "Memetic warfare", 98234, false),
        MediaData(R.drawable.i10, "Anger and Wraith", "Not even mad", 9380468, true),
        MediaData(R.drawable.none, "No track", "No image", 12312, false),
    )
    var currentData = MutableLiveData(test.first())
    var previousData: MediaData? = null
    var isPlaying = MutableLiveData(false)
    var isShuffling = MutableLiveData(false)
    var isRepeating = MutableLiveData(false)
    var isFavorite = MutableLiveData(false)
    var currentProgress = MutableLiveData(0)

    private fun setStates(data: MediaData?) {
        previousData = currentData.value
        currentData.value = data

        isFavorite.value = iter.value?.isFavorite
        currentProgress.value = (0..currentData.value!!.duration).random()
    }

    fun prev() {
        setStates(iter.prev())
    }

    fun next() {
        setStates(iter.next())
    }

    fun setSeekPosition(progress: Int) {
        currentProgress.value = progress
    }

    fun start() {
        isPlaying.value = true
    }

    fun pause() {
        isPlaying.value = false
    }

    fun shuffle() {
        isShuffling.value = !isShuffling.value!!
    }

    fun repeat() {
        isRepeating.value = !isRepeating.value!!
    }

    fun favorite() {
        val newValue = !currentData.value!!.isFavorite

        currentData.value!!.isFavorite = newValue
        isFavorite.value = newValue
    }

    fun share(context: Context) {
        val name = currentData.value!!.name
        val subtitle = currentData.value!!.subtitle
        File("").share(context, name, subtitle)
    }
}