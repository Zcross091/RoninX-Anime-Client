package com.roninx.anime.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_history")
data class MangaHistoryEntity(
    @PrimaryKey val mangaId: Int,
    val title: String,
    val imageUrl: String?,
    val lastChapterRead: Int,
    val lastPageRead: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)
