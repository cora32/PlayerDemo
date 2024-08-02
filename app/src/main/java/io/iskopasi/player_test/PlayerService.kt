package io.iskopasi.player_test

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Handler
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.paramsen.noise.Noise
import io.iskopasi.player_test.activities.MainActivity
import io.iskopasi.player_test.models.SAMPLE_SIZE
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.views.FFTView.Companion.FREQUENCY_BAND_LIMITS
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@UnstableApi
class PlayerService : MediaSessionService() {
    companion object {
        var onFlush: ((Int, Int, String) -> Unit)? = null
        var onFrequencyFFTReady: ((MutableMap<Int, Float>, Float) -> Unit)? = null
        var onSpectrumReady: ((FloatArray, Float) -> Unit)? = null
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private val pendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private val bufferSink by lazy {
        object : AudioBufferSink {
            private var sampleRateHz = 0
            private var channelCount = 0
            private var encoding = 0
            private val noise = Noise.real(SAMPLE_SIZE)
            private var fftBuffer = EMPTY_BUFFER
            private val chartData = FloatArray(SAMPLE_SIZE)
            private val src = FloatArray(SAMPLE_SIZE)
            private val dst = FloatArray(SAMPLE_SIZE + 2) { 0f } //real output length equals src+2
            private val frequencyMap = mutableMapOf<Int, Float>()
            private var isProcessing = false

            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                this.sampleRateHz = sampleRateHz
                this.channelCount = channelCount
                this.encoding = encoding

                val encodingString = when (encoding) {
                    AudioFormat.ENCODING_INVALID -> "ENCODING_INVALID"
                    AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
                    AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
                    AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
                    AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
                    AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
                    AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
                    AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
                    AudioFormat.ENCODING_MP3 -> "ENCODING_MP3"
                    AudioFormat.ENCODING_AAC_LC -> "ENCODING_AAC_LC"
                    AudioFormat.ENCODING_AAC_HE_V1 -> "ENCODING_AAC_HE_V1"
                    AudioFormat.ENCODING_AAC_HE_V2 -> "ENCODING_AAC_HE_V2"
                    AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
                    AudioFormat.ENCODING_DOLBY_TRUEHD -> "ENCODING_DOLBY_TRUEHD"
                    AudioFormat.ENCODING_AAC_ELD -> "ENCODING_AAC_ELD"
                    AudioFormat.ENCODING_AAC_XHE -> "ENCODING_AAC_XHE"
                    AudioFormat.ENCODING_AC4 -> "ENCODING_AC4"
                    AudioFormat.ENCODING_E_AC3_JOC -> "ENCODING_E_AC3_JOC"
                    AudioFormat.ENCODING_DOLBY_MAT -> "ENCODING_DOLBY_MAT"
                    AudioFormat.ENCODING_OPUS -> "ENCODING_OPUS"
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> "ENCODING_PCM_24BIT_PACKED"
                    AudioFormat.ENCODING_PCM_32BIT -> "ENCODING_PCM_32BIT"
                    AudioFormat.ENCODING_MPEGH_BL_L3 -> "ENCODING_MPEGH_BL_L3"
                    AudioFormat.ENCODING_MPEGH_BL_L4 -> "ENCODING_MPEGH_BL_L4"
                    AudioFormat.ENCODING_MPEGH_LC_L3 -> "ENCODING_MPEGH_LC_L3"
                    AudioFormat.ENCODING_MPEGH_LC_L4 -> "ENCODING_MPEGH_LC_L4"
                    AudioFormat.ENCODING_DTS_UHD_P1 -> "ENCODING_DTS_UHD_P1"
                    AudioFormat.ENCODING_DRA -> "ENCODING_DRA"
                    AudioFormat.ENCODING_DTS_HD_MA -> "ENCODING_DTS_HD_MA"
                    AudioFormat.ENCODING_DTS_UHD_P2 -> "ENCODING_DTS_UHD_P2"
                    AudioFormat.ENCODING_DSD -> "ENCODING_DSD"
                    else -> "invalid encoding $encoding"
                }

                onFlush?.invoke(sampleRateHz, channelCount, encodingString)
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                if (isProcessing) return

                isProcessing = true

                // Cannot access buffer in different thread
                fillBuffer(buffer)

                bg {
                    processFFT()

                    isProcessing = false
                }
            }

            private fun fillBuffer(buffer: ByteBuffer) {
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
            }

            private fun processFFT() {
                val maxLimit = min(fftBuffer.limit(), SAMPLE_SIZE)

                for (i in 0 until maxLimit step 2) {
                    val short = fftBuffer.getShort(i)

                    src[i] = short.toFloat()
                }

                // DC bin is located at index 0, 1, nyquist at index n-2, n-1
                val fft: FloatArray = noise.fft(src, dst)

                // Fill amplitude data
                // The resulting graph is mirrored, so get only left part
                for (i in 0 until fft.size / 2) {
                    val real = fft[i * 2]
                    val imaginary = fft[i * 2 + 1]
                    val amplitude = sqrt(real.pow(2f) + imaginary.pow(2f))

                    chartData[i] = amplitude
                }

                val size = SAMPLE_SIZE / 2
                var startIndex = 0
                var maxAvgAmplitude = 0f
                var maxRawAmplitude = 0f

                // Group amplitudes by their frequencies
                for (frequency in FREQUENCY_BAND_LIMITS) {
                    val endIndex = floor(frequency / 20000f * size).toInt()

                    var accum = 0f
                    // Sum amplitudes for current frequency bin
                    for (i in startIndex until endIndex) {
                        val amplitude = chartData[i]
                        accum += amplitude

                        // Find max amplitude of all frequencies
                        if (amplitude > maxRawAmplitude) {
                            maxRawAmplitude = amplitude
                        }
                    }

                    // Sometimes frequency group range can be 0
                    val amplitude = if (endIndex - startIndex == 0)
                        0f
                    else {
                        // Get avg amplitude for frequency bin
                        accum / (endIndex - startIndex).toFloat()
                    }

                    // Fill avg amplitude for each frequency
                    frequencyMap[frequency] = amplitude

                    // Get max avg amplitude
                    if (amplitude > maxAvgAmplitude) {
                        maxAvgAmplitude = amplitude
                    }

                    startIndex = endIndex
                }

                onSpectrumReady?.invoke(chartData, maxRawAmplitude)

                onFrequencyFFTReady?.invoke(frequencyMap, maxAvgAmplitude)
            }
        }
    }

    private val rendererFactory by lazy {
        object : DefaultRenderersFactory(this) {
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
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(rendererFactory)
            .build().apply {
                prepare()
            }
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}