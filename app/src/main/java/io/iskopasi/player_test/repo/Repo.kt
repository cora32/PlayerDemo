package io.iskopasi.player_test

import android.content.Context
import android.provider.MediaStore
import io.iskopasi.player_test.Utils.e
import javax.inject.Inject
import javax.inject.Singleton

data class MediaFile(
    val path: String = "",
    val name: String? = "",
    val album: String? = "",
    val artist: String? = "",
) {
    override fun toString(): String {
        return "$path $name $album $artist"
    }
}

@Singleton
class Repo @Inject constructor() {

    fun read(context: Context): List<MediaFile> {
        "--> Reading... ${Thread.currentThread().name}".e

        val files = mutableListOf<MediaFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.ArtistColumns.ARTIST
            ), null, null, null
        )?.use {
            while (it.moveToNext()) {
                val path: String = it.getString(0)
                val name: String = it.getString(1)
                val album: String = it.getString(2)
                val artist: String = it.getString(3)

                val mFile = MediaFile(path, name, album, artist)

                files.add(mFile)

                "->> file: $mFile".e
            }
        }

        return files
    }
}