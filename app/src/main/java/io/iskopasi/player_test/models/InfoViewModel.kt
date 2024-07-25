package io.iskopasi.player_test.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.room.MediaDao
import javax.inject.Inject

class InfoViewModel @Inject constructor(
    context: Application,
    private val repo: Repo,
    private val dao: MediaDao
) : AndroidViewModel(context) {
    val currentData by lazy { MutableLiveData(repo.currentData) }
}