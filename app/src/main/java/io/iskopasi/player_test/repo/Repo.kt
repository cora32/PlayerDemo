package io.iskopasi.player_test

import android.content.Context
import android.provider.MediaStore
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.utils.Utils.e
import javax.inject.Inject
import javax.inject.Singleton

data class MediaFile(
    val id: Int = -1,
    val albumId: Int = -1,
    val path: String = "",
    val name: String = "",
    val album: String = "",
    val artist: String = "",
    val duration: Long = 0L,
) {
    override fun toString(): String {
        return "$id $albumId $path $name $album $artist $duration"
    }
}

@Singleton
class Repo @Inject constructor() {
    val currentData = MediaData.empty

    fun read(context: Context): List<MediaFile> {
        "--> Reading... ${Thread.currentThread().name}".e

        val files = mutableListOf<MediaFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
            ), null, null, null
        )?.use {
            var id = 0
            while (it.moveToNext()) {
                val albumId: Int = it.getInt(0)
                val path: String = it.getString(1)
                val name: String = it.getString(2)
                val album: String = it.getString(3)
                val artist: String = it.getString(4)
                val duration: Long = it.getLong(5)

                val mFile = MediaFile(id++, albumId, path, name, album, artist, duration)

                val text = "hui"
                val text2 = "Beach"
                if (path.contains(text) || name.contains(text) || album.contains(text)
                    || path.contains(text2) || name.contains(text2) || album.contains(text2)
                ) {
                files.add(mFile)
//                    "->> file: $mFile".e
                }

            }
        }

        return files
    }
}