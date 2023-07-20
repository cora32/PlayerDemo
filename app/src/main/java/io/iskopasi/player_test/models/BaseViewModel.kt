package io.iskopasi.player_test.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel(
    context: Application,
    isLoadingInitialValue: Boolean
) : AndroidViewModel(context) {
    val isLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData(isLoadingInitialValue)
    }

    fun bg(task: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        task()
    }

    fun main(task: () -> Unit) = viewModelScope.launch(Dispatchers.Main) {
        task()
    }
}