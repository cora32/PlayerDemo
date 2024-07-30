package io.iskopasi.player_test

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.repo.JsonApi
import io.iskopasi.player_test.repo.getJSoupDocument
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
    var genre: String = "",
    var composer: String = "",
    var author: String = "",
    var title: String = "",
    var writer: String = "",
    var albumArtist: String = "",
    var compilation: String = "",
) {
    override fun toString(): String {
        return "$id $albumId $path $name $album $artist $duration"
    }
}

@Singleton
class Repo @Inject constructor(
    private val jsonApi: JsonApi,
) {
    val currentData by lazy { MutableLiveData(MediaData.empty) }

    private val projectionGeneral = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.COMPOSER,
        MediaStore.Audio.Media.AUTHOR,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.WRITER,
        MediaStore.Audio.Media.ALBUM_ARTIST,
        MediaStore.Audio.Media.COMPILATION,
    )

    @RequiresApi(Build.VERSION_CODES.R)
    private val projectionR = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.COMPOSER,
        MediaStore.Audio.Media.AUTHOR,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.WRITER,
        MediaStore.Audio.Media.ALBUM_ARTIST,
        MediaStore.Audio.Media.COMPILATION,
        MediaStore.Audio.Media.GENRE,
    )

    fun read(context: Context): List<MediaFile> {
        "--> Reading... ${Thread.currentThread().name}".e

        val files = mutableListOf<MediaFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) projectionR
            else projectionGeneral,
            null, null, null
        )?.use {
            var id = 0
            while (it.moveToNext()) {
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Albums._ID)
                val pathIdCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val nameIdCol = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val durationCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val composerCol = it.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }
                val authorCol = it.getColumnIndex(MediaStore.Audio.Media.AUTHOR)
                val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val writerCol = it.getColumnIndex(MediaStore.Audio.Media.WRITER)
                val albumArtistCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val compilationCol = it.getColumnIndex(MediaStore.Audio.Media.COMPILATION)

                val albumId: Int = it.getInt(albumIdCol)
                val path: String = it.getString(pathIdCol)
                val name: String = it.getString(nameIdCol)
                val album: String = it.getString(albumCol)
                val artist: String = it.getString(artistCol)
                val duration: Long = it.getLong(durationCol)
                val genre: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    it.getString(genreCol)
                else "(Unknown genre)"
                val composer: String? = it.getString(composerCol)
                val author: String? = it.getString(authorCol)
                val title: String? = it.getString(titleCol)
                val writer: String? = it.getString(writerCol)
                val albumArtist: String? = it.getString(albumArtistCol)
                val compilation: String? = it.getString(compilationCol)

                val mFile = MediaFile(
                    id++,
                    albumId,
                    path,
                    name,
                    album,
                    artist,
                    duration,
                    genre ?: "",
                    composer ?: "",
                    author ?: "",
                    title ?: "",
                    writer ?: "",
                    albumArtist ?: "",
                    compilation ?: "",
                )

                val text = "data"
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

    suspend fun getLyrics(name: String): String? {
        "[LYRICS] Trying to get lyrics for track: $name".e

        jsonApi.getTracks(name)?.let { data ->
            "[LYRICS] Search request [OK]".e

            data.tracks.first().slug?.let { slug ->
                "[LYRICS] Slug: $slug".e

                getJSoupDocument(slug)?.let { doc ->
                    "[LYRICS] Received jsoup document!".e

                    val html = doc.html()
                    val firstAnchor = "\"lyrics\":\""
                    val indexStart = html.indexOf(firstAnchor)
                    val indexEnd =
                        html.indexOf(
                            "\"",
                            startIndex = indexStart + firstAnchor.lastIndex + 1
                        )

                    if (indexStart >= 0 && indexEnd >= 0) {
                        val lyrics =
                            html.substring(indexStart + firstAnchor.lastIndex + 1..<indexEnd)
                        "-> lyrics: $indexStart $indexEnd $lyrics".e

                        return lyrics
                    } else {
                        "[LYRICS] Cannot find indexes! $indexStart $indexEnd".e
                    }
                } ?: "[LYRICS] JSoup document [FAIL]".e
            } ?: "[LYRICS] Slug [FAIL]".e
        } ?: "[LYRICS] Search request [FAIL]".e

        return null
    }
}