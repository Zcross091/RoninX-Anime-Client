package com.roninx.anime.data.repository

import com.roninx.anime.data.api.AniListApi
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.AniListQueries
import com.roninx.anime.data.api.AniListRequest
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepository @Inject constructor(
    private val aniListApi: AniListApi
) {
    suspend fun getBanners(malIds: List<Int>): Resource<Map<Int, String?>> {
        return safeApiCall {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_BANNERS,
                    variables = mapOf("ids" to malIds)
                )
            )
            response.data.Page?.media?.associate { (it.idMal ?: 0) to it.bannerImage } ?: emptyMap()
        }
    }

    suspend fun getTrendingManga(): Resource<List<AniListMedia>> {
        return safeApiCall {
            val response = aniListApi.query(AniListRequest(query = AniListQueries.GET_TRENDING_MANGA))
            response.data.Page?.media ?: emptyList()
        }
    }

    suspend fun getPopularManga(): Resource<List<AniListMedia>> {
        return safeApiCall {
            val response = aniListApi.query(AniListRequest(query = AniListQueries.GET_POPULAR_MANGA))
            response.data.Page?.media ?: emptyList()
        }
    }

    suspend fun getAnimeBySchedule(status: String? = null, format: String? = null): Resource<List<AniListMedia>> {
        return safeApiCall {
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
        }
    }

    suspend fun getTrendingAnime(): Resource<List<AniListMedia>> {
        return safeApiCall {
            val response = aniListApi.query(AniListRequest(query = AniListQueries.GET_TRENDING_ANIME))
            response.data.Page?.media ?: emptyList()
        }
    }

    suspend fun getAnimeByGenre(genre: String): Resource<List<AniListMedia>> {
        return safeApiCall {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_ANIME_BY_GENRE,
                    variables = mapOf("genre" to genre)
                )
            )
            response.data.Page?.media ?: emptyList()
        }
    }

    suspend fun searchAnime(query: String): Resource<List<AniListMedia>> {
        return safeApiCall {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.SEARCH_ANIME,
                    variables = mapOf("search" to query)
                )
            )
            response.data.Page?.media ?: emptyList()
        }
    }

    suspend fun getAnimeDetails(idMal: Int): Resource<AniListMedia> {
        return safeApiCall {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_ANIME_DETAILS,
                    variables = mapOf("id" to idMal)
                )
            )
            response.data.Media ?: throw Exception("Anime details not found in AniList")
        }
    }

    suspend fun getMangaDetails(id: Int): Resource<AniListMedia> {
        return safeApiCall {
            val response = aniListApi.query(
                AniListRequest(
                    query = AniListQueries.GET_MANGA_DETAILS,
                    variables = mapOf("id" to id)
                )
            )
            response.data.Media ?: throw Exception("Manga details not found in AniList")
        }
    }
}
