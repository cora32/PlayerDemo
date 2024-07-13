package io.iskopasi.player_test.utils

import android.content.Context
import android.os.Handler
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.paramsen.noise.Noise
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.views.FFTView.Companion.FREQUENCY_BAND_LIMITS
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


@UnstableApi
class FFTPlayer(context: Context, listener: (List<Float>, MutableMap<Int, Float>) -> Unit) {
    companion object {
        const val SAMPLE_SIZE = 4096
    }

    //    private val fftAudioProcessor = FFTAudioProcessor()
    private val bufferSink by lazy {
        object : AudioBufferSink {
            private var sampleRateHz = 0
            private var channelCount = 0
            private var encoding = 0
            private val noise = Noise.real(SAMPLE_SIZE)

            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                this.sampleRateHz = sampleRateHz
                this.channelCount = channelCount
                this.encoding = encoding

                "--> FFT flush $sampleRateHz $channelCount $encoding".e
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                var fftBuffer = EMPTY_BUFFER

                val limit = buffer.limit()
                val frameCount = (limit - buffer.position()) / (2 * channelCount)
                val singleChannelOutputSize = frameCount * 2

                if (fftBuffer.capacity() < singleChannelOutputSize) {
                    fftBuffer =
                        ByteBuffer.allocateDirect(singleChannelOutputSize)
                            .order(ByteOrder.nativeOrder())
                } else {
                    fftBuffer.clear()
                }

                // Each signal = short (2 bytes)
                // Use average of 2 channels as an input signal
                for (position in buffer.position() until limit step channelCount * 2) {
                    var sum = 0

                    for (channelIndex in 0 until channelCount) {
                        val current = buffer.getShort(position + channelIndex * 2)
                        sum += current
                    }

                    fftBuffer.putShort((sum / channelCount).toShort())
                }

                bg {
                    processFFT(fftBuffer)
                }
            }

            private fun processFFT(buffer: ByteBuffer) {
                val src = FloatArray(SAMPLE_SIZE)
                val dst = FloatArray(SAMPLE_SIZE + 2) { 0f } //real output length equals src+2

                val maxLimit = min(buffer.limit(), SAMPLE_SIZE)
                "--> limit: ${buffer.limit()} size: ${buffer.array().size}; maxLimit: $maxLimit".e
                for (i in 0 until maxLimit step 2) {
                    val byte = buffer.getShort(i)

                    src[i] = byte.toFloat()
                }

                // DC bin is located at index 0, 1, nyquist at index n-2, n-1
                val fft: FloatArray = noise.fft(src, dst)

                // Fill amplitude data
                // The resulting graph is mirrored, so get only left data
                val chartData = mutableListOf<Float>()
                for (i in 0 until fft.size / 2) {
                    val real = fft[i * 2]
                    val imaginary = fft[i * 2 + 1]

                    chartData.add(sqrt(real.pow(2f) + imaginary.pow(2f)))
                }

                val size = SAMPLE_SIZE / 2
                var startIndex = 0
                val frequencyMap = mutableMapOf<Int, Float>()

                // Group amplitude by frequency
                for (frequency in FREQUENCY_BAND_LIMITS) {
                    val endIndex = floor(frequency / 20_000.toFloat() * size).toInt()

                    var accum = 0f
                    for (i in startIndex until endIndex) {
                        accum += chartData[i]
                    }

                    if (endIndex - startIndex > 0) {
                        frequencyMap[frequency] = accum / (endIndex - startIndex).toFloat()
                    } else {
                        frequencyMap[frequency] = 0f
                    }
                    startIndex = endIndex
                }


                "---> frequencyMap: ${frequencyMap}".e
                listener.invoke(chartData, frequencyMap)
            }
        }
    }
    private val rendererFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            audioSink: AudioSink,
            eventHandler: Handler,
            eventListener: AudioRendererEventListener,
            out: ArrayList<Renderer>
        ) {
//            val sink = DefaultAudioSink.Builder(context)
//                .setAudioProcessors(arrayOf(fftAudioProcessor))
//                .build()

            val sink2 = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(TeeAudioProcessor(bufferSink)))
                .build()

            out.add(
                MediaCodecAudioRenderer(
                    context,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    sink2
                )
            )

            super.buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                enableDecoderFallback,
                audioSink,
                eventHandler,
                eventListener,
                out
            )
        }
    }
    private val player by lazy {
        ExoPlayer.Builder(context)
            .setRenderersFactory(rendererFactory)
            .build()
    }

    init {
        player.prepare()
    }

    fun play() {
        player.play()
    }

    fun add(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
    }

    fun pause() {
        player.pause()
    }

    fun shuffle(value: Boolean) {
        player.shuffleModeEnabled = value
    }

    fun repeat(value: Boolean) {
        player.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun seekTo(progress: Int) {
        player.seekTo(progress.toLong())
    }
}