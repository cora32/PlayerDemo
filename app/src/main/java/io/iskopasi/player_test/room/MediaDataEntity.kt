package io.iskopasi.player_test.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MediaDataEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = -1,
    @ColumnInfo(index = true, name = "media_id") val mediaId: Int = -1,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean
)

@Entity
data class CachedTextEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = -1,
    @ColumnInfo(index = true, name = "name") val name: String,
    @ColumnInfo(index = false, name = "text") val text: String,
)