package io.iskopasi.player_test.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel(isLoadingInitialValue: Boolean = false) : ViewModel() {
    val isLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData(isLoadingInitialValue)
    }
}