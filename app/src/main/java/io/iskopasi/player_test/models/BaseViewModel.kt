package io.iskopasi.player_test.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel(isLoadingInitialValue: Boolean = false) : ViewModel() {
    val isLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData(isLoadingInitialValue)
    }

    fun bg(task: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            task()
        }
    }
}