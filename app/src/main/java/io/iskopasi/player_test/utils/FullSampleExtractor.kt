package io.iskopasi.player_test.utils

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.core.graphics.ColorUtils
import androidx.media3.common.util.UnstableApi
import com.paramsen.noise.Noise
import io.iskopasi.player_test.utils.FFTPlayer.Companion.SAMPLE_SIZE
import io.iskopasi.player_test.utils.Utils.e
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt


@UnstableApi
class FullSampleExtractor(private val onFullSpectrumReady: (Bitmap, Float) -> Unit) {
    companion object {
        const val BUFFER_SIZE = 1024 * 32

        init {
            System.loadLibrary("player_test")
        }
    }

    private val extractor by lazy { MediaExtractor() }
    private var mFormat: MediaFormat? = null
    private var sampleSize: Int = 0
    private var maxAmplitude = 0f
    private val noise = Noise.real(SAMPLE_SIZE)


    private external fun fft(
        extractor: MediaExtractor,
        buffer: ByteBuffer,
        base_color: Int
    ): Bitmap

    private fun getAllBytes(): ByteBuffer {
        val bufferHolder = mutableListOf<ByteBuffer>()
        var size = 0

        do {
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder())
            sampleSize = extractor.readSampleData(buffer, 0)
            bufferHolder.add(buffer)
            size += sampleSize
        } while (extractor.advance())
        extractor.release()

        val result = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())

        for (buff in bufferHolder) {
            result.put(buff)
        }

        "--> Recvd ${bufferHolder.size} chunks; total size: $size bytes"
        bufferHolder.clear()

        return result
    }

    private fun doFFT(chunk: ByteBuffer): List<Float> {
        val limit = chunk.limit()
        val src = FloatArray(limit)
        val dst = FloatArray(limit + 2) { 0f }

        for (i in 0 until limit step 2) {
            val short = chunk.getShort(i)

            src[i] = short.toFloat()
        }

        val fft: FloatArray = noise.fft(src, dst)

        val chartData = mutableListOf<Float>()
        for (i in 0 until fft.size / 2) {
            val real = fft[i * 2]
            val imaginary = fft[i * 2 + 1]
            val r1 = real.pow(2)
            val r2 = imaginary.pow(2)
            val amplitude = sqrt(r1 + r2)

            if (amplitude == Float.POSITIVE_INFINITY || amplitude == Float.NEGATIVE_INFINITY) {
                chartData.add(0f)
            } else {
                if (amplitude > maxAmplitude) {
                    maxAmplitude = amplitude
                }

                chartData.add(amplitude)
            }
        }

        return chartData
    }

    fun extract(path: String, baseColor: Int) {
        maxAmplitude = 0f

        extractor.setDataSource(path)
        mFormat = extractor.getTrackFormat(0)
        extractor.selectTrack(0)

        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder())
        val bitmap = fft(extractor, buffer, baseColor)


//        "--> mFormat: ${mFormat.toString()}".e

//        for (i in 0 until extractor.trackCount) {
//            "formats: $i ${extractor.getTrackFormat(i)}".e
//        }

//        val resultList = mutableListOf<List<Float>>()

        // Copy all music data to buffer
//        val bufferAll = getAllBytes()
//        bufferAll.order(ByteOrder.nativeOrder())
//        bufferAll.rewind()
//        val sampleList = getSamples(bufferAll)
//        val bitmap = getBitmap(sampleList, baseColor)

        onFullSpectrumReady.invoke(bitmap, maxAmplitude)
    }

    private fun getSamples(bufferAll: ByteBuffer): List<List<Float>> {
        val chunk = ByteBuffer.allocateDirect(SAMPLE_SIZE).order(ByteOrder.nativeOrder())
        val limit = bufferAll.limit()
        val result = mutableListOf<List<Float>>()

        // Slice whole buffer to chunks of SAMPLE_SIZE bytes
        do {
            chunk.putShort(bufferAll.getShort())

            // When chunk is filled, process it
            if (bufferAll.position() % SAMPLE_SIZE == 0) {
                chunk.rewind()
                val spectrumChunk = doFFT(chunk)

                // Set frequency data to result
                result.add(spectrumChunk)

                chunk.rewind()
            }
        } while (bufferAll.position() < limit)

        "--> Extracted ${result.size} buffers".e

        return result
    }

    private fun getBitmap(resultList: List<List<Float>>, baseColor: Int): Bitmap {
        val width = resultList.size
        val height = SAMPLE_SIZE / 4
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val valueFactor = 255 / maxAmplitude

        "--> width: $width; height: $height; valueFactor: $valueFactor".e

        for (x in 0 until width) {
            val buff = resultList[x]

            for (y in 0 until height) {
                val amplitude = buff[height - 1 - y]
                val pixel = ColorUtils.setAlphaComponent(
                    baseColor,
                    (amplitude * valueFactor).toInt()
                )

                bitmap.setPixel(x, y, pixel)
            }
        }

        return bitmap
    }
}