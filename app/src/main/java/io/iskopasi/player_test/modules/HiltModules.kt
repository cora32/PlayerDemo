package io.iskopasi.player_test.modules

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.converter.JsoupConverterFactory
import io.iskopasi.player_test.repo.HtmlApi
import io.iskopasi.player_test.repo.JsonApi
import io.iskopasi.player_test.room.MediaDB
import io.iskopasi.player_test.room.MediaDao
import io.iskopasi.player_test.utils.getClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class HiltModules {
    @Provides
    @Singleton
    fun getRepo(): Repo = Repo(getApiJson())

    @Provides
    @Singleton
    fun getDB(
        @ApplicationContext context: Context
    ): MediaDB = Room
        .databaseBuilder(context, MediaDB::class.java, "media_db")
        .fallbackToDestructiveMigration()
        .build()


    @Provides
    @Singleton
    fun getDao(db: MediaDB): MediaDao = db.dao()

    private var gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create()

    @Provides
    @Singleton
    fun getRetrofitWithGson(): Retrofit = Retrofit.Builder()
        .client(getClient())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl("https://lyrics.lyricfind.com/").build()

    @Provides
    @Singleton
    fun getRetrofitWithJSoup(): Retrofit = Retrofit.Builder()
        .client(getClient())
        .addConverterFactory(JsoupConverterFactory)
        .baseUrl("https://lyrics.lyricfind.com/").build()

    @Provides
    @Singleton
    fun getApiJson(): JsonApi = getRetrofitWithGson().create(JsonApi::class.java)

    @Provides
    @Singleton
    fun getApiHtml(): HtmlApi = getRetrofitWithJSoup().create(HtmlApi::class.java)
}