package io.iskopasi.player_test.models

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.utils.LoopIterator
import io.iskopasi.player_test.utils.Utils.e
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MediaState {
    IDLE,
    PREPARED,
    PLAYING,
    PAUSED,
    STOPPED
}

@HiltViewModel
class PlayerXMLViewModel @Inject constructor(
    context: Application,
    private val repo: Repo,
) : BaseViewModel(
    context = context,
    isLoadingInitialValue = true
) {
    private val mediaPlayer = MediaPlayer().apply {
        setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
            this@PlayerXMLViewModel.reset()
            mediaState.value = MediaState.IDLE
            true
        }
        setOnPreparedListener {
            mediaState.value = MediaState.PREPARED
            this@PlayerXMLViewModel.play()
        }
        setOnCompletionListener {
            this@PlayerXMLViewModel.reset()
            mediaState.value = MediaState.IDLE
        }
        setOnSeekCompleteListener {

        }
    }
    val image: MutableLiveData<Bitmap?> by lazy {
        MutableLiveData(null)
    }
    val currentTrack: MutableLiveData<MediaFile> by lazy {
        MutableLiveData(MediaFile())
    }
    val mediaState: MutableLiveData<MediaState> by lazy {
        MutableLiveData(MediaState.IDLE)
    }
    val position: MutableLiveData<Int> by lazy {
        MutableLiveData(0)
    }
    val list: MutableLiveData<List<MediaFile>> by lazy {
        MutableLiveData(emptyList())
    }
    private var iter = LoopIterator<MediaFile>()
    private lateinit var timerJob: Job

    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                position.value = mediaPlayer.currentPosition
                delay(500)
            }
        }
    }

    private fun setImage(path: String?) {
        if (path == null) {
            image.postValue(null)
        } else {
//            bg {
//                image.postValue(repo.getImageBitmap(path))
//            }
        }
    }

    fun read() {
        isLoading.value = true

        bg {
            try {
                iter = LoopIterator(repo.read(getApplication()).apply {
                    if (isNotEmpty()) {
                        main { setTrack(first()) }

                        list.postValue(this)
                    }
                })
            } catch (ex: Exception) {
                throw ex
            }
        }.invokeOnCompletion {
            isLoading.postValue(false)

            it?.apply { throw this@apply }
        }
    }

    private fun setTrack(track: MediaFile?): MediaFile? {
        "---> Setting track: $track; state: ${mediaState.value}".e
        currentTrack.value = track
        setImage(track?.path)

        val previousState = mediaState.value
        reset()

        if (previousState == MediaState.PLAYING) {
            prepare(track)
        }

        return track
    }

    fun next() = setTrack(iter.next())

    fun prev() =
        setTrack(iter.prev())

    fun play() {
        if (mediaPlayer.isPlaying) {
            pause()
        } else {
            "---> Current state: ${mediaState.value}".e
            when (mediaState.value) {
                MediaState.IDLE -> prepare(currentTrack.value)
                MediaState.PAUSED, MediaState.PREPARED -> start()
                MediaState.PLAYING -> null
                MediaState.STOPPED -> null
                null -> null
            }
        }
    }

    fun play(id: Int) {
        setTrack(iter.setIndex(id))
        play()
    }

    fun stop() {
        position.postValue(0)
        mediaPlayer.stop()
        mediaState.value = MediaState.STOPPED
    }

    fun pause() {
        mediaPlayer.pause()
        mediaState.value = MediaState.PAUSED
        resetTimer()
    }

    fun reset() {
        position.postValue(0)
        mediaPlayer.reset()
        mediaState.value = MediaState.IDLE
        resetTimer()
    }

    private fun resetTimer() {
        when (mediaState.value) {
            MediaState.PLAYING, MediaState.PAUSED -> timerJob.cancel()
            else -> {}
        }
    }

    fun start() {
        mediaPlayer.start()
        mediaState.value = MediaState.PLAYING
        startTimer()
    }

    private fun prepare(track: MediaFile?) {
        track?.let { file ->
            mediaPlayer.reset()
            "--> Preparing: ${file.path}".e
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepareAsync()
        }
    }

    fun onSeek(value: Int) {
        "onSeek: $value".e
        mediaPlayer.seekTo(value)
    }

//    companion object {
//        fun getFactory(context: Application) = viewModelFactory {
//            initializer {
//                PlayerXMLViewModel(
//                    context = context,
//                    repo = Repo(),
//                )
//            }
//        }
//    }

}