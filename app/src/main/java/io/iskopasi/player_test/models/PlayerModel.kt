package io.iskopasi.player_test.models

import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaFormat.KEY_BIT_RATE
import android.media.MediaFormat.KEY_CHANNEL_COUNT
import android.media.MediaFormat.KEY_MIME
import android.media.MediaFormat.KEY_SAMPLE_RATE
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.MediaFormatUtil.KEY_MAX_BIT_RATE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.App
import io.iskopasi.player_test.PlayerService
import io.iskopasi.player_test.R
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.adapters.ItemState
import io.iskopasi.player_test.fragments.MainFragment
import io.iskopasi.player_test.fragments.MainFragmentDirections
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.room.MediaDataEntity
import io.iskopasi.player_test.utils.FifoBitmap
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.hasEmbeddedPicture
import io.iskopasi.player_test.utils.share
import io.iskopasi.player_test.views.FFTChartData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.min

const val SAMPLE_SIZE = 4096

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
    val currentData: MutableLiveData<MediaData>
        get() = repo.currentData

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
    private var allowPlayingStateChange = true
    private var fifoBitmap: FifoBitmap? = null
    private val listener by lazy {
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                "--> onIsPlayingChanged: $isPlaying".e

                if (isPlaying) {
//                    onPlaylistStarted()
                } else {
                    // Not playing because playback is paused, ended, suppressed, or the player
                    // is buffering, stopped or failed. Check player.playWhenReady,
                    // player.playbackState, player.playbackSuppressionReason and
                    // player.playerError for details.

                    if (allowPlayingStateChange) onPlaylistFinished()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                "--> onPlaybackStateChanged: $playbackState".e

                when (playbackState) {
                    Player.STATE_IDLE -> {
                        "FFTPlayer: STATE_IDLE".e
                    }

                    Player.STATE_BUFFERING -> {
                        "FFTPlayer: STATE_BUFFERING".e
                    }

                    Player.STATE_READY -> {
                        "FFTPlayer: STATE_READY".e
                    }

                    Player.STATE_ENDED -> {
                        "FFTPlayer: STATE_ENDED".e
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                "--> onEvents: $events".e

                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    if (allowPlayingStateChange) onMediaSet(player.currentMediaItemIndex)
                } else if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    if (allowPlayingStateChange) onPlayStatusChanged(player.isPlaying)
                }
            }
        }
    }

    private val sessionToken =
        SessionToken(context, ComponentName(context, PlayerService::class.java))
    private val controllerFuture =
        MediaController.Builder(context, sessionToken).buildAsync()
    private lateinit var controller: MediaController

    init {
        PlayerService.onFlush = ::onFlush
        PlayerService.onFrequencyFFTReady = ::onFrequencyFFTReady
        PlayerService.onSpectrumReady = ::onSpectrumReady

        controllerFuture.addListener({
            refreshData()

            controller = controllerFuture.get()
            controller.addListener(listener)

            restoreCurrentUI()
        }, MoreExecutors.directExecutor())
    }

    private fun restoreCurrentUI() {
        for (i in 0..<controller.mediaItemCount) {
            val item = controller.getMediaItemAt(i)
            val mediaId = item.mediaId.toInt()
            val mediaIndex = repo.idToIndex.getOrDefault(mediaId, -1)

            addToPlaylist(index = mediaIndex, id = mediaId, invokeController = false)
        }

        isPlaying.value = controller.isPlaying
        isShuffling.value = controller.shuffleModeEnabled
        isRepeating.value = controller.repeatMode != Player.REPEAT_MODE_OFF
        isPlaying.value = controller.isPlaying

        onMediaSet(controller.currentMediaItemIndex)

        onPlayStatusChanged(controller.isPlaying)
    }

    private fun onSpectrumReady(chartData: FloatArray, maxRawAmplitude: Float) {
        fifoBitmap?.add(chartData, maxRawAmplitude)
    }

    private fun onFullSpectrumReady(bitmap: Bitmap) = ui {
        spectrumChartData.value = FFTChartData(bitmap = bitmap)
    }

    private fun onFlush(sampleRateHz: Int, channelCount: Int, encoding: String) {
        ui {
            metadata.value = metadata.value?.apply {
                this.encoding = encoding
            }
        }
    }

    private fun onPlayStatusChanged(isPlaying: Boolean) {
        setPlayStatus(isPlaying)
    }

    private fun onPlaylistFinished() {
//        pause()
    }

    private fun startRequestingSeekerPositions() = ui {
        while (controller.isPlaying) {
            currentProgress.value = controller.getCurrentPosition()
            delay(500L)
        }
    }

    private fun onFrequencyFFTReady(frequencyMap: MutableMap<Int, Float>, maxAmplitude: Float) {
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

    private fun setStates(data: MediaData, playlistIndex: Int) {
        if (data.path.isEmpty()) {
            return
        }

        "----> setStates: ${data.id} ${data.title} ${data.subtitle}".e
        currentData.value = data

        setCurrentActiveIndexes(data, playlistIndex)

        isFavorite.value = repo.iter.value?.isFavorite
        currentProgress.value = 0
    }

    private fun setCurrentActiveIndexes(data: MediaData, playlistIndex: Int) {
        "--> active playlistIndex: $playlistIndex".e
        currentActiveIndex.value = playlistIndex

        "--> active mediaIndex: ${data.path} ${data.id} ${repo.idToIndex.keys} ${repo.idToIndex.values}".e
        currentActiveMediaIndex.value = repo.idToIndex[data.id]
    }

    private fun resetUi() {
        currentData.value = MediaData.empty
        currentActiveIndex.value = -1

        currentProgress.value = 0
    }

    private fun onMediaSet(index: Int) {
        if (playlist.value?.isEmpty() == true || playlist.value == null) {
            resetUi()
        } else {
            setStates(playlist.value!![index], index)
        }
    }

    fun prev() {
        controller.seekToPreviousMediaItem()

        val asd = controller.getMediaItemAt(controller.currentMediaItemIndex)

        "----> asd: ${asd.mediaId} ${asd.mediaMetadata.title} ${asd.mediaMetadata.subtitle}".e
    }

    fun next() {
        controller.seekToNextMediaItem()

        val asd = controller.getMediaItemAt(controller.currentMediaItemIndex)
        "----> asd: ${asd.mediaId} ${asd.mediaMetadata.title} ${asd.mediaMetadata.subtitle}".e
    }

    fun setSeekPosition(progress: Long) {
        "Setting progress to: $progress".e
        currentProgress.value = progress
        controller.seekTo(progress)
    }

    private fun prepareFifoBitmap(path: String, baseColor: Int) {
        fifoBitmap?.recycle()

        val width = File(path).length() / SAMPLE_SIZE
        val height = SAMPLE_SIZE / 4
        val bufferSize = width.toInt() * height * 2

        fifoBitmap = FifoBitmap(
            bufferSize,
            min(width.toInt(), 500),
            height,
            baseColor,
            ::onFullSpectrumReady
        )
    }

    private fun extractMetadata(path: String): MediaMetadata {
        val extractor = MediaExtractor().apply { setDataSource(path) }
        val format = extractor.getTrackFormat(0)
        val maxBitrate = format.getIntegerSafe(KEY_MAX_BIT_RATE)
        val bitrate = format.getIntegerSafe(KEY_BIT_RATE)
        val sampleRate = format.getIntegerSafe(KEY_SAMPLE_RATE)
        val channelCount = format.getIntegerSafe(KEY_CHANNEL_COUNT)
        val mime = format.getStringSafe(KEY_MIME)

        return MediaMetadata(
            maxBitrate = maxBitrate,
            bitrate = bitrate,
            sampleRateHz = sampleRate,
            channelCount = channelCount,
            mime = mime
        )
    }

    private fun setPlayStatus(isPlayingValue: Boolean) {
        "--> setPlayStatus: $isPlayingValue".e
        isPlaying.value = isPlayingValue

        if (isPlayingValue) {
            currentActiveState.value = ItemState.PLAYING
            val path = currentData.value!!.path

            prepareFifoBitmap(path, baseColor)
            val tempMetadata = extractMetadata(path)

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
        controller.play()
    }

    fun pause() {
        isPlaying.value = false
        controller.pause()
    }

    fun onPause() {
        controller.playWhenReady = false
        pause()
    }

    fun shuffle() {
        isShuffling.value = !isShuffling.value!!
        controller.shuffleModeEnabled = isShuffling.value!!
    }

    fun repeat() {
        isRepeating.value = !isRepeating.value!!
        controller.repeatMode =
            if (isRepeating.value!!) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
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

    fun showInfo(fragment: MainFragment, mediaId: Int) {
        fragment.findNavController().navigate(MainFragmentDirections.toInfo(mediaId))
    }

    fun removeFromPlaylist(index: Int, id: Int) {
        val listIndex = repo.idToIndex.getOrDefault(id, -1)
        if (listIndex >= 0) {
            allMediaActiveMapData.value = allMediaActiveMapData.value?.apply {
                controller.removeMediaItem(index)
                remove(listIndex)
            }

            updatePlaylist()
        }
    }

    private fun getIdToPlaylistIndexMap(): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (i in 0 until controller.mediaItemCount) {
            map[controller.getMediaItemAt(i).mediaId.toInt()] = i
        }

        return map
    }

    private fun updatePlaylist() {
        playlist.value = getIdToPlaylistIndexMap().map {
            repo.iter.get(repo.idToIndex.getOrDefault(it.key, -1))
        }
    }

    fun setAsPlaylist(index: Int, id: Int) {
        controller.clearMediaItems()
        allMediaActiveMapData.value = mutableMapOf()
//        idToListIndexMap.clear()

        controller.playWhenReady = true
        "Playing single media at $index (playlist): id = $id".e
        addToPlaylist(index, id)
    }

    /*
    * index - index in media RecyclerView
    * */
    fun addToPlaylist(index: Int, id: Int, invokeController: Boolean = true) {
        val item = repo.iter.get(index)
        val path = item.path
        val idToPlaylistIndexMap = getIdToPlaylistIndexMap()

        // Add/Remove highlight from Media screen
        val isActive = allMediaActiveMapData.value?.getOrDefault(index, false)
        allMediaActiveMapData.value = allMediaActiveMapData.value?.apply {
            if (isActive == true) {
                "--> Deactivating $index in Media".e
                remove(index)
            } else {
                "--> Highlighting $index in Media; id = $id".e
                put(index, true)
            }
        }

        // Add/Remove from Playlist
        if (invokeController) {
            val indexInPlaylist = idToPlaylistIndexMap.getOrDefault(id, -1)
            if (indexInPlaylist != -1) {
                "--> Removing $indexInPlaylist from playlist".e
                controller.removeMediaItem(indexInPlaylist)
            } else {
                "--> Adding $path to playlist with ID: $id".e
                val app = getApplication<App>()
                val imageUri = Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(app.resources.getResourcePackageName(item.imageId))
                    .appendPath(app.resources.getResourceTypeName(item.imageId))
                    .appendPath(app.resources.getResourceEntryName(item.imageId))
                    .build()

                add(path, id, item.title, item.subtitle, imageUri)
            }
        }

        updatePlaylist()
    }

    private fun add(
        uri: String,
        mediaId: Int,
        displayTitle: String,
        albumArtist: String,
        artworkUri: Uri,
    ) {
        ("--> Adding media: $uri\n" +
                "$mediaId\n" +
                "$displayTitle\n" +
                "$albumArtist\n" +
                "$artworkUri").e
        val item = MediaItem.Builder()
            .setMediaId(mediaId.toString())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setDisplayTitle(displayTitle)
                    .setAlbumArtist(albumArtist)
                    .setArtworkUri(if (uri.hasEmbeddedPicture) null else artworkUri)
                    .build()
            )
            .setUri(uri)
            .build()

        controller.addMediaItem(item)
    }

    fun seekToDefaultPosition(index: Int) {
        controller.playWhenReady = true
        controller.seekToDefaultPosition(index)
        controller.play()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Loading media to memory
            repo.read(getApplication())

            // Updating Media panel
            viewModelScope.launch(Dispatchers.Main) {
                mediaList.value = repo.dataList
            }
        }
    }
}


private fun MediaFormat.getIntegerSafe(key: String): Int =
    try {
        getInteger(key)
    } catch (ex: Exception) {
        -1
    }

private fun MediaFormat.getStringSafe(key: String): String =
    try {
        getString(key) ?: "-"
    } catch (ex: Exception) {
        "-"
    }