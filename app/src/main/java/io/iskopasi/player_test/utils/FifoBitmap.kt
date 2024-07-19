package io.iskopasi.player_test.utils

import android.graphics.Bitmap


class FifoBitmap(
    bufferSize: Int,
    val width: Int,
    val height: Int,
    val baseColor: Int
) {
    private var pixels = IntArray(bufferSize)
    private var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var x = 0

    fun add(
        data: MutableList<Float>,
        maxRawAmplitude: Float,
        onFullSpectrumReady: (Bitmap) -> Unit
    ) {
        val valueFactor: Float = 255 / maxRawAmplitude
        val length = data.size / 2

        for (y in 0 until length) {
            val alpha: Int = (data[length - y - 1] * valueFactor).toInt()
            val currentPixel: Int = (baseColor and 0x00ffffff) or (alpha shl 24)
            val position = x + (y * width)

            pixels[position] = currentPixel
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        onFullSpectrumReady(bitmap)

        x++
    }

    fun recycle() {
        bitmap.recycle()
    }
}