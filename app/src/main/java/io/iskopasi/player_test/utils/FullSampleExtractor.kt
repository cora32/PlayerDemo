package io.iskopasi.player_test.utils

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.core.graphics.ColorUtils
import androidx.media3.common.util.UnstableApi
import com.paramsen.noise.Noise
import io.iskopasi.player_test.utils.FFTPlayer.Companion.SAMPLE_SIZE
import io.iskopasi.player_test.utils.Utils.e
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt


@UnstableApi
class FullSampleExtractor(private val onFullSpectrumReady: (Bitmap) -> Unit) {
    companion object {
        const val BUFFER_SIZE = 1024 * 32
        const val TAG = "FullSampleExtractor"

        init {
            System.loadLibrary("player_test")
        }
    }

    //    private val extractor by lazy { MediaExtractor() }
    private var mFormat: MediaFormat? = null
    private var sampleSize: Int = 0
    private var maxAmplitude = 0f
    private val noise = Noise.real(SAMPLE_SIZE)

    fun extract(path: String, baseColor: Int) {
        "--> mFormat: ${mFormat.toString()}".e

//        for (i in 0 until extractor.trackCount) {
//            "formats: $i ${extractor.getTrackFormat(i)}".e
//        }

        // Copy all music data to buffer
        val data = getAllBytes(path)
        val bufferAll = data.first
        val size = data.second

        bufferAll.order(ByteOrder.nativeOrder())
        bufferAll.rewind()
        val sampleList = getSamples(bufferAll)
        val bitmap = getBitmap(sampleList, baseColor)
        onFullSpectrumReady.invoke(bitmap)
    }

    fun extractJNI(path: String, baseColor: Int) {
        maxAmplitude = 0f

        MediaExtractor().apply {
            setDataSource(path)
            mFormat = getTrackFormat(0)
            selectTrack(0)
            "---> Decoding...".e
            val decoded = decodeToMemory(path, true)
            release()

            "---> Decoding complete.".e
            val size = decoded.size * 2L
            val bufferDecodedData =
                ByteBuffer.allocateDirect(size.toInt()).order(ByteOrder.nativeOrder())
            for (short in decoded) {
                bufferDecodedData.putShort(short)
            }
            val bitmap = getSpectrumFromDecoded(bufferDecodedData, baseColor, size)

            onFullSpectrumReady.invoke(bitmap)
        }
    }

    private external fun getWavSpectrum(
        extractor: MediaExtractor,
        buffer: ByteBuffer,
        baseColor: Int,
        fileSize: Long
    ): Bitmap

    private external fun getSpectrumFromDecoded(
        buffer: ByteBuffer,
        baseColor: Int,
        fileSize: Long
    ): Bitmap

    //
    // Copied from https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/DecoderTest.java
    //
    @Throws(IOException::class)
    private fun decodeToMemory(path: String, reconfigure: Boolean): ShortArray {
        var reconfigure = reconfigure
        var decoded = ShortArray(0)
        var decodedIdx = 0
        val codec: MediaCodec
        var codecInputBuffers: Array<ByteBuffer?>
        var codecOutputBuffers: Array<ByteBuffer>
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        // assertEquals("wrong number of tracks", 1, extractor.trackCount)
        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME)
        // assertTrue("not an audio file", mime!!.startsWith("audio/"))

        codec = MediaCodec.createDecoderByType(mime!!)
        codec.configure(format, null,  /* surface */null,  /* crypto */0 /* flags */)
        codec.start()
        codecInputBuffers = codec.inputBuffers
        codecOutputBuffers = codec.outputBuffers
        if (reconfigure) {
            codec.stop()
            codec.configure(format, null,  /* surface */null,  /* crypto */0 /* flags */)
            codec.start()
            codecInputBuffers = codec.inputBuffers
            codecOutputBuffers = codec.outputBuffers
        }
        extractor.selectTrack(0)

        // start decoding
        val kTimeOutUs: Long = 5000
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        var noOutputCounter = 0

        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++
            if (!sawInputEOS) {
                val inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs)
                if (inputBufIndex >= 0) {
                    val dstBuf = codecInputBuffers[inputBufIndex]
                    var sampleSize =
                        extractor.readSampleData(dstBuf!!, 0 /* offset */)
                    var presentationTimeUs: Long = 0
                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.")
                        sawInputEOS = true
                        sampleSize = 0
                    } else {
                        presentationTimeUs = extractor.sampleTime
                    }
                    codec.queueInputBuffer(
                        inputBufIndex,
                        0,  /* offset */
                        sampleSize,
                        presentationTimeUs,
                        if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                    if (!sawInputEOS) {
                        extractor.advance()
                    }
                }
            }
            val res = codec.dequeueOutputBuffer(info, kTimeOutUs)
            if (res >= 0) {
                //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0
                }
                if (info.size > 0 && reconfigure) {
                    // once we've gotten some data out of the decoder, reconfigure it again
                    reconfigure = false
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC)
                    sawInputEOS = false
                    codec.stop()
                    codec.configure(format, null,  /* surface */null,  /* crypto */0 /* flags */)
                    codec.start()
                    codecInputBuffers = codec.inputBuffers
                    codecOutputBuffers = codec.outputBuffers
                    continue
                }
                val outputBufIndex = res
                val buf = codecOutputBuffers[outputBufIndex]
                if (decodedIdx + (info.size / 2) >= decoded.size) {
                    decoded = decoded.copyOf(decodedIdx + (info.size / 2))
                }
                var i = 0
                while (i < info.size) {
                    decoded[decodedIdx++] = buf.getShort(i)
                    i += 2
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.")
                    sawOutputEOS = true
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.outputBuffers
                Log.d(TAG, "output buffers have changed.")
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val oformat = codec.outputFormat
                Log.d(TAG, "output format has changed to $oformat")
            } else {
                Log.d(TAG, "dequeueOutputBuffer returned $res")
            }
        }
        codec.stop()
        codec.release()
        return decoded
    }

    // Produces OOM; doesn't decode MP3
    private fun getAllBytes(path: String): Pair<ByteBuffer, Long> {
        val bufferHolder = mutableListOf<ByteBuffer>()
        var size = 0
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        extractor.selectTrack(0)

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

        "--> Recvd ${result} chunks; total size: ${size * 2} bytes".e
        bufferHolder.clear()

        return Pair(result, size * 2L)
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
        } while (bufferAll.position() < limit - 2)

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