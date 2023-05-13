package io.iskopasi.player_test.models

import android.app.Application
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val currentTrack: MutableLiveData<MediaFile> by lazy {
        MutableLiveData(MediaFile())
    }

    fun read() {
        bg {
            repo.read(getApplication()).run {
                if (isNotEmpty()) {
                    currentTrack.postValue(first())
                }
            }
        }
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