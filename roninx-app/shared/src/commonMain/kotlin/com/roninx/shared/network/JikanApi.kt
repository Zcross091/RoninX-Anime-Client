package com.roninx.shared.network

import com.roninx.shared.models.Anime
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class JikanResponse(val data: List<JikanAnime>)

@Serializable
data class JikanAnime(
    val title: String,
    val title_english: String? = null,
    val title_synonyms: List<String> = emptyList(),
    val images: JikanImages,
    val episodes: Int? = null,
    val score: Double? = null,
    val synopsis: String? = null
)

@Serializable
data class JikanImages(val jpg: JikanJpg)

@Serializable
data class JikanJpg(val large_image_url: String)

class JikanApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getTrending(): List<Anime> {
        val response: JikanResponse = client.get("https://api.jikan.moe/v4/seasons/now?limit=15").body()
        return response.data.map { it.toAnime() }
    }

    private fun JikanAnime.toAnime() = Anime(
        title = title_english ?: title,
        originalTitle = title,
        synonyms = title_synonyms,
        image = images.jpg.large_image_url,
        epCount = episodes,
        score = score?.toString() ?: "N/A",
        synopsis = synopsis ?: "No synopsis available."
    )
}
