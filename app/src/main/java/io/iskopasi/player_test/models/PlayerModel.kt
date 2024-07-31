package io.iskopasi.player_test.models

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.App
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.PlayerService
import io.iskopasi.player_test.R
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.adapters.ItemState
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.room.MediaDataEntity
import io.iskopasi.player_test.utils.FFTPlayer
import io.iskopasi.player_test.utils.LoopIterator
import io.iskopasi.player_test.utils.ServiceCommunicator
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.share
import io.iskopasi.player_test.views.FFTChartData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


data class MediaData(
    val id: Int,
    val imageId: Int,
    val name: String,
    val subtitle: String,
    val duration: Int,
    var isFavorite: Boolean,
    var path: String,
    var genre: String,
    var composer: String,
    var author: String,
    var title: String,
    var writer: String,
    var albumArtist: String,
    var compilation: String,
) {
    companion object {
        val empty: MediaData = MediaData(
            -1,
            R.drawable.none,
            "",
            "",
            0,
            false,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
        )
    }
}

data class MediaMetadata(
    var maxBitrate: Int = 0,
    var bitrate: Int = 0,
    var sampleRateHz: Int = 0,
    var channelCount: Int = 0,
    var encoding: String = "-",
    var mime: String = "-",
)

@UnstableApi
@HiltViewModel
class PlayerModel @Inject constructor(
    context: Application,
    private val repo: Repo,
    private val dao: MediaDao,
) : AndroidViewModel(context) {
    private val images = listOf(
        R.drawable.none,
        R.drawable.i2,
        R.drawable.i3,
        R.drawable.i4,
        R.drawable.i5,
        R.drawable.i6,
        R.drawable.i7,
        R.drawable.i10,
    )
    private lateinit var iter: LoopIterator<MediaData>
    val currentData: MutableLiveData<MediaData>
        get() = repo.currentData

    private var previousData: MediaData? = null
    var playlist = MutableLiveData(listOf<MediaData>())
    var isPlaying = MutableLiveData(false)
    var isShuffling = MutableLiveData(false)
    var isRepeating = MutableLiveData(false)
    var isFavorite = MutableLiveData(false)
    var currentActiveIndex = MutableLiveData(-1)
    var currentActiveMediaIndex = MutableLiveData(-1)
    var currentActiveState = MutableLiveData(ItemState.NONE)
    var currentProgress = MutableLiveData(0L)
    var mediaList = MutableLiveData(listOf<MediaData>())
    var fftChartData = MutableLiveData(FFTChartData())
    var spectrumChartData = MutableLiveData(FFTChartData())
    var allMediaActiveMapData = MutableLiveData(mutableMapOf<Int, Boolean>())
    var metadata = MutableLiveData(MediaMetadata())
    private val baseColor = ContextCompat.getColor(getApplication(), R.color.text_color_1_trans3)
    private val idToListIndexMap = mutableMapOf<Int, Int>()

    private val serviceCommunicator by lazy {
        ServiceCommunicator("PlayerModel") { data, obj, comm ->
            when (data) {
//                MDEvent.CAMERA_REAR.name -> {
//                    isFront = false
//                }
//
//                MDEvent.CAMERA_FRONT.name -> {
//                    isFront = true
//                }
//
//                MDEvent.VIDEO_START.name -> {
//                    isRecording = true
//                    isBrightnessUp.value = true
//
//                    logMotionDetectionStart()
//                }
//
//                MDEvent.VIDEO_STOP.name -> {
//                    isRecording = false
//                    isBrightnessUp.value = false
//
//                    logVideoStop()
//                }
//
//                MDEvent.ARMED.name -> {
//                    isArmed = true
//                    isArming = false
//                }
//
//                MDEvent.DISARMED.name -> {
//                    isArmed = false
//                }
//
//                MDEvent.TIMER.name -> {
//                    val value = ((obj as Long) / 1000L).toInt()
//
//                    timerValue = if (value == 0) {
//                        null
//                    } else {
//                        value.toString()
//                    }
//                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startService() {
        val application = getApplication<App>()

        application.bindService(
            Intent(
                application,
                PlayerService::class.java
            ),
            serviceCommunicator.serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        ContextCompat.startForegroundService(
            application,
            Intent(application, PlayerService::class.java)
        )
    }

    private val player by lazy {
        FFTPlayer(
            context,
            onHandleBuffer = ::onHandleBuffer,
            onFullSpectrumReady = ::onFullSpectrumReady,
            onPlaylistFinished = ::onPlaylistFinished,
            onMediaSet = ::onMediaSet,
            onPlayStatusChanged = ::onPlayStatusChanged,
            onFlush = ::onFlush,
        )
    }

    private fun onFlush(sampleRateHz: Int, channelCount: Int, encoding: String) {
        ui {
            metadata.value = metadata.value?.apply {
                this.encoding = encoding
            }
        }
    }

    private fun onMediaSet(index: Int) {
        setMedia(index)
    }

    private fun onPlayStatusChanged(isPlaying: Boolean) {
        setPlayStatus(isPlaying)
    }

    private fun onPlaylistFinished() {
//        pause()
    }

    private fun onFullSpectrumReady(bitmap: Bitmap) {
        ui {
            spectrumChartData.value =
                FFTChartData(
                    bitmap = bitmap
                )
        }
    }

    private fun startRequestingSeekerPositions() = ui {
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

    init {
        startService()

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
            .getIsFavourite(map { it.id })
            .associateBy { it.mediaId }

        return map { item ->
            MediaData(
                item.id,
                images.random(),
                item.name,
                item.artist,
                item.duration.toInt(),
                favMap[item.id]?.isFavorite == true,
                item.path,
                genre = item.genre,
                composer = item.composer,
                author = item.author,
                title = item.title,
                writer = item.writer,
                albumArtist = item.albumArtist,
                compilation = item.compilation,
            )
        }
    }

    private fun setStates(data: MediaData, playlistIndex: Int) {
        if (data.path.isEmpty()) {
            return
        }

        previousData = currentData.value
        currentData.value = data
        currentActiveIndex.value = playlistIndex

        "--> currentActiveMediaIndex: ${data.path} ${data.id} ${idToListIndexMap.keys}".e
        currentActiveMediaIndex.value = idToListIndexMap[currentData.value!!.id]

        isFavorite.value = iter.value?.isFavorite
        currentProgress.value = 0
    }

    private fun resetUi() {
        currentData.value = MediaData.empty
        currentActiveIndex.value = -1

        currentProgress.value = 0
    }

    private fun setMedia(index: Int) {
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
        "Setting progress to: $progress".e
        currentProgress.value = progress
        player.seekTo(progress)
    }

    private fun setPlayStatus(isPlayingValue: Boolean) {
        "--> setPlayStatus: $isPlayingValue".e
        isPlaying.value = isPlayingValue

        if (isPlayingValue) {
            currentActiveState.value = ItemState.PLAYING
            val path = currentData.value!!.path

            player.prepareFifoBitmap(path, baseColor)
            val tempMetadata = player.extractMetadata(path)

            metadata.value = metadata.value?.apply {
                this.maxBitrate = tempMetadata.maxBitrate
                this.bitrate = tempMetadata.bitrate
                this.sampleRateHz = tempMetadata.sampleRateHz
                this.channelCount = tempMetadata.channelCount
                this.mime = tempMetadata.mime
            }

            startRequestingSeekerPositions()
        } else {
            currentActiveState.value = ItemState.PAUSE
//            if (player.isLastMedia()) {
////                player.setAutoPlay(false)
////                setSeekPosition(0L)
////                currentProgress.value = 0
//            }
        }
    }

    fun play(auto: Boolean = false) {
        player.setAutoPlay(true)
        player.play(auto)
    }

    fun pause() {
        isPlaying.value = false
        player.pause()
    }

    fun onPause() {
        player.setAutoPlay(false)
        pause()
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

        viewModelScope.launch(Dispatchers.IO) {
            if (newValue) {
                dao.insert(
                    MediaDataEntity(
                        mediaId = currentData.value!!.id,
                        isFavorite = newValue
                    )
                )
            } else {
                dao.remove(currentData.value!!.id)
            }
        }
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
                player.remove(index)
                remove(listIndex)
            }

            updatePlaylist()
        }
    }

    private fun updatePlaylist() {
        playlist.value = player.getIdToPlaylistIndexMap().map {
            iter.get(idToListIndexMap.getOrDefault(it.key, -1))
        }
    }

    fun setAsPlaylist(index: Int, id: Int) {
        player.clearPlaylist()
        allMediaActiveMapData.value = mutableMapOf()
        idToListIndexMap.clear()

        player.setAutoPlay(true)
        "Playing single media at $index (playlist): id = $id".e
        addToPlaylist(index, id)
    }

    /*
    * index - index in media RecyclerView
    * */
    fun addToPlaylist(index: Int, id: Int) {
        val item = iter.get(index)
        val path = item.path
        val idToPlaylistIndexMap = player.getIdToPlaylistIndexMap()

        // Add/Remove highlight from Media screen
        val isActive = allMediaActiveMapData.value?.getOrDefault(index, false)
        allMediaActiveMapData.value = allMediaActiveMapData.value?.apply {
            if (isActive == true) {
                "--> Deactivating $index in Media".e
                remove(index)
                idToListIndexMap.remove(id)
            } else {
                "--> Highlighting $index in Media; id = $id".e
                put(index, true)
                idToListIndexMap[id] = index
            }
        }

        // Add/Remove from Playlist
        val indexInPlaylist = idToPlaylistIndexMap.getOrDefault(id, -1)
        if (indexInPlaylist != -1) {
            "--> Removing $indexInPlaylist from playlist".e
            player.remove(indexInPlaylist)
        } else {
            "--> Adding $path to playlist with ID: $id".e
            player.add(path, id, item.title, item.subtitle)
        }

        updatePlaylist()
    }

    fun seekToDefaultPosition(index: Int) {
        player.setAutoPlay(true)
        player.seekToDefaultPosition(index)
        player.play(true)
    }
}
