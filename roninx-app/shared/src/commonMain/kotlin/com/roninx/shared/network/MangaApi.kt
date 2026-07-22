package com.roninx.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MangaSearchResult(val results: List<MangaResult>)

@Serializable
data class MangaResult(val id: String, val title: String)

@Serializable
data class MangaInfo(val chapters: List<MangaChapter>)

@Serializable
data class MangaChapter(val id: String, val title: String, val scanlator: String? = null)

@Serializable
data class MangaPage(val page: Int, val img: String)

class MangaApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val proxyBaseUrl = "https://ronin-api-proxy.vercel.app/manga"

    suspend fun searchManga(title: String): String? {
        val response: MangaSearchResult = client.get("$proxyBaseUrl/mangadex/${title}").body()
        return response.results.firstOrNull()?.id
    }

    suspend fun getMangaInfo(mangaId: String, provider: String = "mangadex"): MangaInfo {
        return client.get("$proxyBaseUrl/${provider.toLowerCase()}/info/$mangaId").body()
    }

    suspend fun getChapterPages(chapterId: String): List<MangaPage> {
        return client.get("$proxyBaseUrl/mangadex/read/$chapterId").body()
    }
}
