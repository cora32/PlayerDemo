package io.iskopasi.player_test.utils

import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import io.iskopasi.player_test.utils.FFTPlayer.Companion.SAMPLE_SIZE


class FifoBitmap(
    bufferSize: Int,
    private val width: Int,
    private val height: Int,
    private val baseColor: Int
) {
    private var pixels = IntArray(bufferSize)
    private var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var x = 0

    @UnstableApi
    fun add(
        data: FloatArray,
        maxRawAmplitude: Float,
        onFullSpectrumReady: (Bitmap) -> Unit
    ) {
        val valueFactor: Float = 255 / maxRawAmplitude
//        val dataSize = data.size / 2
        val bitmapHeight = SAMPLE_SIZE / 4

        for (y in 0 until bitmapHeight) {
            val alpha: Int = (data[y + bitmapHeight] * valueFactor).toInt()
            val currentPixel: Int = (baseColor and 0x00ffffff) or (alpha shl 24)
            val position = x + (y * width)

            pixels[position] = currentPixel
        }

        bitmap.setPixels(pixels, x + 1, width, 0, 0, width, height)

        onFullSpectrumReady(bitmap)

        x++
    }

    fun recycle() {
        bitmap.recycle()
    }
}