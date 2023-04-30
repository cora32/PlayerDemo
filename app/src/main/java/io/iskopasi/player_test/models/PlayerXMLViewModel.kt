package io.iskopasi.player_test.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.iskopasi.player_test.MediaFile
import io.iskopasi.player_test.Repo


class PlayerXMLViewModel(
    private val repo: Repo
) : BaseViewModel(true) {
    val currentTrack: MutableLiveData<MediaFile> by lazy {
        MutableLiveData(MediaFile())
    }

    fun read(
        context: Context
    ) {
        bg {
            repo.read(context).run {
                if (isNotEmpty()) {
                    currentTrack.postValue(first())
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlayerXMLViewModel(
                    repo = Repo(),
                )
            }
        }
    }

}