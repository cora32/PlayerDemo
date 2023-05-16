package io.iskopasi.player_test

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import io.iskopasi.player_test.Utils.e
import io.iskopasi.player_test.Utils.toBitmap
import javax.inject.Inject
import javax.inject.Singleton

data class MediaFile(
    val albumId: Int = -1,
    val path: String = "",
    val name: String? = "",
    val album: String? = "",
    val artist: String? = "",
) {
    override fun toString(): String {
        return "$albumId $path $name $album $artist"
    }
}

@Singleton
class Repo @Inject constructor() {

    fun getImage(path: String): Bitmap? = MediaMetadataRetriever().run {
        setDataSource(path)

        embeddedPicture?.toBitmap()
    }

    fun read(context: Context): List<MediaFile> {
        "--> Reading... ${Thread.currentThread().name}".e

        val files = mutableListOf<MediaFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.ArtistColumns.ARTIST,
            ), null, null, null
        )?.use {
            while (it.moveToNext()) {
                val albumId: Int = it.getInt(0)
                val path: String = it.getString(1)
                val name: String = it.getString(2)
                val album: String = it.getString(3)
                val artist: String = it.getString(4)

                val mFile = MediaFile(albumId, path, name, album, artist)

                files.add(mFile)

                "->> file: $mFile".e
            }
        }

        return files
    }
}