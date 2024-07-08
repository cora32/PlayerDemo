package io.iskopasi.player_test.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.iskopasi.player_test.Repo
import io.iskopasi.player_test.room.MediaDB
import io.iskopasi.player_test.room.MediaDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltModules {
    @Provides
    @Singleton
    fun getRepo(): Repo = Repo()

    @Provides
    @Singleton
    fun getDB(
        @ApplicationContext context: Context
    ): MediaDB = Room.databaseBuilder(context, MediaDB::class.java, "media_db").build()


    @Provides
    @Singleton
    fun getDao(db: MediaDB): MediaDao = db.dao()
}