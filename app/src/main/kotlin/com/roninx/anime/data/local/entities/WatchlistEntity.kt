package com.roninx.anime.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String,
    val score: String,
    val episodes: Int?,
    val addedAt: Long = System.currentTimeMillis()
)
