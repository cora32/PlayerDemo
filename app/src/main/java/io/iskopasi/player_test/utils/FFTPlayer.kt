package io.iskopasi.player_test.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioFormat
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
import io.iskopasi.player_test.models.MediaMetadata
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.views.FFTView.Companion.FREQUENCY_BAND_LIMITS
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


@UnstableApi
class FFTPlayer(
    context: Context,
    onHandleBuffer: (
        MutableMap<Int, Float>,
        Float
    ) -> Unit,
    onFullSpectrumReady: (
        Bitmap,
    ) -> Unit,
    onPlayStatusChanged: (Boolean) -> Unit,
    onPlaylistFinished: () -> Unit,
    onMediaSet: (Int) -> Unit,
    onFlush: (Int, Int, String) -> Unit,
) {
    private var fifoBitmap: FifoBitmap? = null

    companion object {
        const val SAMPLE_SIZE = 4096
    }

    private val listener by lazy {
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                "->> onIsPlayingChanged onIsPlayingChanged onIsPlayingChanged: $isPlaying".e
                if (isPlaying) {
//                    onPlaylistStarted()
                } else {
                    // Not playing because playback is paused, ended, suppressed, or the player
                    // is buffering, stopped or failed. Check player.playWhenReady,
                    // player.playbackState, player.playbackSuppressionReason and
                    // player.playerError for details.

                    onPlaylistFinished()
                }
            }

//            override fun onPlaybackStateChanged(playbackState: Int) {
//                super.onPlaybackStateChanged(playbackState)
//
//                when (playbackState) {
//                    Player.STATE_IDLE -> {}
//                    Player.STATE_BUFFERING -> {}
//                    Player.STATE_READY -> {}
//                    Player.STATE_ENDED -> {}
//                }
//            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)

                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    onMediaSet(player.currentMediaItemIndex)
                } else if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    onPlayStatusChanged(isPlaying)
                }

//                if (
//                    events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
//                    events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
//                ) {
//                    uiModule.updateUi(player)
//                }
            }
        }
    }
    private val player by lazy {
        ExoPlayer.Builder(context)
            .setRenderersFactory(rendererFactory)
            .build().apply {
                addListener(listener)
            }
    }

    val isPlaying: Boolean
        get() = player.isPlaying

    private val extractor by lazy {
        FullSampleExtractor(onFullSpectrumReady)
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

                onFlush(sampleRateHz, channelCount, encodingString)
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

                fifoBitmap?.add(chartData, maxRawAmplitude, onFullSpectrumReady)

                onHandleBuffer.invoke(frequencyMap, maxAvgAmplitude)
            }
        }
    }

    private val rendererFactory by lazy {
        object : DefaultRenderersFactory(context) {
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

    init {
        player.prepare()
//        System.loadLibrary("player_test")
    }

//    private external fun fft(
//        src: FloatArray,
//        dst: FloatArray,
//    ): Bitmap

    fun prepare() {
        player.prepare()
    }

    fun play(auto: Boolean = false): MediaItem? {
        player.playWhenReady = auto

        player.play()
        return player.currentMediaItem
    }

    fun seekToDefaultPosition(index: Int) {
        player.seekToDefaultPosition(index)
    }

    fun next(): Int {
        player.seekToNextMediaItem()
        return player.currentMediaItemIndex
    }

    fun prev(): Int {
        player.seekToPreviousMediaItem()
        return player.currentMediaItemIndex
    }

//    fun set(uri: String, indexInMedia: Int) {
//        val item = MediaItem.Builder()
//            .setMediaId(indexInMedia.toString())
//            .setUri(uri)
//            .build()
//        player.setMediaItem(item)
//    }

    fun add(uri: String, indexInMedia: Int) {
        val item = MediaItem.Builder()
            .setMediaId(indexInMedia.toString())
            .setUri(uri)
            .build()
        player.addMediaItem(item)

        return
    }

    fun remove(index: Int) {
        player.removeMediaItem(index)
    }

    fun getPlaylistIds(): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until player.mediaItemCount) {
            result.add(player.getMediaItemAt(i).mediaId.toInt())
        }

        return result
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

    fun seekTo(progress: Long) {
        player.seekTo(progress)
    }

    fun requestFullSpectrum(path: String, baseColor: Int) = bg {
//        extractor.extractJNI(path, baseColor)
    }

    fun prepareFifoBitmap(path: String, baseColor: Int) {
        fifoBitmap?.recycle()

        val width = File(path).length() / SAMPLE_SIZE
        val height = SAMPLE_SIZE / 4
        val bufferSize = width.toInt() * height * 2

        fifoBitmap = FifoBitmap(
            bufferSize,
            min(width.toInt(), 500),
            height,
            baseColor
        )
    }

    fun getCurrentPosition() = player.currentPosition

    fun clearPlaylist() {
        player.clearMediaItems()
    }

    fun setAutoPlay(auto: Boolean) {
        player.playWhenReady = auto
    }

    fun extractMetadata(path: String): MediaMetadata = extractor.extractMetadata(path)
}