package com.roninx.anime.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Shikimori REST API — https://shikimori.one/api/doc
 * Base URL: https://shikimori.one/api/
 * Requires User-Agent header (set in OkHttp interceptor).
 * No auth required for read access.
 * Image URLs are relative — prefix with https://shikimori.one
 */
interface ShikimoriApi {
    @GET("animes")
    suspend fun searchAnime(
        @Query("search") query: String,
        @Query("limit") limit: Int = 15
    ): List<ShikimoriAnime>

    @GET("animes")
    suspend fun getTrending(
        @Query("status") status: String = "ongoing",
        @Query("order") order: String = "popularity",
        @Query("limit") limit: Int = 15
    ): List<ShikimoriAnime>

    @GET("animes")
    suspend fun getByGenre(
        @Query("genre") genreId: String,
        @Query("order") order: String = "popularity",
        @Query("limit") limit: Int = 15
    ): List<ShikimoriAnime>

    @GET("animes/{id}")
    suspend fun getAnimeById(
        @Path("id") id: Int
    ): ShikimoriAnime
}

// --- Data Classes ---

data class ShikimoriAnime(
    val id: Int,
    val name: String,
    val russian: String?,
    val image: ShikimoriImage?,
    val score: String?,
    val status: String?,
    val episodes: Int?,
    val episodes_aired: Int?,
    val kind: String?,
    val description: String? = null
)

data class ShikimoriImage(
    val original: String?,
    val preview: String?,
    val x96: String?,
    val x48: String?
)
