package io.iskopasi.player_test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
}