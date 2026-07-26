package com.roninx.anime.data.repository

import com.roninx.anime.data.api.AniListApi
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.AniListQueries
import com.roninx.anime.data.api.AniListRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepository @Inject constructor(
    private val aniListApi: AniListApi
) {
    suspend fun getBanners(malIds: List<Int>): Map<Int, String?> {
        return try {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_BANNERS,
                    variables = mapOf("ids" to malIds)
                )
            )
            response.data.Page?.media?.associate { (it.idMal ?: 0) to it.bannerImage } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getTrendingManga(): List<AniListMedia> {
        return try {
            val response = aniListApi.query(AniListRequest(query = AniListQueries.GET_TRENDING_MANGA))
            response.data.Page?.media ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularManga(): List<AniListMedia> {
        return try {
            val response = aniListApi.query(AniListRequest(query = AniListQueries.GET_POPULAR_MANGA))
            response.data.Page?.media ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAnimeBySchedule(status: String? = null, format: String? = null): List<AniListMedia> {
        return try {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_SCHEDULE,
                    variables = mutableMapOf<String, Any?>().apply {
                        if (status != null) put("status", status)
                        if (format != null) put("format", format)
                    }
                )
            )
            response.data.Page?.media ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
