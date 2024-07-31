package io.iskopasi.player_test.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.iskopasi.player_test.PlayerService
import io.iskopasi.player_test.models.MediaMetadata
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.e
import java.io.File
import javax.inject.Inject
import kotlin.math.min


@UnstableApi
class FFTPlayer @Inject constructor(
    context: Context,
    onHandleBuffer: (MutableMap<Int, Float>, Float) -> Unit,
    private val onFullSpectrumReady: (Bitmap) -> Unit,
    onPlayStatusChanged: (Boolean) -> Unit,
    onPlaylistFinished: () -> Unit,
    onMediaSet: (Int) -> Unit,
    onFlush: (Int, Int, String) -> Unit
) {
    private var fifoBitmap: FifoBitmap? = null
    private var allowPlayingStateChange = true

    companion object {
        const val SAMPLE_SIZE = 4096
    }

    private val listener by lazy {
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
//                    onPlaylistStarted()
                } else {
                    // Not playing because playback is paused, ended, suppressed, or the player
                    // is buffering, stopped or failed. Check player.playWhenReady,
                    // player.playbackState, player.playbackSuppressionReason and
                    // player.playerError for details.

                    if (allowPlayingStateChange) onPlaylistFinished()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                when (playbackState) {
                    Player.STATE_IDLE -> {
                        "FFTPlayer: STATE_IDLE".e
                    }

                    Player.STATE_BUFFERING -> {
                        "FFTPlayer: STATE_BUFFERING".e
                    }

                    Player.STATE_READY -> {
                        "FFTPlayer: STATE_READY".e
                    }

                    Player.STATE_ENDED -> {
                        "FFTPlayer: STATE_ENDED".e
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)

                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    if (allowPlayingStateChange) onMediaSet(player.currentMediaItemIndex)
                } else if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    if (allowPlayingStateChange) onPlayStatusChanged(isPlaying)
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

    val isPlaying: Boolean
        get() = player.isPlaying

    private val extractor by lazy {
        FullSampleExtractor(onFullSpectrumReady)
    }

    private val player: ExoPlayer
        get() = PlayerService.player

    init {
        player.addListener(listener)
        player.pauseAtEndOfMediaItems = true
        player.prepare()

        PlayerService.onHandleBuffer = onHandleBuffer
//        PlayerService.onFullSpectrumReady = onFullSpectrumReady
        PlayerService.onPlayStatusChanged = onPlayStatusChanged
        PlayerService.onPlaylistFinished = onPlaylistFinished
        PlayerService.onMediaSet = onMediaSet
        PlayerService.onFlush = onFlush
        PlayerService.addBitmap = ::addBitmap

//        System.loadLibrary("player_test")
    }

    private fun addBitmap(chartData: FloatArray, maxRawAmplitude: Float) {
        fifoBitmap?.add(chartData, maxRawAmplitude)
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

    fun add(
        uri: String,
        indexInMedia: Int,
        displayTitle: String,
        albumArtist: String
    ) {
        val item = MediaItem.Builder()
            .setMediaId(indexInMedia.toString())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setDisplayTitle(displayTitle)
                    .setAlbumArtist(albumArtist)
                    .build()
            )
            .setUri(uri)
            .build()
        player.addMediaItem(item)
    }

    fun remove(index: Int) {
        player.removeMediaItem(index)
    }

    fun getIdToPlaylistIndexMap(): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (i in 0 until player.mediaItemCount) {
            map[player.getMediaItemAt(i).mediaId.toInt()] = i
        }

        return map
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
        allowPlayingStateChange = false
        val wasPlaying = isPlaying
        player.seekTo(progress)
        allowPlayingStateChange = true

        // seekTo() interrupts playing: https://github.com/google/ExoPlayer/issues/11058
        // So restarting manually
        if (wasPlaying) player.play()
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
            baseColor,
            onFullSpectrumReady
        )
    }

    fun getCurrentPosition() = player.currentPosition

    fun clearPlaylist() {
        player.clearMediaItems()
    }

    fun setAutoPlay(auto: Boolean) {
        "Setting setAutoPlay: $auto".e
        player.playWhenReady = auto
    }

    fun extractMetadata(path: String): MediaMetadata = extractor.extractMetadata(path)

    fun isLastMedia(): Boolean = player.currentMediaItemIndex == player.mediaItemCount - 1
}