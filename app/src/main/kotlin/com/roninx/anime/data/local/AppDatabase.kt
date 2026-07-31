package com.roninx.anime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.roninx.anime.data.local.dao.AnimeDao
import com.roninx.anime.data.local.entities.MangaHistoryEntity
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.local.entities.WatchlistEntity

@Database(
    entities = [WatchHistoryEntity::class, WatchlistEntity::class, MangaHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
}
