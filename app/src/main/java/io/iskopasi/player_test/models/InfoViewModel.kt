package io.iskopasi.player_test.models

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.LyricsStates
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
    private val currentData: MutableLiveData<MediaData>
        get() = repo.currentData
    val isLoading = MutableLiveData(true)
    val lyrics = MutableLiveData("")
    val error = MutableLiveData("")

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
                val lyricsState = repo.getLyrics(name)

                ui {
                    when (lyricsState) {
                        LyricsStates.LYR_OK -> {
                            val text = lyricsState.text?.replace("\\n", "\n")

                            if (text == null) {
                                error.value = ContextCompat.getString(
                                    getApplication(),
                                    R.string.no_lyrics
                                )
                            } else {
                                repo.cacheText(name, text)
                                lyrics.value = text
                                error.value = ""
                            }
                        }

                        LyricsStates.LYR_ERROR -> error.value = ContextCompat.getString(
                            getApplication(),
                            R.string.lyrics_error
                        )

                        LyricsStates.LYR_NOT_FOUND -> error.value = ContextCompat.getString(
                            getApplication(),
                            R.string.no_lyrics
                        )

                        LyricsStates.LYR_NOT_AVAIL -> error.value = ContextCompat.getString(
                            getApplication(),
                            R.string.lyrics_not_avail
                        )

                        else -> error.value =
                            ContextCompat.getString(getApplication(), R.string.no_lyrics)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                ui {
                    error.value = "${
                        ContextCompat.getString(
                            getApplication(),
                            R.string.lyrics_error
                        )
                    } ${ex.message}"
                }
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

    fun getItemById(id: Int): MediaData = repo.dataList[repo.idToIndex[id]!!]
}