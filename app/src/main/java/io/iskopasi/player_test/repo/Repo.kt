package io.iskopasi.player_test

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import io.iskopasi.player_test.adapters.RecommendItemData
import io.iskopasi.player_test.models.MediaData
import io.iskopasi.player_test.repo.JsonApi
import io.iskopasi.player_test.repo.getJSoupDocument
import io.iskopasi.player_test.room.CachedTextDao
import io.iskopasi.player_test.room.CachedTextEntity
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.utils.LoopIterator
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.e
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LyricsStates(var text: String? = null) {
    LYR_OK,
    LYR_NOT_FOUND,
    LYR_NOT_AVAIL,
    LYR_ERROR,
}

@Singleton
class Repo @Inject constructor(
    private val jsonApi: JsonApi,
    private val dao: MediaDao,
    private val cachedDao: CachedTextDao,
) {
    lateinit var iter: LoopIterator<MediaData>
    var idToIndex: Map<Int, Int> = mutableMapOf<Int, Int>()
    val currentData by lazy { MutableLiveData(MediaData.empty) }
    val dataList
        get() = iter.dataList
    val recommendTracks = mutableListOf<RecommendItemData>()
    val recommendAlbums = mutableListOf<RecommendItemData>()

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

    private val images = listOf(
        R.drawable.none,
        R.drawable.i2,
        R.drawable.i3,
        R.drawable.i4,
        R.drawable.i5,
        R.drawable.i6,
        R.drawable.i7,
        R.drawable.i8,
        R.drawable.i9,
        R.drawable.i10,
    )

    private val textForRecommendations =
        "warning: Unable to read Kotlin metadata due to unsupported metadata kind null".split(" ")
    private val text2ForRecommendations =
        "generateDebugAssets packageDebugResources parseDebugLocalResources checkDebugAarMetadata mapDebugSourceSetPaths mergeDebugAssets mergeDebugResources dataBindingMergeDependencyArtifactsDebug compressDebugAssets checkDebugDuplicateClasses mergeLibDexDebug configureCMakeDebug[arm64-v8a] processDebugMainManifest processDebugManifest processDebugManifestForPackage dataBindingGenBaseClassesDebug mergeExtDexDebug processDebugResources buildCMakeDebug[arm64-v8a] mergeDebugNativeLibs stripDebugDebugSymbols kspDebugKotlin kaptGenerateStubsDebugKotlin kaptDebugKotlin compileDebugKotlin compileDebugJavaWithJavac hiltAggregateDepsDebug hiltJavaCompileDebug processDebugJavaRes transformDebugClassesWithAsm mergeDebugJavaResource dexBuilderDebug mergeProjectDexDebug packageDebug assembleDebug createDebugApkListingFileRedirect UP-TO-DATE Task".split(
            " "
        )

    fun read(context: Context): List<MediaData> {
        "--> Reading... ${Thread.currentThread().name}".e

        val dataList = mutableListOf<MediaData>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) projectionR
            else projectionGeneral,
            null, null, null
        )?.use {
            var id = 0
            try {
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

                    val text = "for_test"
                    val text2 = "Beach"
                    if (path.contains(text) || name.contains(text) || album.contains(text)
                        || path.contains(text2) || name.contains(text2) || album.contains(text2)
                    ) {
                        val file = MediaData(
                            id,
                            images[id % 10],
                            name,
                            artist,
                            duration.toInt(),
                            false,
                            path,
                            genre = genre ?: "",
                            composer = composer ?: "",
                            author = author ?: "",
                            title = title ?: "",
                            writer = writer ?: "",
                            albumArtist = albumArtist ?: "",
                            compilation = compilation ?: "",
                        )
                        dataList.add(file)

                        id++
//                    "->> file: $mFile".e
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        dataList.sortBy { it.title }

        // Updating favorite variable
        val ids = dataList.map { it.id }
        idToIndex = dataList.mapIndexed { index, item -> item.id to index }.toMap()
        for (item in dao.getIsFavourite(ids)) {
            dataList[idToIndex[item.mediaId]!!].isFavorite = item.isFavorite
        }

        iter = LoopIterator(dataList)

        generateRecommendations()

        return dataList
    }

    private fun generateRecommendations() {
        repeat(10) {
            val text1 =
                "${textForRecommendations.random().capital()} ${textForRecommendations.random()}"
            val text2 =
                "${textForRecommendations.random().capital()} ${text2ForRecommendations.random()}"
            val text3 =
                "${text2ForRecommendations.random().capital()} ${text2ForRecommendations.random()}"

            recommendTracks.add(RecommendItemData(text1, "", images.random()))
            recommendAlbums.add(RecommendItemData(text2, text3, images.random()))
        }
    }

    suspend fun getLyrics(name: String): LyricsStates {
        "[LYRICS] Trying to get lyrics for track: $name".e

        val cachedLyrics = cachedDao.getLyrics(name)?.text
        return if (cachedLyrics != null)
            LyricsStates.LYR_OK.apply {
                text = cachedLyrics
            }
        else
            getLyricsFromInet(name)
    }

    private suspend fun getLyricsFromInet(name: String): LyricsStates {
        jsonApi.getTracks(name)?.let { data ->
            "[LYRICS] Search request [OK]".e

            if (data.tracks.isEmpty()) return LyricsStates.LYR_NOT_FOUND

            data.tracks.first().slug?.let { slug ->
                "[LYRICS] Slug: $slug".e

                getJSoupDocument(slug)?.let { doc ->
                    "[LYRICS] Received jsoup document!".e

                    val html = doc.html()

                    if (html.indexOf("LYRIC NOT AVAILABLE") != -1) return LyricsStates.LYR_NOT_AVAIL

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

                        return LyricsStates.LYR_OK.apply {
                            text = lyrics
                        }
                    } else {
                        "[LYRICS] Cannot find indexes! $indexStart $indexEnd".e
                    }
                } ?: "[LYRICS] JSoup document [FAIL]".e
            } ?: "[LYRICS] Slug [FAIL]".e
        } ?: "[LYRICS] Search request [FAIL]".e

        return LyricsStates.LYR_NOT_FOUND
    }

    fun cacheText(name: String, lyricsText: String) = bg {
        "--> Caching lyrics for $name...".e

        cachedDao.cacheLyrics(CachedTextEntity(name = name, text = lyricsText))
    }

    fun getItemByIndex(id: Int): MediaData = iter.dataList[idToIndex[id]!!]
}

private fun String.capital(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
