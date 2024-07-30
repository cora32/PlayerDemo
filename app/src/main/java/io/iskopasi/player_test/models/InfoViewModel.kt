package io.iskopasi.player_test.models

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.R
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.ui
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    context: Application,
    private val repo: Repo,
    private val dao: MediaDao,
) : AndroidViewModel(context) {
    val currentData: MutableLiveData<MediaData>
        get() = repo.currentData
    val isLoading = MutableLiveData(true)
    val lyrics = MutableLiveData("")
    val error = MutableLiveData("")
    private val noLyrics = ContextCompat.getString(context, R.string.no_lyrics)
    private val lyricsError = ContextCompat.getString(context, R.string.lyrics_error)

    init {
        getLyrics()
    }

    fun share(applicationContext: Context?, id: Int) {

    }

    private fun getLyrics() {
        isLoading.value = true

        bg {
            val name = getName()

            try {
                val lyricsText = repo.getLyrics(name)?.replace("\\n", "\n")

                ui {
                    if (lyricsText == null) {
                        error.value = noLyrics
                    } else {
                        repo.cacheText(name, lyricsText)
                        lyrics.value = lyricsText
                        error.value = ""
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ui { error.value = "$lyricsError ${ex.message}" }
            } finally {
                ui { isLoading.value = false }
            }
        }
    }

    private fun getName(): String {
        val artist = currentData.value!!.subtitle.replace(" ", "+")
        val trackName = currentData.value!!.title.replace(" ", "+")

        return "$artist+$trackName".toLowerCase(Locale.current)
    }
}