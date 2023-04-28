package io.iskopasi.player_test

import android.content.Context
import android.provider.MediaStore
import io.iskopasi.player_test.Utils.e

data class MediaFile(
    val path: String,
    val album: String?,
    val artist: String?,
) {
    override fun toString(): String {
        return "$path $album $artist"
    }
}

class PlayerController {
    private val files = mutableListOf<MediaFile>()

    fun read(context: Context) {
        "--> Reading...".e

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.ArtistColumns.ARTIST
            ), null, null, null
        )?.apply {

            while (moveToNext()) {
                val path: String = getString(0)
                val album: String = getString(1)
                val artist: String = getString(2)

                val mFile = MediaFile(path, album, artist)

                files.add(mFile)

                "->> file: $mFile".e
            }

            close()
        }
    }
}