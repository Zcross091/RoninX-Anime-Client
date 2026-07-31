package com.roninx.anime.data.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun query(@Body request: AniListRequest): AniListResponse
}

data class AniListRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

data class AniListResponse(
    val data: AniListData
)

data class AniListData(
    val Page: AniListPage? = null,
    val Media: AniListMedia? = null
)

data class AniListPage(
    val media: List<AniListMedia>? = null
)

data class AniListMedia(
    val id: Int,
    val idMal: Int?,
    val title: AniListTitle?,
    val coverImage: AniListCoverImage?,
    val bannerImage: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val averageScore: Int?,
    val description: String?,
    val genres: List<String>? = null,
    val status: String? = null
)

data class AniListTitle(
    val english: String?,
    val romaji: String?
)

data class AniListCoverImage(
    val large: String?
)

object AniListQueries {
    val GET_BANNERS = """
        query (${'$'}ids: [Int]) {
          Page(page: 1, perPage: 25) {
            media(idMal_in: ${'$'}ids, type: ANIME) {
              idMal
              bannerImage
            }
          }
        }
    """.trimIndent()

    val GET_TRENDING_MANGA = """
        query {
          Page(page: 1, perPage: 15) {
            media(type: MANGA, sort: TRENDING_DESC) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              bannerImage
              chapters
              volumes
              averageScore
              description
              genres
              status
            }
          }
        }
    """.trimIndent()

    val GET_POPULAR_MANGA = """
        query {
          Page(page: 1, perPage: 15) {
            media(type: MANGA, sort: POPULARITY_DESC) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              bannerImage
              chapters
              volumes
              averageScore
              description
              genres
              status
            }
          }
        }
    """.trimIndent()

    val GET_MANGA_DETAILS = """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: MANGA) {
            id
            idMal
            title { english romaji }
            coverImage { large }
            bannerImage
            chapters
            volumes
            averageScore
            description
            genres
            status
          }
        }
    """.trimIndent()

    val GET_SCHEDULE = """
        query (${'$'}status: MediaStatus, ${'$'}format: MediaFormat) {
          Page(page: 1, perPage: 24) {
            media(status: ${'$'}status, format: ${'$'}format, type: ANIME, sort: POPULARITY_DESC) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              episodes
              averageScore
              description
            }
          }
        }
    """.trimIndent()

    val GET_TRENDING_ANIME = """
        query {
          Page(page: 1, perPage: 24) {
            media(type: ANIME, sort: TRENDING_DESC) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              bannerImage
              episodes
              averageScore
              description
              synopsis: description
            }
          }
        }
    """.trimIndent()

    val GET_ANIME_BY_GENRE = """
        query (${'$'}genre: String) {
          Page(page: 1, perPage: 24) {
            media(genre: ${'$'}genre, type: ANIME, sort: POPULARITY_DESC) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              bannerImage
              episodes
              averageScore
              description
            }
          }
        }
    """.trimIndent()

    val SEARCH_ANIME = """
        query (${'$'}search: String) {
          Page(page: 1, perPage: 24) {
            media(search: ${'$'}search, type: ANIME) {
              id
              idMal
              title { english romaji }
              coverImage { large }
              bannerImage
              episodes
              averageScore
              description
            }
          }
        }
    """.trimIndent()

    val GET_ANIME_DETAILS = """
        query (${'$'}id: Int) {
          Media(idMal: ${'$'}id, type: ANIME) {
            id
            idMal
            title { english romaji }
            coverImage { large }
            bannerImage
            episodes
            averageScore
            description
          }
        }
    """.trimIndent()
}
