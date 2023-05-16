package io.iskopasi.player_test.models

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.LoopIterator
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.Repo
import javax.inject.Inject


@HiltViewModel
class PlayerXMLViewModel @Inject constructor(
    context: Application,
    private val repo: Repo,
) : BaseViewModel(
    context = context,
    isLoadingInitialValue = true
) {
    private var iter = LoopIterator<MediaFile>()
    val image: MutableLiveData<Bitmap?> by lazy {
        MutableLiveData(null)
    }
    val currentTrack: MutableLiveData<MediaFile> by lazy {
        MutableLiveData(MediaFile())
    }
    val isEmpty: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(true)
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

                    isEmpty.postValue(isEmpty())
                })
            } catch (ex: Exception) {
                isEmpty.postValue(true)
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
    }

    fun next() {
        setTrack(iter.next())
    }

    fun prev() {
        setTrack(iter.prev())
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