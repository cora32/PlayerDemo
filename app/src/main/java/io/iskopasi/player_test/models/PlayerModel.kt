package io.iskopasi.player_test.models

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.R
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.utils.FFTPlayer
import io.iskopasi.player_test.utils.LoopIterator
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.share
import io.iskopasi.player_test.views.FFTChartData
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject

data class MediaData(
    val id: Int,
    val imageId: Int,
    val name: String,
    val subtitle: String,
    val duration: Int,
    var isFavorite: Boolean,
    var path: String
) {
    companion object {
        val empty: MediaData = MediaData(
            -1,
            R.drawable.none,
            "",
            "",
            0,
            false,
            ""
        )
    }
}

@UnstableApi
@HiltViewModel
class PlayerModel @Inject constructor(
    context: Application,
    private val repo: Repo,
    private val dao: MediaDao
) : AndroidViewModel(context) {
    private val images = listOf(
        R.drawable.none,
        R.drawable.wat,
        R.drawable.billy,
        R.drawable.i2,
        R.drawable.i3,
        R.drawable.i4,
        R.drawable.i5,
        R.drawable.i6,
        R.drawable.i7,
        R.drawable.i8,
        R.drawable.i9,
        R.drawable.i10,
    )
    private lateinit var iter: LoopIterator<MediaData>
    private var previousData: MediaData? = null
    var currentData: MutableLiveData<MediaData?> = MutableLiveData(MediaData.empty)
    var isPlaying = MutableLiveData(false)
    var isShuffling = MutableLiveData(false)
    var isRepeating = MutableLiveData(false)
    var isFavorite = MutableLiveData(false)
    var currentActiveIndex = MutableLiveData(0)
    var currentProgress = MutableLiveData(0L)
    var mediaList = MutableLiveData(listOf<MediaData>())
    var fftChartData = MutableLiveData(FFTChartData())
    var spectrumChartData = MutableLiveData(FFTChartData())
    private val baseColor = ContextCompat.getColor(getApplication(), R.color.text_color_1_trans3)

    private val player by lazy {
        FFTPlayer(
            context,
            onHandleBuffer = ::onHandleBuffer,
            onFullSpectrumReady = ::onFullSpectrumReady,
            onPlaylistFinished = ::onPlaylistFinished
        )
    }

    private fun onFullSpectrumReady(bitmap: Bitmap) {
        ui {
            spectrumChartData.value =
                FFTChartData(
                    bitmap = bitmap
                )
        }
    }

    private fun requestCurrentPosition() = ui {
        while (player.isPlaying) {
            currentProgress.value = player.getCurrentPosition()
            delay(500L)
        }
    }

    private fun onHandleBuffer(frequencyMap: MutableMap<Int, Float>, maxAmplitude: Float) {
        if (isPlaying.value == true) {
            ui {
                fftChartData.value =
                    FFTChartData(
                        map = frequencyMap,
                        maxAmplitude = maxAmplitude
                    )
            }
        }
    }

    private fun onPlaylistFinished() {
        pause()
        currentProgress.value = 0
    }

    init {
        bg {
            val dataList = repo.read(getApplication()).toMediaData()
                .sortedBy { it.subtitle }
            iter = LoopIterator(dataList)

            ui {
                setStates(iter.value)
                mediaList.value = dataList
            }
        }
    }

    private fun List<MediaFile>.toMediaData(): List<MediaData> {
        val favMap = dao
            .getIsFavourite(this.map { it.id })
            .associateBy { it.uid }

        return map { item ->
            MediaData(
                item.id,
                images.random(),
                item.name,
                item.artist,
                item.duration.toInt(),
                favMap[item.id]?.isFavorite == true,
                item.path
            )
        }
    }

    private fun setStates(data: MediaData?) {
        if (data == null || data.path.isEmpty()) {
            return
        }

        previousData = currentData.value
        currentData.value = data
        currentActiveIndex.value = iter.index

        isFavorite.value = iter.value?.isFavorite
        currentProgress.value = 0

        player.set(currentData.value!!.path)
    }

    fun setMedia(index: Int) {
        setStates(iter.setIndex(index))
    }

    fun prev() {
        setStates(iter.prev())
    }

    fun next() {
        setStates(iter.next())
    }

    fun setSeekPosition(progress: Long) {
        currentProgress.value = progress
        player.seekTo(progress)
    }

    fun start() {
        isPlaying.value = true
        player.prepareFifoBitmap(currentData.value!!.path, baseColor)
        player.play()

        requestCurrentPosition()
//        player.requestFullSpectrum(currentData.value!!.path, baseColor)
    }

    fun pause() {
        isPlaying.value = false
        player.pause()
    }

    fun shuffle() {
        isShuffling.value = !isShuffling.value!!
        player.shuffle(isShuffling.value!!)
    }

    fun repeat() {
        isRepeating.value = !isRepeating.value!!
        player.repeat(isRepeating.value!!)
    }

    fun favorite() {
        val newValue = !currentData.value!!.isFavorite

        currentData.value!!.isFavorite = newValue
        isFavorite.value = newValue
    }

    fun share(context: Context, index: Int) {
        val name = currentData.value!!.name
        val subtitle = currentData.value!!.subtitle
        File("").share(context, name, subtitle)
    }

    fun showInfo(index: Int) {

    }
}
