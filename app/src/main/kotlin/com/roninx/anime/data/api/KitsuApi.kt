package com.roninx.anime.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Kitsu JSON:API — https://kitsu.docs.apiary.io
 * Base URL: https://kitsu.app/api/edge/
 * No auth required for read access.
 */
interface KitsuApi {
    @GET("anime")
    suspend fun searchAnime(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 15
    ): KitsuResponse

    @GET("anime")
    suspend fun getTrending(
        @Query("filter[status]") status: String = "current",
        @Query("sort") sort: String = "-userCount",
        @Query("page[limit]") limit: Int = 15
    ): KitsuResponse

    @GET("anime")
    suspend fun getByCategory(
        @Query("filter[categories]") category: String,
        @Query("sort") sort: String = "-userCount",
        @Query("page[limit]") limit: Int = 15
    ): KitsuResponse

    @GET("anime/{id}")
    suspend fun getAnimeById(
        @Path("id") id: String
    ): KitsuSingleResponse
}

// --- Data Classes (JSON:API envelope) ---

data class KitsuResponse(
    val data: List<KitsuAnime>
)

data class KitsuSingleResponse(
    val data: KitsuAnime
)

data class KitsuAnime(
    val id: String,
    val attributes: KitsuAttributes?
)

data class KitsuAttributes(
    val canonicalTitle: String?,
    val titles: KitsuTitles?,
    val synopsis: String?,
    val averageRating: String?,
    val episodeCount: Int?,
    val posterImage: KitsuPosterImage?,
    val coverImage: KitsuCoverImage?,
    val status: String?
)

data class KitsuTitles(
    val en: String?,
    val en_jp: String?,
    val ja_jp: String?
)

data class KitsuPosterImage(
    val large: String?,
    val medium: String?,
    val original: String?
)

data class KitsuCoverImage(
    val large: String?,
    val original: String?
)
