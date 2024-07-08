package io.iskopasi.player_test.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface MediaDao {
    @Query("SELECT * FROM mediadataentity WHERE uid IN (:id)")
    fun getIsFavourite(id: List<Int>): List<MediaDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(mediaData: MediaDataEntity)

}