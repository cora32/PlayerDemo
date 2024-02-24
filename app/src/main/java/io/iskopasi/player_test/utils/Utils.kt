package io.iskopasi.player_test.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Utils {
//    fun blur(bitmap: Bitmap): Bitmap {
//        return Toolkit.blur(bitmap, 8)
//    }

    val String.d: Unit
        get() {
            Log.d("--> DEBUG:", this)
        }

    val String.e: Unit
        get() {
            Log.e("--> ERR:", this)
        }

    fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)

    fun bg(task: () -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        task()
    }

    fun getSectorBitmap(
        view: View,
        x: Float, y: Float,
        width: Int, height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap!!)

        view.draw(canvas)

        return bitmap
    }
}