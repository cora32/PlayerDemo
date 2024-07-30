package io.iskopasi.player_test.room

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [MediaDataEntity::class, CachedTextEntity::class],
    version = 3,
//    autoMigrations = [
//        MIGRATION_1_2
//    ]
)
abstract class MediaDB : RoomDatabase() {
    abstract fun dao(): MediaDao
    abstract fun cachedTextDao(): CachedTextDao

//    @Add(fromColumnName = "User", toColumnName = "AppUser")
//    class MyAutoMigration : AutoMigrationSpec
}

//val MIGRATION_1_2: Migration = object : Migration(1, 2) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        database.execSQL("CREATE TABLE `Fruit` (`id` INTEGER, `name` TEXT, PRIMARY KEY(`id`))")
//    }
//}