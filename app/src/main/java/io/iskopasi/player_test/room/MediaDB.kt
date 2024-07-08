package io.iskopasi.player_test.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaDataEntity::class], version = 1)
abstract class MediaDB : RoomDatabase() {
    abstract fun dao(): MediaDao
}