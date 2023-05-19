package io.iskopasi.player_test.models

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.LoopIterator
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.Utils.e
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
            mediaState.postValue(MediaState.IDLE)
            true
        }
        setOnPreparedListener {
            mediaState.postValue(MediaState.PREPARED)
            this@PlayerXMLViewModel.play()
        }
        setOnCompletionListener {
            this@PlayerXMLViewModel.reset()
            mediaState.postValue(MediaState.IDLE)
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
            bg {
                image.postValue(repo.getImage(path))
            }
        }
    }

    fun read() {
        isLoading.value = true

        bg {
            try {
                iter = LoopIterator(repo.read(getApplication()).apply {
                    if (isNotEmpty()) {
                        setTrack(first())
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

    private fun setTrack(track: MediaFile?) {
        currentTrack.postValue(track)
        setImage(track?.path)

        if (mediaState.value == MediaState.PLAYING) {
            reset()
            prepare()
        }
    }

    fun next() {
        setTrack(iter.next())
    }

    fun prev() {
        setTrack(iter.prev())
    }

    fun play() {
        if (mediaPlayer.isPlaying) {
            pause()
        } else {
            when (mediaState.value) {
                MediaState.IDLE -> prepare()
                MediaState.PAUSED, MediaState.PREPARED -> start()
                MediaState.PLAYING -> null
                MediaState.STOPPED -> null
                null -> null
            }
        }
    }

    fun stop() {
        position.postValue(0)
        mediaPlayer.stop()
        mediaState.postValue(MediaState.STOPPED)
    }

    fun pause() {
        mediaPlayer.pause()
        mediaState.postValue(MediaState.PAUSED)
        timerJob.cancel()
    }

    fun reset() {
        position.postValue(0)
        mediaPlayer.reset()
        mediaState.postValue(MediaState.IDLE)
        timerJob.cancel()
    }

    fun start() {
        mediaPlayer.start()
        mediaState.postValue(MediaState.PLAYING)
        startTimer()
    }

    private fun prepare() {
        currentTrack.value?.let { file ->
            mediaPlayer.reset()
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