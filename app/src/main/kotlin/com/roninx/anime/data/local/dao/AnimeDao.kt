package com.roninx.anime.data.local.dao

import androidx.room.*
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.local.entities.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE malId = :malId")
    suspend fun deleteHistory(malId: Int)

    // Watchlist
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE malId = :malId")
    suspend fun removeFromWatchlist(malId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE malId = :malId)")
    fun isInWatchlist(malId: Int): Flow<Boolean>

    // Manga History
    @Query("SELECT * FROM manga_history ORDER BY updatedAt DESC")
    fun getAllMangaHistory(): Flow<List<com.roninx.anime.data.local.entities.MangaHistoryEntity>>

    @Query("SELECT * FROM manga_history WHERE mangaId = :mangaId LIMIT 1")
    fun getMangaHistoryItem(mangaId: Int): Flow<com.roninx.anime.data.local.entities.MangaHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMangaHistory(history: com.roninx.anime.data.local.entities.MangaHistoryEntity)

    @Query("DELETE FROM manga_history WHERE mangaId = :mangaId")
    suspend fun deleteMangaHistory(mangaId: Int)
}
