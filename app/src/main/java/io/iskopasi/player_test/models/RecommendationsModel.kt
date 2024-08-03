package io.iskopasi.player_test.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.adapters.RecommendItemData
import io.iskopasi.player_test.room.MediaDao
import javax.inject.Inject

@HiltViewModel
class RecommendationsModel @Inject constructor(
    context: Application,
    private val repo: Repo,
    private val dao: MediaDao,
) : AndroidViewModel(context) {
    private var shown = false
    val recommendTracks = MutableLiveData<List<RecommendItemData>>(listOf())
    val recommendAlbums = MutableLiveData<List<RecommendItemData>>(listOf())

    fun show() {
        if (!shown) {
            shown = true

            recommendTracks.value = repo.recommendTracks
            recommendAlbums.value = repo.recommendAlbums
        }
    }
}