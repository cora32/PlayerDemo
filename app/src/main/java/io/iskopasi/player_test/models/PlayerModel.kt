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
import io.iskopasi.player_test.utils.Utils.e
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
    var currentData: MutableLiveData<MediaData> = MutableLiveData(MediaData.empty)
    var playlist = MutableLiveData(listOf<MediaData>())
    var isPlaying = MutableLiveData(false)
    var isShuffling = MutableLiveData(false)
    var isRepeating = MutableLiveData(false)
    var isFavorite = MutableLiveData(false)
    var currentActiveIndex = MutableLiveData(-1)
    var currentProgress = MutableLiveData(0L)
    var mediaList = MutableLiveData(listOf<MediaData>())
    var fftChartData = MutableLiveData(FFTChartData())
    var spectrumChartData = MutableLiveData(FFTChartData())
    var allMediaActiveMapData = MutableLiveData(mutableMapOf<Int, Boolean>())
    private val baseColor = ContextCompat.getColor(getApplication(), R.color.text_color_1_trans3)
    private val idToListIndexMap = mutableMapOf<Int, Int>()

    private val player by lazy {
        FFTPlayer(
            context,
            onHandleBuffer = ::onHandleBuffer,
            onFullSpectrumReady = ::onFullSpectrumReady,
            onPlaylistFinished = ::onPlaylistFinished,
            onMediaSet = ::onMediaSet,
            onPlayStatusChanged = ::onPlayStatusChanged,
        )
    }

    private fun onMediaSet(index: Int) {
        setMedia(index)
    }

    private fun onPlayStatusChanged(isPlaying: Boolean) {
        setPlayStatus(isPlaying)
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
    }

    init {
        bg {
            val dataList = repo.read(getApplication()).toMediaData()
                .sortedBy { it.subtitle }
            iter = LoopIterator(dataList)

            ui {
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

    private fun setStates(data: MediaData, playlistIndex: Int) {
        if (data.path.isEmpty()) {
            return
        }

        previousData = currentData.value
        currentData.value = data
//        currentActiveIndex.value = iter.index
        currentActiveIndex.value = playlistIndex

        isFavorite.value = iter.value?.isFavorite
        currentProgress.value = 0

//        player.set(currentData.value!!.path)
    }

    private fun resetUi() {
        currentData.value = MediaData.empty
        currentActiveIndex.value = -1

        currentProgress.value = 0
    }

    fun setMedia(index: Int) {
        if (playlist.value?.isEmpty() == true || playlist.value == null) {
            resetUi()
        } else {
            setStates(playlist.value!![index], index)
        }
    }

    fun prev() {
        player.prev()
    }

    fun next() {
        player.next()
    }

    fun setSeekPosition(progress: Long) {
        currentProgress.value = progress
        player.seekTo(progress)
    }

    private fun setPlayStatus(isPlayingValue: Boolean) {
        isPlaying.value = isPlayingValue

        if (isPlayingValue) {
            player.prepareFifoBitmap(currentData.value!!.path, baseColor)
            requestCurrentPosition()
        }
    }

    fun play(auto: Boolean = false) {
        if (player.isPlaying) {
            pause()
        }

        player.play(auto)
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

    fun removeFromPlaylist(index: Int, id: Int) {
        val listIndex = idToListIndexMap.getOrDefault(id, -1)
        if (listIndex >= 0) {
            allMediaActiveMapData.value = allMediaActiveMapData.value?.apply {
                "--> Removing index: $index; indexInPlaylist: $index".e
                player.remove(index)
                remove(listIndex)
            }

            updatePlaylist()
        }
    }

    private fun updatePlaylist() {
        playlist.value = player.getPlaylistIds().map {
            iter.get(it)
        }
    }

    fun setAsPlaylist(index: Int, id: Int) {
        player.clearPlaylist()
        allMediaActiveMapData.value = mutableMapOf()
        idToListIndexMap.clear()

        player.setAutoPlay(true)
        addToPlaylist(index, id)
    }

    /*
    * index - index in media RecyclerView
    * */
    fun addToPlaylist(index: Int, id: Int) {
        val item = iter.get(index)
        val path = item.path

        val isAdded = allMediaActiveMapData.value?.getOrDefault(index, false)
        allMediaActiveMapData.value = allMediaActiveMapData.value?.apply {
            "--> index: $index; actualId: ${id} item: $path; isAdded: $isAdded".e

            if (isAdded == true) {
                "--> Removing index: $index; indexInPlaylist: $index".e
                player.remove(index)
                remove(index)
                idToListIndexMap.remove(id)
            } else {
                "--> Adding index: $index".e
                player.add(path, index)
                put(index, true)
                idToListIndexMap[id] = index
            }
        }

        updatePlaylist()
    }

    fun seekToDefaultPosition(index: Int) {
        player.setAutoPlay(true)
        player.seekToDefaultPosition(index)
        player.play(true)
    }
}
