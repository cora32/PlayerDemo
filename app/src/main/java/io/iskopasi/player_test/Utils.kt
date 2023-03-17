package io.iskopasi.player_test

import android.util.Log

object Utils {
    val String.d: Unit
        get() {
            Log.d("--> DEBUG:", this)
        }

    val String.e: Unit
        get() {
            Log.e("--> ERR:", this)
        }
}