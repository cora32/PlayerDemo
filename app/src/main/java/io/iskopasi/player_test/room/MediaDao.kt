package io.iskopasi.player_test.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface MediaDao {
    @Query("SELECT * FROM mediadataentity WHERE media_id IN (:id)")
    fun getIsFavourite(id: List<Int>): List<MediaDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(mediaData: MediaDataEntity)

    @Query("SELECT * FROM mediadataentity")
    fun getAll(): List<MediaDataEntity>

    @Query("DELETE FROM mediadataentity WHERE media_id IN (:id)")
    fun remove(id: Int)
}

@Dao
interface CachedTextDao {
    @Query("SELECT * FROM cachedtextentity WHERE name IN (:name)")
    fun getLyrics(name: String): CachedTextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun cacheLyrics(item: CachedTextEntity)
}