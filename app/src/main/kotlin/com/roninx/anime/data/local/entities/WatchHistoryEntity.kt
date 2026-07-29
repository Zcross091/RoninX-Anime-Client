package com.roninx.anime.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String,
    val lastEpisodeWatched: Int,
    val progressMs: Long,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
