package io.iskopasi.player_test.utils

import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi


class FifoBitmap(
    bufferSize: Int,
    private val width: Int,
    private val height: Int,
    private val baseColor: Int,
    private val onFullSpectrumReady: (Bitmap) -> Unit
) {
    private var pixels = IntArray(bufferSize)
    private var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var x = 0

    @UnstableApi
    fun add(
        data: FloatArray,
        maxRawAmplitude: Float
    ) {
        val valueFactor: Float = 255 / maxRawAmplitude

        for (y in 0 until height) {
            val alpha: Int = (data[y + height] * valueFactor).toInt()
            val currentPixel: Int = (baseColor and 0x00ffffff) or (alpha shl 24)
            val position = x + (y * width)

            pixels[position] = currentPixel
        }

        bitmap.setPixels(pixels, x + 1, width, 0, 0, width, height)

        onFullSpectrumReady(bitmap)

        if (x++ >= width) {
            x = 0
        }
    }

    fun recycle() {
        bitmap.recycle()
    }
}