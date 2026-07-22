package com.roninx.shared.network

import com.roninx.shared.models.Anime
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GraphQLRequest(val query: String, val variables: Map<String, String> = emptyMap())

@Serializable
data class AniListResponse(val data: AniListData)

@Serializable
data class AniListData(val Page: AniListPage? = null, val Media: AniListMedia? = null)

@Serializable
data class AniListPage(val media: List<AniListMedia>)

@Serializable
data class AniListMedia(
    val title: AniListTitle,
    val coverImage: AniListCoverImage,
    val bannerImage: String? = null,
    val episodes: Int? = null,
    val averageScore: Int? = null,
    val description: String? = null
)

@Serializable
data class AniListTitle(val english: String? = null, val romaji: String? = null)

@Serializable
data class AniListCoverImage(val large: String)

class AniListApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun searchAnime(term: String): List<Anime> {
        val query = """
            query (${'$'}search: String) {
                Page (page: 1, perPage: 15) {
                    media (search: ${'$'}search, type: ANIME, sort: POPULARITY_DESC) {
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

        val response: AniListResponse = client.post("https://graphql.anilist.co") {
            contentType(ContentType.Application.Json)
            setBody(GraphQLRequest(query, mapOf("search" to term)))
        }.body()

        return response.data.Page?.media?.map { it.toAnime() } ?: emptyList()
    }

    private fun AniListMedia.toAnime() = Anime(
        title = title.english ?: title.romaji ?: "Unknown",
        originalTitle = title.romaji,
        image = coverImage.large,
        banner = bannerImage,
        epCount = episodes,
        score = averageScore?.let { (it / 10.0).toString() } ?: "N/A",
        synopsis = description?.replace(Regex("<[^>]*>"), "") ?: ""
    )
}
